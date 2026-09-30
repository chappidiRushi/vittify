package com.reddy.vittify.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveAutoDeleteScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ArchiveAutoDeleteScheduler"
    }

    private val workManager by lazy { WorkManager.getInstance(context) }

    fun schedulePeriodicCleanup() {
        Log.d(TAG, "Scheduling periodic archive auto-delete work (24 hours interval)")
        val periodicRequest = PeriodicWorkRequestBuilder<ArchiveAutoDeleteWorker>(
            24, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            ArchiveAutoDeleteWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun triggerImmediateCleanup() {
        Log.d(TAG, "Triggering immediate one-time archive cleanup work")
        val oneTimeRequest = OneTimeWorkRequestBuilder<ArchiveAutoDeleteWorker>().build()
        workManager.enqueueUniqueWork(
            "${ArchiveAutoDeleteWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}

