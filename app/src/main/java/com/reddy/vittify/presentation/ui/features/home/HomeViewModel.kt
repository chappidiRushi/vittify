package com.reddy.vittify.presentation.ui.features.home

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.domain.model.BalanceCalculator
import com.reddy.vittify.utils.sumOfBigDecimal
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.manager.InAppReviewManager
import com.reddy.vittify.data.manager.InAppUpdateManager
import com.reddy.vittify.data.preferences.HomeWidget
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.BudgetRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.CurrencyRepository
import com.reddy.vittify.data.nlp.NlpParsingMode
import com.reddy.vittify.data.nlp.NlpTransactionParser
import com.reddy.vittify.data.cloud.engine.CloudSyncEngine
import android.widget.Toast
import com.reddy.vittify.R
import com.reddy.vittify.data.nlp.ParsedTransactionDraft

import com.reddy.vittify.data.sync.model.filterByViewMode
import com.reddy.vittify.data.sync.model.coupleViewState
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.repository.UnrecognizedSmsRepository
import com.reddy.vittify.presentation.ui.components.BalancePoint
import com.reddy.vittify.worker.OptimizedSmsReaderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.ViewMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,

    private val nlpTransactionParser: NlpTransactionParser,
    private val currencyConversionService: CurrencyConversionService,
    private val currencyRepository: CurrencyRepository,
    private val inAppUpdateManager: InAppUpdateManager,
    private val inAppReviewManager: InAppReviewManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val cloudSyncEngine: CloudSyncEngine,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        HomeUiState(
            isCoupleTrackingEnabled = p2pPreferences.isCoupleTrackingEnabled(),
            activeViewMode = p2pPreferences.getActiveViewMode(),
            partnerName = p2pPreferences.getPartnerName(),
            partnerUserId = p2pPreferences.getPartnerUserId(),
            myUserId = p2pPreferences.getDeviceId()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _deletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val deletedTransaction: StateFlow<TransactionEntity?> = _deletedTransaction.asStateFlow()

    // SMS scanning work progress tracking
    private val _smsScanWorkInfo = MutableStateFlow<WorkInfo?>(null)
    val smsScanWorkInfo: StateFlow<WorkInfo?> = _smsScanWorkInfo.asStateFlow()

    private val _homeWidgets = MutableStateFlow<List<HomeWidgetUiModel>>(emptyList())
    val homeWidgets: StateFlow<List<HomeWidgetUiModel>> = _homeWidgets.asStateFlow()

    private val _showEditWidgetsSheet = MutableStateFlow(false)
    val showEditWidgetsSheet: StateFlow<Boolean> = _showEditWidgetsSheet.asStateFlow()

    private val _showMoreBottomSheet = MutableStateFlow(false)
    val showMoreBottomSheet: StateFlow<Boolean> = _showMoreBottomSheet.asStateFlow()

    val categoriesMap = categoryRepository.getAllCategories()
        .map { cats -> cats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subcategoriesMap = subcategoryRepository.getAllSubcategories()
        .map { subcats -> subcats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val accountsMap = accountBalanceRepository.getAllLatestBalances()
        .map { accountList ->
            accountList.associateBy { it.id.toString() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Store currency breakdown maps for quick access when switching currencies
    private var currentMonthBreakdownMap: Map<String, TransactionRepository.MonthlyBreakdown> =
        emptyMap()
    private var lastMonthBreakdownMap: Map<String, TransactionRepository.MonthlyBreakdown> =
        emptyMap()
    private var currentYearBreakdownMap: Map<String, TransactionRepository.MonthlyBreakdown> =
        emptyMap()

    private val baseCurrency = currencyRepository.effectiveBaseCurrencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    private val _selectedCurrency = MutableStateFlow<String?>(null)
    private val selectedCurrencyCombined = combine(
        baseCurrency,
        _selectedCurrency
    ) { base, selected ->
        selected ?: base
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    init {
        loadHomeData()
        loadUserData()
        observeCoupleTracker()
    }

    private fun observeCoupleTracker() {
        viewModelScope.launch {
            combine(
                p2pPreferences.coupleTrackingEnabledFlow,
                p2pPreferences.activeViewModeFlow,
                p2pPreferences.partnerNameFlow,
                p2pPreferences.partnerUserIdFlow
            ) { enabled, viewMode, partnerName, partnerUserId ->
                _uiState.update {
                    it.copy(
                        isCoupleTrackingEnabled = enabled,
                        activeViewMode = viewMode,
                        partnerName = partnerName,
                        partnerUserId = partnerUserId,
                        myUserId = p2pPreferences.getDeviceId()
                    )
                }
            }.collectLatest { }
        }
    }

    fun setViewMode(mode: ViewMode) {
        p2pPreferences.setActiveViewMode(mode)
    }

    private fun loadUserData() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { preferences ->
                _uiState.value = _uiState.value.copy(
                    userName = preferences.userName,
                    profileImageUri = preferences.profileImageUri?.toUri(),
                    profileBackgroundColor = Color(preferences.profileBackgroundColor)
                )
            }
        }

        viewModelScope.launch {
            unrecognizedSmsRepository.getUnreportedCount().collect { count ->
                _uiState.value = _uiState.value.copy(unreadUpdatesCount = count)
            }
        }

        viewModelScope.launch {
            combine(
                userPreferencesRepository.homeWidgetsOrder,
                userPreferencesRepository.hiddenHomeWidgets
            ) { order, hidden ->
                val orderedWidgets = mutableListOf<HomeWidgetUiModel>()
                val missingWidgets = HomeWidget.entries.filter { it !in order }.sortedBy { it.defaultOrder }

                // Saved order widgets
                order.forEach { widget ->
                    orderedWidgets.add(HomeWidgetUiModel(widget, !hidden.contains(widget)))
                }

                // Insert any missing/newly introduced widgets at their natural defaultOrder position
                missingWidgets.forEach { widget ->
                    val insertIndex = widget.defaultOrder.coerceIn(0, orderedWidgets.size)
                    orderedWidgets.add(insertIndex, HomeWidgetUiModel(widget, !hidden.contains(widget)))
                }

                orderedWidgets
            }.collect { widgets ->
                _homeWidgets.value = widgets
            }
        }

        viewModelScope.launch {
            _uiState
                .map { Triple(it.totalBalance, it.currentMonthIncome to it.currentMonthExpenses, it.upcomingSubscriptions.size) }
                .collectLatest {
                    com.reddy.vittify.widget.FinancialOverviewWidgetProvider.requestUpdate(context)
                }
        }
    }

    private fun getOwnerFilter(
        viewMode: ViewMode,
        coupleEnabled: Boolean,
        accountOwnerMap: Map<String, String> = emptyMap(),
        accountLast4OwnerMap: Map<String, String> = emptyMap()
    ): ((TransactionEntity) -> Boolean)? {
        if (!coupleEnabled) return null
        val myDeviceId = p2pPreferences.getDeviceId()
        return when (viewMode) {
            ViewMode.PERSONAL -> { tx ->
                val owner = tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                    ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                    ?: tx.ownerId
                owner.isBlank() || owner == myDeviceId
            }
            ViewMode.PARTNER -> { tx ->
                val owner = tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                    ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                    ?: tx.ownerId
                owner.isNotBlank() && owner != myDeviceId
            }
            ViewMode.COMBINED -> null
        }
    }

    private fun loadHomeData() {
        // Observe base currency changes
        viewModelScope.launch {
            baseCurrency.collect { mainAccountCurrency ->
                val prevCurrency = _uiState.value.selectedCurrency
                if (mainAccountCurrency != prevCurrency) {
                    _uiState.update { it.copy(
                        selectedCurrency = mainAccountCurrency,
                        baseCurrency = mainAccountCurrency
                    ) }
                    _selectedCurrency.value = mainAccountCurrency
                    // Recalculate breakdown totals and account balances for the new currency
                    val availableCurrencies = _uiState.value.availableCurrencies
                    if (availableCurrencies.isNotEmpty()) {
                        updateUIStateForCurrency(mainAccountCurrency, availableCurrencies)
                    }
                }
            }
        }

        viewModelScope.launch {
            // Load current month breakdown by currency with view mode filtering
            combine(
                p2pPreferences.activeViewModeFlow,
                p2pPreferences.coupleTrackingEnabledFlow,
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger,
                accountBalanceRepository.getAllLatestBalances()
            ) { viewMode, coupleEnabled, selectedCurrency, _, accountsList ->
                val accountOwnerMap = accountsList.associate { it.id to it.ownerId }
                val accountLast4OwnerMap = accountsList.filter { it.ownerId.isNotBlank() }.associate { it.accountLast4 to it.ownerId }
                val filter = getOwnerFilter(viewMode, coupleEnabled, accountOwnerMap, accountLast4OwnerMap)
                Triple(filter, selectedCurrency, Unit)
            }.flatMapLatest { (filter, selectedCurrency, _) ->
                transactionRepository.getCurrentMonthBreakdownByCurrency(filter).map { breakdown ->
                    Pair(breakdown, selectedCurrency)
                }
            }.collectLatest { (breakdownByCurrency, selectedCurrency) ->
                updateBreakdownForSelectedCurrency(breakdownByCurrency, period = FinancialPeriod.CURRENT_MONTH, selectedCurrency = selectedCurrency)
            }
        }

        viewModelScope.launch {
            // Load account balances and react to currency and view mode changes
            combine(
                accountBalanceRepository.getAllLatestBalances(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger,
                p2pPreferences.activeViewModeFlow,
                p2pPreferences.coupleTrackingEnabledFlow
            ) { allBalances, selectedCurrency, _, viewMode, coupleEnabled ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val viewModeFilteredBalances = if (!coupleEnabled) {
                    allBalances
                } else {
                    when (viewMode) {
                        ViewMode.PERSONAL -> allBalances.filter { it.ownerId.isBlank() || it.ownerId == myDeviceId }
                        ViewMode.PARTNER -> allBalances.filter { it.ownerId.isNotBlank() && it.ownerId != myDeviceId }
                        ViewMode.COMBINED -> allBalances
                    }
                }

                // Get hidden accounts from SharedPreferences
                val hiddenAccounts =
                    sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()

                // Filter out hidden accounts
                val balances = viewModeFilteredBalances.filter { account ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    !hiddenAccounts.contains(key)
                }
                // Separate credit cards from regular accounts (hide zero balance accounts)
                val regularAccounts =
                    balances.filter { !it.isCreditCard && it.balance != BigDecimal.ZERO }
                val creditCards = balances.filter { it.isCreditCard }

                // Account loading completed
                Log.d("HomeViewModel", "Loaded ${balances.size} account(s)")

                // Check if we have multiple currencies and refresh exchange rates if needed
                val accountCurrencies = regularAccounts.map { it.currency }.distinct()
                val hasMultipleCurrencies = accountCurrencies.size > 1

                if (hasMultipleCurrencies && accountCurrencies.isNotEmpty()) {
                    currencyConversionService.refreshExchangeRatesForAccount(accountCurrencies)
                }

                // Convert all account balances to selected currency for total
                var assetBalanceInSelectedCurrency = BigDecimal.ZERO
                for (account in regularAccounts) {
                    val amt = if (account.currency == selectedCurrency) {
                        account.balance
                    } else {
                        currencyConversionService.convertAmount(
                            amount = account.balance,
                            fromCurrency = account.currency,
                            toCurrency = selectedCurrency
                        )
                    }
                    assetBalanceInSelectedCurrency = assetBalanceInSelectedCurrency.add(amt)
                }

                var liabilityBalanceInSelectedCurrency = BigDecimal.ZERO
                for (card in creditCards) {
                    val amt = if (card.currency == selectedCurrency) {
                        card.balance
                    } else {
                        currencyConversionService.convertAmount(
                            amount = card.balance,
                            fromCurrency = card.currency,
                            toCurrency = selectedCurrency
                        )
                    }
                    liabilityBalanceInSelectedCurrency = liabilityBalanceInSelectedCurrency.add(amt)
                }

                val totalBalanceInSelectedCurrency = assetBalanceInSelectedCurrency - liabilityBalanceInSelectedCurrency

                var totalAvailableCreditInSelectedCurrency = BigDecimal.ZERO
                for (card in creditCards) {
                    val availableInCardCurrency = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
                    val amt = if (card.currency == selectedCurrency) {
                        availableInCardCurrency
                    } else {
                        currencyConversionService.convertAmount(
                            amount = availableInCardCurrency,
                            fromCurrency = card.currency,
                            toCurrency = selectedCurrency
                        )
                    }
                    totalAvailableCreditInSelectedCurrency = totalAvailableCreditInSelectedCurrency.add(amt)
                }

                _uiState.update { 
                    it.copy(
                        accountBalances = regularAccounts,
                        creditCards = creditCards,
                        totalBalance = totalBalanceInSelectedCurrency,
                        totalAvailableCredit = totalAvailableCreditInSelectedCurrency,
                        selectedCurrency = selectedCurrency
                    )
                }
            }.collectLatest { }
        }

        viewModelScope.launch {
            // Load current month transactions by type (currency-filtered)
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1)
            val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())

            combine(
                transactionRepository.getTransactionsBetweenDates(
                    startDate = startOfMonth,
                    endDate = endOfMonth
                ),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger,
                p2pPreferences.coupleViewState,
                accountBalanceRepository.getAllLatestBalances()
            ) { allTransactions, selectedCurrency, _, coupleState, accountsList ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val accountOwnerMap = accountsList.associate { it.id to it.ownerId }
                val accountLast4OwnerMap = accountsList.filter { it.ownerId.isNotBlank() }.associate { it.accountLast4 to it.ownerId }
                val transactions = if (!coupleState.coupleEnabled) {
                    allTransactions
                } else {
                    when (coupleState.viewMode) {
                        ViewMode.PERSONAL -> allTransactions.filter { tx ->
                            val owner = tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.ownerId
                            owner.isBlank() || owner == myDeviceId
                        }
                        ViewMode.PARTNER -> allTransactions.filter { tx ->
                            val owner = tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.ownerId
                            owner.isNotBlank() && owner != myDeviceId
                        }
                        ViewMode.COMBINED -> allTransactions
                    }
                }
                var creditCardTotal = BigDecimal.ZERO
                var transferTotal = BigDecimal.ZERO
                var investmentTotal = BigDecimal.ZERO

                transactions.forEach { tx ->
                    val convertedAmount = if (tx.currency == selectedCurrency) {
                        tx.amount
                    } else {
                        currencyConversionService.convertAmount(
                            amount = tx.amount,
                            fromCurrency = tx.currency,
                            toCurrency = selectedCurrency
                        ) ?: tx.amount
                    }

                    when (tx.transactionType) {
                        TransactionType.CREDIT -> creditCardTotal += convertedAmount
                        TransactionType.TRANSFER -> transferTotal += convertedAmount
                        TransactionType.INVESTMENT -> investmentTotal += convertedAmount
                        else -> {}
                    }
                }

                _uiState.update { it.copy(
                    currentMonthCreditCard = creditCardTotal,
                    currentMonthTransfer = transferTotal,
                    currentMonthInvestment = investmentTotal
                ) }
            }.collectLatest { }
        }

        viewModelScope.launch {
            // Load heatmap data (last 26 weeks)
            val endOfHeatmap = LocalDate.now()
            val startOfHeatmap = endOfHeatmap.minusWeeks(26).with(java.time.DayOfWeek.MONDAY)
            
            combine(
                transactionRepository.getTransactionsBetweenDates(
                    startDate = startOfHeatmap,
                    endDate = endOfHeatmap
                ),
                p2pPreferences.activeViewModeFlow,
                p2pPreferences.coupleTrackingEnabledFlow,
                accountBalanceRepository.getAllLatestBalances()
            ) { allTransactions, viewMode, coupleEnabled, accountsList ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val accountOwnerMap = accountsList.associate { it.id to it.ownerId }
                val accountLast4OwnerMap = accountsList.filter { it.ownerId.isNotBlank() }.associate { it.accountLast4 to it.ownerId }
                val transactions = if (!coupleEnabled) {
                    allTransactions
                } else {
                    when (viewMode) {
                        ViewMode.PERSONAL -> allTransactions.filter { tx ->
                            val owner = tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.ownerId
                            owner.isBlank() || owner == myDeviceId
                        }
                        ViewMode.PARTNER -> allTransactions.filter { tx ->
                            val owner = tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.ownerId
                            owner.isNotBlank() && owner != myDeviceId
                        }
                        ViewMode.COMBINED -> allTransactions
                    }
                }
                val heatmap = transactions.groupBy { it.dateTime.toLocalDate() }
                    .mapValues { it.value.size }
                _uiState.update { it.copy(transactionHeatmap = heatmap) }
            }.collectLatest { }
        }

        viewModelScope.launch {
            // Load last month breakdown by currency with view mode filtering
            combine(
                p2pPreferences.activeViewModeFlow,
                p2pPreferences.coupleTrackingEnabledFlow,
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger,
                accountBalanceRepository.getAllLatestBalances()
            ) { viewMode, coupleEnabled, selectedCurrency, _, accountsList ->
                val accountOwnerMap = accountsList.associate { it.id to it.ownerId }
                val accountLast4OwnerMap = accountsList.filter { it.ownerId.isNotBlank() }.associate { it.accountLast4 to it.ownerId }
                val filter = getOwnerFilter(viewMode, coupleEnabled, accountOwnerMap, accountLast4OwnerMap)
                Triple(filter, selectedCurrency, Unit)
            }.flatMapLatest { (filter, selectedCurrency, _) ->
                transactionRepository.getLastMonthBreakdownByCurrency(filter).map { breakdown ->
                    Pair(breakdown, selectedCurrency)
                }
            }.collectLatest { (breakdownByCurrency, selectedCurrency) ->
                updateBreakdownForSelectedCurrency(breakdownByCurrency, period = FinancialPeriod.LAST_MONTH, selectedCurrency = selectedCurrency)
            }
        }

        viewModelScope.launch {
            // Load current year breakdown by currency with view mode filtering
            combine(
                p2pPreferences.activeViewModeFlow,
                p2pPreferences.coupleTrackingEnabledFlow,
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger,
                accountBalanceRepository.getAllLatestBalances()
            ) { viewMode, coupleEnabled, selectedCurrency, _, accountsList ->
                val accountOwnerMap = accountsList.associate { it.id to it.ownerId }
                val accountLast4OwnerMap = accountsList.filter { it.ownerId.isNotBlank() }.associate { it.accountLast4 to it.ownerId }
                val filter = getOwnerFilter(viewMode, coupleEnabled, accountOwnerMap, accountLast4OwnerMap)
                Triple(filter, selectedCurrency, Unit)
            }.flatMapLatest { (filter, selectedCurrency, _) ->
                transactionRepository.getCurrentYearBreakdownByCurrency(filter).map { breakdown ->
                    Pair(breakdown, selectedCurrency)
                }
            }.collectLatest { (breakdownByCurrency, selectedCurrency) ->
                updateBreakdownForSelectedCurrency(breakdownByCurrency, period = FinancialPeriod.CURRENT_YEAR, selectedCurrency = selectedCurrency)
            }
        }

        viewModelScope.launch {
            // Load recent transactions (last 3) and react to base currency and view mode changes
            combine(
                transactionRepository.getAllTransactions(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger,
                p2pPreferences.coupleViewState,
                accountBalanceRepository.getAllLatestBalances()
            ) { allTransactions, selectedCurrency, _, coupleState, accountsList ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val accountOwnerMap = accountsList.associate { it.id to it.ownerId }
                val accountLast4OwnerMap = accountsList.filter { it.ownerId.isNotBlank() }.associate { it.accountLast4 to it.ownerId }
                val transactions = if (!coupleState.coupleEnabled) {
                    allTransactions.filter { it.transactionType != TransactionType.BALANCE_UPDATE }
                        .sortedByDescending { it.dateTime }
                        .take(3)
                } else {
                    val filteredByOwner = when (coupleState.viewMode) {
                        ViewMode.PERSONAL -> allTransactions.filter { tx ->
                            val owner = tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.ownerId
                            owner.isBlank() || owner == myDeviceId
                        }
                        ViewMode.PARTNER -> allTransactions.filter { tx ->
                            val owner = tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                                ?: tx.ownerId
                            owner.isNotBlank() && owner != myDeviceId
                        }
                        ViewMode.COMBINED -> allTransactions
                    }
                    filteredByOwner.filter { it.transactionType != TransactionType.BALANCE_UPDATE }
                        .sortedByDescending { it.dateTime }
                        .take(3)
                }
                // Calculate converted amounts for shown transactions if transaction currency differs from selected currency
                val converted = transactions
                    .filter { it.currency != selectedCurrency }
                    .associate { tx ->
                        tx.id to (currencyConversionService.convertAmount(tx.amount, tx.currency, selectedCurrency) ?: tx.amount)
                    }

                val splitIds = transactionRepository.getSplitTransactionIds(transactions.map { it.id }).toSet()
                
                _uiState.update { it.copy(
                    recentTransactions = transactions,
                    convertedAmounts = converted,
                    splitTransactionIds = splitIds,
                    isLoading = false
                ) }
            }.collectLatest { }
        }

        viewModelScope.launch {
            // Load all active subscriptions and react to currency and view mode changes
            combine(
                subscriptionRepository.getActiveSubscriptions(),
                selectedCurrencyCombined,
                p2pPreferences.coupleViewState,
                currencyConversionService.rateChangeTrigger
            ) { allSubscriptions, targetCurrency, coupleState, _ ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val subscriptions = allSubscriptions.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { it.ownerId }

                // Check if we need to refresh rates for subscription currencies
                val subscriptionCurrencies = subscriptions.map { it.currency }.distinct()
                if (subscriptionCurrencies.any { it != targetCurrency }) {
                    currencyConversionService.refreshExchangeRatesForAccount(subscriptionCurrencies + targetCurrency)
                }

                var totalAmount = BigDecimal.ZERO
                for (subscription in subscriptions) {
                    val amt = if (subscription.currency == targetCurrency) {
                        subscription.amount
                    } else {
                        currencyConversionService.convertAmount(
                            amount = subscription.amount,
                            fromCurrency = subscription.currency,
                            toCurrency = targetCurrency
                        ) ?: subscription.amount
                    }
                    totalAmount = totalAmount.add(amt)
                }

                _uiState.update {
                    it.copy(
                        upcomingSubscriptions = subscriptions,
                        upcomingSubscriptionsTotal = totalAmount,
                        upcomingSubscriptionsCurrency = targetCurrency
                    )
                }
            }.collectLatest { }
        }

        viewModelScope.launch {
            // Load active budgets for current month and react to currency and view mode changes
            val yearMonth = YearMonth.now()
            combine(
                budgetRepository.getBudgetsWithSpendingForMonth(yearMonth.year, yearMonth.monthValue),
                selectedCurrencyCombined,
                p2pPreferences.coupleViewState,
                currencyConversionService.rateChangeTrigger
            ) { allBudgets, targetCurrency, coupleState, _ ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val budgets = allBudgets.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { it.budget.ownerId }

                // Convert budgets to match the selected main currency for display
                val convertedBudgets = budgets.map { budgetWithSpending ->
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
                
                _uiState.update { 
                    it.copy(activeBudgets = convertedBudgets)
                }
            }.collectLatest { }
        }

        viewModelScope.launch {
            // Load portfolio balance history dynamically computed from account balances and transactions
            combine(
                accountBalanceRepository.getAllLatestBalances(),
                transactionRepository.getAllTransactions(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger,
                p2pPreferences.coupleViewState
            ) { allBalances, allTransactions, selectedCurrency, _, coupleState ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val viewModeFilteredBalances = allBalances.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { it.ownerId }
                val accountOwnerMap = allBalances.associate { it.id to it.ownerId }
                val accountLast4OwnerMap = allBalances.filter { it.ownerId.isNotBlank() }.associate { it.accountLast4 to it.ownerId }
                val viewModeFilteredTransactions = allTransactions.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { tx ->
                    tx.accountId?.let { accountOwnerMap[it] }?.takeIf { it.isNotBlank() }
                        ?: tx.fromAccount?.let { accountLast4OwnerMap[it] }?.takeIf { it.isNotBlank() }
                        ?: tx.ownerId
                }

                val hiddenAccounts = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
                val visibleBalances = viewModeFilteredBalances.filter { account ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    !hiddenAccounts.contains(key)
                }

                var currentNetWorth = BigDecimal.ZERO
                for (account in visibleBalances) {
                    val balanceValue = if (account.isCreditCard) account.balance.negate() else account.balance
                    val converted = if (account.currency == selectedCurrency) {
                        balanceValue
                    } else {
                        currencyConversionService.convertAmount(
                            amount = balanceValue,
                            fromCurrency = account.currency,
                            toCurrency = selectedCurrency
                        ) ?: balanceValue
                    }
                    currentNetWorth = currentNetWorth.add(converted)
                }

                val validTransactions = viewModeFilteredTransactions
                    .filter { !it.isDeleted }
                    .sortedBy { it.dateTime }
                val txByDate = validTransactions.groupBy { it.dateTime.toLocalDate() }

                val endDate = LocalDate.now()
                val firstTxDate = validTransactions.minOfOrNull { it.dateTime.toLocalDate() }
                val startDate = BalanceCalculator.calculatePortfolioStartDate(
                    firstTransactionDate = firstTxDate,
                    endDate = endDate,
                    maxDays = 180
                )

                val historyMap = mutableMapOf<LocalDate, BigDecimal>()
                var runningNetWorth = currentNetWorth

                var currentDate = endDate
                while (!currentDate.isBefore(startDate)) {
                    historyMap[currentDate] = runningNetWorth

                    val dayTransactions = (txByDate[currentDate] ?: emptyList()).sortedByDescending { it.dateTime }
                    var dayNetChange = BigDecimal.ZERO

                    for (tx in dayTransactions) {
                        val convertedAmt = if (tx.currency == selectedCurrency) {
                            tx.amount
                        } else {
                            currencyConversionService.convertAmount(
                                amount = tx.amount,
                                fromCurrency = tx.currency,
                                toCurrency = selectedCurrency
                            ) ?: tx.amount
                        }

                        when (tx.transactionType) {
                            TransactionType.INCOME, TransactionType.CREDIT -> dayNetChange = dayNetChange.add(convertedAmt)
                            TransactionType.EXPENSE, TransactionType.INVESTMENT -> dayNetChange = dayNetChange.subtract(convertedAmt)
                            else -> {}
                        }
                    }

                    runningNetWorth = runningNetWorth.subtract(dayNetChange)
                    currentDate = currentDate.minusDays(1)
                }

                // Record the baseline balance before startDate's transactions took place
                historyMap[startDate.minusDays(1)] = runningNetWorth

                historyMap.toSortedMap().map { (date, total) ->
                    BalancePoint(
                        timestamp = date.atStartOfDay(),
                        balance = total,
                        currency = selectedCurrency
                    )
                }
            }.collectLatest { dailyPortfolioHistory ->
                _uiState.update { it.copy(
                    balanceHistory = dailyPortfolioHistory
                ) }
            }
        }
    }

    private fun calculateMonthlyChange() {
        val currentExpenses = _uiState.value.currentMonthExpenses
        val lastExpenses = _uiState.value.lastMonthExpenses
        val currentTotal = _uiState.value.currentMonthTotal
        val lastTotal = _uiState.value.lastMonthTotal

        // Calculate expense change for simple comparison
        val expenseChange = currentExpenses - lastExpenses
        val totalChange = currentTotal - lastTotal

        val monthlyChangePercent = if (lastTotal != BigDecimal.ZERO) {
            ((totalChange.toDouble() / lastTotal.toDouble()) * 100).toInt()
        } else if (totalChange != BigDecimal.ZERO) {
            100 // Assume 100% growth if starting from zero
        } else {
            0
        }

        _uiState.value = _uiState.value.copy(
            monthlyChange = totalChange,
            monthlyChangePercent = monthlyChangePercent
        )
    }

    fun refreshHiddenAccounts() {
        viewModelScope.launch {
            // Force re-read of hidden accounts from SharedPreferences
            val hiddenAccounts =
                sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()

            val viewMode = p2pPreferences.getActiveViewMode()
            val coupleEnabled = p2pPreferences.isCoupleTrackingEnabled()
            val myDeviceId = p2pPreferences.getDeviceId()

            // Re-fetch all accounts and filter
            accountBalanceRepository.getAllLatestBalances().first().let { allBalances: List<AccountBalanceEntity> ->
                val viewModeFilteredBalances = if (!coupleEnabled) {
                    allBalances
                } else {
                    when (viewMode) {
                        ViewMode.PERSONAL -> allBalances.filter { it.ownerId.isBlank() || it.ownerId == myDeviceId }
                        ViewMode.PARTNER -> allBalances.filter { it.ownerId.isNotBlank() && it.ownerId != myDeviceId }
                        ViewMode.COMBINED -> allBalances
                    }
                }

                val visibleBalances: List<AccountBalanceEntity> = viewModeFilteredBalances.filter { account: AccountBalanceEntity ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    !hiddenAccounts.contains(key)
                }

                // Separate credit cards from regular accounts (hide zero balance accounts)
                val regularAccounts: List<AccountBalanceEntity> =
                    visibleBalances.filter { !it.isCreditCard && it.balance != BigDecimal.ZERO }
                val creditCards: List<AccountBalanceEntity> = visibleBalances.filter { it.isCreditCard }

                // Update UI state
                _uiState.update {
                    it.copy(
                        accountBalances = regularAccounts,
                        creditCards = creditCards,
                        totalBalance = regularAccounts.sumOfBigDecimal { acc: AccountBalanceEntity -> acc.balance },
                        totalAvailableCredit = creditCards.sumOfBigDecimal { card: AccountBalanceEntity ->
                            // Available = Credit Limit - Outstanding Balance
                            (card.creditLimit ?: BigDecimal.ZERO) - card.balance
                        }
                    )
                }
            }
        }
    }

    /**
     * Scans SMS messages for transactions.
     * @param forceResync If true, performs a full resync from scratch, reprocessing all SMS messages.
     *                    This is useful when bank parsers have been updated and old transactions need to be re-parsed.
     *                    If false (default), performs an incremental scan for new messages only.
     */
    fun scanSmsMessages(forceResync: Boolean = false) {
        val inputData = workDataOf(
            OptimizedSmsReaderWorker.INPUT_FORCE_RESYNC to forceResync
        )

        val workRequest = OneTimeWorkRequestBuilder<OptimizedSmsReaderWorker>()
            .setInputData(inputData)
            .addTag(OptimizedSmsReaderWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            OptimizedSmsReaderWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // Update UI to show scanning
        _uiState.value = _uiState.value.copy(isScanning = true)

        // Track work progress
        observeWorkProgress()
    }

    private fun observeWorkProgress() {
        val workManager = WorkManager.getInstance(context)

        // Use getWorkInfosById for more direct observation
        workManager.getWorkInfosByTagLiveData(OptimizedSmsReaderWorker.WORK_NAME).observeForever { workInfos ->
            val currentWork = workInfos.firstOrNull { it.tags.contains(OptimizedSmsReaderWorker.WORK_NAME) }
            if (currentWork != null) {
                _smsScanWorkInfo.value = currentWork

                // Update scanning state based on work state
                when (currentWork.state) {
                    WorkInfo.State.SUCCEEDED,
                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED,
                    WorkInfo.State.BLOCKED -> {
                        _uiState.value = _uiState.value.copy(isScanning = false)
                    }
                    else -> {
                        // Still running or enqueued
                        _uiState.value = _uiState.value.copy(isScanning = true)
                    }
                }
            }
        }
    }

    fun cancelSmsScan() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(OptimizedSmsReaderWorker.WORK_NAME)
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun refreshAccountBalances() {
        viewModelScope.launch {
            // Force refresh the account balances by fetching once (prevents memory leaks)
            val allBalances = accountBalanceRepository.getAllLatestBalances().first()
            val viewMode = p2pPreferences.getActiveViewMode()
            val coupleEnabled = p2pPreferences.isCoupleTrackingEnabled()
            val myDeviceId = p2pPreferences.getDeviceId()

            val viewModeFilteredBalances = if (!coupleEnabled) {
                allBalances
            } else {
                when (viewMode) {
                    ViewMode.PERSONAL -> allBalances.filter { it.ownerId.isBlank() || it.ownerId == myDeviceId }
                    ViewMode.PARTNER -> allBalances.filter { it.ownerId.isNotBlank() && it.ownerId != myDeviceId }
                    ViewMode.COMBINED -> allBalances
                }
            }

            val hiddenAccounts = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()

            // Filter out hidden accounts
            val balances = viewModeFilteredBalances.filter { account ->
                val key = "${account.bankName}_${account.accountLast4}"
                !hiddenAccounts.contains(key)
            }
            val regularAccounts = balances.filter { !it.isCreditCard && it.balance != BigDecimal.ZERO }
            val creditCards = balances.filter { it.isCreditCard }

            val accountCurrencies = regularAccounts.map { it.currency }.distinct()
            val creditCardCurrencies = creditCards.map { it.currency }.distinct()
            val allAccountCurrencies = (accountCurrencies + creditCardCurrencies).distinct()
            val hasMultipleCurrencies = allAccountCurrencies.size > 1

            if (hasMultipleCurrencies && allAccountCurrencies.isNotEmpty()) {
                currencyConversionService.refreshExchangeRatesForAccount(allAccountCurrencies)
            }

            val currentAvailableCurrencies = _uiState.value.availableCurrencies.toSet()
            val updatedAvailableCurrencies = (currentAvailableCurrencies + allAccountCurrencies)
                .sortedWith { a, b ->
                    when {
                        a == "INR" -> -1
                        b == "INR" -> 1
                        else -> a.compareTo(b)
                    }
                }

            val selectedCurrency = _selectedCurrency.value ?: baseCurrency.value
            var assetBalanceInSelectedCurrency = BigDecimal.ZERO
            for (account in regularAccounts) {
                val amt = if (account.currency == selectedCurrency) account.balance
                else currencyConversionService.convertAmount(account.balance, account.currency, selectedCurrency)
                assetBalanceInSelectedCurrency = assetBalanceInSelectedCurrency.add(amt)
            }
            var liabilityBalanceInSelectedCurrency = BigDecimal.ZERO
            for (card in creditCards) {
                val amt = if (card.currency == selectedCurrency) card.balance
                else currencyConversionService.convertAmount(card.balance, card.currency, selectedCurrency)
                liabilityBalanceInSelectedCurrency = liabilityBalanceInSelectedCurrency.add(amt)
            }
            val totalBalanceInSelectedCurrency = assetBalanceInSelectedCurrency - liabilityBalanceInSelectedCurrency
            var totalAvailableCreditInSelectedCurrency = BigDecimal.ZERO
            for (card in creditCards) {
                val availableInCardCurrency = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
                val amt = if (card.currency == selectedCurrency) availableInCardCurrency
                else currencyConversionService.convertAmount(availableInCardCurrency, card.currency, selectedCurrency)
                totalAvailableCreditInSelectedCurrency = totalAvailableCreditInSelectedCurrency.add(amt)
            }

            _uiState.update { 
                it.copy(
                    accountBalances = regularAccounts,
                    creditCards = creditCards,
                    totalBalance = totalBalanceInSelectedCurrency,
                    totalAvailableCredit = totalAvailableCreditInSelectedCurrency,
                    availableCurrencies = updatedAvailableCurrencies,
                    selectedCurrency = selectedCurrency
                )
            }
        }
    }


    fun hideBreakdownDialog() {
        _uiState.value = _uiState.value.copy(showBreakdownDialog = false)
    }

    /**
     * Checks for app updates using Google Play In-App Updates.
     * Should be called with the current activity context.
     * @param activity The activity context
     * @param snackbarHostState Optional SnackbarHostState for showing restart prompt
     * @param scope Optional CoroutineScope for launching the snackbar
     */
    fun checkForAppUpdate(
        activity: ComponentActivity,
        snackbarHostState: SnackbarHostState? = null,
        scope: CoroutineScope? = null
    ) {
        inAppUpdateManager.checkForUpdate(activity, snackbarHostState, scope)
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _deletedTransaction.value = transaction
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun undoDelete() {
        _deletedTransaction.value?.let { transaction ->
            viewModelScope.launch {
                transactionRepository.undoDeleteTransaction(transaction)
                _deletedTransaction.value = null
            }
        }
    }

    fun undoDeleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.undoDeleteTransaction(transaction)
        }
    }

    fun clearDeletedTransaction() {
        _deletedTransaction.value = null
    }

    /**
     * Checks if eligible for in-app review and shows if appropriate.
     * Should be called with the current activity context.
     */
    fun checkForInAppReview(activity: ComponentActivity) {
        viewModelScope.launch {
            // Get current transaction count as additional eligibility factor
            val transactionCount = transactionRepository.getAllTransactions().first().size
            inAppReviewManager.checkAndShowReviewIfEligible(activity, transactionCount)
        }
    }

    fun selectCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    private suspend fun updateBreakdownForSelectedCurrency(
        breakdownByCurrency: Map<String, TransactionRepository.MonthlyBreakdown>,
        period: FinancialPeriod,
        selectedCurrency: String
    ) {
        // Store the breakdown map for later use when switching currencies
        when (period) {
            FinancialPeriod.CURRENT_MONTH -> currentMonthBreakdownMap = breakdownByCurrency
            FinancialPeriod.LAST_MONTH -> lastMonthBreakdownMap = breakdownByCurrency
            FinancialPeriod.CURRENT_YEAR -> currentYearBreakdownMap = breakdownByCurrency
        }

        // Update available currencies from all stored data and include the effective base currency
        val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()
        val allCurrencies = (currentMonthBreakdownMap.keys + lastMonthBreakdownMap.keys + currentYearBreakdownMap.keys + effectiveCurrency).distinct()
        val availableCurrencies = allCurrencies.sortedWith { a, b ->
            when {
                a == "INR" -> -1 // INR first
                b == "INR" -> 1
                else -> a.compareTo(b) // Alphabetical for others
            }
        }

        // Update UI state with values for selected currency
        updateUIStateForCurrency(selectedCurrency, availableCurrencies)
    }

    private suspend fun calculateAggregatedBreakdown(
        selectedCurrency: String,
        breakdownMap: Map<String, TransactionRepository.MonthlyBreakdown>
    ): TransactionRepository.MonthlyBreakdown {
        var total = BigDecimal.ZERO
        var income = BigDecimal.ZERO
        var expenses = BigDecimal.ZERO

        breakdownMap.forEach { (currency, breakdown) ->
            if (currency == selectedCurrency) {
                total += breakdown.total
                income += breakdown.income
                expenses += breakdown.expenses
            } else {
                total += currencyConversionService.convertAmount(breakdown.total, currency, selectedCurrency)
                income += currencyConversionService.convertAmount(breakdown.income, currency, selectedCurrency)
                expenses += currencyConversionService.convertAmount(breakdown.expenses, currency, selectedCurrency)
            }
        }
        return TransactionRepository.MonthlyBreakdown(total, income, expenses)
    }

    private suspend fun updateUIStateForCurrency(selectedCurrency: String, availableCurrencies: List<String>) {
        // Calculate aggregated breakdown across all currencies by converting to selected currency
        val currentBreakdown = calculateAggregatedBreakdown(selectedCurrency, currentMonthBreakdownMap)
        val lastBreakdown = calculateAggregatedBreakdown(selectedCurrency, lastMonthBreakdownMap)
        val currentYearBreakdown = calculateAggregatedBreakdown(selectedCurrency, currentYearBreakdownMap)

        _uiState.update {
            it.copy(
                currentMonthTotal = currentBreakdown.total,
                currentYearTotal = currentYearBreakdown.total,
                currentMonthIncome = currentBreakdown.income,
                currentMonthExpenses = currentBreakdown.expenses,
                currentYearExpenses = currentYearBreakdown.expenses,
                lastMonthTotal = lastBreakdown.total,
                lastMonthIncome = lastBreakdown.income,
                lastMonthExpenses = lastBreakdown.expenses,
                selectedCurrency = selectedCurrency,
                availableCurrencies = availableCurrencies
            )
        }
        calculateMonthlyChange()
    }

    private enum class FinancialPeriod {
        CURRENT_MONTH,
        LAST_MONTH,
        CURRENT_YEAR
    }



    fun openEditWidgetsSheet() { _showEditWidgetsSheet.value = true }
    fun closeEditWidgetsSheet() { _showEditWidgetsSheet.value = false }
    fun openMoreBottomSheet() { _showMoreBottomSheet.value = true }
    fun closeMoreBottomSheet() { _showMoreBottomSheet.value = false }

    fun toggleHomeWidgetVisibility(widget: HomeWidget, visible: Boolean) {
        viewModelScope.launch {
            val currentHidden = userPreferencesRepository.hiddenHomeWidgets.first().toMutableSet()
            if (visible) {
                currentHidden.remove(widget)
            } else {
                currentHidden.add(widget)
            }
            userPreferencesRepository.updateHiddenHomeWidgets(currentHidden)
        }
    }

    private var saveOrderJob: kotlinx.coroutines.Job? = null

    fun updateWidgetsOrder(widgets: List<HomeWidget>) {
        saveOrderJob?.cancel()
        saveOrderJob = viewModelScope.launch {
            // Debounce writes to avoid rapid DataStore updates and UI jank
            kotlinx.coroutines.delay(500)
            userPreferencesRepository.updateHomeWidgetsOrder(widgets)
        }
    }

    val homeShortcuts: StateFlow<Set<String>> = userPreferencesRepository.homeShortcuts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = setOf("PROFILE", "BACKUP_SYNC", "ACCOUNTS", "CATEGORIES", "BUDGETS", "RULES")
        )

    fun updateHomeShortcuts(shortcuts: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.updateHomeShortcuts(shortcuts)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // NLP Quick Add
    // ─────────────────────────────────────────────────────────────────────

    private val _nlpDraft = MutableStateFlow<ParsedTransactionDraft?>(null)
    val nlpDraft: StateFlow<ParsedTransactionDraft?> = _nlpDraft.asStateFlow()

    /**
     * Parses [input] using the on-device LLM (or regex fallback) and exposes
     * the result via [nlpDraft]. Sets nlpIsProcessing while working.
     */
    fun parseNaturalLanguageTransaction(input: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(nlpIsProcessing = true, nlpError = null) }
            try {
                val draft = nlpTransactionParser.parse(
                    input = input,
                    onStatusChange = { mode ->
                        _uiState.update { it.copy(nlpParsingMode = mode) }
                    },
                    onAiError = { errorMessage ->
                        _uiState.update { it.copy(nlpError = errorMessage) }
                    }
                )
                if (draft != null) {
                    _nlpDraft.value = draft
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "NLP parse error", e)
                _nlpDraft.value = null
            } finally {
                _uiState.update { it.copy(nlpIsProcessing = false, nlpParsingMode = null) }
            }
        }
    }

    /**
     * Parses [input] directly with the regex parser, bypassing AI.
     */
    fun parseNaturalLanguageWithRegex(input: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(nlpIsProcessing = true, nlpError = null, nlpParsingMode = NlpParsingMode.REGEX) }
            try {
                val draft = nlpTransactionParser.parseWithRegex(input)
                _nlpDraft.value = draft
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Regex parse error", e)
                _nlpDraft.value = null
            } finally {
                _uiState.update { it.copy(nlpIsProcessing = false, nlpParsingMode = null) }
            }
        }
    }

    /** Clears the NLP error state. */
    fun clearNlpError() {
        _uiState.update { it.copy(nlpError = null) }
    }

    /** Clears the NLP draft after navigation so it isn't re-applied on back-press. */
    fun clearNlpDraft() {
        _nlpDraft.value = null
    }

    fun syncDevices() {
        viewModelScope.launch {
            Toast.makeText(context, context.getString(R.string.synchronizing_devices), Toast.LENGTH_SHORT).show()
            val result = cloudSyncEngine.synchronize()
            if (result.isSuccess) {
                val syncData = result.getOrNull()
                val message = if (syncData != null) {
                    context.getString(R.string.sync_complete_format, syncData.peersSynced, syncData.importedTransactions)
                } else {
                    "Sync complete"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: context.getString(R.string.device_sync_failed)
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        inAppUpdateManager.cleanup()
    }
}
