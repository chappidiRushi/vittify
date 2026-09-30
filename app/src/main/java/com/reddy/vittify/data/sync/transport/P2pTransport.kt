package com.reddy.vittify.data.sync.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class P2pTransportType {
    LOCAL_NEARBY,
    REMOTE_WEBRTC,
    NONE
}

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data class Searching(val info: String) : ConnectionState()
    data class Connecting(val peerId: String, val peerName: String = "Partner Device") : ConnectionState()
    data class Connected(val peerCount: Int, val activeTransport: P2pTransportType) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

interface P2pTransport {
    val transportType: P2pTransportType
    val connectionState: StateFlow<ConnectionState>
    fun start(clusterId: String, deviceId: String, deviceName: String)
    fun stop()
    suspend fun broadcastPacket(packetBytes: ByteArray): Boolean
    fun incomingPackets(): Flow<ByteArray>
}

