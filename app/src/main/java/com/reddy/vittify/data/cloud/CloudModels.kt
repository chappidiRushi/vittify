package com.reddy.vittify.data.cloud

import java.time.LocalDateTime

/**
 * Supported cloud storage providers
 */
enum class CloudProviderType(val displayName: String) {
    WEBDAV("Nextcloud / WebDAV"),
    GOOGLE_DRIVE("Google Drive"),
    LOCAL_ONLY("Local Only")
}

/**
 * Information about a file stored in the cloud
 */
data class CloudFileInfo(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val providerType: CloudProviderType
)

/**
 * Configuration options for cloud storage providers
 */
sealed class CloudProviderConfig {
    abstract val providerType: CloudProviderType
    abstract val isConfigured: Boolean

    data class WebDavConfig(
        val url: String = "",
        val username: String = "",
        val passwordOrToken: String = "",
        val isEnabled: Boolean = false
    ) : CloudProviderConfig() {
        override val providerType = CloudProviderType.WEBDAV
        override val isConfigured: Boolean
            get() = url.isNotBlank() && username.isNotBlank() && passwordOrToken.isNotBlank() && isEnabled
    }

    data class GoogleDriveConfig(
        val accountEmail: String = "",
        val accessToken: String = "",
        val isEnabled: Boolean = false
    ) : CloudProviderConfig() {
        override val providerType = CloudProviderType.GOOGLE_DRIVE
        override val isConfigured: Boolean
            get() = accountEmail.isNotBlank() && isEnabled
    }
}

/**
 * Status of background synchronization or backup operations
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    data class Syncing(val message: String = "Syncing with cloud...") : SyncStatus()
    data class BackingUp(val progress: Int = 0, val total: Int = 100, val message: String = "Creating backup...") : SyncStatus()
    data class Restoring(val progress: Int = 0, val total: Int = 100, val message: String = "Restoring data...") : SyncStatus()
    data class Success(val message: String, val timestamp: LocalDateTime = LocalDateTime.now()) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

/**
 * Schedule options for automatic background backups
 */
enum class BackupSchedule(val displayName: String, val intervalDays: Int) {
    MANUAL("Manual only", 0),
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30)
}

/**
 * Schedule options for automatic background device synchronization
 */
enum class DeviceSyncSchedule(val displayName: String, val intervalHours: Int) {
    MANUAL("Manual only", 0),
    EVERY_3_HOURS("Every 3 hours", 3),
    EVERY_6_HOURS("Every 6 hours", 6),
    DAILY("Daily", 24),
    WEEKLY("Weekly", 168),
    SAME_AS_BACKUP("Same as backup schedule", -1)
}

/**
 * Summary result of a multi-device synchronization cycle
 */
data class CloudSyncResult(
    val peersSynced: Int,
    val importedTransactions: Int,
    val importedCategories: Int,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

