package com.reddy.vittify.data.sync.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.worker.CoupleSyncWorker
import com.reddy.vittify.receiver.CoupleSyncAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages scheduling for periodic hourly sync between couple devices.
 * Aligns wakeups to UTC hour boundaries (top of each hour, XX:00:00 UTC) so both devices
 * trigger connection attempts simultaneously even when the app is closed.
 * Also configures a persistent WorkManager PeriodicWorkRequest as a robust fallback.
 */
@Singleton
class CoupleSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val p2pPreferences: P2pSyncPreferencesRepository
) {
    companion object {
        const val WORK_NAME = "couple_hourly_periodic_sync"
        const val PERIODIC_WORK_NAME = "couple_hourly_periodic_sync_work"
        const val ALARM_REQUEST_CODE = 8088
        const val ACTION_COUPLE_PERIODIC_SYNC = "com.reddy.vittify.ACTION_COUPLE_PERIODIC_SYNC"
        const val ONE_HOUR_MILLIS = 3_600_000L
        private const val TAG = "CoupleSyncScheduler"
    }

    private val workManager = WorkManager.getInstance(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Applies current scheduling configuration based on preferences.
     * Arms the hourly UTC alarm if both couple tracking and hourly background sync are enabled
     * with a valid cluster ID; otherwise, cancels any pending alarms and work.
     */
    fun applyScheduling() {
        val isCoupleEnabled = p2pPreferences.isCoupleTrackingEnabled()
        val isHourlyEnabled = p2pPreferences.isHourlySyncEnabled()
        val clusterId = p2pPreferences.getClusterId()

        if (!isCoupleEnabled || !isHourlyEnabled || clusterId.isNullOrBlank()) {
            Log.d(TAG, "Hourly couple sync disabled or not configured; cancelling scheduling.")
            cancelHourlySync()
            return
        }

        scheduleNextHourlyAlarm()
        schedulePeriodicWork()
    }

    private fun schedulePeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicRequest = PeriodicWorkRequestBuilder<CoupleSyncWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    /**
     * Schedules the next exact alarm targeting the upcoming top of the UTC hour.
     */
    fun scheduleNextHourlyAlarm() {
        val isCoupleEnabled = p2pPreferences.isCoupleTrackingEnabled()
        val isHourlyEnabled = p2pPreferences.isHourlySyncEnabled()
        val clusterId = p2pPreferences.getClusterId()

        if (!isCoupleEnabled || !isHourlyEnabled || clusterId.isNullOrBlank()) {
            cancelHourlySync()
            return
        }

        val triggerAtMillis = calculateNextUtcHourMillis(System.currentTimeMillis())
        val intent = Intent(context, CoupleSyncAlarmReceiver::class.java).apply {
            action = ACTION_COUPLE_PERIODIC_SYNC
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        Log.d(TAG, "Scheduling next hourly UTC sync at $triggerAtMillis (exact=$canScheduleExact)")

        if (canScheduleExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: SCHEDULE_EXACT_ALARM not granted, falling back to inexact alarm", e)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Enqueues CoupleSyncWorker to run the 5-minute background sync window.
     * Enqueues CoupleSyncWorker to run the 5-minute background sync window immediately.
     */
    fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<CoupleSyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Cancels any pending hourly alarms and background work.
     */
    fun cancelHourlySync() {
        Log.d(TAG, "Cancelling hourly sync alarm and work")
        val intent = Intent(context, CoupleSyncAlarmReceiver::class.java).apply {
            action = ACTION_COUPLE_PERIODIC_SYNC
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        workManager.cancelUniqueWork(WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /**
     * Calculates the millisecond epoch timestamp of the next top of the UTC hour.
     * Since epoch milliseconds are universally reference to 1970-01-01T00:00:00Z (UTC),
     * this guarantees identical target timestamps across all time zones.
     */
    fun calculateNextUtcHourMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val currentHourStart = nowMillis - (nowMillis % ONE_HOUR_MILLIS)
        var nextHour = currentHourStart + ONE_HOUR_MILLIS
        // If the calculated next hour is within 10 seconds of now (e.g. alarm triggered slightly early),
        // advance to the subsequent hour to prevent immediate re-trigger loops.
        if (nextHour - nowMillis < 10_000L) {
            nextHour += ONE_HOUR_MILLIS
        }
        return nextHour
    }
}

