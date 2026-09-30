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
import com.reddy.vittify.data.cloud.CloudFileInfo
import com.reddy.vittify.data.cloud.CloudProviderConfig
import com.reddy.vittify.data.cloud.CloudProviderType
import com.reddy.vittify.data.cloud.CloudStorageProvider
import com.reddy.vittify.data.cloud.providers.GoogleDriveStorageProvider
import com.reddy.vittify.data.cloud.providers.WebDavStorageProvider
import com.reddy.vittify.data.cloud.security.BackupEncryptionEngine
import com.reddy.vittify.data.cloud.security.CloudCredentialStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates cloud backups, retention pruning, and snapshot restores across active cloud storage providers.
 */
@Singleton
class CloudBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter,
    private val webDavProvider: WebDavStorageProvider,
    private val googleDriveProvider: GoogleDriveStorageProvider,
    private val cloudCredentialStore: CloudCredentialStore,
    private val encryptionEngine: BackupEncryptionEngine
) {

    companion object {
        const val BACKUPS_FOLDER = "vittify_backups"
    }

    /**
     * Resolves the active provider and valid credentials configuration.
     */
    fun getActiveProvider(): Pair<CloudStorageProvider, CloudProviderConfig>? {
        return when (cloudCredentialStore.getActiveProviderType()) {
            CloudProviderType.WEBDAV -> {
                val config = cloudCredentialStore.getWebDavConfig()
                if (config.isConfigured) webDavProvider to config else null
            }
            CloudProviderType.GOOGLE_DRIVE -> {
                val config = cloudCredentialStore.getGoogleDriveConfig()
                if (config.isConfigured) googleDriveProvider to config else null
            }
            CloudProviderType.LOCAL_ONLY -> null
        }
    }

    /**
     * Export complete application snapshot and upload to active cloud provider folder.
     */
    suspend fun performBackup(
        config: BackupConfiguration = BackupConfiguration(),
        progressListener: ((Int) -> Unit)? = null
    ): Result<CloudFileInfo> = withContext(Dispatchers.IO) {
        val (provider, providerConfig) = getActiveProvider()
            ?: return@withContext Result.failure(IllegalStateException("No active or configured cloud storage provider selected."))

        try {
            progressListener?.invoke(5)
            val exportResult = backupExporter.exportBackup(config)
            if (exportResult !is ExportResult.Success) {
                val errorMsg = if (exportResult is ExportResult.Error) exportResult.message else "Backup export failed"
                return@withContext Result.failure(Exception(errorMsg))
            }

            var fileToUpload = exportResult.file
            val isEncrypted = cloudCredentialStore.isE2eEncryptionEnabled()
            if (isEncrypted) {
                val passphrase = cloudCredentialStore.getE2ePassphrase()
                if (passphrase.isNotBlank()) {
                    val encryptedFile = File(context.cacheDir, "${fileToUpload.nameWithoutExtension}.enc")
                    val encResult = encryptionEngine.encryptFile(fileToUpload, encryptedFile, passphrase)
                    if (encResult.isSuccess && encResult.getOrNull() != null) {
                        // Clean up plaintext zip
                        fileToUpload.delete()
                        fileToUpload = encResult.getOrNull()!!
                    } else {
                        return@withContext Result.failure(Exception("Failed to encrypt backup file before upload."))
                    }
                }
            }

            progressListener?.invoke(25)
            val remotePath = "$BACKUPS_FOLDER/${fileToUpload.name}"
            val uploadResult = provider.uploadFile(fileToUpload, remotePath, providerConfig) { uploadProgress ->
                // Map 0-100 upload progress to 25-95 total progress
                val mapped = 25 + ((uploadProgress * 70) / 100)
                progressListener?.invoke(mapped)
            }

            // Cleanup local temporary export file after successful upload
            if (fileToUpload.exists()) {
                fileToUpload.delete()
            }

            if (uploadResult.isSuccess) {
                cloudCredentialStore.setLastBackupTimestamp(System.currentTimeMillis())
                // Enforce retention limit after successful upload
                enforceRetentionPolicy(cloudCredentialStore.getRetentionLimit(), provider, providerConfig)
                progressListener?.invoke(100)
                Result.success(uploadResult.getOrNull()!!)
            } else {
                Result.failure(uploadResult.exceptionOrNull() ?: Exception("Cloud upload failed"))
            }
        } catch (e: Exception) {
            Log.e("CloudBackupManager", "Perform backup error", e)
            Result.failure(e)
        }
    }

    /**
     * List all remote backup snapshots stored inside vittify_backups folder.
     */
    suspend fun listRemoteBackups(): Result<List<CloudFileInfo>> = withContext(Dispatchers.IO) {
        val (provider, providerConfig) = getActiveProvider()
            ?: return@withContext Result.failure(IllegalStateException("No active or configured cloud storage provider."))

        try {
            val listResult = provider.listFiles(BACKUPS_FOLDER, providerConfig)
            if (listResult.isSuccess) {
                val backups = listResult.getOrNull() ?: emptyList()
                val filtered = backups.filter {
                    it.name.startsWith("Vittify_Backup_") && (it.name.endsWith(".zip") || it.name.endsWith(".enc"))
                }.sortedByDescending { it.lastModified }
                Result.success(filtered)
            } else {
                Result.failure(listResult.exceptionOrNull() ?: Exception("Failed to list backups"))
            }
        } catch (e: Exception) {
            Log.e("CloudBackupManager", "List backups error", e)
            Result.failure(e)
        }
    }

    /**
     * Download a remote backup snapshot and restore it into Vittify Room database.
     */
    suspend fun restoreBackup(
        fileInfo: CloudFileInfo,
        progressListener: ((Int) -> Unit)? = null
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        val (provider, providerConfig) = getActiveProvider()
            ?: return@withContext Result.failure(IllegalStateException("No active or configured cloud storage provider."))

        try {
            val cacheDir = File(context.cacheDir, "cloud_restores")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val downloadedFile = File(cacheDir, fileInfo.name)
            progressListener?.invoke(10)

            val downloadResult = provider.downloadFile(fileInfo.path, downloadedFile, providerConfig) { downProgress ->
                val mapped = 10 + ((downProgress * 60) / 100)
                progressListener?.invoke(mapped)
            }

            if (downloadResult.isFailure || !downloadedFile.exists()) {
                return@withContext Result.failure(downloadResult.exceptionOrNull() ?: Exception("Failed to download remote backup file."))
            }

            progressListener?.invoke(75)
            var importFile = downloadedFile

            // Check if encrypted
            if (fileInfo.name.endsWith(".enc") || encryptionEngine.isEncryptedBackup(downloadedFile)) {
                val passphrase = cloudCredentialStore.getE2ePassphrase()
                if (passphrase.isBlank()) {
                    downloadedFile.delete()
                    return@withContext Result.failure(IllegalArgumentException("This backup is encrypted. Please set your E2E Passphrase in Cloud Backup settings to restore."))
                }
                val decryptedFile = File(cacheDir, "${fileInfo.name.removeSuffix(".enc")}.zip")
                val decryptResult = encryptionEngine.decryptFile(downloadedFile, decryptedFile, passphrase)
                if (decryptResult.isSuccess && decryptedFile.exists()) {
                    downloadedFile.delete()
                    importFile = decryptedFile
                } else {
                    downloadedFile.delete()
                    return@withContext Result.failure(IllegalArgumentException("Failed to decrypt backup archive. Ensure your E2E passphrase matches the passphrase used to create this backup."))
                }
            }

            progressListener?.invoke(85)
            val importResult = backupImporter.importBackup(Uri.fromFile(importFile), ImportStrategy.REPLACE_ALL)
            progressListener?.invoke(100)

            // Cleanup temp files
            if (importFile.exists()) importFile.delete()

            when (importResult) {
                is ImportResult.Success -> Result.success(importResult)
                is ImportResult.Error -> Result.failure(Exception(importResult.message))
            }
        } catch (e: Exception) {
            Log.e("CloudBackupManager", "Restore error", e)
            Result.failure(e)
        }
    }
    /**
     * Delete a remote backup snapshot.
     */
    suspend fun deleteBackup(fileInfo: CloudFileInfo): Result<Boolean> = withContext(Dispatchers.IO) {
        val (provider, providerConfig) = getActiveProvider()
            ?: return@withContext Result.failure(IllegalStateException("No active or configured cloud storage provider."))

        provider.deleteFile(fileInfo.path, providerConfig)
    }

    /**
     * Delete everything from the cloud storage (currently only supported on Google Drive).
     */
    suspend fun deleteEverythingFromCloud(): Result<Boolean> = withContext(Dispatchers.IO) {
        val (provider, providerConfig) = getActiveProvider()
            ?: return@withContext Result.failure(IllegalStateException("No active or configured cloud storage provider."))

        if (provider is GoogleDriveStorageProvider) {
            provider.deleteEverything(providerConfig)
        } else {
            Result.failure(UnsupportedOperationException("Clear operation only supported for Google Drive."))
        }
    }

    /**
     * Delete multi-device synced accounts data from cloud storage.
     */
    suspend fun deleteSyncedDataFromCloud(): Result<Boolean> = withContext(Dispatchers.IO) {
        val (provider, providerConfig) = getActiveProvider()
            ?: return@withContext Result.failure(IllegalStateException("No active or configured cloud storage provider."))

        if (provider is GoogleDriveStorageProvider) {
            provider.deleteSyncedData(providerConfig)
        } else {
            Result.failure(UnsupportedOperationException("Delete sync operation only supported for Google Drive."))
        }
    }

    /**
     * Prune older backups keeping only up to maxBackups snapshots.
     */
    private suspend fun enforceRetentionPolicy(
        maxBackups: Int,
        provider: CloudStorageProvider,
        config: CloudProviderConfig
    ) {
        if (maxBackups <= 0) return
        try {
            val listResult = provider.listFiles(BACKUPS_FOLDER, config)
            if (listResult.isSuccess) {
                val files = listResult.getOrNull() ?: return
                val backups = files.filter {
                    it.name.startsWith("Vittify_Backup_") && (it.name.endsWith(".zip") || it.name.endsWith(".enc"))
                }.sortedByDescending { it.lastModified }

                if (backups.size > maxBackups) {
                    val toDelete = backups.drop(maxBackups)
                    for (oldFile in toDelete) {
                        provider.deleteFile(oldFile.path, config)
                        Log.d("CloudBackupManager", "Retention pruned old backup: ${oldFile.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("CloudBackupManager", "Failed to enforce retention limit", e)
        }
    }
}
