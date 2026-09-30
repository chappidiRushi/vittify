package com.reddy.vittify.data.sync.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_changes",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["is_synced"]),
        Index(value = ["entity_type", "entity_id"])
    ]
)
data class SyncChangeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "entity_type")
    val entityType: String, // "TRANSACTION", "ACCOUNT", "CATEGORY", "SUBCATEGORY", "BUDGET", "RULE"

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "operation")
    val operation: String, // "INSERT", "UPDATE", "DELETE"

    @ColumnInfo(name = "payload_json")
    val payloadJson: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "origin_device_id")
    val originDeviceId: String,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false
) {
    companion object {
        const val OP_INSERT = "INSERT"
        const val OP_UPDATE = "UPDATE"
        const val OP_DELETE = "DELETE"

        const val TYPE_TRANSACTION = "TRANSACTION"
        const val TYPE_ACCOUNT = "ACCOUNT"
        const val TYPE_CATEGORY = "CATEGORY"
        const val TYPE_SUBCATEGORY = "SUBCATEGORY"
        const val TYPE_BUDGET = "BUDGET"
        const val TYPE_RULE = "RULE"
        const val TYPE_CARD = "CARD"
        const val TYPE_SUBSCRIPTION = "SUBSCRIPTION"
    }
}

