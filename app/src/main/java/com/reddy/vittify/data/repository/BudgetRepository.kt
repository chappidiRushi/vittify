package com.reddy.vittify.data.repository

import com.reddy.vittify.data.database.dao.BudgetDao
import com.reddy.vittify.data.database.dao.TransactionDao
import com.reddy.vittify.data.database.dao.TransactionItemDao
import com.reddy.vittify.data.database.entity.BudgetCategoryLimitEntity
import com.reddy.vittify.data.database.entity.BudgetEntity
import com.reddy.vittify.data.database.entity.BudgetTrackType
import com.reddy.vittify.data.database.entity.BudgetType
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionItemEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.database.dao.AccountBalanceDao
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

data class BudgetWithSpending(
    val budget: BudgetEntity,
    val currentSpending: BigDecimal,
    val categoryLimits: List<BudgetCategoryLimitEntity>,
    val categorySpending: Map<String, BigDecimal>,
    val daysRemaining: Int,
    val daysInMonth: Int
) {
    val remaining: BigDecimal get() = budget.amount - currentSpending
    val percentUsed: Float get() = if (budget.amount > BigDecimal.ZERO) {
        (currentSpending.toFloat() / budget.amount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isOverBudget: Boolean get() = currentSpending > budget.amount
    val spendingPerDay: BigDecimal get() {
        val daysPassed = daysInMonth - daysRemaining
        return if (daysPassed > 0) {
            currentSpending.divide(BigDecimal(daysPassed), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    val recommendedDailySpending: BigDecimal get() {
        return if (daysRemaining > 0 && remaining > BigDecimal.ZERO) {
            remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
}

data class CategoryLimitWithSpending(
    val limit: BudgetCategoryLimitEntity,
    val currentSpending: BigDecimal
) {
    val remaining: BigDecimal get() = limit.limitAmount - currentSpending
    val percentUsed: Float get() = if (limit.limitAmount > BigDecimal.ZERO) {
        (currentSpending.toFloat() / limit.limitAmount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isOverLimit: Boolean get() = currentSpending > limit.limitAmount
}

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val accountBalanceDao: AccountBalanceDao,
    private val currencyConversionService: CurrencyConversionService,
    @ApplicationScope private val externalScope: CoroutineScope,
    private val p2pPreferences: dagger.Lazy<com.reddy.vittify.data.sync.P2pSyncPreferencesRepository>? = null,
    private val syncChangeTracker: dagger.Lazy<com.reddy.vittify.data.sync.SyncChangeTracker>? = null
) {

    val allBudgets: StateFlow<List<BudgetEntity>> = budgetDao.getAllBudgets()
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun getAllBudgets(): Flow<List<BudgetEntity>> {
        return budgetDao.getAllBudgets()
    }

    fun getActiveBudgets(): Flow<List<BudgetEntity>> {
        return budgetDao.getActiveBudgets()
    }

    suspend fun getBudgetById(budgetId: Long): BudgetEntity? {
        return budgetDao.getBudgetById(budgetId)
    }

    suspend fun getBudgetByYearMonth(year: Int, month: Int): BudgetEntity? {
        return budgetDao.getBudgetByYearMonth(year, month)
    }

    fun getActiveBudgetsForMonth(year: Int, month: Int): Flow<List<BudgetEntity>> {
        return budgetDao.getActiveBudgetsForMonth(year, month)
    }

    suspend fun createBudget(
        name: String,
        amount: BigDecimal,
        year: Int,
        month: Int,
        currency: String = "INR"
    ): Long {
        val myDeviceId = p2pPreferences?.get()?.getDeviceId() ?: ""
        val budget = BudgetEntity(
            name = name,
            amount = amount,
            year = year,
            month = month,
            currency = currency,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            ownerId = myDeviceId
        )
        val id = budgetDao.insertBudget(budget)
        syncChangeTracker?.get()?.trackBudget(budget.copy(id = id), com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
        return id
    }

    suspend fun insertBudget(budget: BudgetEntity): Long {
        val owner = if (budget.ownerId.isBlank()) {
            p2pPreferences?.get()?.getDeviceId() ?: ""
        } else budget.ownerId
        val id = budgetDao.insertBudget(budget.copy(ownerId = owner))
        syncChangeTracker?.get()?.trackBudget(budget.copy(id = id, ownerId = owner), com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
        return id
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        val updated = budget.copy(updatedAt = LocalDateTime.now())
        budgetDao.updateBudget(updated)
        syncChangeTracker?.get()?.trackBudget(updated, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
    }

    suspend fun deleteBudget(budgetId: Long) {
        val existing = budgetDao.getBudgetById(budgetId)
        budgetDao.deleteBudget(budgetId)
        if (existing != null) {
            syncChangeTracker?.get()?.trackBudget(existing, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
        }
    }

    suspend fun deleteSampleBudgets() {
        budgetDao.deleteSampleBudgets()
    }

    suspend fun deleteAllBudgets() {
        budgetDao.deleteAllBudgets()
    }

    fun getCategoryLimitsForBudget(budgetId: Long): Flow<List<BudgetCategoryLimitEntity>> {
        return budgetDao.getCategoryLimitsForBudget(budgetId)
    }

    suspend fun getCategoryLimitsForBudgetSync(budgetId: Long): List<BudgetCategoryLimitEntity> {
        return budgetDao.getCategoryLimitsForBudgetSync(budgetId)
    }

    suspend fun addCategoryLimit(
        budgetId: Long,
        categoryName: String,
        limitAmount: BigDecimal
    ): Long {
        val limit = BudgetCategoryLimitEntity(
            budgetId = budgetId,
            categoryName = categoryName,
            limitAmount = limitAmount,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return budgetDao.insertCategoryLimit(limit)
    }

    suspend fun updateCategoryLimit(limit: BudgetCategoryLimitEntity) {
        budgetDao.updateCategoryLimit(limit.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteCategoryLimit(limitId: Long) {
        budgetDao.deleteCategoryLimit(limitId)
    }

    suspend fun deleteCategoryLimitsForBudget(budgetId: Long) {
        budgetDao.deleteCategoryLimitsForBudget(budgetId)
    }

    // Spending calculation methods
    suspend fun getBudgetWithSpending(budget: BudgetEntity): BudgetWithSpending {
        val startDate = budget.startDate
        val endDate = budget.endDate
        val now = LocalDateTime.now()

        // Get transactions for the budget period
        var transactions = transactionDao.getTransactionsBetweenDatesList(startDate, endDate)
            .filter { !it.isDeleted }
            
        // Filter by budget type
        transactions = if (budget.budgetType == BudgetType.EXPENSE) {
            transactions.filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.CREDIT }
        } else {
            transactions.filter { it.transactionType == TransactionType.INCOME }
        }
        
        // Filter by tracking type
        if (budget.trackType == BudgetTrackType.ADDED_ONLY) {
            transactions = transactions.filter { it.smsBody.isNullOrBlank() }
        }
        
        // Filter by accounts if specified
        if (budget.accountIds.isNotEmpty()) {
            val accounts = accountBalanceDao.getAllLatestBalances().first().associateBy { it.id.toString() }
            transactions = transactions.filter { txn ->
                val account = txn.accountId?.let { accounts[it] }
                if (account != null) {
                    val key = "${account.bankName}:${account.accountLast4}"
                    budget.accountIds.contains(key) || budget.accountIds.contains(txn.accountId)
                } else false
            }
        }
        
        // Convert currencies to match the budget's currency
        transactions = transactions.map { txn ->
            if (txn.currency != budget.currency) {
                val convertedAmount = currencyConversionService.convertAmount(txn.amount, txn.currency, budget.currency)
                txn.copy(amount = convertedAmount ?: txn.amount, currency = budget.currency)
            } else {
                txn
            }
        }

        // Calculate total spending and category spending, taking transaction items into account
        var totalSpending = BigDecimal.ZERO
        val categorySpending = mutableMapOf<String, BigDecimal>()

        for (txn in transactions) {
            val items = transactionItemDao.getItemsForTransactionSync(txn.id)
            if (items.isNotEmpty()) {
                for (item in items) {
                    val itemAmount = if (txn.currency != budget.currency) {
                        currencyConversionService.convertAmount(item.amount, txn.currency, budget.currency) ?: item.amount
                    } else {
                        item.amount
                    }
                    totalSpending += itemAmount
                    val currentCatSpending = categorySpending[item.category] ?: BigDecimal.ZERO
                    categorySpending[item.category] = currentCatSpending + itemAmount
                }
            } else {
                totalSpending += txn.amount
                val currentCatSpending = categorySpending[txn.category] ?: BigDecimal.ZERO
                categorySpending[txn.category] = currentCatSpending + txn.amount
            }
        }

        val categoryLimits = budgetDao.getCategoryLimitsForBudgetSync(budget.id)

        // Calculate days remaining
        val duration = Duration.between(startDate, endDate)
        val totalDays = duration.toDays().toInt().coerceAtLeast(1)
        
        val daysRemaining = if (now.isBefore(startDate)) {
            totalDays
        } else if (now.isAfter(endDate)) {
            0
        } else {
            Duration.between(now, endDate).toDays().toInt().coerceAtLeast(0)
        }

        return BudgetWithSpending(
            budget = budget,
            currentSpending = totalSpending,
            categoryLimits = categoryLimits,
            categorySpending = categorySpending,
            daysRemaining = daysRemaining,
            daysInMonth = totalDays
        )
    }

    fun getBudgetsWithSpendingForMonth(year: Int, month: Int): Flow<List<BudgetWithSpending>> {
        val startOfMonth = YearMonth.of(year, month).atDay(1).atStartOfDay()
        val endOfMonth = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59)
        
        return combine(
            budgetDao.getAllBudgets(),
            transactionDao.getAllTransactions(),
            budgetDao.getAllCategoryLimits()
        ) { budgets, transactions, categoryLimits ->
            budgets.filter { budget ->
                budget.isActive && (
                    (budget.startDate.isBefore(endOfMonth) || budget.startDate.isEqual(endOfMonth)) &&
                    (budget.endDate.isAfter(startOfMonth) || budget.endDate.isEqual(startOfMonth))
                )
            }.map { budget ->
                calculateSpending(budget, transactions, categoryLimits)
            }
        }
    }

    fun getAllBudgetsWithSpending(): Flow<List<BudgetWithSpending>> {
        return combine(
            budgetDao.getAllBudgets(),
            transactionDao.getAllTransactions(),
            budgetDao.getAllCategoryLimits()
        ) { budgets, transactions, categoryLimits ->
            budgets.map { budget ->
                calculateSpending(budget, transactions, categoryLimits)
            }
        }
    }

    private suspend fun calculateSpending(
        budget: BudgetEntity,
        allTransactions: List<TransactionEntity>,
        allCategoryLimits: List<BudgetCategoryLimitEntity>
    ): BudgetWithSpending {
        val startDate = budget.startDate
        val endDate = budget.endDate
        val now = LocalDateTime.now()

        // Filter transactions for this specific budget (already excludes deleted by DAO)
        var transactions = allTransactions.filter { txn ->
            (txn.dateTime.isAfter(startDate) || txn.dateTime.isEqual(startDate)) &&
            (txn.dateTime.isBefore(endDate) || txn.dateTime.isEqual(endDate))
        }

        // Filter by budget type
        transactions = if (budget.budgetType == BudgetType.EXPENSE) {
            transactions.filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.CREDIT }
        } else {
            transactions.filter { it.transactionType == TransactionType.INCOME }
        }
        
        // Filter by tracking type
        if (budget.trackType == BudgetTrackType.ADDED_ONLY) {
            transactions = transactions.filter { it.smsBody.isNullOrBlank() }
        }
        
        // Filter by accounts if specified
        if (budget.accountIds.isNotEmpty()) {
            val accounts = accountBalanceDao.getAllLatestBalances().first().associateBy { it.id.toString() }
            transactions = transactions.filter { txn ->
                val account = txn.accountId?.let { accounts[it] }
                if (account != null) {
                    val key = "${account.bankName}:${account.accountLast4}"
                    budget.accountIds.contains(key) || budget.accountIds.contains(txn.accountId)
                } else false
            }
        }

        // Convert currencies to match the budget's currency
        transactions = transactions.map { txn ->
            if (txn.currency != budget.currency) {
                val convertedAmount = currencyConversionService.convertAmount(txn.amount, txn.currency, budget.currency)
                txn.copy(amount = convertedAmount ?: txn.amount, currency = budget.currency)
            } else {
                txn
            }
        }

        // Calculate total spending and category spending, taking transaction items into account
        var totalSpending = BigDecimal.ZERO
        val categorySpending = mutableMapOf<String, BigDecimal>()

        for (txn in transactions) {
            val items = transactionItemDao.getItemsForTransactionSync(txn.id)
            if (items.isNotEmpty()) {
                for (item in items) {
                    val itemAmount = if (txn.currency != budget.currency) {
                        currencyConversionService.convertAmount(item.amount, txn.currency, budget.currency) ?: item.amount
                    } else {
                        item.amount
                    }
                    totalSpending += itemAmount
                    val currentCatSpending = categorySpending[item.category] ?: BigDecimal.ZERO
                    categorySpending[item.category] = currentCatSpending + itemAmount
                }
            } else {
                totalSpending += txn.amount
                val currentCatSpending = categorySpending[txn.category] ?: BigDecimal.ZERO
                categorySpending[txn.category] = currentCatSpending + txn.amount
            }
        }

        // Get category limits for this budget
        val categoryLimits = allCategoryLimits.filter { it.budgetId == budget.id }

        // Calculate days remaining
        val duration = Duration.between(startDate, endDate)
        val totalDays = duration.toDays().toInt().coerceAtLeast(1)
        
        val daysRemaining = if (now.isBefore(startDate)) {
            totalDays
        } else if (now.isAfter(endDate)) {
            0
        } else {
            Duration.between(now, endDate).toDays().toInt().coerceAtLeast(0)
        }

        return BudgetWithSpending(
            budget = budget,
            currentSpending = totalSpending,
            categoryLimits = categoryLimits,
            categorySpending = categorySpending,
            daysRemaining = daysRemaining,
            daysInMonth = totalDays
        )
    }

    suspend fun getCategoryLimitsWithSpending(budgetId: Long): List<CategoryLimitWithSpending> {
        val budget = budgetDao.getBudgetById(budgetId) ?: return emptyList()
        val budgetWithSpending = getBudgetWithSpending(budget)

        return budgetWithSpending.categoryLimits.map { limit ->
            CategoryLimitWithSpending(
                limit = limit,
                currentSpending = budgetWithSpending.categorySpending[limit.categoryName] ?: BigDecimal.ZERO
            )
        }
    }

    fun getTransactionsForBudget(budget: BudgetEntity): Flow<List<TransactionEntity>> {
        val startDate = budget.startDate
        val endDate = budget.endDate

        return combine(
            transactionDao.getTransactionsBetweenDates(startDate, endDate),
            accountBalanceDao.getAllLatestBalances()
        ) { transactions, accounts ->
            val accountsMap = accounts.associateBy { it.id.toString() }
            var filtered = transactions.filter { !it.isDeleted }
            
            // Filter by budget type
            filtered = if (budget.budgetType == BudgetType.EXPENSE) {
                filtered.filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.CREDIT }
            } else {
                filtered.filter { it.transactionType == TransactionType.INCOME }
            }
            
            // Filter by tracking type
            if (budget.trackType == BudgetTrackType.ADDED_ONLY) {
                filtered = filtered.filter { it.smsBody.isNullOrBlank() }
            }
            
            // Filter by accounts if specified
            if (budget.accountIds.isNotEmpty()) {
                filtered = filtered.filter { txn ->
                    val account = txn.accountId?.let { accountsMap[it] }
                    if (account != null) {
                        val key = "${account.bankName}:${account.accountLast4}"
                        budget.accountIds.contains(key) || budget.accountIds.contains(txn.accountId)
                    } else false
                }
            }

            // Convert currencies to match the budget's currency
            filtered.map { txn ->
                if (txn.currency != budget.currency) {
                    val convertedAmount = currencyConversionService.convertAmount(txn.amount, txn.currency, budget.currency)
                    txn.copy(amount = convertedAmount ?: txn.amount, currency = budget.currency)
                } else {
                    txn
                }
            }
        }
    }
}
