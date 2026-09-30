package com.reddy.vittify.presentation.ui.features.analytics


import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.CurrencyRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.presentation.common.TimePeriod
import com.reddy.vittify.presentation.common.TransactionTypeFilter
import com.reddy.vittify.presentation.common.getDateRangeForPeriod
import com.reddy.vittify.presentation.ui.components.BalancePoint
import com.reddy.vittify.utils.CurrencyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.ViewMode
import com.reddy.vittify.data.sync.model.coupleViewState
import com.reddy.vittify.data.sync.model.filterByViewMode

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _transactionTypeFilter = MutableStateFlow<Set<TransactionTypeFilter>>(setOf(TransactionTypeFilter.EXPENSE))
    val transactionTypeFilter: StateFlow<Set<TransactionTypeFilter>> = _transactionTypeFilter.asStateFlow()

    private val _selectedCurrency = MutableStateFlow<String?>(null)
    val selectedCurrency: StateFlow<String?> = _selectedCurrency.asStateFlow()

    init {
        viewModelScope.launch {
            var lastBaseCurrency: String? = null
            currencyRepository.effectiveBaseCurrencyCode.collectLatest { effectiveCurrency ->
                if (lastBaseCurrency == null || effectiveCurrency != lastBaseCurrency) {
                    // Do not auto-select. Default is null to show "All"
                    lastBaseCurrency = effectiveCurrency
                }
            }
        }
    }

    // Store custom date range as epoch days to survive process death
    // Stored as Pair<Long, Long> (startEpochDay, endEpochDay) in SavedStateHandle
    private val _customDateRangeEpochDays = savedStateHandle.getStateFlow<Pair<Long, Long>?>("customDateRange", null)

    // Expose as LocalDate pair for convenience
    val customDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _customDateRangeEpochDays
        .map { epochDays ->
            epochDays?.let { (startEpochDay, endEpochDay) ->
                LocalDate.ofEpochDay(startEpochDay) to LocalDate.ofEpochDay(endEpochDay)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    val categoriesMap = categoryRepository.getAllCategories()
        .map { cats -> cats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subcategoriesMap = subcategoryRepository.getAllSubcategories()
        .map { subcats -> subcats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    private val filterStateFlow = combine(
        _selectedPeriod,
        customDateRange,
        _transactionTypeFilter,
        _selectedCurrency
    ) { period, customRange, typeFilter, currency ->
        FilterState(period, customRange, typeFilter, currency)
    }

    private data class AnalyticsCombinedData(
        val filterState: FilterState,
        val accounts: List<com.reddy.vittify.data.database.entity.AccountBalanceEntity>,
        val allAccounts: List<com.reddy.vittify.data.database.entity.AccountBalanceEntity>,
        val baseCurrency: String,
        val coupleState: com.reddy.vittify.data.sync.model.CoupleViewState,
        val myDeviceId: String
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = combine(
        filterStateFlow,
        accountBalanceRepository.getAllLatestBalances(),
        currencyRepository.effectiveBaseCurrencyCode,
        p2pPreferences.coupleViewState,
        currencyConversionService.rateChangeTrigger
    ) { filterState, allAccounts, baseCurrency, coupleState, _ ->
        val myDeviceId = p2pPreferences.getDeviceId()
        val filteredAccounts = allAccounts.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { it.ownerId }
        AnalyticsCombinedData(filterState, filteredAccounts, allAccounts, baseCurrency, coupleState, myDeviceId)
    }.flatMapLatest { data ->
        val accountsMap = data.accounts.associateBy { it.id.toString() }
        val accountOwnerMap = data.allAccounts.associate { it.id to it.ownerId }
        val accountLast4OwnerMap = data.allAccounts.filter { it.ownerId.isNotBlank() }.associate { it.accountLast4 to it.ownerId }

        val dateRange = if (data.filterState.period == TimePeriod.CUSTOM) {
            val customRange = data.filterState.customRange
            if (customRange == null) {
                Log.e("AnalyticsViewModel",
                    "CUSTOM period selected but no date range set - falling back to THIS_MONTH")
                // Auto-correct the invalid state
                _selectedPeriod.value = TimePeriod.THIS_MONTH
                getDateRangeForPeriod(TimePeriod.THIS_MONTH)
            } else {
                customRange
            }
        } else {
            getDateRangeForPeriod(data.filterState.period)
        }

        if (dateRange == null) {
            // No valid date range, return empty state
            flowOf(AnalyticsUiState(
                isLoading = false,
                isCoupleTrackingEnabled = data.coupleState.coupleEnabled,
                activeViewMode = data.coupleState.viewMode,
                partnerName = data.coupleState.partnerName,
                myDeviceId = data.myDeviceId
            ))
        } else {
            // First load all transactions for the date range filtered by couple view mode
            transactionRepository.getTransactionsBetweenDates(
                startDate = dateRange.first,
                endDate = dateRange.second
            ).map { list ->
                list.filterByViewMode(data.coupleState.viewMode, data.coupleState.coupleEnabled, data.myDeviceId, data.coupleState.partnerDeviceId) { it.ownerId }
                list.filterByViewMode(data.coupleState.viewMode, data.coupleState.coupleEnabled, data.myDeviceId, data.coupleState.partnerDeviceId) { tx ->
                    tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                        ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                        ?: tx.ownerId
                }
            }.flatMapLatest { allTransactions: List<TransactionEntity> ->
                // Update available currencies using standard sorting (INR first, then alphabetical)
                val allCurrencies = CurrencyUtils.sortCurrencies(
                    allTransactions.map { it.currency }.distinct()
                )
                _availableCurrencies.value = allCurrencies

                val currentSelectedCurrency = data.filterState.currency
                if (currentSelectedCurrency != null && !allCurrencies.contains(currentSelectedCurrency) && allCurrencies.isNotEmpty()) {
                    _selectedCurrency.value = null
                }

                // Use database-level filtering for better performance
                // Convert TransactionTypeFilter to TransactionType for memory filtering
                val dbTransactionTypes = data.filterState.typeFilter.mapNotNull { type ->
                    when (type) {
                        TransactionTypeFilter.ALL -> null
                        TransactionTypeFilter.INCOME -> TransactionType.INCOME
                        TransactionTypeFilter.EXPENSE -> TransactionType.EXPENSE
                        TransactionTypeFilter.CREDIT -> TransactionType.CREDIT
                        TransactionTypeFilter.TRANSFER -> TransactionType.TRANSFER
                        TransactionTypeFilter.INVESTMENT -> TransactionType.INVESTMENT
                    }
                }.toSet()

                // Filter transactions from already view-mode-filtered list
                val typeFiltered = if (data.filterState.typeFilter.contains(TransactionTypeFilter.ALL)) {
                    allTransactions
                } else {
                    allTransactions.filter { it.transactionType in dbTransactionTypes }
                }

                // Fetch items for the filtered transactions
                val transactionIds = typeFiltered.map { it.id }

                flow {
                    val allItems = if (transactionIds.isNotEmpty()) {
                        transactionRepository.getItemsForTransactions(transactionIds)
                    } else emptyList()
                    val itemsByTransactionId = allItems.groupBy { it.transactionId }

                    val targetCurrency = data.filterState.currency ?: data.baseCurrency

                    // Expand transactions into items
                    val expandedItems = typeFiltered.flatMap { tx ->
                        val items = itemsByTransactionId[tx.id]
                        if (!items.isNullOrEmpty()) {
                            items.map { item ->
                                tx.copy(
                                    amount = item.amount,
                                    category = item.category,
                                    subcategory = item.subcategory
                                )
                            }
                        } else {
                            listOf(tx)
                        }
                    }

                    // If a specific currency is chosen, filter down to it. Otherwise, keep all.
                    val currencyTargetList = if (data.filterState.currency != null) {
                        expandedItems.filter { it.currency == data.filterState.currency }
                    } else {
                        expandedItems
                    }

                    // Convert all transaction amounts to the target currency
                    val convertedList = currencyTargetList.map { tx ->
                        if (tx.currency != targetCurrency) {
                            val convertedAmt = currencyConversionService.convertAmount(tx.amount, tx.currency, targetCurrency) ?: tx.amount
                            tx.copy(amount = convertedAmt, currency = targetCurrency)
                        } else {
                            tx
                        }
                    }
                    emit(convertedList)
                }
            }.map { filteredTransactions: List<TransactionEntity> ->

                // Calculate total precisely
                val totalSpending = filteredTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }

                // Group by category
                val categoryBreakdown = filteredTransactions
                    .groupBy { it.category ?: "Miscellaneous" }
                    .map { (categoryName, txns) ->
                        val categoryTotal = txns.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
                        CategoryData(
                            name = categoryName,
                            amount = categoryTotal,
                            percentage = if (totalSpending.signum() > 0) {
                                (categoryTotal.divide(totalSpending, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))).toFloat()
                            } else 0f,
                            transactionCount = txns.size
                        )
                    }
                    .sortedByDescending { it.amount }

                // Group by merchant
                val merchantBreakdown = filteredTransactions
                    .groupBy { it.merchantName }
                    .mapValues { (merchant, txns) ->
                        val primaryCategory = txns.groupBy { it.category }.maxByOrNull { it.value.size }?.key
                        val primarySubcategory = txns.groupBy { it.subcategory }.maxByOrNull { it.value.size }?.key
                        val firstTxn = txns.firstOrNull()
                        val accountIconName = firstTxn?.accountId?.let { accountsMap[it]?.iconName }

                        MerchantData(
                            name = merchant,
                            amount = txns.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) },
                            transactionCount = txns.size,
                            isSubscription = txns.any { it.isRecurring },
                            categoryName = primaryCategory,
                            subcategoryName = primarySubcategory,
                            accountIconName = accountIconName
                        )
                    }
                    .values
                    .sortedByDescending { it.amount }
                    .take(10) // Top 10 merchants

                // Calculate average amount
                val averageAmount = if (filteredTransactions.isNotEmpty()) {
                    totalSpending.divide(BigDecimal(filteredTransactions.size), 2, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }

                // Get top category info
                val topCategory = categoryBreakdown.firstOrNull()

                // Calculate converted totals for top merchants if needed
                val currentCurrency = data.filterState.currency
                val merchantConversions = if (currentCurrency != null && currentCurrency != data.baseCurrency) {
                    merchantBreakdown.associate { merchant ->
                        merchant.name to (currencyConversionService.convertAmount(
                            merchant.amount,
                            currentCurrency,
                            data.baseCurrency
                        ) ?: merchant.amount)
                    }
                } else emptyMap()

                AnalyticsUiState(
                    totalSpending = totalSpending,
                    categoryBreakdown = categoryBreakdown,
                    topMerchants = merchantBreakdown,
                    transactionCount = filteredTransactions.size,
                    averageAmount = averageAmount,
                    topCategory = topCategory?.name,
                    topCategoryPercentage = topCategory?.percentage ?: 0f,
                    currency = data.filterState.currency ?: data.baseCurrency,
                    baseCurrency = data.baseCurrency,
                    isLoading = false,
                    spendingTrend = calculateSpendingTrend(filteredTransactions, dateRange.first, dateRange.second, data.filterState.currency ?: data.baseCurrency),
                    convertedMerchantAmounts = merchantConversions,
                    isCoupleTrackingEnabled = data.coupleState.coupleEnabled,
                    activeViewMode = data.coupleState.viewMode,
                    partnerName = data.coupleState.partnerName,
                    myDeviceId = data.myDeviceId
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = AnalyticsUiState(isLoading = true)
    )

    fun setViewMode(mode: ViewMode) {
        p2pPreferences.setActiveViewMode(mode)
    }

    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun toggleTransactionTypeFilter(filter: TransactionTypeFilter) {
        val current = _transactionTypeFilter.value
        if (filter == TransactionTypeFilter.ALL) {
            _transactionTypeFilter.value = setOf(TransactionTypeFilter.ALL)
        } else {
            val newSet = current.toMutableSet()
            newSet.remove(TransactionTypeFilter.ALL)
            if (newSet.contains(filter)) {
                newSet.remove(filter)
            } else {
                newSet.add(filter)
            }
            if (newSet.isEmpty()) {
                newSet.add(TransactionTypeFilter.ALL)
            }
            _transactionTypeFilter.value = newSet
        }
    }

    fun selectCurrency(currency: String?) {
        _selectedCurrency.value = currency
    }

    /**
     * Sets a custom date range filter and switches the period to CUSTOM.
     * Date range is persisted in SavedStateHandle to survive process death.
     *
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @throws IllegalArgumentException if startDate > endDate
     */
    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        require(startDate <= endDate) {
            "Start date ($startDate) must be before or equal to end date ($endDate)"
        }
        // Store as epoch days for process death survival
        savedStateHandle["customDateRange"] = startDate.toEpochDay() to endDate.toEpochDay()
        _selectedPeriod.value = TimePeriod.CUSTOM
    }

    /**
     * Clears the custom date range and resets to THIS_MONTH period.
     * Always safe to call - ensures we never have CUSTOM period with null dates.
     */
    fun clearCustomDateRange() {
        savedStateHandle["customDateRange"] = null
        // Always reset to a valid period to prevent CUSTOM with null dates
        if (_selectedPeriod.value == TimePeriod.CUSTOM) {
            _selectedPeriod.value = TimePeriod.THIS_MONTH
        }
    }

    private fun calculateSpendingTrend(
        transactions: List<TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        currency: String
    ): List<BalancePoint> {
        val selectedPeriod = _selectedPeriod.value
        val trend = mutableListOf<BalancePoint>()

        val daysInRange = ChronoUnit.DAYS.between(startDate, endDate)
        val yearsInRange = ChronoUnit.YEARS.between(startDate, endDate)

        val shouldAggregateByYear = (selectedPeriod == TimePeriod.ALL && yearsInRange >= 2) ||
                (selectedPeriod == TimePeriod.CUSTOM && yearsInRange >= 2)

        val shouldAggregateByMonth = selectedPeriod == TimePeriod.CURRENT_FY ||
                (selectedPeriod == TimePeriod.ALL && !shouldAggregateByYear) ||
                (selectedPeriod == TimePeriod.CUSTOM && daysInRange > 31 && !shouldAggregateByYear)

        when {
            shouldAggregateByYear -> {
                val actualStartDate = if (selectedPeriod == TimePeriod.ALL && transactions.isNotEmpty()) {
                    val firstTxDate = transactions.minByOrNull { it.dateTime }?.dateTime?.toLocalDate() ?: startDate
                    if (firstTxDate.isAfter(startDate)) firstTxDate.withDayOfYear(1) else startDate
                } else {
                    startDate
                }

                var currentYear = actualStartDate.withDayOfYear(1)
                val lastYear = endDate.withDayOfYear(1)

                while (!currentYear.isAfter(lastYear) && !currentYear.isAfter(LocalDate.now().withDayOfYear(1))) {
                    val endOfYear = currentYear.withDayOfYear(currentYear.lengthOfYear())
                    val transactionsForYear = transactions.filter {
                        !it.dateTime.toLocalDate().isBefore(currentYear) && !it.dateTime.toLocalDate().isAfter(endOfYear)
                    }
                    val totalAmount = transactionsForYear.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }

                    trend.add(
                        BalancePoint(
                            timestamp = currentYear.atStartOfDay(),
                            balance = totalAmount,
                            currency = currency
                        )
                    )
                    currentYear = currentYear.plusYears(1)
                }
            }
            shouldAggregateByMonth -> {
                val actualStartDate = if (selectedPeriod == TimePeriod.ALL && transactions.isNotEmpty()) {
                    val firstTxDate = transactions.minByOrNull { it.dateTime }?.dateTime?.toLocalDate() ?: startDate
                    if (firstTxDate.isAfter(startDate)) firstTxDate.withDayOfYear(1) else startDate
                } else {
                    startDate
                }

                var currentMonth = actualStartDate.withDayOfMonth(1)
                val lastMonth = endDate.withDayOfMonth(1)

                while (!currentMonth.isAfter(lastMonth) && !currentMonth.isAfter(LocalDate.now().withDayOfMonth(1))) {
                    val endOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())
                    val transactionsForMonth = transactions.filter {
                        !it.dateTime.toLocalDate().isBefore(currentMonth) && !it.dateTime.toLocalDate().isAfter(endOfMonth)
                    }
                    val totalAmount = transactionsForMonth.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }

                    trend.add(
                        BalancePoint(
                            timestamp = currentMonth.atStartOfDay(),
                            balance = totalAmount,
                            currency = currency
                        )
                    )
                    currentMonth = currentMonth.plusMonths(1)
                }
            }
            else -> {
                // Daily aggregation for smaller periods (This Month, Last Month, or Custom range <= 31 days)
                val transactionsByDate = transactions.groupBy { it.dateTime.toLocalDate() }
                var currentDate = startDate
                while (!currentDate.isAfter(endDate) && !currentDate.isAfter(LocalDate.now())) {
                    val transactionsForDay = transactionsByDate[currentDate] ?: emptyList()
                    val totalAmount = transactionsForDay.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }

                    trend.add(
                        BalancePoint(
                            timestamp = currentDate.atStartOfDay(),
                            balance = totalAmount,
                            currency = currency
                        )
                    )
                    currentDate = currentDate.plusDays(1)
                }
            }
        }
        return trend
    }
}


