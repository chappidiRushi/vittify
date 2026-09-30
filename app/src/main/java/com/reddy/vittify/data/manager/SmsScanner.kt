package com.reddy.vittify.data.manager

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Base64
import android.util.Log
import com.reddy.parser.core.bank.BankParserFactory
import com.reddy.vittify.data.mapper.toEntity
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.presentation.ui.features.sync.SyncMessageItem
import com.reddy.vittify.utils.capitalizeFirst
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class SmsScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "SmsScanner"
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE
        )
        private const val PROGRESS_REPORT_INTERVAL = 10
    }

    suspend fun scanMessages(
        forceResync: Boolean = false,
        onProgress: (scanned: Int, total: Int) -> Unit
    ): List<SyncMessageItem> = withContext(Dispatchers.IO) {
        val lastScanTimestamp = userPreferencesRepository.getLastScanTimestamp().first() ?: 0L
        val scanMonths = userPreferencesRepository.getSmsScanMonths()
        val scanAllTime = userPreferencesRepository.getSmsScanAllTime()
        val lastScanPeriod = userPreferencesRepository.getLastScanPeriod().first() ?: 0
        val now = System.currentTimeMillis()

        val needsFullScan = forceResync || lastScanTimestamp == 0L || scanAllTime || scanMonths > lastScanPeriod

        val scanStartTime = if (needsFullScan) {
            val calendar = Calendar.getInstance().apply {
                if (scanAllTime) {
                    add(Calendar.YEAR, -10)
                } else {
                    add(Calendar.MONTH, -scanMonths)
                }
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.timeInMillis
        } else {
            val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L)
            val periodLimit = Calendar.getInstance().apply {
                add(Calendar.MONTH, -scanMonths)
            }.timeInMillis

            maxOf(minOf(lastScanTimestamp, threeDaysAgo), periodLimit)
        }

        val totalMsgCount = getSmsAndRcsCount(scanStartTime)
        onProgress(0, totalMsgCount)

        val discoveredMessages = mutableListOf<SyncMessageItem>()
        val seenSignatures = mutableSetOf<String>()
        var processedCount = 0

        suspend fun processCandidate(sms: RawSms) {
            val senderUpper = sms.sender.uppercase()
            val isKnownBank = BankParserFactory.isKnownBankSender(sms.sender)
            if ((senderUpper.endsWith("-P") || senderUpper.endsWith("-G")) && !isKnownBank) {
                return
            }

            val matchingParsers = BankParserFactory.getParsers(sms.sender)
            if (matchingParsers.isEmpty()) return

            if (matchingParsers.any { it.isBalanceUpdateNotification(sms.body) }) {
                return
            }

            val parsedTransaction = matchingParsers.firstNotNullOfOrNull { parser ->
                parser.parse(sms.body, sms.sender, sms.timestamp)
            } ?: return

            val signature = parsedTransaction.generateTransactionId()
            if (seenSignatures.contains(signature)) return

            // Check if already saved in Room database
            val existing = transactionRepository.getTransactionBySmsBody(sms.body)
            if (existing != null) {
                return
            }

            // Check UPI reference duplicates
            val entity = parsedTransaction.toEntity()
            if (TransactionDeduplication.hasUpiReference(entity)) {
                val windowEnd = entity.dateTime
                val windowStart = windowEnd.minus(TransactionDeduplication.UPI_DUPLICATE_WINDOW)
                val upiCandidates = transactionRepository.getTransactionsByReferenceAndAmount(
                    reference = entity.reference!!,
                    amount = entity.amount,
                    accountId = entity.accountId,
                    startDate = windowStart,
                    endDate = windowEnd
                )
                if (upiCandidates.any { TransactionDeduplication.isSameUpiTransaction(it, entity) }) {
                    return
                }
            }

            seenSignatures.add(signature)
            discoveredMessages.add(
                SyncMessageItem(
                    id = signature,
                    parsedTransaction = parsedTransaction,
                    sender = sms.sender,
                    smsBody = sms.body,
                    timestamp = sms.timestamp,
                    isSelected = true
                )
            )
        }

        // 1. Read standard SMS
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                SMS_PROJECTION,
                "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), scanStartTime.toString()),
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)

                while (it.moveToNext() && coroutineContext.isActive) {
                    val raw = RawSms(
                        id = it.getLong(idIndex),
                        sender = it.getString(addressIndex) ?: "",
                        timestamp = it.getLong(dateIndex),
                        body = it.getString(bodyIndex) ?: ""
                    )
                    processCandidate(raw)
                    processedCount++

                    if (processedCount % PROGRESS_REPORT_INTERVAL == 0 || processedCount >= totalMsgCount) {
                        onProgress(processedCount, totalMsgCount)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SMS: ${e.message}", e)
        }

        // 2. Read RCS messages
        try {
            val scanStartTimeSeconds = scanStartTime / 1000
            val mmsCursor = context.contentResolver.query(
                Uri.parse("content://mms"),
                arrayOf("_id", "date", "tr_id"),
                "date >= ?",
                arrayOf(scanStartTimeSeconds.toString()),
                "date DESC"
            )

            mmsCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow("_id")
                val dateCol = cursor.getColumnIndexOrThrow("date")
                val trIdCol = cursor.getColumnIndex("tr_id")

                while (cursor.moveToNext() && coroutineContext.isActive) {
                    val trId = if (trIdCol >= 0) cursor.getString(trIdCol) ?: "" else ""
                    if (trId.startsWith("proto:")) {
                        val sender = extractRcsSender(trId)
                        if (sender != null) {
                            val senderUpper = sender.uppercase()
                            if (senderUpper.contains("PUNJAB NATIONAL BANK") ||
                                senderUpper.contains("DEPARTMENT OF POST") ||
                                senderUpper.contains("DOPBNK")
                            ) {
                                val messageId = cursor.getLong(idCol)
                                val date = cursor.getLong(dateCol)
                                var messageText = getRcsMessageText(messageId)
                                if (messageText != null && messageText.trim().startsWith("{")) {
                                    messageText = extractTextFromRcsJson(messageText)
                                }

                                if (!messageText.isNullOrBlank()) {
                                    val raw = RawSms(
                                        id = messageId,
                                        sender = sender,
                                        timestamp = date * 1000,
                                        body = messageText
                                    )
                                    processCandidate(raw)
                                }
                            }
                        }
                    }
                    processedCount++
                    if (processedCount % PROGRESS_REPORT_INTERVAL == 0 || processedCount >= totalMsgCount) {
                        onProgress(processedCount, totalMsgCount)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying RCS: ${e.message}", e)
        }

        onProgress(totalMsgCount, totalMsgCount)
        discoveredMessages.sortedByDescending { it.timestamp }
    }

    private fun getSmsAndRcsCount(scanStartTime: Long): Int {
        var total = 0
        try {
            val smsCursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), scanStartTime.toString()),
                null
            )
            smsCursor?.use { total += it.count }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting SMS: ${e.message}")
        }

        try {
            val scanStartTimeSeconds = scanStartTime / 1000
            val mmsCursor = context.contentResolver.query(
                Uri.parse("content://mms"),
                arrayOf("_id", "tr_id"),
                "date >= ?",
                arrayOf(scanStartTimeSeconds.toString()),
                null
            )
            mmsCursor?.use { cursor ->
                val trIdIndex = cursor.getColumnIndex("tr_id")
                while (cursor.moveToNext()) {
                    val trId = if (trIdIndex >= 0) cursor.getString(trIdIndex) ?: "" else ""
                    if (trId.startsWith("proto:")) {
                        val sender = extractRcsSender(trId)
                        if (sender != null) {
                            val senderUpper = sender.uppercase()
                            if (senderUpper.contains("PUNJAB NATIONAL BANK") ||
                                senderUpper.contains("DEPARTMENT OF POST") ||
                                senderUpper.contains("DOPBNK")
                            ) {
                                total++
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting RCS: ${e.message}")
        }
        return total
    }

    private fun extractRcsSender(trId: String): String? {
        return try {
            val base64Data = trId.removePrefix("proto:")
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val decodedString = String(decodedBytes)

            val agentPattern = Regex("""([a-z_]+)_[a-z0-9]+_agent@rbm\.goog""")
            agentPattern.find(decodedString)?.let { match ->
                return match.groupValues[1].split("_").joinToString(" ") { it.capitalizeFirst() }
            }

            val namePattern = Regex("""[\x12\x1a][\x00-\x20]([A-Za-z][A-Za-z\s]+)""")
            namePattern.find(decodedString)?.let { match ->
                val name = match.groupValues[1].trim()
                if (name.length in 4..49) return name
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun getRcsMessageText(messageId: Long): String? {
        return try {
            val partsCursor = context.contentResolver.query(
                Uri.parse("content://mms/part"),
                null,
                "mid = ?",
                arrayOf(messageId.toString()),
                null
            )
            partsCursor?.use { cursor ->
                while (cursor.moveToNext()) {
                    val partId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                    val ctIndex = cursor.getColumnIndex("ct")
                    val contentType = if (ctIndex >= 0) cursor.getString(ctIndex) ?: "" else ""

                    if (contentType.startsWith("text/") || contentType == "application/smil") {
                        val textIndex = cursor.getColumnIndex("text")
                        if (textIndex >= 0) {
                            val text = cursor.getString(textIndex)
                            if (!text.isNullOrEmpty()) return text
                        }

                        val dataIndex = cursor.getColumnIndex("_data")
                        if (dataIndex >= 0) {
                            val dataPath = cursor.getString(dataIndex)
                            if (!dataPath.isNullOrEmpty()) {
                                try {
                                    val partUri = Uri.parse("content://mms/part/$partId")
                                    val inputStream = context.contentResolver.openInputStream(partUri)
                                    val text = inputStream?.bufferedReader()?.use { it.readText() }
                                    if (!text.isNullOrEmpty()) return text
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTextFromRcsJson(json: String): String? {
        return try {
            val jsonObject = JSONObject(json)
            val texts = mutableListOf<String>()

            fun extractTexts(obj: Any?, depth: Int = 0) {
                if (depth > 10) return
                when (obj) {
                    is JSONObject -> {
                        val textFields = listOf("text", "message", "body", "title", "description", "content", "caption")
                        for (field in textFields) {
                            if (obj.has(field)) {
                                val value = obj.getString(field)
                                if (value.isNotEmpty() && !value.startsWith("{")) {
                                    texts.add(value)
                                }
                            }
                        }
                        obj.keys().forEach { key ->
                            if (key !in listOf("media", "suggestions", "postback", "urlAction")) {
                                try {
                                    extractTexts(obj.get(key), depth + 1)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            extractTexts(obj.get(i), depth + 1)
                        }
                    }
                }
            }

            if (jsonObject.has("text")) return jsonObject.getString("text")
            if (jsonObject.has("message")) {
                val message = jsonObject.getJSONObject("message")
                if (message.has("text")) return message.getString("text")
            }

            extractTexts(jsonObject)
            if (texts.isNotEmpty()) texts.distinct().joinToString(" | ") else null
        } catch (_: Exception) {
            json
        }
    }

    private data class RawSms(
        val id: Long,
        val sender: String,
        val timestamp: Long,
        val body: String
    )
}
