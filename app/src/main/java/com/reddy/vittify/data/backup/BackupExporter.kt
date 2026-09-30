package com.reddy.vittify.data.backup

import android.content.Context
import android.os.Build
import com.google.gson.GsonBuilder
import com.reddy.vittify.BuildConfig
import android.net.Uri
import com.reddy.vittify.data.database.VittifyDatabase
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.WebhookRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.FileOutputStream
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import com.reddy.vittify.data.database.entity.*
import androidx.core.net.toUri
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import java.security.MessageDigest

@Singleton
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: VittifyDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val webhookRepository: WebhookRepository,
    private val p2pPreferences: P2pSyncPreferencesRepository
) {
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalTypeAdapter())
        .create()
    
    /**
     * Export complete app data to a backup file
     */
    suspend fun exportBackup(
        config: BackupConfiguration = BackupConfiguration()
    ): ExportResult {
        return try {
            // Collect all data
            var backup = createBackup(config)
            
            // Compute checksum over canonical JSON (without checksum field)
            val canonicalJson = gson.toJson(backup.copy(checksum = ""))
            val digest = MessageDigest.getInstance("SHA-256")
            val checksumBytes = digest.digest(canonicalJson.toByteArray(Charsets.UTF_8))
            val checksum = checksumBytes.joinToString("") { "%02x".format(it) }
            
            // Embed checksum and re-serialize
            backup = backup.copy(checksum = checksum)
            val backupJson = gson.toJson(backup)
            
            // Create backup file
            val file = createBackupFile()
            
            ZipOutputStream(FileOutputStream(file)).use { zipOut ->
                // Write JSON to file
                val jsonEntry = ZipEntry("backup.json")
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(backupJson.toByteArray())
                zipOut.closeEntry()

                // Write Attachments
                if (config.privacy == ExportPrivacy.FULL && config.includeTransactionalData) {
                    val filesDir = context.filesDir
                    // Collect all unique attachment paths
                    val allAttachments = mutableSetOf<String>()
                    for (txn in backup.database.transactions) {
                        for (part in txn.attachments.split(",")) {
                            if (part.isNotBlank()) allAttachments.add(part)
                        }
                    }

                    allAttachments.forEach { path ->
                        // path from DB is like "attachments/filename.ext"
                        val attachmentFile = File(filesDir, path)
                        if (attachmentFile.exists()) {
                            // Use the path directly as entry name to preserve structure
                            val entry = ZipEntry(path)
                            zipOut.putNextEntry(entry)
                            attachmentFile.inputStream().use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }

                // Write Profile Images
                if (config.includeProfileData) {
                    val prefs = userPreferencesRepository.userPreferences.first()
                    
                    prefs.profileImageUri?.let { uriStr ->
                        copyToZip(uriStr, "profile/profile_image", zipOut)
                    }
                }
            }
            
            ExportResult.Success(file)
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Create backup data structure
     */
    private suspend fun createBackup(config: BackupConfiguration): VittifyBackup {
        val currentDeviceId = p2pPreferences.getDeviceId()
        val partnerId = p2pPreferences.getPartnerUserId()

        fun isUserOwned(ownerId: String): Boolean {
            if (!partnerId.isNullOrBlank() && ownerId == partnerId) return false
            return ownerId.isBlank() || currentDeviceId.isBlank() || ownerId == currentDeviceId
        }

        // Get all database data, filtered to only include current user's data (no partner data)
        val rawTransactions = if (config.includeTransactionalData) database.transactionDao().getAllTransactionsIncludingDeleted().first() else emptyList()
        val transactions = rawTransactions.filter { isUserOwned(it.ownerId) }

        val categories = if (config.includeProfileData) database.categoryDao().getAllCategories().first() else emptyList()

        val rawCards = if (config.includeProfileData || config.includeTransactionalData) database.cardDao().getAllCards().first() else emptyList()
        val cards = rawCards.filter { isUserOwned(it.ownerId) }

        val rawAccountBalances = if (config.includeTransactionalData || config.includeProfileData) database.accountBalanceDao().getAllBalances().first() else emptyList()
        val accountBalances = rawAccountBalances.filter { isUserOwned(it.ownerId) }

        val userTxnIds = transactions.map { it.id }.toSet()
        val rawTransactionItems = if (config.includeTransactionalData) database.transactionItemDao().getAllItems().first() else emptyList()
        val transactionItems = rawTransactionItems.filter { it.transactionId in userTxnIds }

        val rawSubscriptions = if (config.includeBudgets) database.subscriptionDao().getAllSubscriptions().first() else emptyList()
        val subscriptions = rawSubscriptions.filter { isUserOwned(it.ownerId) }

        val merchantMappings = if (config.includeProfileData) database.merchantMappingDao().getAllMappings().first() else emptyList()
        val unrecognizedSms = if (config.includeTransactionalData) database.unrecognizedSmsDao().getAllUnrecognizedSms().first() else emptyList()

        val rawBudgets = if (config.includeBudgets) database.budgetDao().getAllBudgets().first() else emptyList()
        val budgets = rawBudgets.filter { isUserOwned(it.ownerId) }

        val userBudgetIds = budgets.map { it.id }.toSet()
        val rawBudgetCategoryLimits = if (config.includeBudgets) database.budgetDao().getAllCategoryLimits().first() else emptyList()
        val budgetCategoryLimits = rawBudgetCategoryLimits.filter { it.budgetId in userBudgetIds }

        val subcategories = if (config.includeProfileData) database.subcategoryDao().getAllSubcategories().first() else emptyList()
        val rules = if (config.includeAppPreferences) database.ruleDao().getAllRules().first() else emptyList()
        val exchangeRates = if (config.includeAppPreferences) database.exchangeRateDao().getAllRates().first() else emptyList()
        val rawRuleApplications = if (config.includeTransactionalData) database.ruleApplicationDao().getRecentApplications(1000).first() else emptyList() // Limit to recent apps for backup size
        val ruleApplications = rawRuleApplications.filter {
            it.transactionId.toLongOrNull()?.let { id -> id in userTxnIds } ?: true
        }
        val webhookProfiles = if (config.includeAppPreferences) {
            database.webhookProfileDao().getAllProfiles().first().map { profile ->
                WebhookProfileBackup(
                    id = profile.id,
                    name = profile.name,
                    url = profile.url,
                    enabled = profile.enabled,
                    dataTypes = profile.dataTypes,
                    rangePreset = profile.rangePreset.name,
                    customStart = profile.customStart?.toString(),
                    customEnd = profile.customEnd?.toString(),
                    headers = com.reddy.vittify.data.repository.WebhookHeaderEncoder.sanitizeForExport(
                        webhookRepository.decodeHeaders(profile.headersJson)
                    )
                )
            }
        } else emptyList()
        
        // Get preferences from repository
        val prefs = userPreferencesRepository.userPreferences.first()
        val systemPrompt = userPreferencesRepository.getSystemPrompt().first()
        val firstLaunchTime = userPreferencesRepository.getFirstLaunchTime().first()
        val hasShownReviewPrompt = userPreferencesRepository.getHasShownReviewPrompt().first()
        val lastReviewPromptTime = userPreferencesRepository.getLastReviewPromptTime().first()
        val lastScanTimestamp = userPreferencesRepository.getLastScanTimestamp().first()
        val lastScanPeriod = userPreferencesRepository.getLastScanPeriod().first()
        
        // Calculate statistics
        val dateRange = if (transactions.isNotEmpty()) {
            val sorted = transactions.sortedBy { it.dateTime }
            DateRange(
                earliest = sorted.first().dateTime.toString(),
                latest = sorted.last().dateTime.toString()
            )
        } else null
        
        // Resolve accountId on transactions if missing and matches an account balance
        val accountByLast4 = accountBalances.associateBy { it.accountLast4 }
        val resolvedTransactions = transactions.map { txn ->
            var updated = txn
            val fromAcc = txn.fromAccount?.let { accountByLast4[it] }
            val toAcc = txn.toAccount?.let { accountByLast4[it] }
            val matchedAccount = fromAcc ?: toAcc

            if (updated.accountId.isNullOrBlank() && matchedAccount != null) {
                updated = updated.copy(accountId = matchedAccount.id)
            }
            if (updated.fromAccountId.isNullOrBlank() && fromAcc != null) {
                updated = updated.copy(fromAccountId = fromAcc.id)
            }
            if (updated.toAccountId.isNullOrBlank() && toAcc != null) {
                updated = updated.copy(toAccountId = toAcc.id)
            }
            updated
        }

        // Apply privacy settings if needed
        val finalTransactions = when (config.privacy) {
            ExportPrivacy.FULL -> resolvedTransactions
            ExportPrivacy.MASKED -> resolvedTransactions.map { it.copy(
                smsBody = "[REDACTED]",
                fromAccount = it.fromAccount?.takeLast(4)?.let { "****$it" },
                toAccount = it.toAccount?.takeLast(4)?.let { "****$it" }
            )}
            ExportPrivacy.ANONYMOUS -> resolvedTransactions.map { it.copy(
                merchantName = "Merchant",
                description = null,
                smsBody = "[REDACTED]",
                fromAccount = "****",
                toAccount = "****"
            )}
        }
        
        return VittifyBackup(
            metadata = BackupMetadata(
                exportId = UUID.randomUUID().toString(),
                appVersion = BuildConfig.VERSION_NAME,
                databaseVersion = 61,
                device = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = Build.VERSION.SDK_INT,
                ownerId = currentDeviceId.ifBlank { null },
                statistics = BackupStatistics(
                    totalTransactions = transactions.size,
                    totalCategories = categories.size,
                    totalCards = cards.size,
                    totalSubscriptions = subscriptions.size,
                    totalSubcategories = subcategories.size,
                    totalRules = rules.size,
                    totalAccountBalances = accountBalances.size,
                    dateRange = dateRange
                )
            ),
            database = DatabaseSnapshot(
                transactions = finalTransactions,
                categories = categories,
                cards = cards,
                accountBalances = accountBalances,
                subscriptions = subscriptions,
                merchantMappings = merchantMappings,
                unrecognizedSms = if (config.privacy == ExportPrivacy.FULL) unrecognizedSms else emptyList(),
                budgets = budgets,
                budgetCategoryLimits = budgetCategoryLimits,
                subcategories = subcategories,
                rules = rules,
                ruleApplications = ruleApplications,
                webhookProfiles = webhookProfiles,
                exchangeRates = exchangeRates,
                transactionItems = transactionItems
            ),
            preferences = PreferencesSnapshot(
                theme = ThemePreferences(
                    isDarkThemeEnabled = if (config.includeAppPreferences) prefs.isDarkThemeEnabled else null,
                    isDynamicColorEnabled = if (config.includeAppPreferences) prefs.isDynamicColorEnabled else true,
                    isAmoledMode = if (config.includeAppPreferences) prefs.isAmoledMode else null,
                    navigationBarStyle = if (config.includeAppPreferences) prefs.navigationBarStyle.name else null,
                    appFont = if (config.includeAppPreferences) prefs.appFont.name else null,
                    themeStyle = if (config.includeAppPreferences) prefs.themeStyle.name else null,
                    accentColor = if (config.includeAppPreferences) prefs.accentColor.name else null,
                    hideNavigationLabels = if (config.includeAppPreferences) prefs.hideNavigationLabels else null,
                    hidePillIndicator = if (config.includeAppPreferences) prefs.hidePillIndicator else null,
                    profileSwitcherInFooter = if (config.includeAppPreferences) prefs.profileSwitcherInFooter else null,
                    blurEffects = if (config.includeAppPreferences) prefs.blurEffects else null,
                    appIcon = if (config.includeAppPreferences) prefs.appIcon.name else null
                ),
                sms = SmsPreferences(
                    hasSkippedSmsPermission = prefs.hasSkippedSmsPermission,
                    smsScanMonths = prefs.smsScanMonths,
                    lastScanTimestamp = lastScanTimestamp,
                    lastScanPeriod = lastScanPeriod
                ),
                developer = DeveloperPreferences(
                    isDeveloperModeEnabled = prefs.isDeveloperModeEnabled,
                    isWebhookModeEnabled = prefs.isWebhookModeEnabled,
                    isTokenInfoEnabled = prefs.isTokenInfoEnabled,
                    systemPrompt = systemPrompt
                ),
                app = AppPreferences(
                    hasShownScanTutorial = prefs.hasShownScanTutorial,
                    firstLaunchTime = firstLaunchTime,
                    hasShownReviewPrompt = hasShownReviewPrompt,
                    lastReviewPromptTime = lastReviewPromptTime
                ),
                profile = if (config.includeProfileData) ProfilePreferences(
                    userName = prefs.userName,
                    profileImageUri = if (prefs.profileImageUri != null) "profile/profile_image" else null,
                    profileBackgroundColor = prefs.profileBackgroundColor
                ) else null,
                homeWidgets = if (config.includeAppPreferences) {
                    val order = userPreferencesRepository.homeWidgetsOrder.first()
                    val hidden = userPreferencesRepository.hiddenHomeWidgets.first()
                    HomeWidgetPreferences(
                        order = order.map { it.name },
                        hidden = hidden.map { it.name }
                    )
                } else null,
                currency = if (config.includeAppPreferences) {
                    val unifiedCurrencyEnabled = userPreferencesRepository.unifiedCurrencyEnabled.first()
                    val unifiedCurrencyCode = userPreferencesRepository.unifiedCurrencyCode.first()
                    val defaultCurrencyEnabled = userPreferencesRepository.defaultCurrencyEnabled.first()
                    val defaultCurrencyCode = userPreferencesRepository.defaultCurrencyCode.first()
                    val customCurrencies = userPreferencesRepository.customCurrencies.first()
                    CurrencyPreferences(
                        unifiedCurrencyEnabled = unifiedCurrencyEnabled,
                        unifiedCurrencyCode = unifiedCurrencyCode,
                        defaultCurrencyEnabled = defaultCurrencyEnabled,
                        defaultCurrencyCode = defaultCurrencyCode,
                        customCurrencies = customCurrencies
                    )
                } else null
            )
        )
    }
    
    /**
     * Create backup file in cache directory
     */
    private fun createBackupFile(): File {
        val exportDir = File(context.cacheDir, "backups")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss")
        )
        val fileName = "Vittify_Backup_$timestamp.zip"
        
        return File(exportDir, fileName)
    }


    private fun copyToZip(uriString: String, entryName: String, zipOut: ZipOutputStream) {
        try {
            val uri = uriString.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            
            inputStream?.use { input ->
                val entry = ZipEntry(entryName)
                zipOut.putNextEntry(entry)
                input.copyTo(zipOut)
                zipOut.closeEntry()
            }
        } catch (e: Exception) {
            // Log or ignore image copy failure to avoid failing entire backup
            e.printStackTrace()
        }
    }
}
