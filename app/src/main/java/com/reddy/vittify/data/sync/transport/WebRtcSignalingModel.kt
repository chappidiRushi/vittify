package com.reddy.vittify.data.sync.transport

data class SignalingMessage(
    val type: String, // "OFFER", "ANSWER", "ICE_CANDIDATE", "DISCOVERY"
    val fromDeviceId: String,
    val fromDeviceName: String,
    val toDeviceId: String? = null,
    val clusterId: String,
    val sdp: String? = null,
    val candidateSdp: String? = null,
    val candidateSdpMid: String? = null,
    val candidateSdpMLineIndex: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val messageId: String = java.util.UUID.randomUUID().toString()
) {
    companion object {
        const val TYPE_OFFER = "OFFER"
        const val TYPE_ANSWER = "ANSWER"
        const val TYPE_ICE_CANDIDATE = "ICE_CANDIDATE"
        const val TYPE_DISCOVERY = "DISCOVERY"
    }
}

