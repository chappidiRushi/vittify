package com.reddy.vittify.presentation.ui.features.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.reddy.vittify.MainActivity
import com.reddy.vittify.R
import com.reddy.vittify.receiver.SmsBroadcastReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.repository.UnrecognizedSmsRepository
import com.reddy.vittify.data.cloud.security.CloudCredentialStore
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.backup.BackupExporter
import com.reddy.vittify.data.backup.BackupImporter
import com.reddy.vittify.data.backup.ExportResult
import com.reddy.vittify.data.backup.ImportResult
import com.reddy.vittify.data.backup.ImportStrategy
import com.reddy.vittify.data.repository.MerchantMappingRepository
import com.reddy.vittify.data.webhook.WebhookSyncScheduler
import com.reddy.vittify.domain.repository.RuleRepository
import android.content.Intent
import androidx.core.content.FileProvider
import com.reddy.vittify.core.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.net.URLEncoder
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import com.reddy.vittify.data.manager.SmsScanManager
import java.io.File
import javax.inject.Inject
import androidx.core.net.toUri
import com.reddy.vittify.data.database.VittifyDatabase
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CardRepository
import com.reddy.vittify.data.repository.BudgetRepository
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.CardType
import com.reddy.vittify.data.database.entity.BudgetEntity
import com.reddy.vittify.data.database.entity.BudgetPeriod
import com.reddy.vittify.data.database.entity.BudgetTrackType
import com.reddy.vittify.data.database.entity.BudgetType
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.SubscriptionState
import com.reddy.vittify.data.manager.SmsTransactionProcessor
import com.reddy.vittify.data.manager.TransactionNotificationManager
import com.reddy.vittify.utils.IconResolutionUtils
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val budgetRepository: BudgetRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val ruleRepository: RuleRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter,
    private val database: VittifyDatabase,
    private val webhookSyncScheduler: WebhookSyncScheduler,
    private val cloudCredentialStore: CloudCredentialStore,
    private val smsScanManager: SmsScanManager,
    private val smsTransactionProcessor: SmsTransactionProcessor,
    private val transactionNotificationManager: TransactionNotificationManager,
    private val randomDataGeneratorService: com.reddy.vittify.data.generator.RandomDataGeneratorService,
) : ViewModel() {

    val databaseVersion: Int
        get() = database.openHelper.readableDatabase.version

    suspend fun simulateSmsTransaction(sender: String, body: String): SmsTransactionProcessor.ProcessingResult {
        val effectiveSender = sender.ifBlank { "HDFCBK" }
        val timestamp = System.currentTimeMillis()
        val result = smsTransactionProcessor.processAndSaveTransaction(
            sender = effectiveSender,
            body = body,
            timestamp = timestamp
        )
        if (result.success && result.transactionId != null) {
            transactionNotificationManager.showTransactionNotification(
                transactionId = result.transactionId,
                sender = effectiveSender,
                body = body,
                timestamp = timestamp
            )
        } else {
            val existing = transactionRepository.getTransactionBySmsBody(body)
            if (existing != null) {
                transactionNotificationManager.showTransactionNotification(
                    transactionId = existing.id,
                    sender = effectiveSender,
                    body = body,
                    timestamp = timestamp
                )
            }
        }
        return result
    }

    val userPreferences = userPreferencesRepository.userPreferences

    val totalTransactions = transactionRepository.getTransactionCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val archivedTransactionsCount = transactionRepository.getArchivedTransactionsCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val googleDriveEmail: StateFlow<String?> = cloudCredentialStore.googleDriveConfigFlow
        .map { it.accountEmail.ifBlank { null } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )


    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Developer mode state
    val isDeveloperModeEnabled = userPreferencesRepository.isDeveloperModeEnabled
    val isWebhookModeEnabled = userPreferencesRepository.isWebhookModeEnabled
    val isTokenInfoEnabled = userPreferencesRepository.isTokenInfoEnabled
    val isTestNotificationAlertsEnabled = userPreferencesRepository.isTestNotificationAlertsEnabled

    // SMS scan period state
    val smsScanMonths = userPreferencesRepository.smsScanMonths
    val smsScanAllTime = userPreferencesRepository.smsScanAllTime

    // Unrecognized SMS state
    val unreportedSmsCount = unrecognizedSmsRepository.getUnreportedCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val isSampleDataSeeded: Flow<Boolean> = userPreferencesRepository.isSampleDataSeeded

    init {
    }

    fun toggleWebhookMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setWebhookModeEnabled(enabled)
            try {
                webhookSyncScheduler.applyScheduling()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e("SettingsViewModel", "Failed to apply webhook scheduling", t)
            }
        }
    }

    fun toggleTokenInfoMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTokenInfoEnabled(enabled)
        }
    }

    fun toggleTestNotificationAlerts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTestNotificationAlertsEnabled(enabled)
            if (enabled) {
                sendTestNotification()
            }
        }
    }

    fun toggleSampleData(enabled: Boolean) {
        if (enabled) {
            seedSampleData()
        } else {
            removeSampleData()
        }
    }

    private fun sendTestNotification() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Ensure channel exists
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

            // Create intent to open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, SmsBroadcastReceiver.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Test Notification")
                .setContentText("This is a test notification from Vittify.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(999, notification) // ID 999 for test
            Log.d("SettingsViewModel", "Sent test notification")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error sending test notification", e)
        }
    }

    fun updateSmsScanMonths(months: Int) {
        viewModelScope.launch {
            val currentMonths = userPreferencesRepository.getSmsScanMonths()

            // If increasing scan period, reset scan timestamp to force full scan
            if (months > currentMonths) {
                userPreferencesRepository.setLastScanTimestamp(0L)
                Log.d("SettingsViewModel", "Scan period increased from $currentMonths to $months months - will perform full scan")
            }

            userPreferencesRepository.updateSmsScanMonths(months)
        }
    }

    fun updateSmsScanAllTime(allTime: Boolean) {
        viewModelScope.launch {
            // If enabling all time scanning, reset scan timestamp to force full scan
            if (allTime) {
                userPreferencesRepository.setLastScanTimestamp(0L)
                Log.d("SettingsViewModel", "All time scanning enabled - will perform full scan")
            }

            userPreferencesRepository.updateSmsScanAllTime(allTime)
        }
    }

    val isScanningSms: StateFlow<Boolean> = smsScanManager.getSmsScanWorkInfo()
        .asFlow()
        .map { workInfos ->
            workInfos?.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } == true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun rescanSms() {
        viewModelScope.launch {
            userPreferencesRepository.setLastScanTimestamp(0L)
            smsScanManager.startSmsLoggingScan()
        }
    }

    fun openUnrecognizedSmsReport(context: Context) {
        viewModelScope.launch {
            try {
                val firstUnreported = unrecognizedSmsRepository.getFirstUnreported()

                if (firstUnreported != null) {
                    val issueTitle = "[Unrecognized SMS] From: ${firstUnreported.sender}"
                    val issueBody = """
                        ### Unrecognized SMS Details
                        - **Sender:** ${firstUnreported.sender}
                        - **Date:** ${firstUnreported.receivedAt}
                        
                        ### Original SMS
                        ```
                        ${firstUnreported.smsBody}
                        ```
                        
                        ### Expected Behavior
                        _Describe how this SMS should have been parsed_
                    """.trimIndent()

                    val encodedTitle = URLEncoder.encode(issueTitle, "UTF-8")
                    val encodedBody = URLEncoder.encode(issueBody, "UTF-8")

                    val url = "https://github.com/RReddy/Vittify/issues/new?title=$encodedTitle&body=$encodedBody"

                    // Open in browser
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)

                    // Mark as reported
                    unrecognizedSmsRepository.markAsReported(listOf(firstUnreported.id))

                    Log.d("SettingsViewModel", "Opened report for unrecognized SMS from: ${firstUnreported.sender}")
                } else {
                    Log.d("SettingsViewModel", "No unreported SMS messages found")
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error opening unrecognized SMS report", e)
            }
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                when (val result = backupExporter.exportBackup()) {
                    is ExportResult.Success -> {
                        // Store the file for later saving
                        _uiState.update { it.copy(
                            exportedBackupFile = result.file,
                            importExportMessage = "Backup created successfully! Choose where to save it."
                        ) }
                    }
                    is ExportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Export failed: ${result.message}") }
                        Log.e("SettingsViewModel", "Export failed: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Export error: ${e.message}") }
                Log.e("SettingsViewModel", "Export error", e)
            }
        }
    }

    fun saveBackupToFile(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value.exportedBackupFile?.let { file ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    _uiState.update { it.copy(
                        importExportMessage = "Backup saved successfully!",
                        exportedBackupFile = null
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Failed to save backup: ${e.message}") }
                Log.e("SettingsViewModel", "Error saving backup", e)
            }
        }
    }


    fun shareBackup() {
        _uiState.value.exportedBackupFile?.let { file ->
            shareBackupFile(file)
        }
    }

    private fun shareBackupFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Vittify Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error sharing backup file", e)
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(importExportMessage = "Importing backup...") }
                when (val result = backupImporter.importBackup(uri, ImportStrategy.MERGE)) {
                    is ImportResult.Success -> {
                        _uiState.update { it.copy(importExportMessage = "Import successful! Imported ${result.importedTransactions} transactions, ${result.importedCategories} categories. Skipped ${result.skippedDuplicates} duplicates.") }
                    }
                    is ImportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Import failed: ${result.message}") }
                        Log.e("SettingsViewModel", "Import failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Import error: ${e.message}") }
                Log.e("SettingsViewModel", "Import error", e)
            }
        }
    }

    fun clearImportExportMessage() {
        _uiState.update { it.copy(importExportMessage = null) }
    }

    fun seedSampleData(config: com.reddy.vittify.data.generator.RandomDataConfig = com.reddy.vittify.data.generator.RandomDataConfig()) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSeeding = true) }
                val result = randomDataGeneratorService.generateRandomData(config)
                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        seedMessage = "Generated ${result.accountsCreated} accounts & ${result.transactionsCreated} transactions (${config.historyDuration.label}) — all balances verified!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        seedMessage = "Failed to generate random data: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeSampleData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSeeding = true) }
                randomDataGeneratorService.clearSampleData()
                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        seedMessage = "Sample data removed successfully"
                    )
                }
                delay(3000)
                _uiState.update { it.copy(seedMessage = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        seedMessage = "Failed to remove sample data: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSeedMessage() {
        _uiState.update { it.copy(seedMessage = null) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            try {
                transactionRepository.deleteAllTransactions()
                accountBalanceRepository.deleteAllBalances()
                budgetRepository.deleteAllBudgets()
                subscriptionRepository.deleteAllSubscriptions()
                cardRepository.deleteAllCards()
                ruleRepository.deleteAllRules()
                merchantMappingRepository.deleteAllMappings()
                unrecognizedSmsRepository.deleteAll()
                
                // Clear some relevant preferences
                userPreferencesRepository.setSampleDataSeeded(false)
                
                _uiState.update { it.copy(seedMessage = "All data deleted successfully") }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error deleting data", e)
                _uiState.update { it.copy(seedMessage = "Failed to delete data: ${e.message}") }
            }
        }
    }
}
