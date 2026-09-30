package com.reddy.vittify.presentation.ui.features.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.repository.CurrencyRepository
import com.reddy.vittify.presentation.ui.components.BalancePoint
import com.reddy.vittify.utils.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService
) : ViewModel() {
    
    private val bankName: String = savedStateHandle.get<String>("bankName") ?: ""
    private val accountLast4: String = savedStateHandle.get<String>("accountLast4") ?: ""
    
    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    val categoriesMap = categoryRepository.getAllCategories()
        .map { cats -> cats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subcategoriesMap = subcategoryRepository.getAllSubcategories()
        .map { subcats -> subcats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    private val _selectedDateRange = MutableStateFlow(DateRange.LAST_30_DAYS)
    val selectedDateRange: StateFlow<DateRange> = _selectedDateRange.asStateFlow()
    
    init {
        loadAccountData()
        observeTransactions()
        observeBalanceHistory()
        observeBalanceChartData()
    }
    
    private fun loadAccountData() {
        _uiState.update { it.copy(
            bankName = bankName,
            accountLast4 = accountLast4,
            isLoading = true
        ) }
    }
    
    private fun observeTransactions() {
        viewModelScope.launch {
            combine(
                selectedDateRange,
                transactionRepository.getTransactionsByAccount(bankName, accountLast4),
                currencyRepository.effectiveBaseCurrencyCode,
                accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4),
                currencyConversionService.rateChangeTrigger
            ) { dateRange, allTransactions, mainCurrency, latestBalance, _ ->
                val (startDate, endDate) = getDateRangeValues(dateRange)

                val filteredTransactions = if (dateRange == DateRange.ALL_TIME) {
                    allTransactions
                } else {
                    allTransactions.filter { transaction ->
                        transaction.dateTime.isAfter(startDate) &&
                        transaction.dateTime.isBefore(endDate)
                    }
                }

                val accountPrimaryCurrency = latestBalance?.currency ?: getPrimaryCurrencyForAccount(bankName)
                val hasMultipleCurrencies = filteredTransactions
                    .map { it.currency }
                    .distinct()
                    .size > 1

                // Refresh exchange rates if we have multiple currencies
                if (hasMultipleCurrencies) {
                    val accountCurrencies = filteredTransactions.map { it.currency }.distinct()
                    currencyConversionService.refreshExchangeRatesForAccount(accountCurrencies)
                }

                // Calculate total income and expenses converted to the effective base currency
                var totalIncome = BigDecimal.ZERO
                var totalExpenses = BigDecimal.ZERO

                filteredTransactions.forEach { transaction ->
                    val convertedAmount = if (transaction.currency != mainCurrency) {
                        currencyConversionService.convertAmount(
                            amount = transaction.amount,
                            fromCurrency = transaction.currency,
                            toCurrency = mainCurrency
                        ) ?: transaction.amount
                    } else {
                        transaction.amount
                    }

                    if (transaction.transactionType == TransactionType.INCOME) {
                        totalIncome += convertedAmount
                    } else if (transaction.transactionType == TransactionType.TRANSFER) {
                        val isSender = transaction.fromAccount == accountLast4
                        
                        // If it's a transfer and we are not the sender, but this transaction was fetched 
                        // for our account, we must be the receiver.
                        val isReceiver = !isSender && transaction.toAccount == accountLast4

                        if (isReceiver) {
                            totalIncome += convertedAmount
                        } else if (isSender) {
                            totalExpenses += convertedAmount
                        }
                    } else if (transaction.transactionType == TransactionType.CREDIT || transaction.transactionType == TransactionType.BALANCE_UPDATE) {
                        // Optional: do credit or balance update affect total income/expenses? 
                        // Currently everything not INCOME goes to EXPENSE, but leaving it as-is for backward compatibility
                        totalExpenses += convertedAmount
                    } else {
                        totalExpenses += convertedAmount
                    }
                }

                // Calculate converted amounts for the UI (TransactionItem) based on Main App Currency
                val converted = filteredTransactions
                    .filter { it.currency != mainCurrency }
                    .associate { tx ->
                        tx.id to (currencyConversionService.convertAmount(tx.amount, tx.currency, mainCurrency) ?: tx.amount)
                    }

                val splitIds = transactionRepository.getSplitTransactionIds(filteredTransactions.map { it.id }).toSet()

                _uiState.update { state ->
                    state.copy(
                        transactions = filteredTransactions,
                        totalIncome = totalIncome,
                        totalExpenses = totalExpenses,
                        netBalance = totalIncome - totalExpenses,
                        primaryCurrency = mainCurrency,
                        baseCurrency = mainCurrency,
                        hasMultipleCurrencies = hasMultipleCurrencies,
                        convertedAmounts = converted,
                        splitTransactionIds = splitIds,
                        isLoading = false
                    )
                }
            }.collectLatest { }
        }
    }
    
    private fun observeBalanceHistory() {
        viewModelScope.launch {
            accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4)
                .collect { latestBalance ->
                    _uiState.update { state ->
                        state.copy(currentBalance = latestBalance)
                    }
                }
        }
        
        viewModelScope.launch {
            selectedDateRange.flatMapLatest { dateRange ->
                val (startDate, endDate) = getDateRangeValues(dateRange)
                accountBalanceRepository.getBalanceHistory(
                    bankName, 
                    accountLast4,
                    startDate,
                    endDate
                )
            }.collect { balanceHistory ->
                _uiState.update { state ->
                    state.copy(balanceHistory = balanceHistory)
                }
            }
        }
    }
    
    private fun observeBalanceChartData() {
        viewModelScope.launch {
            combine(
                selectedDateRange,
                accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4),
                transactionRepository.getTransactionsByAccount(bankName, accountLast4),
                currencyConversionService.rateChangeTrigger
            ) { dateRange, latestBalanceEntity, transactions, _ ->
                val (startDate, endDate) = getDateRangeValues(dateRange)

                val chartStartDate = when (dateRange) {
                    DateRange.LAST_7_DAYS -> endDate.minusDays(14)
                    DateRange.LAST_30_DAYS -> endDate.minusMonths(2)
                    DateRange.LAST_3_MONTHS -> endDate.minusMonths(4)
                    DateRange.LAST_6_MONTHS -> endDate.minusMonths(8)
                    DateRange.LAST_YEAR -> endDate.minusMonths(15)
                    DateRange.ALL_TIME -> LocalDateTime.of(2000, 1, 1, 0, 0)
                }

                val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()
                val currentBalance = latestBalanceEntity?.balance ?: BigDecimal.ZERO
                val accountCurrency = latestBalanceEntity?.currency ?: "INR"

                val validTransactions = transactions.filter { !it.isDeleted }
                val txByDate = validTransactions.groupBy { it.dateTime.toLocalDate() }

                val startLocalDate = chartStartDate.toLocalDate()
                val endLocalDate = endDate.toLocalDate()

                val historyMap = mutableMapOf<java.time.LocalDate, BigDecimal>()
                var runningBalance = currentBalance

                var currentDate = endLocalDate
                while (!currentDate.isBefore(startLocalDate)) {
                    historyMap[currentDate] = runningBalance

                    val dayTransactions = txByDate[currentDate] ?: emptyList()
                    var dayNetChange = BigDecimal.ZERO

                    for (tx in dayTransactions) {
                        val isTransferIn = tx.transactionType == TransactionType.TRANSFER && tx.toAccount == accountLast4
                        val isTransferOut = tx.transactionType == TransactionType.TRANSFER && tx.fromAccount == accountLast4

                        when {
                            tx.transactionType == TransactionType.INCOME || tx.transactionType == TransactionType.CREDIT || isTransferIn -> {
                                dayNetChange = dayNetChange.add(tx.amount)
                            }
                            tx.transactionType == TransactionType.EXPENSE || tx.transactionType == TransactionType.INVESTMENT || isTransferOut -> {
                                dayNetChange = dayNetChange.subtract(tx.amount)
                            }
                        }
                    }

                    runningBalance = runningBalance.subtract(dayNetChange)
                    currentDate = currentDate.minusDays(1)
                }

                historyMap.toSortedMap().map { (date, balance) ->
                    val convertedBalance = if (accountCurrency != effectiveCurrency) {
                        currencyConversionService.convertAmount(
                            balance,
                            accountCurrency,
                            effectiveCurrency
                        ) ?: balance
                    } else balance

                    BalancePoint(
                        timestamp = date.atStartOfDay(),
                        balance = convertedBalance,
                        currency = effectiveCurrency
                    )
                }
            }.collectLatest { chartData ->
                _uiState.update { state ->
                    state.copy(balanceChartData = chartData)
                }
            }
        }
    }
    
    fun selectDateRange(dateRange: DateRange) {
        _selectedDateRange.value = dateRange
    }
    
    private fun getDateRangeValues(dateRange: DateRange): Pair<LocalDateTime, LocalDateTime> {
        val endDate = LocalDateTime.now()
        val startDate = when (dateRange) {
            DateRange.LAST_7_DAYS -> endDate.minusDays(7)
            DateRange.LAST_30_DAYS -> endDate.minusDays(30)
            DateRange.LAST_3_MONTHS -> endDate.minusMonths(3)
            DateRange.LAST_6_MONTHS -> endDate.minusMonths(6)
            DateRange.LAST_YEAR -> endDate.minusYears(1)
            DateRange.ALL_TIME -> LocalDateTime.of(2000, 1, 1, 0, 0)
        }
        return startDate to endDate
    }

    private fun getPrimaryCurrencyForAccount(bankName: String): String {
        return CurrencyFormatter.getBankBaseCurrency(bankName)
    }
}