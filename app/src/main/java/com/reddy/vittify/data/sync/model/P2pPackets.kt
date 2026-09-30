package com.reddy.vittify.data.sync.model

import androidx.annotation.Keep

@Keep
data class PairedDevice(
    val deviceId: String,
    val deviceName: String,
    val customNickname: String? = null,
    val transportType: String = "UNKNOWN", // "LOCAL", "WEBRTC"
    val lastSeen: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (!customNickname.isNullOrBlank()) customNickname else deviceName
}

@Keep
data class SyncChangePayload(
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val timestamp: Long,
    val originDeviceId: String,
    val changeId: Long? = null
)

@Keep
data class SyncSessionMetaPayload(
    val sessionId: String,
    val totalRecords: Int,
    val step: Int, // 1 for Step 1 (Initiator -> Responder), 2 for Step 2 (Responder -> Initiator)
    val isInitialPairSync: Boolean = false
)

@Keep
data class P2pPacket(
    val type: String, // "HANDSHAKE", "CHANGES", "ACK", "PING", "DEVICE_RENAME", etc.
    val senderDeviceId: String,
    val senderDeviceName: String,
    val senderUserName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val handshake: HandshakePayload? = null,
    val changes: List<SyncChangePayload>? = null,
    val ackTimestamp: Long? = null,
    val signalingMessage: com.reddy.vittify.data.sync.transport.SignalingMessage? = null,
    val syncMeta: SyncSessionMetaPayload? = null,
    val sequenceNumber: Int? = null,
    val totalRecords: Int? = null,
    val singleChange: SyncChangePayload? = null,
    val step: Int? = null,
    val recordIds: List<Long>? = null
) {
    companion object {
        const val TYPE_HANDSHAKE = "HANDSHAKE"
        const val TYPE_CHANGES = "CHANGES"
        const val TYPE_ACK = "ACK"
        const val TYPE_PING = "PING"
        const val TYPE_DEVICE_RENAME = "DEVICE_RENAME"
        const val TYPE_SYNC_REQUEST = "SYNC_REQUEST"
        const val TYPE_WEBRTC_SIGNALING = "WEBRTC_SIGNALING"

        // Lockstep Sequential Protocol
        const val TYPE_SYNC_START_META = "SYNC_START_META"
        const val TYPE_SYNC_META_ACK = "SYNC_META_ACK"
        const val TYPE_SYNC_RECORD = "SYNC_RECORD"
        const val TYPE_SYNC_RECORD_ACK = "SYNC_RECORD_ACK"
        const val TYPE_SYNC_STEP_COMPLETE = "SYNC_STEP_COMPLETE"
        const val TYPE_SYNC_STEP_COMPLETE_ACK = "SYNC_STEP_COMPLETE_ACK"
        const val TYPE_SYNC_SESSION_DONE = "SYNC_SESSION_DONE"
        const val TYPE_SYNC_CANCEL = "SYNC_CANCEL"
        const val TYPE_UNPAIR = "UNPAIR"
    }
}

@Keep
data class HandshakePayload(
    val lastSyncTimestamp: Long,
    val schemaVersion: Int = 64,
    val permissions: Map<String, Boolean> = emptyMap(),
    val partnerUserId: String? = null,
    val userName: String? = null,
    val isScanner: Boolean = false
)


