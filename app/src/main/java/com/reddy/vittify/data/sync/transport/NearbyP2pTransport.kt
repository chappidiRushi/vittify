package com.reddy.vittify.data.sync.transport

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyP2pTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : P2pTransport {

    companion object {
        private const val TAG = "NearbyP2pTransport"
        private const val SERVICE_ID_PREFIX = "vittify_p2p_"
    }

    override val transportType: P2pTransportType = P2pTransportType.LOCAL_NEARBY

    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context)
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override fun incomingPackets(): Flow<ByteArray> = _incomingPackets.asSharedFlow()

    private val endpointNames = ConcurrentHashMap<String, String>() // endpointId -> raw advertisedName
    private val connectedEndpoints = ConcurrentHashMap<String, String>() // endpointId -> advertisedName
    private val _connectedPeers = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val connectedPeers: StateFlow<List<Pair<String, String>>> = _connectedPeers.asStateFlow()

    private var activeClusterId: String? = null
    private var currentDeviceId: String? = null
    private var currentDeviceName: String? = null
    private var isRunning = false

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    _incomingPackets.tryEmit(bytes)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Transfer status tracking if needed
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with endpoint: $endpointId (${connectionInfo.endpointName})")
            endpointNames[endpointId] = connectionInfo.endpointName
            _connectionState.value = ConnectionState.Connecting(endpointId, connectionInfo.endpointName)
            // Automatically accept incoming connections from peers in our cluster
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to accept connection from $endpointId", e)
                }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                Log.d(TAG, "Successfully connected to endpoint: $endpointId")
                connectedEndpoints[endpointId] = endpointId
                val name = endpointNames[endpointId] ?: "Vittify Device"
                Log.d(TAG, "Successfully connected to endpoint: $endpointId ($name)")
                connectedEndpoints[endpointId] = name
                updateConnectedPeers()
                _connectionState.value = ConnectionState.Connected(
                    peerCount = connectedEndpoints.size,
                    activeTransport = P2pTransportType.LOCAL_NEARBY
                )
            } else {
                Log.w(TAG, "Connection to $endpointId failed with status: ${resolution.status.statusCode}")
                connectedEndpoints.remove(endpointId)
                endpointNames.remove(endpointId)
                updateConnectedPeers()
                updateStateAfterDisconnection()
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
            connectedEndpoints.remove(endpointId)
            endpointNames.remove(endpointId)
            updateConnectedPeers()
            updateStateAfterDisconnection()
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Discovered endpoint: $endpointId (${info.endpointName})")
            val myId = currentDeviceId ?: return
            val remoteId = info.endpointName.substringBefore("|")

            // Tie-breaker: device with higher ID calls requestConnection
            val shouldInitiate = if (remoteId != info.endpointName && remoteId.isNotBlank()) {
                myId > remoteId
            } else {
                myId.hashCode() > endpointId.hashCode()
            }

            if (shouldInitiate) {
                Log.d(TAG, "Initiating connection to $endpointId ($myId > $remoteId)")
                val advertisedName = "$myId|${currentDeviceName ?: "Vittify Device"}"
                connectionsClient.requestConnection(advertisedName, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Log.d(TAG, "Requested connection to $endpointId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed requesting connection to $endpointId", e)
                    }
            } else {
                Log.d(TAG, "Waiting for incoming connection from $endpointId ($myId <= $remoteId)")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Lost endpoint: $endpointId")
        }
    }

    override fun start(clusterId: String, deviceId: String, deviceName: String) {
        if (isRunning && activeClusterId == clusterId) return
        stop()

        activeClusterId = clusterId
        currentDeviceId = deviceId
        currentDeviceName = deviceName
        isRunning = true

        val serviceId = SERVICE_ID_PREFIX + clusterId.replace("-", "").take(15)
        _connectionState.value = ConnectionState.Searching("Searching for local devices...")

        val advOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        val advertisedName = "$deviceId|${deviceName.ifBlank { "Vittify Device" }}"
        connectionsClient.startAdvertising(advertisedName, serviceId, connectionLifecycleCallback, advOptions)
            .addOnSuccessListener {
                Log.d(TAG, "Started advertising with serviceId: $serviceId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start advertising", e)
                _connectionState.value = ConnectionState.Error("Nearby advertising failed: ${e.message}")
            }

        val discOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, discOptions)
            .addOnSuccessListener {
                Log.d(TAG, "Started discovery with serviceId: $serviceId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start discovery", e)
            }
    }

    override fun stop() {
        if (!isRunning) return
        isRunning = false
        try {
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Nearby client", e)
        }
        connectedEndpoints.clear()
        endpointNames.clear()
        _connectedPeers.value = emptyList()
        _connectionState.value = ConnectionState.Idle
    }

    private fun updateConnectedPeers() {
        val peers = connectedEndpoints.values.mapNotNull { name ->
            val parts = name.split("|", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) {
                Pair(parts[0], parts[1])
            } else null
        }
        _connectedPeers.value = peers
    }

    override suspend fun broadcastPacket(packetBytes: ByteArray): Boolean {
        if (connectedEndpoints.isEmpty()) return false
        val endpoints = connectedEndpoints.keys.toList()
        return try {
            val payload = Payload.fromBytes(packetBytes)
            connectionsClient.sendPayload(endpoints, payload)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast packet to ${endpoints.size} endpoints", e)
            false
        }
    }

    private fun updateStateAfterDisconnection() {
        if (connectedEndpoints.isNotEmpty()) {
            _connectionState.value = ConnectionState.Connected(
                peerCount = connectedEndpoints.size,
                activeTransport = P2pTransportType.LOCAL_NEARBY
            )
        } else if (isRunning) {
            _connectionState.value = ConnectionState.Searching("Searching for local devices...")
        } else {
            _connectionState.value = ConnectionState.Idle
        }
    }
}

