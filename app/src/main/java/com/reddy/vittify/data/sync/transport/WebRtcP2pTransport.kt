package com.reddy.vittify.data.sync.transport

import android.content.Context
import android.util.Log
import com.reddy.vittify.data.sync.security.P2pCryptoEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import java.util.Collections
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcP2pTransport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signalingClient: WebRtcSignalingClient,
    private val cryptoEngine: P2pCryptoEngine
) : P2pTransport {

    companion object {
        private const val TAG = "WebRtcP2pTransport"
        private const val DATA_CHANNEL_LABEL = "vittify-sync-channel"
        private val STUN_SERVERS = listOf(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun2.l.google.com:19302",
            "stun:stun.cloudflare.com:3478"
        )
    }

    override val transportType: P2pTransportType = P2pTransportType.REMOTE_WEBRTC

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override fun incomingPackets(): Flow<ByteArray> = _incomingPackets.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var signalingJob: Job? = null
    private var discoveryJob: Job? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private var activeClusterId: String? = null
    private var currentDeviceId: String? = null
    private var currentDeviceName: String? = null
    private var activeSecretKey: SecretKey? = null
    private var isInitiator = false
    private var lastConnectionAttemptTime = 0L
    private var activeTargetPeerId: String? = null

    /**
     * Optional direct delegate to transmit signaling messages over Local P2P (Nearby Connections)
     * when both devices are locally connected.
     */
    var directSignalingSender: (suspend (SignalingMessage) -> Boolean)? = null

    private val pendingIceCandidates = Collections.synchronizedList(mutableListOf<IceCandidate>())

    private val processedMessageIds = Collections.synchronizedSet(
        Collections.newSetFromMap(
            object : LinkedHashMap<String, Boolean>(128, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                    return size > 100
                }
            }
        )
    )

    private fun initFactory() {
        if (peerConnectionFactory == null) {
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .createPeerConnectionFactory()
        }
    }

    override fun start(clusterId: String, deviceId: String, deviceName: String) {
        if (activeClusterId == clusterId && currentDeviceId == deviceId && _connectionState.value is ConnectionState.Connected) {
            Log.d(TAG, "WebRTC transport already connected with cluster $clusterId; keeping connection alive")
            return
        }

        stop()

        activeClusterId = clusterId
        currentDeviceId = deviceId
        currentDeviceName = deviceName
        activeSecretKey = cryptoEngine.deriveKey(clusterId)

        initFactory()
        _connectionState.value = ConnectionState.Searching("Starting remote WebRTC signaling...")

        val key = activeSecretKey ?: return
        signalingClient.connect(clusterId, deviceId, key)

        signalingJob = scope.launch {
            signalingClient.incomingMessages().collect { message ->
                handleSignalingMessage(message)
            }
        }

        // Periodic presence discovery until connected
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            while (isActive && _connectionState.value !is ConnectionState.Connected) {
                val currentKey = activeSecretKey ?: break
                val msg = SignalingMessage(
                    type = SignalingMessage.TYPE_DISCOVERY,
                    fromDeviceId = deviceId,
                    fromDeviceName = deviceName,
                    clusterId = clusterId
                )
                transmitSignalingMessage(msg, currentKey)
                delay(3000L)
                delay(5000L)
            }
        }
    }

    fun initiateConnection(targetPeerId: String) {
        if (shouldSkipNewPeerConnection(targetPeerId)) {
            Log.d(TAG, "Connection attempt to $targetPeerId already in progress or connected, skipping duplicate initiation")
            return
        }
        Log.d(TAG, "Explicitly initiating WebRTC connection to peer: $targetPeerId")
        isInitiator = true
        createPeerConnection(targetPeerId, true)
    }

    fun retry(targetPeerId: String? = null) {
        Log.d(TAG, "WebRTC retry triggered (target=$targetPeerId)")
        lastConnectionAttemptTime = 0L
        val target = targetPeerId ?: activeTargetPeerId
        val key = activeSecretKey
        val myId = currentDeviceId
        val clusterId = activeClusterId

        if (target != null && key != null && myId != null) {
            if (myId < target) {
                initiateConnection(target)
            } else {
                val msg = SignalingMessage(
                    type = SignalingMessage.TYPE_DISCOVERY,
                    fromDeviceId = myId,
                    fromDeviceName = currentDeviceName ?: "",
                    toDeviceId = target,
                    clusterId = clusterId ?: ""
                )
                transmitSignalingMessage(msg, key)
            }
        } else if (key != null && myId != null && clusterId != null) {
            val msg = SignalingMessage(
                type = SignalingMessage.TYPE_DISCOVERY,
                fromDeviceId = myId,
                fromDeviceName = currentDeviceName ?: "",
                clusterId = clusterId
            )
            transmitSignalingMessage(msg, key)
        }
    }

    private fun shouldSkipNewPeerConnection(targetPeerId: String): Boolean {
        if (_connectionState.value is ConnectionState.Connected) return true
        val pc = peerConnection ?: return false
        val now = System.currentTimeMillis()
        if (targetPeerId == activeTargetPeerId && (now - lastConnectionAttemptTime) < 15_000L) {
            val state = pc.connectionState()
            if (state == PeerConnection.PeerConnectionState.CONNECTING ||
                state == PeerConnection.PeerConnectionState.CONNECTED ||
                state == PeerConnection.PeerConnectionState.NEW) {
                return true
            }
        }
        return false
    }

    fun handleSignalingMessage(message: SignalingMessage) {
        val key = activeSecretKey ?: return
        val myId = currentDeviceId ?: return

        if (!processedMessageIds.add(message.messageId)) {
            Log.d(TAG, "Ignoring duplicate signaling message ${message.messageId} (${message.type})")
            return
        }

        when (message.type) {
            SignalingMessage.TYPE_DISCOVERY -> {
                Log.d(TAG, "Received DISCOVERY from ${message.fromDeviceId} (${message.fromDeviceName})")
                if (_connectionState.value is ConnectionState.Connected) {
                    return
                }
                activeTargetPeerId = message.fromDeviceId
                if (myId < message.fromDeviceId) {
                    if (!shouldSkipNewPeerConnection(message.fromDeviceId)) {
                        Log.d(TAG, "Initiating WebRTC offer to ${message.fromDeviceId}")
                        isInitiator = true
                        createPeerConnection(message.fromDeviceId, true)
                    } else {
                        Log.d(TAG, "Skipping duplicate WebRTC initiation to ${message.fromDeviceId}")
                    }
                } else {
                    // Lower device initiates offer. Reply with discovery so lower ID knows we are online
                    val msg = SignalingMessage(
                        type = SignalingMessage.TYPE_DISCOVERY,
                        fromDeviceId = myId,
                        fromDeviceName = currentDeviceName ?: "",
                        toDeviceId = message.fromDeviceId,
                        clusterId = activeClusterId ?: ""
                    )
                    transmitSignalingMessage(msg, key)
                }
            }
            SignalingMessage.TYPE_OFFER -> {
                if (message.sdp != null) {
                    Log.d(TAG, "Received OFFER from ${message.fromDeviceId}")
                    isInitiator = false
                    activeTargetPeerId = message.fromDeviceId
                    createPeerConnection(message.fromDeviceId, false)
                    val pc = peerConnection
                    pc?.setRemoteDescription(
                        object : SimpleSdpObserver() {
                            override fun onSetSuccess() {
                                Log.d(TAG, "Remote description (OFFER) set successfully; creating answer")
                                drainPendingIceCandidates()
                                createAnswer(message.fromDeviceId, key)
                            }
                            override fun onSetFailure(error: String?) {
                                Log.e(TAG, "Failed setting remote description (OFFER): $error")
                            }
                        },
                        SessionDescription(SessionDescription.Type.OFFER, message.sdp)
                    )
                }
            }
            SignalingMessage.TYPE_ANSWER -> {
                if (message.sdp != null) {
                    Log.d(TAG, "Received ANSWER from ${message.fromDeviceId}")
                    val pc = peerConnection
                    pc?.setRemoteDescription(
                        object : SimpleSdpObserver() {
                            override fun onSetSuccess() {
                                Log.d(TAG, "Remote description (ANSWER) set successfully; draining queued ICE candidates")
                                drainPendingIceCandidates()
                            }
                            override fun onSetFailure(error: String?) {
                                Log.e(TAG, "Failed setting remote description (ANSWER): $error")
                            }
                        },
                        SessionDescription(SessionDescription.Type.ANSWER, message.sdp)
                    )
                }
            }
            SignalingMessage.TYPE_ICE_CANDIDATE -> {
                if (message.candidateSdp != null && message.candidateSdpMid != null) {
                    val candidate = IceCandidate(
                        message.candidateSdpMid,
                        message.candidateSdpMLineIndex,
                        message.candidateSdp
                    )
                    val pc = peerConnection
                    if (pc != null && pc.remoteDescription != null) {
                        pc.addIceCandidate(candidate)
                    } else {
                        Log.d(TAG, "Remote description not set yet; queuing ICE candidate")
                        pendingIceCandidates.add(candidate)
                    }
                }
            }
        }
    }

    private fun drainPendingIceCandidates() {
        val pc = peerConnection ?: return
        synchronized(pendingIceCandidates) {
            if (pendingIceCandidates.isNotEmpty()) {
                Log.d(TAG, "Draining ${pendingIceCandidates.size} queued ICE candidates")
                for (cand in pendingIceCandidates) {
                    pc.addIceCandidate(cand)
                }
                pendingIceCandidates.clear()
            }
        }
    }

    private fun transmitSignalingMessage(message: SignalingMessage, key: SecretKey) {
        scope.launch {
            // Dual path: send over local P2P if delegate is available
            directSignalingSender?.invoke(message)
            // And send over remote signaling relay
            signalingClient.sendMessage(message, key)
        }
    }

    private fun createPeerConnection(targetPeerId: String, createDataChannel: Boolean) {
        val factory = peerConnectionFactory ?: return
        val iceServers = STUN_SERVERS.map {
            PeerConnection.IceServer.builder(it).createIceServer()
        }
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        lastConnectionAttemptTime = System.currentTimeMillis()
        activeTargetPeerId = targetPeerId
        _connectionState.value = ConnectionState.Connecting(targetPeerId, "Partner Device")

        peerConnection?.close()
        pendingIceCandidates.clear()

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "PeerConnection State: $newState")
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        Log.d(TAG, "PeerConnection state is CONNECTED")
                    }
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        _connectionState.value = ConnectionState.Searching("WebRTC connection lost. Reconnecting...")
                    }
                    else -> {}
                }
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection State: $state")
                when (state) {
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        _connectionState.value = ConnectionState.Searching("WebRTC disconnected. Reconnecting...")
                    }
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val key = activeSecretKey ?: return
                    val msg = SignalingMessage(
                        type = SignalingMessage.TYPE_ICE_CANDIDATE,
                        fromDeviceId = currentDeviceId ?: "",
                        fromDeviceName = currentDeviceName ?: "",
                        toDeviceId = targetPeerId,
                        clusterId = activeClusterId ?: "",
                        candidateSdp = candidate.sdp,
                        candidateSdpMid = candidate.sdpMid,
                        candidateSdpMLineIndex = candidate.sdpMLineIndex
                    )
                    transmitSignalingMessage(msg, key)
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {
                if (channel != null) {
                    setupDataChannel(channel)
                }
            }
            override fun onRenegotiationNeeded() {}
        })

        if (createDataChannel) {
            val init = DataChannel.Init().apply {
                ordered = true
            }
            val channel = peerConnection?.createDataChannel(DATA_CHANNEL_LABEL, init)
            if (channel != null) {
                setupDataChannel(channel)
            }
            createOffer(targetPeerId)
        }
    }

    private fun setupDataChannel(channel: DataChannel) {
        dataChannel = channel
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}

            override fun onStateChange() {
                val state = channel.state()
                Log.d(TAG, "DataChannel State: $state")
                if (state == DataChannel.State.OPEN) {
                    discoveryJob?.cancel()
                    discoveryJob = null
                    _connectionState.value = ConnectionState.Connected(
                        peerCount = 1,
                        activeTransport = P2pTransportType.REMOTE_WEBRTC
                    )
                    Log.d(TAG, "WebRTC DataChannel OPEN! Remote P2P connected.")
                } else if (state == DataChannel.State.CLOSED) {
                    _connectionState.value = ConnectionState.Searching("WebRTC channel closed")
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                if (buffer != null) {
                    val data = ByteArray(buffer.data.remaining())
                    buffer.data.get(data)
                    _incomingPackets.tryEmit(data)
                }
            }
        })
    }

    private fun createOffer(targetPeerId: String) {
        val pc = peerConnection ?: return
        val key = activeSecretKey ?: return
        val constraints = MediaConstraints()

        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    pc.setLocalDescription(SimpleSdpObserver(), desc)
                    val msg = SignalingMessage(
                        type = SignalingMessage.TYPE_OFFER,
                        fromDeviceId = currentDeviceId ?: "",
                        fromDeviceName = currentDeviceName ?: "",
                        toDeviceId = targetPeerId,
                        clusterId = activeClusterId ?: "",
                        sdp = desc.description
                    )
                    transmitSignalingMessage(msg, key)
                }
            }
        }, constraints)
    }

    private fun createAnswer(targetPeerId: String, key: SecretKey) {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints()

        pc.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    pc.setLocalDescription(SimpleSdpObserver(), desc)
                    val msg = SignalingMessage(
                        type = SignalingMessage.TYPE_ANSWER,
                        fromDeviceId = currentDeviceId ?: "",
                        fromDeviceName = currentDeviceName ?: "",
                        toDeviceId = targetPeerId,
                        clusterId = activeClusterId ?: "",
                        sdp = desc.description
                    )
                    transmitSignalingMessage(msg, key)
                }
            }
        }, constraints)
    }

    override fun stop() {
        discoveryJob?.cancel()
        discoveryJob = null
        signalingJob?.cancel()
        signalingJob = null
        signalingClient.disconnect()

        try {
            dataChannel?.close()
            dataChannel?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing WebRTC connection", e)
        }
        pendingIceCandidates.clear()
        processedMessageIds.clear()
        dataChannel = null
        peerConnection = null
        _connectionState.value = ConnectionState.Idle
    }

    override suspend fun broadcastPacket(packetBytes: ByteArray): Boolean {
        val channel = dataChannel ?: return false
        if (channel.state() != DataChannel.State.OPEN) return false

        return try {
            val buffer = DataChannel.Buffer(ByteBuffer.wrap(packetBytes), false)
            channel.send(buffer)
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending packet via WebRTC", e)
            false
        }
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {
            Log.w(TAG, "SDP Create Failure: $error")
        }
        override fun onSetFailure(error: String?) {
            Log.w(TAG, "SDP Set Failure: $error")
        }
    }
}
