package com.reddy.vittify.presentation.ui.features.settings.cloudbackup

import android.content.Intent
import com.reddy.vittify.data.cloud.BackupSchedule
import com.reddy.vittify.data.cloud.DeviceSyncSchedule
import com.reddy.vittify.data.cloud.CloudFileInfo
import com.reddy.vittify.data.cloud.CloudProviderConfig
import com.reddy.vittify.data.cloud.CloudProviderType
import com.reddy.vittify.data.cloud.SyncStatus

data class BackupSyncState(
    val activeProviderType: CloudProviderType = CloudProviderType.LOCAL_ONLY,
    val webDavConfig: CloudProviderConfig.WebDavConfig = CloudProviderConfig.WebDavConfig(),
    val googleDriveConfig: CloudProviderConfig.GoogleDriveConfig = CloudProviderConfig.GoogleDriveConfig(),
    val isGoogleDriveSignedIn: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val backupSchedule: BackupSchedule = BackupSchedule.MANUAL,
    val deviceSyncSchedule: DeviceSyncSchedule = DeviceSyncSchedule.SAME_AS_BACKUP,
    val retentionLimit: Int = 10,
    val isE2eEnabled: Boolean = false,
    val e2ePassphrase: String = "",
    val lastBackupTime: Long = 0L,
    val lastSyncTime: Long = 0L,
    val remoteSnapshots: List<CloudFileInfo> = emptyList(),
    val isLoadingSnapshots: Boolean = false,
    val connectionTestResult: String? = null,
    val isTestingConnection: Boolean = false,
    val recoverableAuthIntent: Intent? = null,
    val showRestorePassphraseDialog: Boolean = false,
    val restoreTargetFile: CloudFileInfo? = null
)
