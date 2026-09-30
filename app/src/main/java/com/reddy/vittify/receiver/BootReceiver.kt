package com.reddy.vittify.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reddy.vittify.data.manager.NotificationScheduler
import com.reddy.vittify.data.sync.scheduler.CoupleSyncScheduler
import com.reddy.vittify.data.webhook.WebhookSyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receiver that reschedules alarms after device reboot.
 * Receiver that reschedules alarms after device reboot or time change.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun notificationScheduler(): NotificationScheduler
        fun webhookSyncScheduler(): WebhookSyncScheduler
        fun coupleSyncScheduler(): CoupleSyncScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) return
        Log.d(TAG, "Device rebooted or time changed ($action), rescheduling alarms")

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java
        )
        val scheduler = entryPoint.notificationScheduler()
        val webhookSyncScheduler = entryPoint.webhookSyncScheduler()
        val coupleSyncScheduler = entryPoint.coupleSyncScheduler()

        // BroadcastReceiver onReceive has only ~10s of guaranteed lifetime. Without goAsync()
        // the OS can kill the process before applyScheduling()/scheduleDailyReminder() finish
        // and alarms would never be restored after a reboot.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Each scheduler in its own try/catch so a failure in one (e.g. notification
                // channels missing on a fresh boot) doesn't prevent the other from re-arming.
                // Explicit catch lets CancellationException propagate to keep structured
                // concurrency intact if anything ever wires this scope to a parent.
                // Each scheduler in its own try/catch so a failure in one doesn't prevent others from re-arming.
                try {
                    scheduler.scheduleDailyReminder()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to reschedule daily reminder", t)
                }
                try {
                    webhookSyncScheduler.applyScheduling()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to re-apply webhook scheduling", t)
                }
                try {
                    coupleSyncScheduler.applyScheduling()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to re-apply couple sync scheduling", t)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
