package com.reddy.vittify.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ArchiveAutoDeleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "ArchiveAutoDeleteWorker"
        const val WORK_NAME = "archive_auto_delete_work"
    }

    override suspend fun doWork(): Result {
        return try {
            val enabled = userPreferencesRepository.getAutoDeleteArchivedEnabled()
            if (!enabled) {
                Log.d(TAG, "Archive auto-delete is disabled in preferences")
                return Result.success()
            }

            val retentionDays = userPreferencesRepository.getAutoDeleteArchivedDays()
            Log.d(TAG, "Running archive auto-delete for transactions older than $retentionDays days")
            val deletedCount = transactionRepository.autoDeleteArchivedTransactions(retentionDays)
            Log.d(TAG, "Auto-deleted $deletedCount expired archived transactions")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during archive auto-delete", e)
            Result.retry()
        }
    }
}

