package com.reddy.vittify.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.reddy.parser.core.bank.BankParserFactory
import com.reddy.vittify.MainActivity
import com.reddy.vittify.R
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.receiver.NotificationActionReceiver
import com.reddy.vittify.receiver.SmsBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages posting transaction notifications with rich interactive actions
 * (category switching chips, inline category reply, and tap-to-edit).
 */
@Singleton
class TransactionNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository
) {
    companion object {
        private const val TAG = "TransactionNotificationManager"
    }

    /**
     * Prepares and displays a notification for a processed transaction.
     *
     * @param transactionId ID of the saved transaction in the database
     * @param sender SMS sender address
     * @param body SMS text content
     * @param timestamp SMS timestamp in milliseconds
     */
    suspend fun showTransactionNotification(
        transactionId: Long,
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        try {
            val parsers = BankParserFactory.getParsers(sender).ifEmpty {
                BankParserFactory.getAllParsers()
            }
            val parsedTransaction = parsers.firstNotNullOfOrNull { parser ->
                parser.parse(body, sender, timestamp)
            }

            val savedTransaction = transactionRepository.getTransactionById(transactionId)

            val amount = parsedTransaction?.amount?.toString()
                ?: savedTransaction?.amount?.toString()
                ?: "0.00"
            val merchant = savedTransaction?.merchantName?.takeIf { it.isNotBlank() }
                ?: parsedTransaction?.merchant
                ?: "Unknown"
            val type = savedTransaction?.transactionType?.name
                ?: parsedTransaction?.type?.name
                ?: "EXPENSE"
            val bankName = parsedTransaction?.bankName
                ?: savedTransaction?.smsSender
                ?: "Bank"
            val category = savedTransaction?.category?.takeIf { it.isNotBlank() }
                ?: "Miscellaneous"

            showNotification(
                transactionId = transactionId,
                amount = amount,
                merchant = merchant,
                type = type,
                bankName = bankName,
                category = category,
                body = body
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing transaction notification", e)
        }
    }

    /**
     * Builds and shows the transaction notification with quick actions and inline reply.
     */
    suspend fun showNotification(
        transactionId: Long,
        amount: String,
        merchant: String,
        type: String,
        bankName: String,
        category: String,
        body: String? = null
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Delete legacy channel with default importance if present so high importance takes effect
            try {
                notificationManager.deleteNotificationChannel("transaction_notifications")
            } catch (e: Exception) {
                // Ignore if not present
            }

            // Ensure notification channel exists with HIGH importance for heads-up alert
            val channel = NotificationChannel(
                SmsBroadcastReceiver.CHANNEL_ID,
                SmsBroadcastReceiver.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new transactions"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)

            // Create intent to open transaction detail in MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                action = SmsBroadcastReceiver.ACTION_EDIT_TRANSACTION
                putExtra(SmsBroadcastReceiver.EXTRA_TRANSACTION_ID, transactionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                transactionId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Format notification content with emoji
            val typeEmoji = when (type) {
                "EXPENSE" -> "💸"
                "INCOME" -> "💰"
                "CREDIT" -> "💳"
                "TRANSFER" -> "🔄"
                "INVESTMENT" -> "📈"
                else -> "💵"
            }

            val title = "$typeEmoji $amount - $merchant"
            val content = "$category • $bankName"

            // Get top 3 categories by usage (personalized for user)
            val topCategories = try {
                transactionRepository.getTopCategoriesByUsage(3)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching top categories", e)
                listOf("Food & Drinks", "Shopping", "Transport")
            }

            val bigTextStyle = NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(if (!body.isNullOrBlank()) "$content\n\n$body" else content)

            // Build notification with category quick actions and high priority
            val notificationBuilder = NotificationCompat.Builder(context, SmsBroadcastReceiver.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(bigTextStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Add quick action buttons for top categories (only if different from current)
            // Limit to at most 2 so that with Search action we don't exceed Android's 3-action limit
            val notificationId = transactionId.toInt()
            topCategories.filter { it != category }.take(2).forEachIndexed { index, topCategory ->
                val categoryIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_CHANGE_CATEGORY
                    putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transactionId)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(NotificationActionReceiver.EXTRA_NEW_CATEGORY, topCategory)
                }

                val categoryPendingIntent = PendingIntent.getBroadcast(
                    context,
                    transactionId.toInt() + index + 1, // Unique request code
                    categoryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                notificationBuilder.addAction(
                    0, // No icon for actions
                    topCategory,
                    categoryPendingIntent
                )
            }

            // Add Search action to open the floating quick category picker overlay (Option 3)
            val searchPickerIntent = Intent(context, com.reddy.vittify.presentation.ui.features.categories.QuickCategoryPickerActivity::class.java).apply {
                putExtra(com.reddy.vittify.presentation.ui.features.categories.QuickCategoryPickerViewModel.EXTRA_TRANSACTION_ID, transactionId)
                putExtra(com.reddy.vittify.presentation.ui.features.categories.QuickCategoryPickerViewModel.EXTRA_NOTIFICATION_ID, notificationId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val searchPickerPendingIntent = PendingIntent.getActivity(
                context,
                transactionId.toInt() + 100,
                searchPickerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder.addAction(
                0,
                context.getString(R.string.action_search_category),
                searchPickerPendingIntent
            )

            val notification = notificationBuilder.build()
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Posted transaction notification for ID: $transactionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }
}

