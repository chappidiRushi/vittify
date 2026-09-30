package com.reddy.vittify.data.sync.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.reddy.vittify.R
import com.reddy.vittify.VittifyApplication
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.dao.SyncChangeDao
import com.reddy.vittify.data.sync.engine.P2pSyncEngine
import com.reddy.vittify.data.sync.transport.ConnectionState
import com.reddy.vittify.data.sync.transport.P2pTransportCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Worker that runs during the hourly periodic sync window.
 * Woken up at the top of the UTC hour, attempts to connect to the partner's device for up to 5 minutes.
 * If connected, performs bidirectional data exchange. If not connected after 5 minutes, stops trying
 * and awaits the next sync window.
 */
@HiltWorker
class CoupleSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val p2pSyncEngine: P2pSyncEngine,
    private val transportCoordinator: P2pTransportCoordinator,
    private val syncChangeDao: SyncChangeDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "couple_hourly_periodic_sync"
        private const val TAG = "CoupleSyncWorker"
        private const val NOTIFICATION_CHANNEL_ID = "couple_background_sync_channel"
        private const val NOTIFICATION_ID = 8089
        private const val MAX_CONNECT_ATTEMPT_MILLIS = 5 * 60 * 1000L // 5 minutes
        private const val RETRY_INTERVAL_MILLIS = 15_000L // 15 seconds
        private const val MIN_SYNC_LINGER_MILLIS = 15_000L // 15 seconds grace period for bidirectional sync
        private const val MAX_SYNC_WAIT_MILLIS = 45_000L // 45 seconds max sync wait
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannelIfNeeded()
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Couple Sync")
            .setContentText("Syncing finances with partner...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Couple Background Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during periodic background sync with partner"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val isCoupleEnabled = p2pPreferences.isCoupleTrackingEnabled()
        val isHourlyEnabled = p2pPreferences.isHourlySyncEnabled()
        val clusterId = p2pPreferences.getClusterId()

        if (!isCoupleEnabled || !isHourlyEnabled || clusterId.isNullOrBlank()) {
            Log.d(TAG, "Hourly couple sync is disabled or device is not paired. Skipping background work.")
            return@withContext Result.success()
        }

        Log.d(TAG, "Starting hourly UTC couple sync window (5-minute connection attempt)...")

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Vittify:CoupleSyncWorkerWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(MAX_CONNECT_ATTEMPT_MILLIS + 30_000L) // 5.5 minutes safety timeout
        }

        try {
            // Ensure the engine and transports are started
            p2pSyncEngine.startSync(force = false)
            p2pSyncEngine.startSync(force = true)
            transportCoordinator.retryAll()

            val startTime = System.currentTimeMillis()
            var connected = false
            var lastRetryTime = startTime

            // Keep trying to connect for up to 5 minutes
            while (System.currentTimeMillis() - startTime < MAX_CONNECT_ATTEMPT_MILLIS && !isStopped) {
                val state = p2pSyncEngine.connectionState.value
                if (state is ConnectionState.Connected) {
                    connected = true
                    Log.d(TAG, "Successfully connected to partner device during periodic sync window!")
                    break
                }

                val now = System.currentTimeMillis()
                if (now - lastRetryTime >= RETRY_INTERVAL_MILLIS) {
                    lastRetryTime = now
                    val remainingSeconds = (MAX_CONNECT_ATTEMPT_MILLIS - (now - startTime)) / 1000
                    Log.d(TAG, "Still attempting connection to partner... (${remainingSeconds}s remaining)")
                    p2pSyncEngine.retryConnection()
                }

                delay(1000L)
            }

            if (!connected) {
                Log.d(TAG, "Partner device not connected after 5 minutes. Stopping attempts and waiting for next sync window.")
                return@withContext Result.success()
            }

            // Connected within 5 minutes! Trigger sync exchange
            Log.d(TAG, "Triggering data exchange with partner...")
            p2pSyncEngine.triggerManualSync()

            // Allow time for bidirectional exchange and ACKs to settle
            val syncStartTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - syncStartTime < MAX_SYNC_WAIT_MILLIS && !isStopped) {
                delay(1000L)
                val elapsed = System.currentTimeMillis() - syncStartTime
                val isSyncing = p2pSyncEngine.isSyncingFlow.value
                val pendingCount = syncChangeDao.getPendingChanges().size

                if (elapsed >= MIN_SYNC_LINGER_MILLIS && !isSyncing && pendingCount == 0) {
                    Log.d(TAG, "Data exchange and ACKs complete between both devices.")
                    break
                }
            }

            Log.d(TAG, "Hourly couple sync window completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during hourly couple sync", e)
            Result.success()
        } finally {
            val isAppInForeground = (applicationContext as? VittifyApplication)?.isAppInForeground == true
            if (!isAppInForeground) {
                Log.d(TAG, "App is in background; stopping sync engine to conserve battery until next window.")
                p2pSyncEngine.stopSync()
            } else {
                Log.d(TAG, "App is currently in foreground; preserving active sync session.")
            }

            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing wake lock", e)
            }
        }
    }
}

