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
import com.reddy.vittify.data.cloud.DeviceSyncSchedule
import com.reddy.vittify.data.cloud.engine.CloudBackupManager
import com.reddy.vittify.data.cloud.engine.CloudSyncEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class DeviceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val cloudBackupManager: CloudBackupManager,
    private val cloudSyncEngine: CloudSyncEngine
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "VittifyDeviceSyncWorker"

        fun updateSchedule(context: Context, schedule: DeviceSyncSchedule, backupSchedule: BackupSchedule) {
            val workManager = WorkManager.getInstance(context)

            val effectiveIntervalHours = when (schedule) {
                DeviceSyncSchedule.MANUAL -> 0
                DeviceSyncSchedule.EVERY_3_HOURS -> 3
                DeviceSyncSchedule.EVERY_6_HOURS -> 6
                DeviceSyncSchedule.DAILY -> 24
                DeviceSyncSchedule.WEEKLY -> 168
                DeviceSyncSchedule.SAME_AS_BACKUP -> when (backupSchedule) {
                    BackupSchedule.MANUAL -> 0
                    BackupSchedule.DAILY -> 24
                    BackupSchedule.WEEKLY -> 168
                    BackupSchedule.MONTHLY -> 720
                }
            }

            if (effectiveIntervalHours <= 0) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DeviceSyncWorker>(
                effectiveIntervalHours.toLong(), TimeUnit.HOURS
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
            Log.d("DeviceSyncWorker", "No active cloud provider configured, cancelling work.")
            return@withContext Result.success()
        }

        try {
            Log.d("DeviceSyncWorker", "Starting scheduled background device synchronization...")
            val syncResult = cloudSyncEngine.synchronize()

            if (syncResult.isSuccess) {
                Log.d("DeviceSyncWorker", "Scheduled device synchronization completed successfully.")
                Result.success()
            } else {
                Log.w("DeviceSyncWorker", "Scheduled device sync had errors.")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e("DeviceSyncWorker", "Exception in background device sync worker", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
