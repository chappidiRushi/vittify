package com.reddy.vittify.data.repository

import com.reddy.vittify.data.database.dao.RecentSubcategoryInfo
import com.reddy.vittify.data.database.dao.TransactionDao
import com.reddy.vittify.data.database.dao.TransactionItemDao
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionItemEntity
import com.reddy.vittify.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import com.reddy.vittify.domain.model.BalanceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val transactionBalanceService: dagger.Lazy<com.reddy.vittify.domain.service.TransactionBalanceService>,
    private val syncChangeTracker: dagger.Lazy<com.reddy.vittify.data.sync.SyncChangeTracker>? = null,
    private val p2pPreferences: dagger.Lazy<com.reddy.vittify.data.sync.P2pSyncPreferencesRepository>? = null,
    private val accountBalanceDao: dagger.Lazy<com.reddy.vittify.data.database.dao.AccountBalanceDao>? = null
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
        .map { list -> list.filter { it.transactionType != TransactionType.BALANCE_UPDATE } }

    fun getTransactionCount(): Flow<Int> = transactionDao.getTransactionCount()

    suspend fun getTransactionById(id: Long): TransactionEntity? =
            transactionDao.getTransactionById(id)

    suspend fun getTransactionsUpdatedBetween(
            updatedAfter: LocalDateTime,
            updatedBefore: LocalDateTime,
            currency: String
    ): List<TransactionEntity> =
            transactionDao.getTransactionsUpdatedBetween(updatedAfter, updatedBefore, currency)

    suspend fun getTransactionsBetweenDatesByCurrency(
            startDate: LocalDateTime,
            endDate: LocalDateTime,
            currency: String
    ): List<TransactionEntity> =
            transactionDao.getTransactionsBetweenDatesByCurrency(startDate, endDate, currency)

    fun getTransactionsBetweenDates(
            startDate: LocalDateTime,
            endDate: LocalDateTime
    ): Flow<List<TransactionEntity>> =
            transactionDao.getTransactionsBetweenDates(startDate, endDate)

    fun getTransactionsBetweenDates(
            startDate: LocalDate,
            endDate: LocalDate
    ): Flow<List<TransactionEntity>> =
            transactionDao.getTransactionsBetweenDates(
                    startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59)
            )

    /**
     * Gets transactions filtered at the database level for better performance. Combines date range,
     * currency, and transaction type filters to reduce memory usage.
     *
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @param currency Currency code to filter by (e.g., "INR", "USD")
     * @param transactionType Optional transaction type filter (null means all types)
     * @return Flow of filtered transactions
     */
    fun getTransactionsFiltered(
            startDate: LocalDate,
            endDate: LocalDate,
            currency: String,
            transactionType: TransactionType? = null
    ): Flow<List<TransactionEntity>> =
            transactionDao.getTransactionsFiltered(
                    startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59),
                    currency,
                    transactionType
            )

    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> =
            transactionDao.getTransactionsByType(type)

    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>> =
            transactionDao.getTransactionsByCategory(category)

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
            transactionDao.searchTransactions(query)

    fun getAllCurrencies(): Flow<List<String>> = transactionDao.getAllCurrencies()

    fun getCurrenciesForPeriod(
            startDate: LocalDateTime,
            endDate: LocalDateTime
    ): Flow<List<String>> = transactionDao.getCurrenciesForPeriod(startDate, endDate)

    fun getAllCategories(): Flow<List<String>> = transactionDao.getAllCategories()

    /**
     * Gets the top N categories by usage count (number of transactions). Useful for showing user's
     * most frequently used categories in notifications.
     *
     * @param limit Maximum number of categories to return (default: 3)
     * @return List of category names ordered by usage count (most used first)
     */
    suspend fun getTopCategoriesByUsage(limit: Int = 3): List<String> =
            transactionDao.getTopCategoriesByUsage(limit)

    fun getAllMerchants(): Flow<List<String>> = transactionDao.getAllMerchants()

    suspend fun getTotalAmountByTypeAndPeriod(
            type: TransactionType,
            startDate: LocalDateTime,
            endDate: LocalDateTime
    ): Double? = transactionDao.getTotalAmountByTypeAndPeriod(type, startDate, endDate)

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val myDeviceId = p2pPreferences?.get()?.getDeviceId() ?: ""
        val owner = if (transaction.ownerId.isBlank()) {
            val accOwner = (transaction.accountId?.let { accountBalanceDao?.get()?.getBalanceById(it) }
                ?: transaction.fromAccount?.let { accountBalanceDao?.get()?.getAccountByLast4(it) })?.ownerId
            accOwner?.takeIf { it.isNotBlank() } ?: myDeviceId
        } else transaction.ownerId
        val stamped = transaction.copy(ownerId = owner)
        val id = transactionDao.insertTransaction(stamped)
        val entity = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        syncChangeTracker?.get()?.trackTransaction(entity, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
        return id
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        val myDeviceId = p2pPreferences?.get()?.getDeviceId() ?: ""
        val stampedList = transactions.map { tx ->
            if (tx.ownerId.isBlank()) {
                val accOwner = (tx.accountId?.let { accountBalanceDao?.get()?.getBalanceById(it) }
                    ?: tx.fromAccount?.let { accountBalanceDao?.get()?.getAccountByLast4(it) })?.ownerId
                val owner = accOwner?.takeIf { it.isNotBlank() } ?: myDeviceId
                tx.copy(ownerId = owner)
            } else tx
        }
        transactionDao.insertTransactions(stampedList)
        stampedList.forEach {
            syncChangeTracker?.get()?.trackTransaction(it, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
        }
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
        syncChangeTracker?.get()?.trackTransaction(transaction, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity, hardDelete: Boolean = false) {
        val now = LocalDateTime.now()
        if (hardDelete) {
            transactionDao.deleteTransaction(transaction)
        } else {
            transactionDao.softDeleteTransaction(transaction.id, now)
        }
        val deletedEntity = transaction.copy(isDeleted = true, updatedAt = now)
        triggerRecalculationForTransaction(deletedEntity)
        syncChangeTracker?.get()?.trackTransaction(deletedEntity, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
    }

    suspend fun deleteTransactionById(id: Long, hardDelete: Boolean = false) {
        val transaction = transactionDao.getTransactionById(id) ?: return
        val now = LocalDateTime.now()
        if (hardDelete) {
            transactionDao.deleteTransactionById(id)
        } else {
            transactionDao.softDeleteTransaction(id, now)
        }
        val deletedEntity = transaction.copy(isDeleted = true, updatedAt = now)
        triggerRecalculationForTransaction(deletedEntity)
        syncChangeTracker?.get()?.trackTransaction(deletedEntity, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
    }

    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()

    suspend fun deleteSampleTransactions() = transactionDao.deleteSampleTransactions()

    // Helper method to check if transaction exists by SMS body
    suspend fun getTransactionBySmsBody(smsBody: String): TransactionEntity? =
            transactionDao.getTransactionBySmsBody(smsBody)

    // Helper method to look up a transaction by its stable UUID
    suspend fun getTransactionByUuid(uuid: String): TransactionEntity? =
            transactionDao.getTransactionByUuid(uuid)

    suspend fun getTransactionsByReferenceAndAmount(
        reference: String,
        amount: BigDecimal,
        accountId: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity> =
        transactionDao.getTransactionsByReferenceAndAmount(
            reference = reference,
            amount = amount,
            accountId = accountId,
            startDate = startDate,
            endDate = endDate
        )

    suspend fun undoDeleteTransaction(transaction: TransactionEntity) {
        val now = LocalDateTime.now()
        val restored = transaction.copy(isDeleted = false, updatedAt = now)
        transactionDao.updateTransaction(restored)
        triggerRecalculationForTransaction(restored)
        syncChangeTracker?.get()?.trackTransaction(restored, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
    }

    suspend fun deleteTransactions(transactions: List<TransactionEntity>, hardDelete: Boolean = false) {
        val now = LocalDateTime.now()
        val transactionIds = transactions.map { it.id }
        if (hardDelete) {
            transactionDao.deleteTransactionsByIds(transactionIds)
        } else {
            transactionDao.softDeleteTransactions(transactionIds, now)
        }
        transactions.forEach { triggerRecalculationForTransaction(it.copy(isDeleted = true, updatedAt = now)) }
        transactions.forEach {
            val deleted = it.copy(isDeleted = true, updatedAt = now)
            triggerRecalculationForTransaction(deleted)
            syncChangeTracker?.get()?.trackTransaction(deleted, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
        }
    }

    suspend fun undoDeleteTransactions(transactions: List<TransactionEntity>) {
        val now = LocalDateTime.now()
        transactions.forEach { transaction ->
            val restored = transaction.copy(isDeleted = false, updatedAt = now)
            transactionDao.updateTransaction(restored)
            triggerRecalculationForTransaction(restored)
            syncChangeTracker?.get()?.trackTransaction(restored, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
        }
    }

    // Archived transactions operations
    fun getArchivedTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getArchivedTransactions()

    fun getArchivedTransactionsCount(): Flow<Int> =
        transactionDao.getArchivedTransactionsCount()

    suspend fun restoreArchivedTransaction(transaction: TransactionEntity) {
        undoDeleteTransaction(transaction)
    }

    suspend fun restoreArchivedTransactions(transactions: List<TransactionEntity>) {
        undoDeleteTransactions(transactions)
    }

    suspend fun permanentlyDeleteArchivedTransactions(transactions: List<TransactionEntity>) {
        val transactionIds = transactions.map { it.id }
        transactionDao.deleteTransactionsByIds(transactionIds)
    }

    suspend fun permanentlyDeleteArchivedTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun permanentlyDeleteAllArchivedTransactions(): Int =
        transactionDao.deleteAllArchivedTransactions()

    suspend fun autoDeleteArchivedTransactions(days: Int): Int {
        val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
        return transactionDao.deleteArchivedTransactionsOlderThan(cutoffDate)
    }

    private suspend fun triggerRecalculationForTransaction(transaction: TransactionEntity) {
        transactionBalanceService.get().recalculateAffectedAccounts(transaction)
    }

    suspend fun updateCategoryForMerchant(merchantName: String, newCategory: String) {
        transactionDao.updateCategoryForMerchant(merchantName, newCategory)
    }

    suspend fun updateCategoryAndSubcategoryForMerchantContains(merchantName: String, newCategory: String, newSubcategory: String?) {
        transactionDao.updateCategoryAndSubcategoryForMerchantContains(merchantName, newCategory, newSubcategory)
    }

    // Transaction Items
    fun getItemsForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>> =
        transactionItemDao.getItemsForTransaction(transactionId)

    suspend fun getItemsForTransactionSync(transactionId: Long): List<TransactionItemEntity> =
        transactionItemDao.getItemsForTransactionSync(transactionId)

    suspend fun getItemsForTransactions(transactionIds: List<Long>): List<TransactionItemEntity> =
        transactionItemDao.getItemsForTransactions(transactionIds)

    suspend fun getSplitTransactionIds(transactionIds: List<Long>): List<Long> =
        transactionItemDao.getSplitTransactionIds(transactionIds)

    suspend fun insertTransactionItem(item: TransactionItemEntity) =
        transactionItemDao.insertItem(item)

    suspend fun insertTransactionItems(items: List<TransactionItemEntity>) =
        transactionItemDao.insertItems(items)

    suspend fun updateTransactionItem(item: TransactionItemEntity) =
        transactionItemDao.updateItem(item)

    suspend fun deleteTransactionItem(item: TransactionItemEntity) =
        transactionItemDao.deleteItem(item)

    suspend fun deleteItemsForTransaction(transactionId: Long) =
        transactionItemDao.deleteItemsForTransaction(transactionId)

    suspend fun getOtherTransactionCountForMerchant(merchantName: String, excludeId: Long): Int {
        return transactionDao.getTransactionCountForMerchant(merchantName, excludeId)
    }

    /** Returns non-deleted transactions whose merchant name contains the given keyword, excluding one id. */
    suspend fun getTransactionsByMerchantContains(
        merchantName: String,
        excludeId: Long
    ): List<TransactionEntity> {
        return transactionDao.getTransactionsByMerchantContains(merchantName, excludeId)
    }

    suspend fun searchTransactionsList(searchQuery: String): List<TransactionEntity> {
        return transactionDao.searchTransactionsList(searchQuery)
    }

    suspend fun searchTransactionsForGlobal(query: String, limit: Int = 20): List<TransactionEntity> {
        return transactionDao.searchTransactionsForGlobal(query, limit)
    }

    // Additional methods for Home screen
    data class MonthlyBreakdown(
            val total: BigDecimal,
            val income: BigDecimal,
            val expenses: BigDecimal
    )

    fun getCurrentMonthBreakdown(): Flow<MonthlyBreakdown> {
        val now = LocalDate.now()
        val startDate = now.withDayOfMonth(1).atStartOfDay()
        val endDate = now.atTime(23, 59, 59)

        return transactionDao.getTransactionsBetweenDates(startDate, endDate).map { transactions ->
            val income =
                    transactions.filter { it.transactionType == TransactionType.INCOME }.fold(
                                    BigDecimal.ZERO
                            ) { acc, transaction -> acc + transaction.amount }
            val expenses =
                    transactions.filter { it.transactionType == TransactionType.EXPENSE }.fold(
                                    BigDecimal.ZERO
                            ) { acc, transaction -> acc + transaction.amount }
            MonthlyBreakdown(total = income - expenses, income = income, expenses = expenses)
        }
    }

    fun getCurrentMonthTotal(): Flow<BigDecimal> {
        return getCurrentMonthBreakdown().map { it.total }
    }

    fun getLastMonthBreakdown(): Flow<MonthlyBreakdown> {
        val now = LocalDate.now()
        val dayOfMonth = now.dayOfMonth
        val lastMonth = now.minusMonths(1)

        // Compare same period: if today is 10th, compare 1st-10th of last month
        val startDate = lastMonth.withDayOfMonth(1).atStartOfDay()
        val lastMonthMaxDay = min(dayOfMonth, lastMonth.lengthOfMonth())
        val endDate = lastMonth.withDayOfMonth(lastMonthMaxDay).atTime(23, 59, 59)

        return transactionDao.getTransactionsBetweenDates(startDate, endDate).map { transactions ->
            val income =
                    transactions.filter { it.transactionType == TransactionType.INCOME }.fold(
                                    BigDecimal.ZERO
                            ) { acc, transaction -> acc + transaction.amount }
            val expenses =
                    transactions.filter { it.transactionType == TransactionType.EXPENSE }.fold(
                                    BigDecimal.ZERO
                            ) { acc, transaction -> acc + transaction.amount }
            MonthlyBreakdown(total = income - expenses, income = income, expenses = expenses)
        }
    }

    fun getLastMonthTotal(): Flow<BigDecimal> {
        return getLastMonthBreakdown().map { it.total }
    }

    // Currency-grouped breakdown methods
    fun getCurrentMonthBreakdownByCurrency(filter: ((TransactionEntity) -> Boolean)? = null): Flow<Map<String, MonthlyBreakdown>> {
        val now = LocalDate.now()
        val startDate = now.withDayOfMonth(1).atStartOfDay()
        val endDate = now.atTime(23, 59, 59)

        return transactionDao.getTransactionsBetweenDates(startDate, endDate).map { transactions ->
            val effectiveList = if (filter != null) transactions.filter(filter) else transactions
            effectiveList.groupBy { it.currency }.mapValues { (_, currencyTransactions) ->
                val income =
                        currencyTransactions
                                .filter { it.transactionType == TransactionType.INCOME }
                                .fold(BigDecimal.ZERO) { acc, transaction ->
                                    acc + transaction.amount
                                }
                val expenses =
                        currencyTransactions
                                .filter { it.transactionType == TransactionType.EXPENSE }
                                .fold(BigDecimal.ZERO) { acc, transaction ->
                                    acc + transaction.amount
                                }
                MonthlyBreakdown(total = income - expenses, income = income, expenses = expenses)
            }
        }
    }

    fun getLastMonthBreakdownByCurrency(filter: ((TransactionEntity) -> Boolean)? = null): Flow<Map<String, MonthlyBreakdown>> {
        val now = LocalDate.now()
        val dayOfMonth = now.dayOfMonth
        val lastMonth = now.minusMonths(1)

        // Compare same period: if today is 10th, compare 1st-10th of last month
        val startDate = lastMonth.withDayOfMonth(1).atStartOfDay()
        val lastMonthMaxDay = min(dayOfMonth, lastMonth.lengthOfMonth())
        val endDate = lastMonth.withDayOfMonth(lastMonthMaxDay).atTime(23, 59, 59)

        return transactionDao.getTransactionsBetweenDates(startDate, endDate).map { transactions ->
            val effectiveList = if (filter != null) transactions.filter(filter) else transactions
            effectiveList.groupBy { it.currency }.mapValues { (_, currencyTransactions) ->
                val income =
                        currencyTransactions
                                .filter { it.transactionType == TransactionType.INCOME }
                                .fold(BigDecimal.ZERO) { acc, transaction ->
                                    acc + transaction.amount
                                }
                val expenses =
                        currencyTransactions
                                .filter { it.transactionType == TransactionType.EXPENSE }
                                .fold(BigDecimal.ZERO) { acc, transaction ->
                                    acc + transaction.amount
                                }
                MonthlyBreakdown(total = income - expenses, income = income, expenses = expenses)
            }
        }
    }

    fun getCurrentYearBreakdownByCurrency(filter: ((TransactionEntity) -> Boolean)? = null): Flow<Map<String, MonthlyBreakdown>> {
        val now = LocalDate.now()
        val startDate = now.withDayOfYear(1).atStartOfDay()
        val endDate = now.atTime(23, 59, 59)

        return transactionDao.getTransactionsBetweenDates(startDate, endDate).map { transactions ->
            val effectiveList = if (filter != null) transactions.filter(filter) else transactions
            effectiveList.groupBy { it.currency }.mapValues { (_, currencyTransactions) ->
                val income =
                    currencyTransactions
                        .filter { it.transactionType == TransactionType.INCOME }
                        .fold(BigDecimal.ZERO) { acc, transaction ->
                            acc + transaction.amount
                        }
                val expenses =
                    currencyTransactions
                        .filter { it.transactionType == TransactionType.EXPENSE }
                        .fold(BigDecimal.ZERO) { acc, transaction ->
                            acc + transaction.amount
                        }
                MonthlyBreakdown(total = income - expenses, income = income, expenses = expenses)
            }
        }
    }

    fun getRecentTransactions(limit: Int = 5, filter: ((TransactionEntity) -> Boolean)? = null): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions().map { transactions ->
            val effectiveList = if (filter != null) transactions.filter(filter) else transactions
            effectiveList
                .filter { it.transactionType != TransactionType.BALANCE_UPDATE }
                .sortedByDescending { it.dateTime }
                .take(limit)
        }
    }

    fun getTransactionsByAccountId(
            accountId: String,
            accountLast4: String
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccountId(accountId, accountLast4)
    }

    fun getTransactionsByAccount(
            bankName: String,
            accountLast4: String
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccount(bankName, accountLast4)
    }

    fun getTransactionsByAccountIdAndDateRange(
            accountId: String,
            accountLast4: String,
            startDate: LocalDateTime,
            endDate: LocalDateTime
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccountIdAndDateRange(
                accountId,
                accountLast4,
                startDate,
                endDate
        )
    }

    fun getTransactionsByAccountAndDateRange(
            bankName: String,
            accountLast4: String,
            startDate: LocalDateTime,
            endDate: LocalDateTime
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccountAndDateRange(
                bankName,
                accountLast4,
                startDate,
                endDate
        )
    }

    // Methods for batch rule application
    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        // Get all non-deleted transactions as a list (not Flow) for batch processing
        // Use a large date range to get all transactions
        val startDate = LocalDateTime.of(2000, 1, 1, 0, 0)
        val endDate = LocalDateTime.now().plusYears(10)
        return transactionDao.getTransactionsBetweenDatesList(startDate, endDate)
    }

    suspend fun getUncategorizedTransactions(): List<TransactionEntity> {
        // Get all transactions without a category or with "Others" category
        return getAllTransactionsList().filter { transaction ->
            transaction.category.isNullOrBlank() || transaction.category == "Others"
        }
    }

    suspend fun updateAccountIdForTransactions(
            oldAccountId: String,
            newAccountId: String
    ) {
        transactionDao.updateAccountIdForTransactions(
                oldAccountId,
                newAccountId
        )
    }

    suspend fun updateTransactionsCategory(oldCategory: String, newCategory: String, newSubcategory: String?) {
        transactionDao.updateTransactionsCategory(oldCategory, newCategory, newSubcategory)
    }

    suspend fun getTransactionCountByCategory(category: String): Int {
        return transactionDao.getTransactionCountByCategory(category)
    }

    suspend fun findPotentialDuplicates(
        amount: BigDecimal,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity> {
        return transactionDao.findPotentialDuplicates(startDate, endDate)
            .filter { it.amount.compareTo(amount) == 0 }
    }

    fun getRecentlyUsedSubcategories(limit: Int = 10): Flow<List<RecentSubcategoryInfo>> =
        transactionDao.getRecentlyUsedSubcategories(limit)

    suspend fun unlinkTransactionsByIds(transactionIds: List<Long>): Int {
        return transactionDao.unlinkTransactionsByIds(transactionIds)
    }

    suspend fun unlinkTransactionsForAccount(accountId: String): Int {
        return transactionDao.unlinkTransactionsForAccount(accountId)
    }

    suspend fun updateMerchantName(transactionId: Long, merchantName: String): Int {
        return transactionDao.updateMerchantName(transactionId, merchantName)
    }

    suspend fun markDuplicateDismissed(transactionId: Long, isDismissed: Boolean = true): Int {
        return transactionDao.updateDuplicateDismissed(transactionId, isDismissed)
    }

    suspend fun markDuplicatesDismissed(transactionIds: List<Long>, isDismissed: Boolean = true): Int {
        return transactionDao.updateDuplicatesDismissed(transactionIds, isDismissed)
    }

    /**
     * Handles transaction cleanup when an account is deleted:
     * 1. Transfers FROM this deleted account TO a surviving account are converted to INCOME on the surviving account.
     * 2. Transfers FROM a surviving account TO this deleted account are converted to EXPENSE on the surviving account.
     * 3. All other transactions (ordinary expenses, incomes, or transfers within this deleted account) are deleted.
     */
    suspend fun handleAccountDeletion(
        bankName: String,
        accountLast4: String,
        accountIds: List<String>
    ) {
        val deletedIdSet = accountIds.filter { it.isNotBlank() }.toSet()
        val allBalances = accountBalanceDao?.get()?.getAllBalances()?.first() ?: emptyList()
        val allSurvivingAccounts = allBalances.filter { it.id !in deletedIdSet }
        val survivingById = allSurvivingAccounts.associateBy { it.id }
        val survivingByLast4 = allSurvivingAccounts.associateBy { it.accountLast4.trim().lowercase() }

        // Fetch all active transactions
        val allTxns = transactionDao.getAllTransactionsIncludingDeleted().first().filter { !it.isDeleted }

        // Find transactions related to the deleted account
        val relatedTxns = allTxns.filter { tx ->
            val matchesId = (tx.accountId != null && tx.accountId in deletedIdSet) ||
                    (tx.fromAccountId != null && tx.fromAccountId in deletedIdSet) ||
                    (tx.toAccountId != null && tx.toAccountId in deletedIdSet)
            val matchesLast4 = isAccountMatch(accountLast4, tx.fromAccount) ||
                    isAccountMatch(accountLast4, tx.toAccount)
            matchesId || matchesLast4
        }

        val toDelete = mutableListOf<TransactionEntity>()

        for (tx in relatedTxns) {
            if (tx.transactionType == TransactionType.TRANSFER) {
                val isSourceDeleted = (tx.fromAccountId != null && tx.fromAccountId in deletedIdSet) ||
                        isAccountMatch(accountLast4, tx.fromAccount)
                val isDestDeleted = (tx.toAccountId != null && tx.toAccountId in deletedIdSet) ||
                        isAccountMatch(accountLast4, tx.toAccount)

                when {
                    isSourceDeleted && !isDestDeleted -> {
                        // Transfer from deleted Account A -> surviving Account B
                        // Converted to INCOME on Account B
                        val targetAcc = tx.toAccountId?.let { survivingById[it] }
                            ?: tx.toAccount?.let { survivingByLast4[it.trim().lowercase()] }

                        val note = "Transferred from deleted account ($bankName ••• $accountLast4)"
                        val newDesc = if (tx.description.isNullOrBlank()) note else "${tx.description} • $note"
                        val converted = tx.copy(
                            transactionType = TransactionType.INCOME,
                            accountId = targetAcc?.id ?: tx.toAccountId,
                            fromAccount = null,
                            fromAccountId = null,
                            toAccount = null,
                            toAccountId = null,
                            amount = tx.targetAmount ?: tx.amount,
                            description = newDesc,
                            category = if (tx.category.isBlank() || tx.category.equals("Transfer", ignoreCase = true) || tx.category.equals("Self Transfer", ignoreCase = true)) "Income" else tx.category,
                            ownerId = targetAcc?.ownerId?.takeIf { it.isNotBlank() } ?: tx.ownerId,
                            updatedAt = LocalDateTime.now()
                        )
                        updateTransaction(converted)
                    }
                    !isSourceDeleted && isDestDeleted -> {
                        // Transfer from surviving Account B -> deleted Account A
                        // Converted to EXPENSE on Account B
                        val sourceAcc = tx.fromAccountId?.let { survivingById[it] }
                            ?: tx.fromAccount?.let { survivingByLast4[it.trim().lowercase()] }

                        val note = "Transferred to deleted account ($bankName ••• $accountLast4)"
                        val newDesc = if (tx.description.isNullOrBlank()) note else "${tx.description} • $note"
                        val converted = tx.copy(
                            transactionType = TransactionType.EXPENSE,
                            accountId = sourceAcc?.id ?: tx.fromAccountId,
                            fromAccount = null,
                            fromAccountId = null,
                            toAccount = null,
                            toAccountId = null,
                            description = newDesc,
                            category = if (tx.category.isBlank() || tx.category.equals("Transfer", ignoreCase = true) || tx.category.equals("Self Transfer", ignoreCase = true)) "Miscellaneous" else tx.category,
                            ownerId = sourceAcc?.ownerId?.takeIf { it.isNotBlank() } ?: tx.ownerId,
                            updatedAt = LocalDateTime.now()
                        )
                        updateTransaction(converted)
                    }
                    else -> {
                        // Both source and destination belong to deleted account (self-transfer)
                        toDelete.add(tx)
                    }
                }
            } else {
                // Ordinary non-transfer transaction belonging to deleted account
                toDelete.add(tx)
            }
        }

        if (toDelete.isNotEmpty()) {
            deleteTransactions(toDelete, hardDelete = false)
            unlinkTransactionsByIds(toDelete.map { it.id })
        }
    }

    private fun isAccountMatch(accountLast4: String, candidate: String?): Boolean {
        if (candidate == null) return false
        val clean = candidate.trim()
        val acct = accountLast4.trim()
        if (clean.isEmpty() || acct.isEmpty()) return false
        if (clean.equals(acct, ignoreCase = true)) return true
        if (clean.endsWith(acct, ignoreCase = true) || acct.endsWith(clean, ignoreCase = true)) return true
        val digits1 = clean.filter { it.isDigit() }
        val digits2 = acct.filter { it.isDigit() }
        if (digits1.isNotEmpty() && digits2.isNotEmpty()) {
            if (digits1.endsWith(digits2) || digits2.endsWith(digits1)) return true
        }
        return false
    }
}
