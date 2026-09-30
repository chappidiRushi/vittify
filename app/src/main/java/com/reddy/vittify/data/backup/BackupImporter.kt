package com.reddy.vittify.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.GsonBuilder
import androidx.room.withTransaction
import com.reddy.vittify.data.database.VittifyDatabase
import com.reddy.vittify.data.database.entity.*
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.WebhookRepository
import com.reddy.vittify.data.preferences.NavigationBarStyle
import com.reddy.vittify.data.preferences.AppFont
import com.reddy.vittify.data.preferences.ThemeStyle
import com.reddy.vittify.data.preferences.AccentColor
import com.reddy.vittify.data.preferences.AppIcon
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.zip.ZipInputStream
import java.io.FileOutputStream
import java.io.File
import java.math.BigDecimal
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: VittifyDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val webhookRepository: WebhookRepository,
    private val p2pPreferences: P2pSyncPreferencesRepository
) {

    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalTypeAdapter())
        .registerTypeAdapter(String::class.java, FlexibleStringTypeAdapter())
        .registerTypeAdapter(DatabaseSnapshot::class.java, DatabaseSnapshotDeserializer())
        .create()

    /**
     * Import backup from a file URI
     */
    suspend fun importBackup(
        uri: Uri,
        strategy: ImportStrategy = ImportStrategy.MERGE
    ): ImportResult = withContext(Dispatchers.IO) {
        importBackup(uri, strategy, SelectiveImportFilter.ALL)
    }

    suspend fun importBackup(
        uri: Uri,
        strategy: ImportStrategy,
        filter: SelectiveImportFilter
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Read and parse the backup file
            val backup = readBackupFile(uri) ?: return@withContext ImportResult.Error("Failed to read backup file")

            // Validate backup version
            if (!isCompatibleVersion(backup)) {
                return@withContext ImportResult.Error("Incompatible backup version")
            }

            val existingTxnCount = database.transactionDao().getTransactionCount().first()
            val existingAccCount = database.accountBalanceDao().getAllBalances().first().size
            val hasExistingData = (existingTxnCount > 0 || existingAccCount > 0)
            val currentDeviceId = p2pPreferences.getDeviceId()

            val targetOwnerId: String = if (!hasExistingData) {
                val backupOwnerId = backup.metadata.ownerId?.ifBlank { null } ?: findDominantOwnerId(backup)
                if (!backupOwnerId.isNullOrBlank()) {
                    Log.d("BackupImporter", "Restoring owner ID from backup: $backupOwnerId (was $currentDeviceId)")
                    p2pPreferences.setDeviceId(backupOwnerId)
                    backupOwnerId
                } else {
                    currentDeviceId
                }
            } else {
                Log.d("BackupImporter", "Existing data present; retaining current device ID ($currentDeviceId) and applying to restoring records")
                currentDeviceId
            }
            p2pPreferences.setInitialDataSeeded(false)

            // Import based on strategy
            val result = when (strategy) {
                ImportStrategy.REPLACE_ALL -> replaceAllData(backup, targetOwnerId)
                ImportStrategy.MERGE -> mergeData(backup, targetOwnerId)
                ImportStrategy.SELECTIVE -> selectiveImport(backup, filter, targetOwnerId)
            }

            // Run post-import migration to map NanoIDs and link legacy account transactions
            runPostImportMigrations()

            result
        } catch (e: Exception) {
            Log.e("BackupImporter", "Import failed", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    /**
     * Read and parse backup file
     */
    /**
     * Read and parse backup file (ZIP or JSON)
     */
    private suspend fun readBackupFile(uri: Uri): VittifyBackup? {
        return withContext(Dispatchers.IO) {
            // Try to read as ZIP first
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val zipInput = ZipInputStream(inputStream)
                    var backup: VittifyBackup? = null
                    var entry = zipInput.nextEntry

                    if (entry != null) {
                        val attachmentsDir = File(context.filesDir, "attachments")
                        if (!attachmentsDir.exists()) attachmentsDir.mkdirs()

                        while (entry != null) {
                            val name = entry.name
                            if (name == "backup.json") {
                                // Read JSON
                                val bytes = zipInput.readBytes()
                                val content = String(bytes, Charsets.UTF_8)
                                backup = gson.fromJson(content, VittifyBackup::class.java)
                            } else if (name.startsWith("attachments/") && !entry.isDirectory) {
                                // Extract Attachment
                                val fileName = File(name).name
                                val outFile = File(attachmentsDir, fileName)
                                FileOutputStream(outFile).use { output ->
                                    zipInput.copyTo(output)
                                }
                            } else if (name.startsWith("profile/") && !entry.isDirectory) {
                                // Extract Profile Images
                                val profileDir = File(context.filesDir, "profile")
                                if (!profileDir.exists()) profileDir.mkdirs()

                                val fileName = File(name).name
                                val outFile = File(profileDir, fileName)
                                FileOutputStream(outFile).use { output ->
                                    zipInput.copyTo(output)
                                }
                            }
                            zipInput.closeEntry()
                            entry = zipInput.nextEntry
                        }
                        // If we found a backup.json, return it
                        if (backup != null) return@withContext backup
                    }
                }
            } catch (e: Exception) {
                // Not a ZIP or failed, fall back to legacy JSON
                Log.d("BackupImporter", "Failed to read as ZIP, trying legacy JSON: ${e.message}")
            }

            // Legacy JSON handling
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                gson.fromJson(content, VittifyBackup::class.java)
            } ?: throw Exception("Failed to read backup file")
        }
    }

    /**
     * Check if backup version is compatible
     */
    private fun isCompatibleVersion(backup: VittifyBackup): Boolean {
        // For now, accept all v1.x backups
        return backup.format.startsWith("Vittify Backup v1")
    }

    private fun findDominantOwnerId(backup: VittifyBackup): String? {
        val partnerId = p2pPreferences.getPartnerUserId()
        val txns = backup.database.transactions ?: emptyList()
        val accounts = backup.database.accountBalances ?: emptyList()

        val candidateOwners = (txns.map { it.ownerId } + accounts.map { it.ownerId })
            .filter { it.isNotBlank() && (partnerId.isNullOrBlank() || it != partnerId) }
            .groupingBy { it }
            .eachCount()

        return candidateOwners.maxByOrNull { it.value }?.key
    }

    /**
     * Replace all existing data with backup data
     */
    private suspend fun replaceAllData(backup: VittifyBackup, targetOwnerId: String): ImportResult {
        var importedTransactions = 0
        var importedCategories = 0

        return database.withTransaction {
            try {
                // Clear existing data
                database.transactionDao().deleteAllTransactions()
                database.categoryDao().deleteAllCategories()
                database.cardDao().deleteAllCards()
                database.accountBalanceDao().deleteAllBalances()
                database.subscriptionDao().deleteAllSubscriptions()
                database.merchantMappingDao().deleteAllMappings()
                database.unrecognizedSmsDao().deleteAll()
                database.budgetDao().deleteAllBudgets()
                database.subcategoryDao().getAllSubcategories().first().forEach {
                    database.subcategoryDao().deleteSubcategory(it)
                }
                database.ruleDao().deleteAllRules()
                database.ruleApplicationDao().deleteAllApplications()
                database.exchangeRateDao().deleteAllRates()
                database.transactionItemDao().deleteAllItems()

                // Import categories
                (backup.database.categories ?: emptyList()).forEach { category ->
                    database.categoryDao().insertCategory(category.sanitize())
                    importedCategories++
                }

                // Import subcategories
                (backup.database.subcategories ?: emptyList()).forEach { subcategory ->
                    database.subcategoryDao().insertSubcategory(subcategory.sanitize())
                }

                // Import account balances
                val accountIdMap = mutableMapOf<String, String>()
                val insertedBalances = (backup.database.accountBalances ?: emptyList())
                    .map { it.sanitize() }
                    .groupBy { it.bankName to it.accountLast4 }
                    .values
                    .mapNotNull { group ->
                        val chosen = group.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                        val finalId = chosen.id.ifBlank { com.reddy.vittify.utils.NanoId.generate() }
                        val toInsert = chosen.copy(id = finalId, ownerId = targetOwnerId)
                        database.accountBalanceDao().insertBalance(toInsert)
                        group.forEach { b ->
                            if (b.id.isNotBlank()) {
                                accountIdMap[b.id] = finalId
                            }
                        }
                        toInsert
                    }
                val accountByLast4 = insertedBalances.associate { it.accountLast4 to it.id }

                // Import cards
                (backup.database.cards ?: emptyList()).forEach { card ->
                    database.cardDao().insertCard(card.sanitize().copy(ownerId = targetOwnerId))
                }

                // Import transactions
                (backup.database.transactions ?: emptyList()).forEach { transaction ->
                    val sanitized = transaction.sanitize()
                    val mappedAccountId = (sanitized.accountId?.let { accountIdMap[it] } ?: sanitized.accountId)
                        ?.takeIf { it.isNotBlank() }
                        ?: sanitized.fromAccount?.let { accountByLast4[it] }
                        ?: sanitized.toAccount?.let { accountByLast4[it] }

                    val mappedFromAccountId = (sanitized.fromAccountId?.let { accountIdMap[it] } ?: sanitized.fromAccountId)
                        ?.takeIf { it.isNotBlank() }
                        ?: sanitized.fromAccount?.let { accountByLast4[it] }

                    val mappedToAccountId = (sanitized.toAccountId?.let { accountIdMap[it] } ?: sanitized.toAccountId)
                        ?.takeIf { it.isNotBlank() }
                        ?: sanitized.toAccount?.let { accountByLast4[it] }

                    database.transactionDao().insertTransaction(
                        sanitized.copy(
                            accountId = mappedAccountId,
                            fromAccountId = mappedFromAccountId,
                            toAccountId = mappedToAccountId,
                            ownerId = targetOwnerId
                        )
                    )
                    importedTransactions++
                }

                // Import transaction items
                (backup.database.transactionItems ?: emptyList()).forEach { item ->
                    database.transactionItemDao().insertItem(item.sanitize())
                }

                // Import subscriptions
                (backup.database.subscriptions ?: emptyList()).forEach { subscription ->
                    database.subscriptionDao().insertSubscription(subscription.sanitize().copy(ownerId = targetOwnerId))
                }

                // Import merchant mappings
                (backup.database.merchantMappings ?: emptyList()).forEach { mapping ->
                    database.merchantMappingDao().insertMapping(mapping.sanitize())
                }

                // Import unrecognized SMS
                (backup.database.unrecognizedSms ?: emptyList()).forEach { sms ->
                    database.unrecognizedSmsDao().insert(sms.sanitize())
                }

                // Import exchange rates
                (backup.database.exchangeRates ?: emptyList()).forEach { rate ->
                    database.exchangeRateDao().insertExchangeRate(rate.sanitize())
                }

                // Import budgets
                (backup.database.budgets ?: emptyList()).forEach { budget ->
                    database.budgetDao().insertBudget(budget.sanitize().copy(ownerId = targetOwnerId))
                }

                (backup.database.budgetCategoryLimits ?: emptyList()).forEach { limit ->
                    database.budgetDao().insertCategoryLimit(limit)
                }

                // Import rules
                (backup.database.rules ?: emptyList()).forEach { rule ->
                    database.ruleDao().insertRule(rule.sanitize())
                }

                (backup.database.ruleApplications ?: emptyList()).forEach { app ->
                    database.ruleApplicationDao().insertApplication(app.sanitize())
                }

                val webhookProfiles = backup.database.webhookProfiles ?: emptyList()
                if (webhookProfiles.isNotEmpty()) {
                    val baseCurrency = userPreferencesRepository.baseCurrency.first()
                    webhookProfiles.forEach { profile ->
                        webhookRepository.saveProfile(profile.toDraft(baseCurrency))
                    }
                }

                // Import preferences
                backup.preferences?.let { importPreferences(it) }

                ImportResult.Success(
                    importedTransactions = importedTransactions,
                    importedCategories = importedCategories,
                    skippedDuplicates = 0
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Merge backup data with existing data
     */
    private suspend fun mergeData(backup: VittifyBackup, targetOwnerId: String): ImportResult {
        var importedTransactions = 0
        var importedCategories = 0
        var skippedDuplicates = 0

        return database.withTransaction {
            try {
                // Build identity lookup maps — UUID takes priority, smsBody is the fallback
                // for backups that pre-date the UUID column.
                val existingTransactions = database.transactionDao()
                    .getAllTransactionsIncludingDeleted().first()
                val existingByUuid = existingTransactions
                    .filter { it.uuid.isNotBlank() }
                    .associateBy { it.uuid }
                val existingBySmsBody = existingTransactions
                    .filter { !it.smsBody.isNullOrBlank() }
                    .associateBy { it.smsBody!! }

                val existingCategories = database.categoryDao()
                    .getAllCategories().first()
                    .map { it.name }
                    .toSet()

                // Mapping to track OldCategoryID -> NewCategoryID for subcategories
                val categoryIdMap = mutableMapOf<Long, Long>()

                // Import categories (merge by name)
                (backup.database.categories ?: emptyList()).forEach { category ->
                    val sanitizedCategory = category.sanitize()
                    val existingCategory = database.categoryDao().getCategoryByName(sanitizedCategory.name)
                    if (existingCategory == null) {
                        // Generate new ID for imported category
                        val newCategory = sanitizedCategory.copy(id = 0)
                        val newId = database.categoryDao().insertCategory(newCategory)
                        categoryIdMap[sanitizedCategory.id] = newId
                        importedCategories++
                    } else {
                        categoryIdMap[sanitizedCategory.id] = existingCategory.id
                    }
                }

                // Import subcategories (merge by name within category)
                (backup.database.subcategories ?: emptyList()).forEach { subcategory ->
                    val sanitizedSubcategory = subcategory.sanitize()
                    val newCategoryId = categoryIdMap[sanitizedSubcategory.categoryId] ?: return@forEach
                    val existingSubcategories = database.subcategoryDao().getSubcategoriesByCategoryId(newCategoryId).first()
                    val alreadyExists = existingSubcategories.any { it.name == sanitizedSubcategory.name }

                    if (!alreadyExists) {
                        val newSubcategory = sanitizedSubcategory.copy(id = 0, categoryId = newCategoryId)
                        database.subcategoryDao().insertSubcategory(newSubcategory)
                    }
                }

                // Import accounts and cards before transactions to resolve and remap account references
                val accountIdMap = importAccountBalancesWithMerge(backup.database.accountBalances ?: emptyList(), targetOwnerId)
                importCardsWithMerge(backup.database.cards ?: emptyList(), targetOwnerId)

                val accountsInDb = database.accountBalanceDao().getAllBalances().first()
                val accountByLast4 = accountsInDb.associate { it.accountLast4 to it.id }

                // Mapping to track OldTransactionID -> NewTransactionID for rule applications and transaction items
                val transactionIdMap = mutableMapOf<Long, Long>()

                // Import transactions — UUID-first identity, smsBody fallback
                (backup.database.transactions ?: emptyList()).forEach { backupTxn ->
                    val sanitizedTxn = backupTxn.sanitize()

                    // Remap account IDs using the accountIdMap or matching accountLast4
                    val mappedAccountId = (sanitizedTxn.accountId?.let { accountIdMap[it] } ?: sanitizedTxn.accountId)
                        ?.takeIf { it.isNotBlank() }
                        ?: sanitizedTxn.fromAccount?.let { accountByLast4[it] }
                        ?: sanitizedTxn.toAccount?.let { accountByLast4[it] }

                    val mappedFromAccountId = (sanitizedTxn.fromAccountId?.let { accountIdMap[it] } ?: sanitizedTxn.fromAccountId)
                        ?.takeIf { it.isNotBlank() }
                        ?: sanitizedTxn.fromAccount?.let { accountByLast4[it] }

                    val mappedToAccountId = (sanitizedTxn.toAccountId?.let { accountIdMap[it] } ?: sanitizedTxn.toAccountId)
                        ?.takeIf { it.isNotBlank() }
                        ?: sanitizedTxn.toAccount?.let { accountByLast4[it] }

                    val preparedTxn = sanitizedTxn.copy(
                        accountId = mappedAccountId,
                        fromAccountId = mappedFromAccountId,
                        toAccountId = mappedToAccountId
                    )

                    // Look up by UUID first (stable identity), then fall back to smsBody
                    val existingTxn = when {
                        preparedTxn.uuid.isNotBlank() -> existingByUuid[preparedTxn.uuid]
                        !preparedTxn.smsBody.isNullOrBlank() -> existingBySmsBody[preparedTxn.smsBody]
                        else -> null
                    }
                    if (existingTxn == null) {
                        // New transaction — ensure it gets a UUID and ownerId even if missing
                        val currentOwner = targetOwnerId.ifBlank { preparedTxn.ownerId.ifBlank { p2pPreferences.getDeviceId() } }
                        val newTransaction = preparedTxn.copy(
                            id = 0,
                            uuid = preparedTxn.uuid.ifBlank { UUID.randomUUID().toString() },
                            ownerId = currentOwner
                        )
                        val newId = database.transactionDao().insertTransaction(newTransaction)
                        transactionIdMap[backupTxn.id] = newId
                        importedTransactions++
                    } else {
                        transactionIdMap[backupTxn.id] = existingTxn.id
                        // Transaction exists locally. Should we update it with backup data?
                        val shouldUpdate = when {
                            preparedTxn.updatedAt.isAfter(existingTxn.updatedAt) -> true
                            preparedTxn.updatedAt.isAfter(preparedTxn.dateTime) &&
                                    !existingTxn.updatedAt.isAfter(existingTxn.dateTime) -> true
                            else -> false
                        }

                        if (shouldUpdate) {
                            // Preserve local id, UUID, and avoid overwriting existing account link with null
                            val updatedTxn = preparedTxn.copy(
                                id = existingTxn.id,
                                uuid = if (existingTxn.uuid.isNotBlank()) existingTxn.uuid
                                       else preparedTxn.uuid.ifBlank { UUID.randomUUID().toString() },
                                accountId = preparedTxn.accountId ?: existingTxn.accountId,
                                fromAccountId = preparedTxn.fromAccountId ?: existingTxn.fromAccountId,
                                toAccountId = preparedTxn.toAccountId ?: existingTxn.toAccountId,
                                ownerId = existingTxn.ownerId.ifBlank { targetOwnerId }
                            )
                            database.transactionDao().updateTransaction(updatedTxn)
                            importedTransactions++
                        } else {
                            // Even if not updating, ensure existingTxn gets accountId linked if it was missing
                            if (existingTxn.accountId.isNullOrBlank() && !preparedTxn.accountId.isNullOrBlank()) {
                                database.transactionDao().updateTransaction(existingTxn.copy(
                                    accountId = preparedTxn.accountId,
                                    fromAccountId = preparedTxn.fromAccountId ?: existingTxn.fromAccountId,
                                    toAccountId = preparedTxn.toAccountId ?: existingTxn.toAccountId
                                ))
                            }
                            skippedDuplicates++
                        }
                    }
                }

                // Import transaction items with re-parenting
                (backup.database.transactionItems ?: emptyList()).forEach { item ->
                    val newTxId = transactionIdMap[item.transactionId] ?: item.transactionId
                    if (newTxId != 0L) {
                        database.transactionItemDao().insertItem(item.sanitize().copy(id = 0, transactionId = newTxId))
                    }
                }

                // Import rules (merge by name/ID)
                (backup.database.rules ?: emptyList()).forEach { rule ->
                    database.ruleDao().insertRule(rule.sanitize())
                }

                // Import rule applications
                (backup.database.ruleApplications ?: emptyList()).forEach { app ->
                    val newTxId = transactionIdMap[app.transactionId.toLongOrNull() ?: -1L]
                    if (newTxId != null) {
                        val newApp = app.sanitize().copy(transactionId = newTxId.toString())
                        database.ruleApplicationDao().insertApplication(newApp)
                    }
                }

                // Import other entities with duplicate checking
                importSubscriptionsWithMerge(backup.database.subscriptions ?: emptyList(), targetOwnerId)
                importMerchantMappingsWithMerge(backup.database.merchantMappings ?: emptyList())
                importBudgetsWithMerge(backup.database.budgets ?: emptyList(), backup.database.budgetCategoryLimits ?: emptyList(), targetOwnerId)
                importWebhookProfilesWithMerge(backup.database.webhookProfiles ?: emptyList())
                importExchangeRatesWithMerge(backup.database.exchangeRates ?: emptyList())

                // Import preferences (merge with existing)
                backup.preferences?.let { importPreferences(it) }

                ImportResult.Success(
                    importedTransactions = importedTransactions,
                    importedCategories = importedCategories,
                    skippedDuplicates = skippedDuplicates
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Selective import: merge only the entity types selected in [filter].
     */
    private suspend fun selectiveImport(
        backup: VittifyBackup,
        filter: SelectiveImportFilter,
        targetOwnerId: String
    ): ImportResult {
        var importedTransactions = 0
        var importedCategories = 0
        var skippedDuplicates = 0

        return database.withTransaction {
            try {
                // Build identity lookup maps — UUID takes priority, smsBody is the fallback
                // for backups that pre-date the UUID column.
                val existingTransactions = database.transactionDao()
                    .getAllTransactionsIncludingDeleted().first()
                val existingByUuid = existingTransactions
                    .filter { it.uuid.isNotBlank() }
                    .associateBy { it.uuid }
                val existingBySmsBody = existingTransactions
                    .filter { !it.smsBody.isNullOrBlank() }
                    .associateBy { it.smsBody!! }

                val existingCategories = database.categoryDao()
                    .getAllCategories().first()
                    .map { it.name }
                    .toSet()

                val categoryIdMap = mutableMapOf<Long, Long>()

                if (filter.includeCategories) {
                    (backup.database.categories ?: emptyList()).forEach { category ->
                        val sanitizedCategory = category.sanitize()
                        val existingCategory = database.categoryDao().getCategoryByName(sanitizedCategory.name)
                        if (existingCategory == null) {
                            val newCategory = sanitizedCategory.copy(id = 0)
                            val newId = database.categoryDao().insertCategory(newCategory)
                            categoryIdMap[sanitizedCategory.id] = newId
                            importedCategories++
                        } else {
                            categoryIdMap[sanitizedCategory.id] = existingCategory.id
                        }
                    }
                }

                if (filter.includeSubcategories) {
                    (backup.database.subcategories ?: emptyList()).forEach { subcategory ->
                        val sanitizedSubcategory = subcategory.sanitize()
                        val newCategoryId = categoryIdMap[sanitizedSubcategory.categoryId] ?: return@forEach
                        val existingSubcategories = database.subcategoryDao()
                            .getSubcategoriesByCategoryId(newCategoryId).first()
                        val alreadyExists = existingSubcategories.any { it.name == sanitizedSubcategory.name }
                        if (!alreadyExists) {
                            val newSubcategory = sanitizedSubcategory.copy(id = 0, categoryId = newCategoryId)
                            database.subcategoryDao().insertSubcategory(newSubcategory)
                        }
                    }
                }

                // Import accounts and cards before transactions to resolve and remap account references
                val accountIdMap: Map<String, String> = if (filter.includeAccountBalances) {
                    importAccountBalancesWithMerge(backup.database.accountBalances ?: emptyList(), targetOwnerId)
                } else {
                    emptyMap()
                }

                if (filter.includeCards) {
                    importCardsWithMerge(backup.database.cards ?: emptyList(), targetOwnerId)
                }

                val accountsInDb = database.accountBalanceDao().getAllBalances().first()
                val accountByLast4 = accountsInDb.associate { it.accountLast4 to it.id }

                val transactionIdMap = mutableMapOf<Long, Long>()

                if (filter.includeTransactions) {
                    (backup.database.transactions ?: emptyList()).forEach { backupTxn ->
                        val sanitizedTxn = backupTxn.sanitize()

                        // Remap account IDs using accountIdMap or matching accountLast4
                        val mappedAccountId = (sanitizedTxn.accountId?.let { accountIdMap[it] } ?: sanitizedTxn.accountId)
                            ?.takeIf { it.isNotBlank() }
                            ?: sanitizedTxn.fromAccount?.let { accountByLast4[it] }
                            ?: sanitizedTxn.toAccount?.let { accountByLast4[it] }

                        val mappedFromAccountId = (sanitizedTxn.fromAccountId?.let { accountIdMap[it] } ?: sanitizedTxn.fromAccountId)
                            ?.takeIf { it.isNotBlank() }
                            ?: sanitizedTxn.fromAccount?.let { accountByLast4[it] }

                        val mappedToAccountId = (sanitizedTxn.toAccountId?.let { accountIdMap[it] } ?: sanitizedTxn.toAccountId)
                            ?.takeIf { it.isNotBlank() }
                            ?: sanitizedTxn.toAccount?.let { accountByLast4[it] }

                        val preparedTxn = sanitizedTxn.copy(
                            accountId = mappedAccountId,
                            fromAccountId = mappedFromAccountId,
                            toAccountId = mappedToAccountId
                        )

                        // Look up by UUID first (stable identity), then fall back to smsBody
                        val existingTxn = when {
                            preparedTxn.uuid.isNotBlank() -> existingByUuid[preparedTxn.uuid]
                            !preparedTxn.smsBody.isNullOrBlank() -> existingBySmsBody[preparedTxn.smsBody]
                            else -> null
                        }
                        if (existingTxn == null) {
                            // New transaction — ensure it gets a UUID and ownerId even if missing
                            val currentOwner = targetOwnerId.ifBlank { preparedTxn.ownerId.ifBlank { p2pPreferences.getDeviceId() } }
                            val newTransaction = preparedTxn.copy(
                                id = 0,
                                uuid = preparedTxn.uuid.ifBlank { UUID.randomUUID().toString() },
                                ownerId = currentOwner
                            )
                            val newId = database.transactionDao().insertTransaction(newTransaction)
                            transactionIdMap[backupTxn.id] = newId
                            importedTransactions++
                        } else {
                            transactionIdMap[backupTxn.id] = existingTxn.id
                            val shouldUpdate = when {
                                preparedTxn.updatedAt.isAfter(existingTxn.updatedAt) -> true
                                preparedTxn.updatedAt.isAfter(preparedTxn.dateTime) &&
                                        !existingTxn.updatedAt.isAfter(existingTxn.dateTime) -> true
                            else -> false
                            }
                            if (shouldUpdate) {
                                // Preserve local id, UUID, and avoid overwriting existing account link with null
                                val updatedTxn = preparedTxn.copy(
                                    id = existingTxn.id,
                                    uuid = if (existingTxn.uuid.isNotBlank()) existingTxn.uuid
                                           else preparedTxn.uuid.ifBlank { UUID.randomUUID().toString() },
                                    accountId = preparedTxn.accountId ?: existingTxn.accountId,
                                    fromAccountId = preparedTxn.fromAccountId ?: existingTxn.fromAccountId,
                                    toAccountId = preparedTxn.toAccountId ?: existingTxn.toAccountId,
                                    ownerId = existingTxn.ownerId.ifBlank { targetOwnerId }
                                )
                                database.transactionDao().updateTransaction(updatedTxn)
                                importedTransactions++
                            } else {
                                if (existingTxn.accountId.isNullOrBlank() && !preparedTxn.accountId.isNullOrBlank()) {
                                    database.transactionDao().updateTransaction(existingTxn.copy(
                                        accountId = preparedTxn.accountId,
                                        fromAccountId = preparedTxn.fromAccountId ?: existingTxn.fromAccountId,
                                        toAccountId = preparedTxn.toAccountId ?: existingTxn.toAccountId
                                    ))
                                }
                                skippedDuplicates++
                            }
                        }
                    }
                }

                if (filter.includeTransactionItems && filter.includeTransactions) {
                    (backup.database.transactionItems ?: emptyList()).forEach { item ->
                        val newTxId = transactionIdMap[item.transactionId] ?: item.transactionId
                        if (newTxId != 0L) {
                            database.transactionItemDao().insertItem(item.sanitize().copy(id = 0, transactionId = newTxId))
                        }
                    }
                }

                if (filter.includeRules) {
                    (backup.database.rules ?: emptyList()).forEach { rule ->
                        database.ruleDao().insertRule(rule.sanitize())
                    }
                }

                if (filter.includeRuleApplications) {
                    (backup.database.ruleApplications ?: emptyList()).forEach { app ->
                        val newTxId = transactionIdMap[app.transactionId.toLongOrNull() ?: -1L]
                        if (newTxId != null) {
                            val newApp = app.sanitize().copy(transactionId = newTxId.toString())
                            database.ruleApplicationDao().insertApplication(newApp)
                        }
                    }
                }

                if (filter.includeSubscriptions) {
                    importSubscriptionsWithMerge(backup.database.subscriptions ?: emptyList(), targetOwnerId)
                }
                if (filter.includeMerchantMappings) {
                    importMerchantMappingsWithMerge(backup.database.merchantMappings ?: emptyList())
                }
                if (filter.includeBudgets) {
                    importBudgetsWithMerge(backup.database.budgets ?: emptyList(), backup.database.budgetCategoryLimits ?: emptyList(), targetOwnerId)
                }
                if (filter.includeWebhookProfiles) {
                    importWebhookProfilesWithMerge(backup.database.webhookProfiles ?: emptyList())
                }
                if (filter.includeExchangeRates) {
                    importExchangeRatesWithMerge(backup.database.exchangeRates ?: emptyList())
                }
                if (filter.includePreferences) {
                    backup.preferences?.let { importPreferences(it) }
                }

                ImportResult.Success(
                    importedTransactions = importedTransactions,
                    importedCategories = importedCategories,
                    skippedDuplicates = skippedDuplicates
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Import cards with duplicate checking
     * Import cards with duplicate checking and merging.
     */
    private suspend fun importCardsWithMerge(cards: List<CardEntity>, targetOwnerId: String) {
        cards.forEach { card ->
            val sanitizedCard = card.sanitize()
            val existing = database.cardDao().getCard(sanitizedCard.bankName, sanitizedCard.cardLast4)
            val owner = targetOwnerId.ifBlank { sanitizedCard.ownerId }
            if (existing == null) {
                val newCard = sanitizedCard.copy(id = 0, ownerId = owner)
                database.cardDao().insertCard(newCard)
            } else if (sanitizedCard.updatedAt.isAfter(existing.updatedAt)) {
                val updatedCard = sanitizedCard.copy(
                    id = existing.id,
                    createdAt = existing.createdAt,
                    ownerId = existing.ownerId.ifBlank { owner }
                )
                database.cardDao().updateCard(updatedCard)
            }
        }
    }

    /**
     * Import account balances with duplicate checking and merging.
     * Returns a mapping of backup AccountBalance ID -> final AccountBalance ID.
     */
    private suspend fun importAccountBalancesWithMerge(
        balances: List<AccountBalanceEntity>,
        targetOwnerId: String
    ): Map<String, String> {
        val accountIdMap = mutableMapOf<String, String>()

        balances
            .groupBy { it.bankName to it.accountLast4 }
            .values
            .forEach { group ->
                val latest = group.maxByOrNull { it.timestamp } ?: return@forEach
                val sanitized = latest.sanitize()
                val existing = database.accountBalanceDao().getLatestBalance(sanitized.bankName, sanitized.accountLast4)
                    ?: (if (sanitized.id.isNotBlank()) database.accountBalanceDao().getAccountById(sanitized.id) else null)

                val owner = targetOwnerId.ifBlank { sanitized.ownerId }
                val finalId = if (existing == null) {
                    val id = sanitized.id.ifBlank { com.reddy.vittify.utils.NanoId.generate() }
                    database.accountBalanceDao().insertBalance(sanitized.copy(id = id, ownerId = owner))
                    id
                } else {
                    val toUpdate = if (sanitized.updatedAt.isAfter(existing.updatedAt)) {
                        sanitized.copy(
                            id = existing.id,
                            createdAt = existing.createdAt,
                            ownerId = existing.ownerId.ifBlank { owner }
                        )
                    } else {
                        existing.copy(
                            accountName = existing.accountName ?: sanitized.accountName,
                            customId = existing.customId ?: sanitized.customId,
                            creditLimit = existing.creditLimit ?: sanitized.creditLimit,
                            iconName = existing.iconName.ifBlank { sanitized.iconName },
                            iconResId = if (existing.iconResId == 0) sanitized.iconResId else existing.iconResId,
                            color = if (existing.color == "#33B5E5") sanitized.color else existing.color,
                            balance = if (sanitized.timestamp.isAfter(existing.timestamp)) sanitized.balance else existing.balance,
                            timestamp = if (sanitized.timestamp.isAfter(existing.timestamp)) sanitized.timestamp else existing.timestamp,
                            ownerId = existing.ownerId.ifBlank { owner }
                        )
                    }
                    database.accountBalanceDao().insertBalance(toUpdate)
                    existing.id
                }

                group.forEach { b ->
                    if (b.id.isNotBlank()) {
                        accountIdMap[b.id] = finalId
                    }
                }
            }

        return accountIdMap
    }

    /**
     * Import subscriptions with duplicate checking
     */
    private suspend fun importSubscriptionsWithMerge(
        subscriptions: List<SubscriptionEntity>,
        targetOwnerId: String
    ) {
        val existingSubscriptions = database.subscriptionDao().getAllSubscriptions().first()
        val existingKeys = existingSubscriptions.map { "${it.merchantName}_${it.amount}" }.toSet()

        subscriptions.forEach { subscription ->
            val sanitizedSubscription = subscription.sanitize()
            val key = "${sanitizedSubscription.merchantName}_${sanitizedSubscription.amount}"
            if (!existingKeys.contains(key)) {
                val owner = targetOwnerId.ifBlank { sanitizedSubscription.ownerId }
                val newSubscription = sanitizedSubscription.copy(id = 0, ownerId = owner)
                database.subscriptionDao().insertSubscription(newSubscription)
            }
        }
    }

    /**
     * Import merchant mappings with merge
     */
    private suspend fun importMerchantMappingsWithMerge(mappings: List<MerchantMappingEntity>) {
        mappings.forEach { mapping ->
            // Merchant mappings use merchant name as primary key, so just insert/replace
            database.merchantMappingDao().insertMapping(mapping)
        }
    }

    /**
     * Import budgets with merge and ID remapping
     */
    private suspend fun importBudgetsWithMerge(
        budgets: List<BudgetEntity>,
        limits: List<BudgetCategoryLimitEntity>,
        targetOwnerId: String
    ) {
        val existingBudgets = database.budgetDao().getAllBudgets().first()
        // Key by Name + Year + Month
        val existingKeys = existingBudgets.map { "${it.name}_${it.year}_${it.month}" }.toSet()

        // Map to track OldID -> NewID for limits
        val idMap = mutableMapOf<Long, Long>()

        budgets.forEach { budget ->
            val sanitizedBudget = budget.sanitize()
            val key = "${sanitizedBudget.name}_${sanitizedBudget.year}_${sanitizedBudget.month}"
            if (!existingKeys.contains(key)) {
                val oldId = sanitizedBudget.id
                val owner = targetOwnerId.ifBlank { sanitizedBudget.ownerId }
                val newBudget = sanitizedBudget.copy(id = 0, ownerId = owner)
                val newId = database.budgetDao().insertBudget(newBudget)

                if (oldId != 0L) {
                    idMap[oldId] = newId
                }
            }
        }

        // Import limits using new budget IDs
        limits.forEach { limit ->
            val newBudgetId = idMap[limit.budgetId]
            if (newBudgetId != null) {
                // Check if limit already exists for this budget?
                // Since this is a NEW budget (checked above), limits naturally won't exist.
                val newLimit = limit.copy(id = 0, budgetId = newBudgetId)
                database.budgetDao().insertCategoryLimit(newLimit)
            }
        }
    }

    private suspend fun importWebhookProfilesWithMerge(profiles: List<WebhookProfileBackup>) {
        if (profiles.isEmpty()) return
        val baseCurrency = userPreferencesRepository.baseCurrency.first()
        profiles.forEach { profile ->
            val existing = database.webhookProfileDao().getProfileById(profile.id)
            if (existing == null) {
                webhookRepository.saveProfile(profile.toDraft(baseCurrency))
            }
        }
    }

    private suspend fun importExchangeRatesWithMerge(rates: List<ExchangeRateEntity>) {
        rates.forEach { rate ->
            database.exchangeRateDao().insertExchangeRate(rate)
        }
    }

    private fun parseRangePreset(value: String): WebhookRangePreset =
        runCatching { WebhookRangePreset.valueOf(value) }
            .getOrElse { WebhookRangePreset.SINCE_LAST_SUCCESS }

    private fun parseLocalDateTime(value: String?): java.time.LocalDateTime? =
        value?.let { runCatching { java.time.LocalDateTime.parse(it) }.getOrNull() }

    // Currency is no longer part of the backup model — derive from the importing user's own
    // baseCurrency preference, same as the runtime sync path does. Caller passes it in so we
    // don't re-read DataStore once per profile when restoring multiple webhooks.
    private fun WebhookProfileBackup.toDraft(currency: String): com.reddy.vittify.data.webhook.WebhookProfileDraft =
        com.reddy.vittify.data.webhook.WebhookProfileDraft(
            id = id,
            name = name,
            url = url,
            enabled = enabled,
            dataTypes = dataTypes
                .mapNotNull {
                    runCatching {
                        com.reddy.vittify.data.database.entity.WebhookDataType.valueOf(it)
                    }.getOrNull()
                }
                .toSet(),
            rangePreset = parseRangePreset(rangePreset),
            customStart = parseLocalDateTime(customStart),
            customEnd = parseLocalDateTime(customEnd),
            currency = currency,
            headers = headers
        )

    /**
     * Import user preferences
     */
    private suspend fun importPreferences(preferences: PreferencesSnapshot?) {
        if (preferences == null) return

        // Theme preferences
        preferences.theme?.let { theme ->
            theme.isDarkThemeEnabled?.let {
                userPreferencesRepository.updateDarkTheme(it)
            }
            userPreferencesRepository.updateDynamicColor(theme.isDynamicColorEnabled)

            // Extended Theme Preferences
            theme.isAmoledMode?.let {
                userPreferencesRepository.updateAmoledMode(it)
            }

            theme.navigationBarStyle?.let { styleName ->
                try {
                    val style = NavigationBarStyle.valueOf(styleName)
                    userPreferencesRepository.updateNavigationBarStyle(style)
                } catch (e: Exception) {
                    // Ignore invalid style
                }
            }

            theme.appFont?.let { fontName ->
                try {
                    val font = AppFont.valueOf(fontName)
                    userPreferencesRepository.updateAppFont(font)
                } catch (e: Exception) {
                    // Ignore invalid font
                }
            }

            theme.themeStyle?.let { styleName ->
                try {
                    val style = ThemeStyle.valueOf(styleName)
                    userPreferencesRepository.updateThemeStyle(style)
                } catch (e: Exception) {
                    // Ignore invalid style
                }
            }

            theme.accentColor?.let { colorName ->
                try {
                    val color = AccentColor.valueOf(colorName)
                    userPreferencesRepository.updateAccentColor(color)
                } catch (e: Exception) {
                    // Ignore invalid color
                }
            }

            theme.hideNavigationLabels?.let {
                userPreferencesRepository.updateHideNavigationLabels(it)
            }

            theme.hidePillIndicator?.let {
                userPreferencesRepository.updateHidePillIndicator(it)
            }

            theme.profileSwitcherInFooter?.let {
                userPreferencesRepository.updateProfileSwitcherInFooter(it)
            }

            theme.blurEffects?.let {
                userPreferencesRepository.updateBlurEffects(it)
            }

            theme.appIcon?.let { iconName ->
                try {
                    val icon = AppIcon.valueOf(iconName)
                    userPreferencesRepository.updateAppIcon(icon)
                } catch (e: Exception) {
                    // Ignore invalid icon
                }
            }
        }

        // SMS preferences
        preferences.sms?.let { sms ->
            userPreferencesRepository.updateHasSkippedSmsPermission(sms.hasSkippedSmsPermission)
            userPreferencesRepository.updateSmsScanMonths(sms.smsScanMonths)
            sms.lastScanTimestamp?.let {
                userPreferencesRepository.updateLastScanTimestamp(it)
            }
            sms.lastScanPeriod?.let {
                userPreferencesRepository.updateLastScanPeriod(it)
            }
        }

        // Developer preferences
        preferences.developer?.let { developer ->
            userPreferencesRepository.updateDeveloperMode(developer.isDeveloperModeEnabled)
            userPreferencesRepository.setWebhookModeEnabled(developer.isWebhookModeEnabled)
            userPreferencesRepository.setTokenInfoEnabled(developer.isTokenInfoEnabled)
            developer.systemPrompt?.let {
                userPreferencesRepository.updateSystemPrompt(it)
            }
        }

        // App preferences
        preferences.app?.let { app ->
            userPreferencesRepository.updateHasShownScanTutorial(app.hasShownScanTutorial)
            app.firstLaunchTime?.let {
                userPreferencesRepository.updateFirstLaunchTime(it)
            }
            app.lastReviewPromptTime?.let {
                userPreferencesRepository.updateLastReviewPromptTime(it)
            }
        }

        // Profile Preferences
        preferences.profile?.let { profile ->
            userPreferencesRepository.updateUserName(profile.userName)

            // Profile Image
            if (profile.profileImageUri == "profile/profile_image") {
                val file = File(context.filesDir, "profile/profile_image")
                if (file.exists()) {
                    userPreferencesRepository.updateProfileImageUri(Uri.fromFile(file).toString())
                }
            } else {
                userPreferencesRepository.updateProfileImageUri(profile.profileImageUri)
            }

            userPreferencesRepository.updateProfileBackgroundColor(profile.profileBackgroundColor)
        }

        // Currency Preferences
        preferences.currency?.let { currencyPrefs ->
            userPreferencesRepository.setUnifiedCurrencyEnabled(currencyPrefs.unifiedCurrencyEnabled)
            currencyPrefs.unifiedCurrencyCode?.let {
                userPreferencesRepository.setUnifiedCurrencyCode(it)
            }
            userPreferencesRepository.setDefaultCurrencyEnabled(currencyPrefs.defaultCurrencyEnabled)
            currencyPrefs.defaultCurrencyCode?.let {
                userPreferencesRepository.setDefaultCurrencyCode(it)
            }
            (currencyPrefs.customCurrencies ?: emptyList()).forEach { customCurrency ->
                userPreferencesRepository.addCustomCurrency(customCurrency)
            }
        }
    }

    /**
     * Post-import migration script to link imported transactions with account balances.
     */
    private suspend fun runPostImportMigrations() {
        val db = database.openHelper.writableDatabase

        // 1. Fetch all account_balances
        val balanceCursor = db.query("SELECT id, bank_name, account_last4 FROM account_balances")
        val accounts = mutableListOf<Triple<String, String, String>>()
        while (balanceCursor.moveToNext()) {
            val id = balanceCursor.getString(0) ?: ""
            val bankName = balanceCursor.getString(1) ?: ""
            val accountLast4 = balanceCursor.getString(2) ?: ""
            if (id.isNotBlank() && bankName.isNotBlank() && accountLast4.isNotBlank()) {
                accounts.add(Triple(id, bankName, accountLast4))
            }
        }
        balanceCursor.close()

        // 2. Link transactions to account_balances
        for ((id, _, accountLast4) in accounts) {
            // Update from_account_id on transfer transactions
            db.execSQL(
                "UPDATE transactions SET from_account_id = ? WHERE (from_account_id IS NULL OR from_account_id = '') AND (from_account = ? OR from_account LIKE '%' || ?)",
                arrayOf<Any?>(id, accountLast4, accountLast4)
            )

            // Update to_account_id on transfer transactions
            db.execSQL(
                "UPDATE transactions SET to_account_id = ? WHERE (to_account_id IS NULL OR to_account_id = '') AND (to_account = ? OR to_account LIKE '%' || ?)",
                arrayOf<Any?>(id, accountLast4, accountLast4)
            )

            // Update primary account_id on transactions if missing
            db.execSQL(
                "UPDATE transactions SET account_id = ? WHERE (account_id IS NULL OR account_id = '') AND (from_account = ? OR from_account LIKE '%' || ? OR to_account = ? OR to_account LIKE '%' || ?)",
                arrayOf<Any?>(id, accountLast4, accountLast4, accountLast4, accountLast4)
            )
        }
    }
}
