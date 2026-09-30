package com.reddy.vittify.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.reddy.vittify.MainActivity
import com.reddy.vittify.R
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receiver that handles the alarm for daily reminders.
 */
class ReminderReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderReceiverEntryPoint {
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Reminder alarm received")

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderReceiverEntryPoint::class.java
        )
        val repository = entryPoint.userPreferencesRepository()

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val scanEnabled = repository.scanNewTransactionsEnabled.first()
                val upcomingEnabled = repository.upcomingNotificationsEnabled.first()

                if (scanEnabled || upcomingEnabled) {
                    sendReminderNotification(context)
                } else {
                    Log.d(TAG, "Notifications are disabled, skipping")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing reminder alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendReminderNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily reminders to check transactions"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily Transaction Update")
            .setContentText("Don't forget to check your latest transactions and upcoming payments!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Daily reminder notification sent via AlarmManager")
    }

    companion object {
        private const val TAG = "ReminderReceiver"
        private const val NOTIFICATION_ID = 100
        const val REMINDER_CHANNEL_ID = "daily_reminders"
        const val REMINDER_CHANNEL_NAME = "Daily Reminders"
    }
}
