package com.reddy.vittify.data.cloud.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.reddy.vittify.data.backup.BackupConfiguration
import com.reddy.vittify.data.backup.BackupExporter
import com.reddy.vittify.data.backup.BackupImporter
import com.reddy.vittify.data.backup.ExportResult
import com.reddy.vittify.data.backup.ImportResult
import com.reddy.vittify.data.backup.ImportStrategy
import com.reddy.vittify.data.cloud.CloudSyncResult
import com.reddy.vittify.data.cloud.security.CloudCredentialStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine implementing differential multi-device synchronization modeled on Cashew (`syncClient.dart`).
 */
@Singleton
class CloudSyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter,
    private val cloudBackupManager: CloudBackupManager,
    private val cloudCredentialStore: CloudCredentialStore
) {

    companion object {
        const val SYNC_FOLDER = "vittify_sync"
    }

    /**
     * Perform differential synchronization across devices via the active cloud provider.
     */
    suspend fun synchronize(progressListener: ((Int) -> Unit)? = null): Result<CloudSyncResult> = withContext(Dispatchers.IO) {
        val (provider, providerConfig) = cloudBackupManager.getActiveProvider()
            ?: return@withContext Result.failure(IllegalStateException("No active or configured cloud storage provider."))

        try {
            val deviceId = cloudCredentialStore.getDeviceId()
            val syncFileName = "sync-$deviceId.zip"
            val remoteSyncPath = "$SYNC_FOLDER/$syncFileName"

            progressListener?.invoke(10)
            // Step 1: Export local state snapshot and publish/upload to sync folder
            val exportResult = backupExporter.exportBackup(BackupConfiguration())
            if (exportResult !is ExportResult.Success) {
                val errorMsg = if (exportResult is ExportResult.Error) exportResult.message else "Sync local export failed"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val localSnapshotFile = exportResult.file
            val uploadResult = provider.uploadFile(localSnapshotFile, remoteSyncPath, providerConfig) { upProg ->
                progressListener?.invoke(10 + (upProg * 25 / 100))
            }

            if (localSnapshotFile.exists()) localSnapshotFile.delete()

            if (uploadResult.isFailure) {
                return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Exception("Failed to publish local sync state."))
            }

            progressListener?.invoke(40)

            // Step 2: List remote sync snapshots and discover peer devices
            val listResult = provider.listFiles(SYNC_FOLDER, providerConfig)
            if (listResult.isFailure) {
                return@withContext Result.failure(listResult.exceptionOrNull() ?: Exception("Failed to list remote sync files."))
            }

            val allSyncFiles = listResult.getOrNull() ?: emptyList()
            val peerSyncFiles = allSyncFiles.filter {
                it.name.startsWith("sync-") && it.name.endsWith(".zip") && !it.name.contains(deviceId)
            }

            var peersSynced = 0
            var totalTransactionsImported = 0
            var totalCategoriesImported = 0
            var anyChangesMerged = false

            val peerFilesToDelete = mutableListOf<com.reddy.vittify.data.cloud.CloudFileInfo>()

            val cacheDir = File(context.cacheDir, "peer_syncs")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Step 3: Compare timestamps and merge new peer updates
            for ((index, peerFile) in peerSyncFiles.withIndex()) {
                val peerId = peerFile.name.removePrefix("sync-").removeSuffix(".zip")
                val lastSyncedTime = cloudCredentialStore.getPeerLastSyncedTimestamp(peerId)

                // If remote peer file is newer than when we last synced with this peer
                if (peerFile.lastModified > lastSyncedTime) {
                    val peerDestFile = File(cacheDir, "peer_$peerId.zip")
                    val downResult = provider.downloadFile(peerFile.path, peerDestFile, providerConfig)
                    if (downResult.isSuccess && peerDestFile.exists()) {
                        val importResult = backupImporter.importBackup(Uri.fromFile(peerDestFile), ImportStrategy.MERGE)
                        if (importResult is ImportResult.Success) {
                            totalTransactionsImported += importResult.importedTransactions
                            totalCategoriesImported += importResult.importedCategories
                            if (importResult.importedTransactions > 0 || importResult.importedCategories > 0) {
                                anyChangesMerged = true
                            }
                            peersSynced++
                            cloudCredentialStore.setPeerLastSyncedTimestamp(peerId, peerFile.lastModified)
                            peerFilesToDelete.add(peerFile)
                        }
                        peerDestFile.delete()
                    }
                } else {
                    peerFilesToDelete.add(peerFile)
                }
                val currentProgress = 40 + ((index + 1) * 45 / peerSyncFiles.size.coerceAtLeast(1))
                progressListener?.invoke(currentProgress)
            }

            // Step 4: If we merged incoming peer updates into our local database, re-publish our updated state
            var uploadUpdatedSuccess = true
            if (anyChangesMerged) {
                val updatedExport = backupExporter.exportBackup(BackupConfiguration())
                if (updatedExport is ExportResult.Success) {
                    val uploadRes = provider.uploadFile(updatedExport.file, remoteSyncPath, providerConfig)
                    if (updatedExport.file.exists()) updatedExport.file.delete()
                    if (uploadRes.isFailure) {
                        uploadUpdatedSuccess = false
                    }
                } else {
                    uploadUpdatedSuccess = false
                }
            }

            if (uploadUpdatedSuccess) {
                for (peerFile in peerFilesToDelete) {
                    try {
                        provider.deleteFile(peerFile.path, providerConfig)
                        Log.d("CloudSyncEngine", "Smart sync pruned redundant peer file: ${peerFile.name}")
                    } catch (e: Exception) {
                        Log.w("CloudSyncEngine", "Failed to delete redundant peer file: ${peerFile.name}", e)
                    }
                }
            }

            cloudCredentialStore.setLastSyncTimestamp(System.currentTimeMillis())
            progressListener?.invoke(100)

            Result.success(
                CloudSyncResult(
                    peersSynced = peersSynced,
                    importedTransactions = totalTransactionsImported,
                    importedCategories = totalCategoriesImported
                )
            )
        } catch (e: Exception) {
            Log.e("CloudSyncEngine", "Synchronization failed", e)
            Result.failure(e)
        }
    }
}
