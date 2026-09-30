package com.reddy.vittify.data.sync.transport

import android.util.Log
import com.google.gson.Gson
import com.reddy.vittify.data.sync.security.P2pCryptoEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * High-reliability MQTT over TLS signaling client for WebRTC handshake.
 * Uses public MQTT SSL brokers (broker.hivemq.com and broker.emqx.io on port 8883)
 * which are globally accessible and not blocked by regional ISP firewalls.
 * All messages are end-to-end encrypted with AES-256-GCM before transmission.
 */
@Singleton
class WebRtcSignalingClient @Inject constructor(
    private val cryptoEngine: P2pCryptoEngine
) {
    companion object {
        private const val TAG = "WebRtcSignaling"
        private val BROKER_HOSTS = listOf(
            "broker.hivemq.com",
            "broker.emqx.io",
            "test.mosquitto.org"
        )
        private const val SSL_PORT = 8883
        private const val PLAIN_PORT = 1883
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null
    private var pingJob: Job? = null

    private var activeSocket: Socket? = null
    private var activeOutputStream: OutputStream? = null
    private val sendLock = Any()

    private var currentClusterId: String? = null
    private var currentDeviceId: String? = null
    private var currentSecretKey: SecretKey? = null
    private var currentTopic: String? = null

    private val _incomingMessages = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 64)
    fun incomingMessages(): Flow<SignalingMessage> = _incomingMessages.asSharedFlow()

    fun connect(clusterId: String, deviceId: String, secretKey: SecretKey) {
        disconnect()
        currentClusterId = clusterId
        currentDeviceId = deviceId
        currentSecretKey = secretKey
        val sanitizedCluster = clusterId.replace("-", "").take(20)
        currentTopic = "vittify/p2p/$sanitizedCluster"

        connectionJob = scope.launch {
            var attempt = 0
            while (isActive) {
                val host = BROKER_HOSTS[attempt % BROKER_HOSTS.size]
                var connected = false

                // Try TLS first, fallback to plain if needed
                for (port in listOf(SSL_PORT, PLAIN_PORT)) {
                    if (!isActive) break
                    try {
                        Log.d(TAG, "Connecting to MQTT signaling broker $host:$port (attempt $attempt)...")
                        val socket = createConnectedSocket(host, port, timeoutMs = 8000)
                        activeSocket = socket
                        val inStream = socket.getInputStream()
                        val outStream = socket.getOutputStream()
                        activeOutputStream = outStream

                        val clientId = "vittify_${deviceId.take(8)}_${UUID.randomUUID().toString().take(6)}"
                        sendConnectPacket(outStream, clientId)
                        readConnAck(inStream)

                        val topic = currentTopic ?: break
                        sendSubscribePacket(outStream, topic)
                        readSubAck(inStream)

                        Log.d(TAG, "Connected & subscribed to MQTT signaling topic: $topic on $host:$port")
                        connected = true
                        attempt = 0

                        // Start keepalive ping
                        startPingLoop(outStream)

                        // Listen for incoming frames until socket closes
                        readLoop(inStream, clusterId, deviceId, secretKey)
                        break
                    } catch (e: CancellationException) {
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Signaling connection failed on $host:$port: ${e.message}")
                        closeCurrentSocket()
                    }
                }

                if (isActive) {
                    attempt++
                    val backoffMs = (2000L * attempt.coerceAtMost(5))
                    Log.d(TAG, "Retrying signaling connection in ${backoffMs}ms...")
                    delay(backoffMs)
                }
            }
        }
    }

    private fun createConnectedSocket(host: String, port: Int, timeoutMs: Int): Socket {
        return if (port == SSL_PORT) {
            val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(host, port), timeoutMs)
            val sslSocket = sslFactory.createSocket(rawSocket, host, port, true) as SSLSocket
            sslSocket.startHandshake()
            sslSocket
        } else {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket
        }
    }

    private fun startPingLoop(outStream: OutputStream) {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(25_000L)
                try {
                    synchronized(sendLock) {
                        outStream.write(byteArrayOf(0xC0.toByte(), 0x00))
                        outStream.flush()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed sending MQTT PINGREQ, closing socket: ${e.message}")
                    closeCurrentSocket()
                    break
                }
            }
        }
    }

    private fun readLoop(
        inStream: InputStream,
        clusterId: String,
        deviceId: String,
        secretKey: SecretKey
    ) {
        while (scope.isActive) {
            val headerByte = inStream.read()
            if (headerByte == -1) throw EOFException("Socket closed by remote broker")

            val remLength = readRemainingLength(inStream)
            val body = ByteArray(remLength)
            var totalRead = 0
            while (totalRead < remLength) {
                val read = inStream.read(body, totalRead, remLength - totalRead)
                if (read == -1) throw EOFException("Unexpected EOF while reading body")
                totalRead += read
            }

            val packetType = headerByte and 0xF0
            if (packetType == 0x30) { // PUBLISH packet
                try {
                    if (body.size >= 2) {
                        val topicLen = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF)
                        val payloadStart = 2 + topicLen
                        if (payloadStart <= body.size) {
                            val payloadStr = String(body, payloadStart, body.size - payloadStart, Charsets.UTF_8)
                            handleIncomingPayload(payloadStr, clusterId, deviceId, secretKey)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing incoming MQTT payload", e)
                }
            } else if (packetType == 0xD0) {
                // PINGRESP received - keepalive acknowledged
            }
        }
    }

    private fun handleIncomingPayload(
        payloadStr: String,
        currentClusterId: String,
        currentDeviceId: String,
        secretKey: SecretKey
    ) {
        try {
            val decryptedJson = cryptoEngine.decryptString(payloadStr, secretKey)
            val message = gson.fromJson(decryptedJson, SignalingMessage::class.java)

            // Only process messages matching our cluster and not from ourselves
            if (message != null &&
                message.clusterId == currentClusterId &&
                message.fromDeviceId != currentDeviceId &&
                (message.toDeviceId == null || message.toDeviceId == currentDeviceId)
            ) {
                _incomingMessages.tryEmit(message)
            }
        } catch (e: Exception) {
            // Ignored - frame might be from other client or non-matching message
        }
    }

    suspend fun sendMessage(message: SignalingMessage, secretKey: SecretKey): Boolean {
        val topic = currentTopic ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(message)
                val encryptedPayload = cryptoEngine.encryptString(json, secretKey)
                val payloadBytes = encryptedPayload.toByteArray(Charsets.UTF_8)

                val outStream = activeOutputStream ?: return@withContext false
                synchronized(sendLock) {
                    sendPublishPacket(outStream, topic, payloadBytes)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending MQTT signaling message", e)
                closeCurrentSocket()
                false
            }
        }
    }

    private fun sendConnectPacket(out: OutputStream, clientId: String) {
        val clientBytes = clientId.toByteArray(Charsets.UTF_8)
        val varHeader = byteArrayOf(
            0x00, 0x04, 'M'.code.toByte(), 'Q'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(),
            0x04, // Protocol Level MQTT 3.1.1
            0x02, // Clean Session flag
            0x00, 0x3C // Keepalive 60s
        )
        val payload = ByteArrayOutputStream()
        payload.write((clientBytes.size shr 8) and 0xFF)
        payload.write(clientBytes.size and 0xFF)
        payload.write(clientBytes)

        val rem = varHeader + payload.toByteArray()
        val packet = byteArrayOf(0x10) + encodeRemainingLength(rem.size) + rem

        synchronized(sendLock) {
            out.write(packet)
            out.flush()
        }
    }

    private fun readConnAck(inStream: InputStream) {
        val type = inStream.read()
        if (type != 0x20) throw IllegalStateException("Expected CONNACK (0x20), got: $type")
        val remLen = readRemainingLength(inStream)
        val ackBytes = ByteArray(remLen)
        var read = 0
        while (read < remLen) {
            val r = inStream.read(ackBytes, read, remLen - read)
            if (r == -1) throw EOFException()
            read += r
        }
        val returnCode = ackBytes.lastOrNull()?.toInt() ?: -1
        if (returnCode != 0) {
            throw IllegalStateException("MQTT connection rejected with code $returnCode")
        }
    }

    private fun sendSubscribePacket(out: OutputStream, topic: String) {
        val topicBytes = topic.toByteArray(Charsets.UTF_8)
        val packetId = 1
        val body = ByteArrayOutputStream()
        body.write((packetId shr 8) and 0xFF)
        body.write(packetId and 0xFF)
        body.write((topicBytes.size shr 8) and 0xFF)
        body.write(topicBytes.size and 0xFF)
        body.write(topicBytes)
        body.write(0x00) // Requested QoS 0

        val rem = body.toByteArray()
        val packet = byteArrayOf(0x82.toByte()) + encodeRemainingLength(rem.size) + rem

        synchronized(sendLock) {
            out.write(packet)
            out.flush()
        }
    }

    private fun readSubAck(inStream: InputStream) {
        val type = inStream.read()
        if (type != 0x90) throw IllegalStateException("Expected SUBACK (0x90), got: $type")
        val remLen = readRemainingLength(inStream)
        val buf = ByteArray(remLen)
        var read = 0
        while (read < remLen) {
            val r = inStream.read(buf, read, remLen - read)
            if (r == -1) throw EOFException()
            read += r
        }
    }

    private fun sendPublishPacket(out: OutputStream, topic: String, payload: ByteArray) {
        val topicBytes = topic.toByteArray(Charsets.UTF_8)
        val varHeader = byteArrayOf(
            ((topicBytes.size shr 8) and 0xFF).toByte(),
            (topicBytes.size and 0xFF).toByte()
        ) + topicBytes
        val rem = varHeader + payload
        val packet = byteArrayOf(0x30) + encodeRemainingLength(rem.size) + rem

        out.write(packet)
        out.flush()
    }

    private fun encodeRemainingLength(length: Int): ByteArray {
        var l = length
        val out = ByteArrayOutputStream()
        do {
            var digit = l % 128
            l /= 128
            if (l > 0) {
                digit = digit or 128
            }
            out.write(digit)
        } while (l > 0)
        return out.toByteArray()
    }

    private fun readRemainingLength(inStream: InputStream): Int {
        var multiplier = 1
        var length = 0
        do {
            val digit = inStream.read()
            if (digit == -1) throw EOFException("Stream closed while reading remaining length")
            length += (digit and 127) * multiplier
            multiplier *= 128
        } while ((digit and 128) != 0)
        return length
    }

    private fun closeCurrentSocket() {
        pingJob?.cancel()
        pingJob = null
        try {
            activeOutputStream?.close()
        } catch (e: Exception) {}
        try {
            activeSocket?.close()
        } catch (e: Exception) {}
        activeOutputStream = null
        activeSocket = null
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        closeCurrentSocket()
        currentTopic = null
    }
}
