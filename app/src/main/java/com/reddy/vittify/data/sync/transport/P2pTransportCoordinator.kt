package com.reddy.vittify.data.sync.transport

import android.util.Log
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.SyncJsonSerializer
import com.reddy.vittify.data.sync.model.P2pPacket
import com.reddy.vittify.data.sync.security.P2pCryptoEngine
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
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class P2pTransportCoordinator @Inject constructor(
    private val nearbyTransport: NearbyP2pTransport,
    private val webRtcTransport: WebRtcP2pTransport,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val cryptoEngine: P2pCryptoEngine,
    private val serializer: SyncJsonSerializer
) {
    companion object {
        private const val TAG = "P2pCoordinator"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var nearbyCollectorJob: Job? = null
    private var webRtcCollectorJob: Job? = null
    private var nearbyStateJob: Job? = null
    private var webRtcStateJob: Job? = null
    private var remoteVerificationJob: Job? = null
    private var foregroundSupervisorJob: Job? = null

    val localConnectionState: StateFlow<ConnectionState> = nearbyTransport.connectionState
    val remoteConnectionState: StateFlow<ConnectionState> = webRtcTransport.connectionState

    private val _activeConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val activeConnectionState: StateFlow<ConnectionState> = _activeConnectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    fun incomingPackets(): Flow<ByteArray> = _incomingPackets.asSharedFlow()

    private var currentClusterId: String? = null
    private var currentDeviceId: String? = null
    private var currentDeviceName: String? = null
    private var activeSecretKey: SecretKey? = null
    private var isRemoteStarted = false
    private var lastKnownPartnerDeviceId: String? = null
    private var isStarted = false
    private var isForeground = true

    fun start(clusterId: String, deviceId: String, deviceName: String, force: Boolean = false) {
        if (isStarted && !force && currentClusterId == clusterId && currentDeviceId == deviceId) {
            val localConnected = nearbyTransport.connectionState.value is ConnectionState.Connected
            val remoteConnected = webRtcTransport.connectionState.value is ConnectionState.Connected
            if (localConnected || remoteConnected) {
                Log.d(TAG, "P2pTransportCoordinator is already started and connected. Preserving active session.")
                return
            }
            Log.d(TAG, "P2pTransportCoordinator is already started. Ensuring background jobs are running.")
            startForegroundSupervisor()
            return
        }

        stop()

        currentClusterId = clusterId
        currentDeviceId = deviceId
        currentDeviceName = deviceName
        activeSecretKey = cryptoEngine.deriveKey(clusterId)
        isRemoteStarted = false
        isStarted = true

        val localEnabled = p2pPreferences.isLocalP2pEnabled()
        val remoteEnabled = false // Remote P2P disabled for now

        Log.d(TAG, "Starting coordinator: localEnabled=$localEnabled, remoteEnabled=$remoteEnabled")

        // Set direct signaling sender on WebRTC transport so it can tunnel through Local Nearby
        webRtcTransport.directSignalingSender = { signalingMsg ->
            if (nearbyTransport.connectionState.value is ConnectionState.Connected) {
                sendSignalingOverNearby(signalingMsg)
            } else {
                false
            }
        }

        if (!localEnabled && !remoteEnabled) {
            _activeConnectionState.value = ConnectionState.Idle
            return
        }

        _activeConnectionState.value = ConnectionState.Searching("Connecting to partner...")

        if (localEnabled) {
            startLocal(clusterId, deviceId, deviceName)
        }
        if (remoteEnabled) {
            startRemote(clusterId, deviceId, deviceName)
        }

        startForegroundSupervisor()
    }

    private fun startLocal(
        clusterId: String,
        deviceId: String,
        deviceName: String
    ) {
        nearbyTransport.start(clusterId, deviceId, deviceName)

        nearbyCollectorJob?.cancel()
        nearbyCollectorJob = scope.launch {
            nearbyTransport.incomingPackets().collect { rawBytes ->
                val key = activeSecretKey
                if (key != null) {
                    try {
                        val decrypted = cryptoEngine.decrypt(rawBytes, key)
                        val json = String(decrypted, Charsets.UTF_8)
                        val packet = serializer.fromJson(json, P2pPacket::class.java)
                        if (packet.type == P2pPacket.TYPE_WEBRTC_SIGNALING) {
                            val sigMsg = packet.signalingMessage
                            if (sigMsg != null) {
                                Log.d(TAG, "Received direct WebRTC signaling packet over Nearby: ${sigMsg.type}")
                                webRtcTransport.handleSignalingMessage(sigMsg)
                                return@collect
                            }
                        }
                    } catch (e: Exception) {
                        // Not a direct signaling packet or decryption failed; forward to sync engine
                    }
                }
                _incomingPackets.emit(rawBytes)
            }
        }

        nearbyStateJob?.cancel()
        nearbyStateJob = scope.launch {
            nearbyTransport.connectionState.collect { state ->
                updateAggregateState()
                if (state is ConnectionState.Connected) {
                    onLocalP2pConnected()
                } else if (state is ConnectionState.Error || state is ConnectionState.Idle) {
                    onLocalP2pDisconnected()
                }
            }
        }
    }

    private fun startRemote(
        clusterId: String,
        deviceId: String,
        deviceName: String
    ) {
        isRemoteStarted = true
        webRtcTransport.start(clusterId, deviceId, deviceName)

        webRtcCollectorJob?.cancel()
        webRtcCollectorJob = scope.launch {
            webRtcTransport.incomingPackets().collect { rawBytes ->
                _incomingPackets.emit(rawBytes)
            }
        }

        webRtcStateJob?.cancel()
        webRtcStateJob = scope.launch {
            webRtcTransport.connectionState.collect {
                updateAggregateState()
            }
        }
    }

    private fun onLocalP2pConnected() {
        if (!p2pPreferences.isRemoteP2pEnabled()) return

        remoteVerificationJob?.cancel()
        remoteVerificationJob = scope.launch {
            val myId = currentDeviceId ?: return@launch
            val clusterId = currentClusterId ?: return@launch
            val myName = currentDeviceName ?: "Vittify Device"

            Log.d(TAG, "Local P2P connected! Exchanging necessary data for Remote P2P...")

            // Wait a brief moment for connectedPeers to be populated if needed
            var peers = nearbyTransport.connectedPeers.value
            var retry = 0
            while (isActive && peers.isEmpty() && retry < 5) {
                delay(300L)
                peers = nearbyTransport.connectedPeers.value
                retry++
            }

            val partner = peers.firstOrNull()
            if (partner != null) {
                lastKnownPartnerDeviceId = partner.first
            }

            // Transmit discovery presence over Local Nearby
            val discoveryMsg = SignalingMessage(
                type = SignalingMessage.TYPE_DISCOVERY,
                fromDeviceId = myId,
                fromDeviceName = myName,
                clusterId = clusterId
            )
            sendSignalingOverNearby(discoveryMsg)

            if (partner != null) {
                val partnerId = partner.first
                Log.d(TAG, "Local peer detected: $partnerId (${partner.second})")
                if (myId < partnerId) {
                    Log.d(TAG, "Local device has lower ID ($myId < $partnerId); initiating WebRTC offer")
                    webRtcTransport.initiateConnection(partnerId)
                }
            }

            // Verify and ensure Remote P2P connects
            var checkCount = 0
            while (isActive && nearbyTransport.connectionState.value is ConnectionState.Connected) {
                val rState = webRtcTransport.connectionState.value
                if (rState is ConnectionState.Connected) {
                    Log.d(TAG, "Remote P2P verified CONNECTED alongside Local P2P!")
                    updateAggregateState()
                    break
                }

                checkCount++
                if (checkCount % 3 == 0) {
                    Log.d(TAG, "Remote P2P not yet connected (attempt $checkCount). Re-transmitting signaling discovery over Local P2P...")
                    sendSignalingOverNearby(discoveryMsg)
                    val p = nearbyTransport.connectedPeers.value.firstOrNull()
                    if (p != null) {
                        lastKnownPartnerDeviceId = p.first
                        if (myId < p.first) {
                            webRtcTransport.initiateConnection(p.first)
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun onLocalP2pDisconnected() {
        Log.d(TAG, "Local P2P disconnected! Evaluating Remote P2P connection...")
        remoteVerificationJob?.cancel()
        remoteVerificationJob = null
        updateAggregateState()

        if (!p2pPreferences.isRemoteP2pEnabled()) return

        if (webRtcTransport.connectionState.value is ConnectionState.Connected) {
            Log.d(TAG, "Remote WebRTC is already connected. Seamless fallback active.")
            return
        }

        // Trigger automatic remote connection to known partner
        scope.launch {
            delay(500L)
            val partnerId = lastKnownPartnerDeviceId
                ?: p2pPreferences.getPartnerUserId()
                ?: p2pPreferences.getPairedDevices().firstOrNull()?.deviceId

            val myId = currentDeviceId
            val clusterId = currentClusterId

            Log.d(TAG, "Initiating automatic Remote P2P connection for partner: $partnerId")
            if (partnerId != null && myId != null && clusterId != null) {
                if (myId < partnerId) {
                    webRtcTransport.initiateConnection(partnerId)
                } else {
                    webRtcTransport.retry(partnerId)
                }
            } else {
                webRtcTransport.retry(null)
            }
        }
    }

    fun setForeground(inForeground: Boolean) {
        isForeground = inForeground
        Log.d(TAG, "P2pTransportCoordinator setForeground: $inForeground")
        if (inForeground) {
            startForegroundSupervisor()
        } else {
            foregroundSupervisorJob?.cancel()
            foregroundSupervisorJob = null
        }
    }

    private fun startForegroundSupervisor() {
        foregroundSupervisorJob?.cancel()
        foregroundSupervisorJob = scope.launch {
            while (isActive && isForeground) {
                delay(8000L)
                if (!p2pPreferences.isSyncEnabled() || !isStarted) continue

                val localConnected = nearbyTransport.connectionState.value is ConnectionState.Connected
                val remoteConnected = webRtcTransport.connectionState.value is ConnectionState.Connected

                // As long as the app is open in the foreground and not connected, keep trying!
                if (!localConnected && !remoteConnected) {
                    Log.d(TAG, "Foreground supervisor: Disconnected while app in foreground. Retrying...")
                    val partnerId = lastKnownPartnerDeviceId
                        ?: p2pPreferences.getPartnerUserId()
                        ?: p2pPreferences.getPairedDevices().firstOrNull()?.deviceId

                    if (p2pPreferences.isRemoteP2pEnabled()) {
                        webRtcTransport.retry(partnerId)
                    }
                    if (p2pPreferences.isLocalP2pEnabled()) {
                        val cId = currentClusterId
                        val dId = currentDeviceId
                        val dName = currentDeviceName
                        if (cId != null && dId != null && dName != null) {
                            nearbyTransport.start(cId, dId, dName)
                        }
                    }
                } else if (localConnected && !remoteConnected && p2pPreferences.isRemoteP2pEnabled()) {
                    // Local connected, make sure remote is also kept ready
                    val rState = webRtcTransport.connectionState.value
                    if (rState !is ConnectionState.Connected && rState !is ConnectionState.Connecting) {
                        val partnerId = lastKnownPartnerDeviceId ?: nearbyTransport.connectedPeers.value.firstOrNull()?.first
                        if (partnerId != null) {
                            webRtcTransport.retry(partnerId)
                        }
                    }
                }
            }
        }
    }

    fun retryAll() {
        Log.d(TAG, "retryAll() invoked: restarting connection attempts across transports")
        val cId = currentClusterId ?: return
        val dId = currentDeviceId ?: return
        val dName = currentDeviceName ?: return

        val partnerId = lastKnownPartnerDeviceId
            ?: p2pPreferences.getPartnerUserId()
            ?: p2pPreferences.getPairedDevices().firstOrNull()?.deviceId

        _activeConnectionState.value = ConnectionState.Searching("Reconnecting to partner...")

        if (p2pPreferences.isLocalP2pEnabled()) {
            nearbyTransport.stop()
            nearbyTransport.start(cId, dId, dName)
        }
        if (p2pPreferences.isRemoteP2pEnabled()) {
            webRtcTransport.retry(partnerId)
        }
    }

    private suspend fun sendSignalingOverNearby(signalingMsg: SignalingMessage): Boolean {
        val key = activeSecretKey ?: return false
        return try {
            val packet = P2pPacket(
                type = P2pPacket.TYPE_WEBRTC_SIGNALING,
                senderDeviceId = currentDeviceId ?: "",
                senderDeviceName = currentDeviceName ?: "",
                signalingMessage = signalingMsg
            )
            val json = serializer.toJson(packet)
            val encryptedBytes = cryptoEngine.encrypt(json.toByteArray(Charsets.UTF_8), key)
            nearbyTransport.broadcastPacket(encryptedBytes)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send signaling message over Nearby", e)
            false
        }
    }

    private fun updateAggregateState() {
        val local = nearbyTransport.connectionState.value
        val remote = if (p2pPreferences.isRemoteP2pEnabled()) webRtcTransport.connectionState.value else ConnectionState.Idle

        _activeConnectionState.value = when {
            local is ConnectionState.Connected -> local
            remote is ConnectionState.Connected -> {
                Log.d(TAG, "Active connection state falling back to Remote P2P (Local is not connected)")
                remote
            }
            local is ConnectionState.Connecting -> local
            remote is ConnectionState.Connecting -> remote
            local is ConnectionState.Searching && remote is ConnectionState.Searching -> local
            local is ConnectionState.Searching -> local
            remote is ConnectionState.Searching -> remote
            local is ConnectionState.Error && remote is ConnectionState.Error -> local
            local is ConnectionState.Error -> if (p2pPreferences.isRemoteP2pEnabled()) remote else local
            remote is ConnectionState.Error -> local
            else -> if (local !is ConnectionState.Idle) local else ConnectionState.Idle
        }
    }

    suspend fun broadcast(packetBytes: ByteArray): Boolean {
        val localConnected = nearbyTransport.connectionState.value is ConnectionState.Connected
        val remoteConnected = webRtcTransport.connectionState.value is ConnectionState.Connected

        // Primary: When Local P2P is connected, send via Local P2P
        if (p2pPreferences.isLocalP2pEnabled() && localConnected) {
            val sentLocal = nearbyTransport.broadcastPacket(packetBytes)
            if (sentLocal) return true
        }

        // Fallback: When Local P2P is disconnected (or failed), fall back to Remote P2P
        if (p2pPreferences.isRemoteP2pEnabled() && remoteConnected) {
            return webRtcTransport.broadcastPacket(packetBytes)
        }

        // Best effort if neither is fully in Connected state
        var sent = false
        if (p2pPreferences.isLocalP2pEnabled()) {
            sent = nearbyTransport.broadcastPacket(packetBytes) || sent
        }
        if (p2pPreferences.isRemoteP2pEnabled() && !sent) {
            sent = webRtcTransport.broadcastPacket(packetBytes) || sent
        }
        return sent
    }

    fun stop() {
        isStarted = false
        remoteVerificationJob?.cancel()
        remoteVerificationJob = null
        foregroundSupervisorJob?.cancel()
        foregroundSupervisorJob = null
        webRtcTransport.directSignalingSender = null

        nearbyCollectorJob?.cancel()
        webRtcCollectorJob?.cancel()
        nearbyStateJob?.cancel()
        webRtcStateJob?.cancel()

        nearbyCollectorJob = null
        webRtcCollectorJob = null
        nearbyStateJob = null
        webRtcStateJob = null
        isRemoteStarted = false

        nearbyTransport.stop()
        webRtcTransport.stop()
        _activeConnectionState.value = ConnectionState.Idle
    }
}
