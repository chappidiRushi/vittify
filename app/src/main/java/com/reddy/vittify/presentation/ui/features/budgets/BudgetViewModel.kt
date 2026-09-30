package com.reddy.vittify.presentation.ui.features.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.BudgetEntity
import com.reddy.vittify.data.repository.BudgetRepository
import com.reddy.vittify.data.repository.CategoryLimitWithSpending
import com.reddy.vittify.data.database.dao.AccountBalanceDao
import com.reddy.vittify.data.database.entity.BudgetPeriod
import com.reddy.vittify.data.database.entity.BudgetTrackType
import com.reddy.vittify.data.database.entity.BudgetType
import com.reddy.vittify.data.repository.CurrencyRepository
import com.reddy.vittify.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import com.reddy.vittify.data.currency.CurrencyConversionService
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest


import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.ViewMode
import com.reddy.vittify.data.sync.model.filterByViewMode
import com.reddy.vittify.data.sync.model.coupleViewState

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceDao: AccountBalanceDao,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val p2pPreferences: P2pSyncPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _editBudgetState = MutableStateFlow(EditBudgetState())
    val editBudgetState: StateFlow<EditBudgetState> = _editBudgetState.asStateFlow()

    private var transactionCollectionJob: kotlinx.coroutines.Job? = null
    private var selectedBudgetCollectionJob: kotlinx.coroutines.Job? = null

    init {
        loadBudgets()
        loadAccounts()
        observeBaseCurrency()
    }

    fun setViewMode(mode: ViewMode) {
        p2pPreferences.setActiveViewMode(mode)
    }

    private fun observeBaseCurrency() {
        viewModelScope.launch {
            currencyRepository.effectiveBaseCurrencyCode.collect { currency ->
                _uiState.update { it.copy(baseCurrency = currency) }
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            combine(
                accountBalanceDao.getAllLatestBalances(),
                p2pPreferences.coupleViewState
            ) { accounts, coupleState ->
                val myDeviceId = p2pPreferences.getDeviceId()
                accounts.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { it.ownerId }
            }.collect { accounts ->
                _uiState.update { it.copy(allAccounts = accounts) }
            }
        }
    }

    private data class BudgetCombineResult(
        val budgets: List<com.reddy.vittify.data.repository.BudgetWithSpending>,
        val viewMode: ViewMode,
        val coupleEnabled: Boolean,
        val partnerName: String?,
        val myDeviceId: String
    )

    fun loadBudgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                combine(
                    budgetRepository.getAllBudgetsWithSpending(),
                    currencyRepository.effectiveBaseCurrencyCode,
                    p2pPreferences.coupleViewState,
                    currencyConversionService.rateChangeTrigger
                ) { budgets, targetCurrency, coupleState, _ ->
                    val myDeviceId = p2pPreferences.getDeviceId()
                    val filteredBudgets = budgets.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { it.budget.ownerId }
                    val converted = filteredBudgets.map { budgetWithSpending ->
                        if (budgetWithSpending.budget.currency != targetCurrency) {
                            val convertedAmount = currencyConversionService.convertAmount(
                                budgetWithSpending.budget.amount,
                                budgetWithSpending.budget.currency,
                                targetCurrency
                            ) ?: budgetWithSpending.budget.amount

                            val convertedSpending = currencyConversionService.convertAmount(
                                budgetWithSpending.currentSpending,
                                budgetWithSpending.budget.currency,
                                targetCurrency
                            ) ?: budgetWithSpending.currentSpending

                            budgetWithSpending.copy(
                                budget = budgetWithSpending.budget.copy(
                                    amount = convertedAmount,
                                    currency = targetCurrency
                                ),
                                currentSpending = convertedSpending
                            )
                        } else {
                            budgetWithSpending
                        }
                    }
                    BudgetCombineResult(converted, coupleState.viewMode, coupleState.coupleEnabled, coupleState.partnerName, myDeviceId)
                }.collect { result ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            budgets = result.budgets,
                            isCoupleTrackingEnabled = result.coupleEnabled,
                            activeViewMode = result.viewMode,
                            partnerName = result.partnerName,
                            myDeviceId = result.myDeviceId,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load budgets"
                    )
                }
            }
        }
    }

    fun selectBudget(budgetId: Long, startDate: String? = null, endDate: String? = null) {
        selectedBudgetCollectionJob?.cancel()
        selectedBudgetCollectionJob = viewModelScope.launch {
            val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()
            combine(
                budgetRepository.getAllBudgetsWithSpending(),
                currencyConversionService.rateChangeTrigger
            ) { allBudgets, _ -> allBudgets }.collect { allBudgets ->
                val found = allBudgets.find { it.budget.id == budgetId }
                if (found != null) {
                    var budgetWithSpending = found
                    
                    // If custom dates provided, recalculate spending for that period
                    if (startDate != null && endDate != null) {
                        try {
                            val start = LocalDateTime.parse(startDate)
                            val end = LocalDateTime.parse(endDate)
                            val customBudget = budgetWithSpending.budget.copy(
                                startDate = start,
                                endDate = end
                            )
                            budgetWithSpending = budgetRepository.getBudgetWithSpending(customBudget)
                        } catch (e: Exception) {
                            // Fallback to normal if parsing fails
                        }
                    }

                    // Convert budget data to effective currency
                    if (effectiveCurrency != budgetWithSpending.budget.currency) {
                        val convertedAmount = currencyConversionService.convertAmount(
                            budgetWithSpending.budget.amount,
                            budgetWithSpending.budget.currency,
                            effectiveCurrency
                        ) ?: budgetWithSpending.budget.amount
                        val convertedSpending = currencyConversionService.convertAmount(
                            budgetWithSpending.currentSpending,
                            budgetWithSpending.budget.currency,
                            effectiveCurrency
                        ) ?: budgetWithSpending.currentSpending
                        val convertedCategorySpending = budgetWithSpending.categorySpending.mapValues { (_, amount) ->
                            currencyConversionService.convertAmount(amount, budgetWithSpending.budget.currency, effectiveCurrency) ?: amount
                        }
                        budgetWithSpending = budgetWithSpending.copy(
                            budget = budgetWithSpending.budget.copy(
                                amount = convertedAmount,
                                currency = effectiveCurrency
                            ),
                            currentSpending = convertedSpending,
                            categorySpending = convertedCategorySpending
                        )
                    }

                    val categoryLimitsWithSpending = budgetWithSpending.categoryLimits.map { limit ->
                        CategoryLimitWithSpending(
                            limit = limit,
                            currentSpending = budgetWithSpending.categorySpending[limit.categoryName] ?: BigDecimal.ZERO
                        )
                    }
                    _uiState.update { 
                        it.copy(
                            selectedBudget = budgetWithSpending,
                            categoryLimitsWithSpending = categoryLimitsWithSpending
                        )
                    }
                }
            }
        }

        // Collect transactions for the selected budget
        transactionCollectionJob?.cancel()
        viewModelScope.launch {
            val budget = budgetRepository.getBudgetById(budgetId) ?: return@launch
            
            // Override dates if provided
            val collectionBudget = if (startDate != null && endDate != null) {
                try {
                    budget.copy(
                        startDate = LocalDateTime.parse(startDate),
                        endDate = LocalDateTime.parse(endDate)
                    )
                } catch (e: Exception) { budget }
            } else budget

            transactionCollectionJob = viewModelScope.launch {
                combine(
                    budgetRepository.getTransactionsForBudget(collectionBudget),
                    currencyRepository.effectiveBaseCurrencyCode,
                    currencyConversionService.rateChangeTrigger
                ) { transactions, mainCurrency, _ ->
                    val converted = transactions
                        .filter { it.currency != mainCurrency }
                        .associate { tx ->
                            tx.id to (currencyConversionService.convertAmount(tx.amount, tx.currency, mainCurrency) ?: tx.amount)
                        }

                    val splitIds = transactionRepository.getSplitTransactionIds(transactions.map { it.id }).toSet()
                    
                    _uiState.update { it.copy(
                        selectedBudgetTransactions = transactions,
                        convertedAmounts = converted,
                        splitTransactionIds = splitIds
                    ) }
                }.collectLatest { }
            }
        }
    }

    fun clearSelectedBudget() {
        transactionCollectionJob?.cancel()
        transactionCollectionJob = null
        selectedBudgetCollectionJob?.cancel()
        selectedBudgetCollectionJob = null
        _uiState.update { 
            it.copy(
                selectedBudget = null,
                categoryLimitsWithSpending = emptyList(),
                selectedBudgetTransactions = emptyList()
            )
        }
    }

    // Edit budget state management
    fun initNewBudget() {
        val now = LocalDateTime.now()
        viewModelScope.launch {
            val currency = currencyRepository.baseCurrencyCode.first()
            _editBudgetState.value = EditBudgetState(
                year = now.year,
                month = now.monthValue,
                startDate = now,
                endDate = now.plusMonths(1).minusDays(1),
                currency = currency
            )
        }
    }

    fun initEditBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            val limits = budgetRepository.getCategoryLimitsForBudgetSync(budget.id)
            _editBudgetState.value = EditBudgetState(
                budgetId = budget.id,
                name = budget.name,
                amount = budget.amount,
                year = budget.year,
                month = budget.month,
                startDate = budget.startDate,
                endDate = budget.endDate,
                periodType = budget.periodType,
                trackType = budget.trackType,
                budgetType = budget.budgetType,
                accountIds = budget.accountIds,
                color = budget.color,
                currency = budget.currency,
                categoryLimits = limits.map { limit ->
                    EditCategoryLimit(
                        id = limit.id,
                        categoryName = limit.categoryName,
                        limitAmount = limit.limitAmount
                    )
                }
            )
        }
    }

    fun updateBudgetAmount(amount: BigDecimal) {
        _editBudgetState.update { it.copy(amount = amount) }
    }

    fun updateBudgetName(name: String) {
        _editBudgetState.update { it.copy(name = name) }
    }

    fun updateBudgetMonth(year: Int, month: Int) {
        val startDate = LocalDateTime.of(year, month, 1, 0, 0)
        val endDate = startDate.plusMonths(1).minusDays(1)
        
        _editBudgetState.update { 
            it.copy(
                year = year, 
                month = month,
                startDate = startDate,
                endDate = endDate
            ) 
        }
    }
    
    fun updateStartDate(date: LocalDateTime) {
        _editBudgetState.update { state -> 
            val newEndDate = calculateEndDate(date, state.periodType)
            state.copy(
                startDate = date,
                endDate = newEndDate,
                year = date.year,
                month = date.monthValue
            )
        }
    }

    fun updateEndDate(date: LocalDateTime) {
         _editBudgetState.update { it.copy(endDate = date) }
    }

    fun updatePeriodType(periodType: BudgetPeriod) {
        _editBudgetState.update { state ->
            val newEndDate = calculateEndDate(state.startDate, periodType)
            state.copy(
                periodType = periodType,
                endDate = newEndDate
            )
        }
    }

    fun updateTrackType(trackType: BudgetTrackType) {
        _editBudgetState.update { it.copy(trackType = trackType) }
    }

    fun updateBudgetType(budgetType: BudgetType) {
        _editBudgetState.update { it.copy(budgetType = budgetType) }
    }

    fun updateAccountIds(accountIds: List<String>) {
        viewModelScope.launch {
            val currency = if (accountIds.isEmpty()) {
                currencyRepository.baseCurrencyCode.first()
            } else {
                val accountKey = accountIds.first()
                val matchedAccount = _uiState.value.allAccounts.find { 
                    accountKey.contains(it.bankName) && accountKey.contains(it.accountLast4) 
                }
                matchedAccount?.currency ?: currencyRepository.baseCurrencyCode.first()
            }
            _editBudgetState.update { it.copy(accountIds = accountIds, currency = currency) }
        }
    }
    
    fun updateColor(color: String) {
        _editBudgetState.update { it.copy(color = color) }
    }

    private fun calculateEndDate(startDate: LocalDateTime, periodType: BudgetPeriod): LocalDateTime {
        return when (periodType) {
            BudgetPeriod.DAILY -> startDate
            BudgetPeriod.WEEKLY -> startDate.plusDays(6)
            BudgetPeriod.MONTHLY -> startDate.plusMonths(1).minusDays(1)
            BudgetPeriod.YEARLY -> startDate.plusYears(1).minusDays(1)
            BudgetPeriod.CUSTOM -> startDate // User selects end date manually
        }
    }

    fun addCategoryLimit(categoryName: String, limitAmount: BigDecimal) {
        _editBudgetState.update { state ->
            val existingIndex = state.categoryLimits.indexOfFirst { it.categoryName == categoryName }
            if (existingIndex >= 0) {
                // Update existing
                val updatedLimits = state.categoryLimits.toMutableList()
                updatedLimits[existingIndex] = updatedLimits[existingIndex].copy(limitAmount = limitAmount)
                state.copy(categoryLimits = updatedLimits)
            } else {
                // Add new
                state.copy(
                    categoryLimits = state.categoryLimits + EditCategoryLimit(
                        categoryName = categoryName,
                        limitAmount = limitAmount
                    )
                )
            }
        }
    }

    fun removeCategoryLimit(categoryName: String) {
        _editBudgetState.update { state ->
            state.copy(
                categoryLimits = state.categoryLimits.filter { it.categoryName != categoryName }
            )
        }
    }

    fun saveBudget(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val state = _editBudgetState.value
                val name = state.name.ifBlank { state.getDefaultName() }
                
                if (state.budgetId != null) {
                    // Update existing budget
                    val existingBudget = budgetRepository.getBudgetById(state.budgetId)
                    if (existingBudget != null) {
                        budgetRepository.updateBudget(
                            existingBudget.copy(
                                name = name,
                                amount = state.amount,
                                year = state.startDate.year,
                                month = state.startDate.monthValue,
                                startDate = state.startDate,
                                endDate = state.endDate,
                                periodType = state.periodType,
                                trackType = state.trackType,
                                budgetType = state.budgetType,
                                accountIds = state.accountIds,
                                color = state.color,
                                currency = state.currency,
                                updatedAt = LocalDateTime.now()
                            )
                        )
                        // Update category limits
                        budgetRepository.deleteCategoryLimitsForBudget(state.budgetId)
                        state.categoryLimits.forEach { limit ->
                            budgetRepository.addCategoryLimit(
                                budgetId = state.budgetId,
                                categoryName = limit.categoryName,
                                limitAmount = limit.limitAmount
                            )
                        }
                    }
                } else {
                    // Create new budget
                    val budget = BudgetEntity(
                        name = name,
                        amount = state.amount,
                        year = state.startDate.year,
                        month = state.startDate.monthValue,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        periodType = state.periodType,
                        trackType = state.trackType,
                        budgetType = state.budgetType,
                        accountIds = state.accountIds,
                        color = state.color,
                        currency = state.currency
                    )
                    
                    val budgetId = budgetRepository.insertBudget(budget)
                    
                    // Add category limits
                    state.categoryLimits.forEach { limit ->
                        budgetRepository.addCategoryLimit(
                            budgetId = budgetId,
                            categoryName = limit.categoryName,
                            limitAmount = limit.limitAmount
                        )
                    }
                }
                
                loadBudgets()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save budget")
            }
        }
    }

    fun deleteBudget(budgetId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(budgetId)
                loadBudgets()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete budget")
            }
        }
    }

    fun clearEditState() {
        _editBudgetState.value = EditBudgetState()
    }
}
