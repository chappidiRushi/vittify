package com.reddy.vittify.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.reddy.vittify.MainActivity
import com.reddy.vittify.VittifyApplication
import com.reddy.vittify.R
import com.reddy.vittify.data.manager.SmsTransactionProcessor
import com.reddy.vittify.data.manager.TransactionNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that intercepts incoming SMS messages in real-time
 * and processes them for transaction data using the shared SmsTransactionProcessor.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsBroadcastReceiverEntryPoint {
        fun smsTransactionProcessor(): SmsTransactionProcessor
        fun transactionRepository(): com.reddy.vittify.data.repository.TransactionRepository
        fun transactionNotificationManager(): TransactionNotificationManager
    }

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
        const val ACTION_EDIT_TRANSACTION = "com.vittifyai.tracker.ACTION_EDIT_TRANSACTION"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val CHANNEL_ID = "transaction_notifications_v2"
        const val CHANNEL_NAME = "Transaction Notifications"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // Combine multi-part SMS messages with their timestamps
        data class SmsData(val body: StringBuilder, var timestamp: Long)
        val smsMap = mutableMapOf<String, SmsData>()
        for (message in messages) {
            val sender = message.originatingAddress ?: continue
            val body = message.messageBody ?: continue
            val timestamp = message.timestampMillis

            val existing = smsMap.getOrPut(sender) { SmsData(StringBuilder(), timestamp) }
            existing.body.append(body)
            // Use the earliest timestamp for multi-part messages
            if (timestamp < existing.timestamp) {
                existing.timestamp = timestamp
            }
        }

        // Get the processor and notification manager via Hilt EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SmsBroadcastReceiverEntryPoint::class.java
        )
        val processor = entryPoint.smsTransactionProcessor()
        val notificationManager = entryPoint.transactionNotificationManager()

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                // Process each unique SMS
                for ((sender, smsData) in smsMap) {
                    val body = smsData.body.toString()
                    val timestamp = smsData.timestamp
                    Log.d(TAG, "Received SMS from: $sender at timestamp: $timestamp")

                    try {
                        // Use the shared processor to parse and save the transaction
                        val result = processor.processAndSaveTransaction(sender, body, timestamp)

                        if (result.success && result.transactionId != null) {
                            Log.d(TAG, "Transaction saved with ID: ${result.transactionId}")

                            // Show notification
                            notificationManager.showTransactionNotification(
                                transactionId = result.transactionId,
                                sender = sender,
                                body = body,
                                timestamp = timestamp
                            )
                        } else {
                            Log.d(TAG, "Transaction not saved: ${result.reason}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing SMS from $sender", e)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        return try {
            val application = context.applicationContext as? VittifyApplication
            application?.isAppInForeground ?: false
        } catch (e: Exception) {
            false
        }
    }
}
