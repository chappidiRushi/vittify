package com.reddy.vittify.data.cloud.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reddy.vittify.data.cloud.BackupSchedule
import com.reddy.vittify.data.cloud.engine.CloudBackupManager
import com.reddy.vittify.data.cloud.engine.CloudSyncEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class CloudBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val cloudBackupManager: CloudBackupManager,
    private val cloudSyncEngine: CloudSyncEngine
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "VittifyCloudBackupWorker"

        fun updateSchedule(context: Context, schedule: BackupSchedule) {
            val workManager = WorkManager.getInstance(context)
            if (schedule == BackupSchedule.MANUAL) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(
                schedule.intervalDays.toLong(), TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val activeProvider = cloudBackupManager.getActiveProvider()
        if (activeProvider == null) {
            Log.d("CloudBackupWorker", "No active cloud provider configured, cancelling work.")
            return@withContext Result.success()
        }

        try {
            Log.d("CloudBackupWorker", "Starting scheduled background backup and synchronization...")
            val backupResult = cloudBackupManager.performBackup()
            val syncResult = cloudSyncEngine.synchronize()

            if (backupResult.isSuccess && syncResult.isSuccess) {
                Log.d("CloudBackupWorker", "Scheduled cloud backup & sync completed successfully.")
                Result.success()
            } else {
                Log.w("CloudBackupWorker", "Scheduled cloud operation had errors. Backup: ${backupResult.isSuccess}, Sync: ${syncResult.isSuccess}")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e("CloudBackupWorker", "Exception in background worker", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
