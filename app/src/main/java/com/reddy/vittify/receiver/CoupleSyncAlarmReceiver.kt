package com.reddy.vittify.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reddy.vittify.data.sync.scheduler.CoupleSyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives exact UTC top-of-the-hour alarm broadcasts to initiate periodic couple sync.
 * Re-arms the next hourly alarm immediately and launches CoupleSyncWorker.
 */
class CoupleSyncAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun coupleSyncScheduler(): CoupleSyncScheduler
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Received hourly UTC couple sync alarm broadcast")

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )
        val scheduler = entryPoint.coupleSyncScheduler()

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // 1. Immediately re-arm the next hourly alarm so next window is secured
                try {
                    scheduler.scheduleNextHourlyAlarm()
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to schedule next hourly alarm", t)
                }

                // 2. Enqueue the worker to perform the 5-minute sync window
                try {
                    scheduler.enqueueSyncWorker()
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to enqueue CoupleSyncWorker", t)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "CoupleSyncAlarmReceiver"
    }
}

