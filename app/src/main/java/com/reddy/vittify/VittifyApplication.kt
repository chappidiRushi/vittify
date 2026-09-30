package com.reddy.vittify

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.reddy.vittify.data.repository.AppLockRepository
import com.reddy.vittify.data.webhook.WebhookSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VittifyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appLockRepository: AppLockRepository

    @Inject
    lateinit var webhookSyncScheduler: WebhookSyncScheduler

    @Inject
    lateinit var coupleSyncScheduler: com.reddy.vittify.data.sync.scheduler.CoupleSyncScheduler

    @Inject
    lateinit var archiveAutoDeleteScheduler: com.reddy.vittify.worker.ArchiveAutoDeleteScheduler

    // Route any unhandled coroutine exception to the CrashHandler so the crash screen
    // appears even when the crash originates inside a coroutine (which normally bypasses
    // Thread.UncaughtExceptionHandler).
    private val applicationExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("VittifyApplication", "Unhandled error in application scope", throwable)
        com.reddy.vittify.utils.CrashHandler.triggerCrash(this, throwable)
    }
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + applicationExceptionHandler
    )
    private var activityReferences = 0
    private var isInForeground = false

    /**
     * Publicly accessible flag to check if the app is in the foreground.
     * Used by SmsBroadcastReceiver to determine whether to show notifications.
     */
    @Volatile
    var isAppInForeground: Boolean = false
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Check if we are running in the isolated :crash process.
        // If so, we skip installing the crash handler (to avoid infinite loops)
        // and skip all background/database initialization which could fail due to multi-process locks.
        val processName = android.app.Application.getProcessName()
        if (processName != null && processName.endsWith(":crash")) {
            Log.d("VittifyApplication", "Started in :crash process. Skipping main initialization.")
            return
        }

        // Install crash handler first — must be before any other initialization
        com.reddy.vittify.utils.CrashHandler.install(this)
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
        registerActivityLifecycleCallbacks(AppLockLifecycleObserver())
        applicationScope.launch {
            try {
                webhookSyncScheduler.applyScheduling()
            } catch (e: Exception) {
                Log.e("VittifyApplication", "Error scheduling webhooks", e)
            }
            try {
                coupleSyncScheduler.applyScheduling()
            } catch (e: Exception) {
                Log.e("VittifyApplication", "Error scheduling couple sync", e)
            }
            try {
                archiveAutoDeleteScheduler.schedulePeriodicCleanup()
            } catch (e: Exception) {
                Log.e("VittifyApplication", "Error scheduling archive cleanup", e)
            }
        }
    }

    /**
     * Lifecycle observer to track app foreground/background state
     * This is used to trigger app lock when app returns from background
     */
    private inner class AppLockLifecycleObserver : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {
            activityReferences++
            if (!isInForeground) {
                // App came to foreground
                isInForeground = true
                isAppInForeground = true
                // Check if app should be locked when returning from background
                checkAndLockApp()
            }
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            activityReferences--
            if (activityReferences == 0) {
                // App went to background
                isInForeground = false
                isAppInForeground = false
                // Note: We don't need to do anything here
                // The lock state will be checked when app returns to foreground
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}

        private fun checkAndLockApp() {
            applicationScope.launch {
                try {
                    appLockRepository.checkAndLock()
                } catch (e: Exception) {
                    Log.e("VittifyApplication", "Error checking app lock status", e)
                }
            }
        }
    }
}
