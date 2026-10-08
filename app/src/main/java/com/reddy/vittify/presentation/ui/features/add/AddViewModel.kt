package com.reddy.vittify.presentation.ui.features.add

import com.reddy.vittify.utils.SubscriptionUtils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.TransactionItemEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.database.dao.RecentSubcategoryInfo
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.service.AttachmentService
import com.reddy.vittify.domain.usecase.AddSubscriptionUseCase
import com.reddy.vittify.domain.usecase.AddTransactionUseCase
import com.reddy.vittify.domain.usecase.GetCategoriesUseCase
import com.reddy.vittify.domain.usecase.UpdateSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import com.reddy.vittify.R
import com.reddy.vittify.data.nlp.ParsedTransactionDraft

import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository

@HiltViewModel
class AddViewModel
@Inject
constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val addSubscriptionUseCase: AddSubscriptionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val updateSubscriptionUseCase: UpdateSubscriptionUseCase,
    private val currencyConversionService: CurrencyConversionService,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val attachmentService: AttachmentService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)


    // General UI State
    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    // Transaction Tab State
    private val _transactionUiState = MutableStateFlow(TransactionUiState())
    val transactionUiState: StateFlow<TransactionUiState> = _transactionUiState.asStateFlow()

    // Subscription Tab State
    private val _subscriptionUiState = MutableStateFlow(SubscriptionUiState())
    val subscriptionUiState: StateFlow<SubscriptionUiState> = _subscriptionUiState.asStateFlow()

    // Categories for dropdowns
    val categories =
        getCategoriesUseCase
            .execute()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Filtered categories for current transaction type
    val transactionCategories: StateFlow<List<CategoryEntity>> =
        combine(categories, _transactionUiState) { allCats, uiState ->
            val expectedCategoryType = CategoryType.fromTransactionType(uiState.transactionType)
            if (expectedCategoryType == null) {
                emptyList()
            } else {
                allCats.filter { it.categoryType == expectedCategoryType }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val useCategoryAsMerchant: StateFlow<Boolean> = userPreferencesRepository.useCategoryAsMerchant
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Accounts for dropdown (filtered to current user only, respecting preserveAccountOrder)
    val accounts: StateFlow<List<AccountBalanceEntity>> = combine(
        accountBalanceRepository.getAllLatestBalances(),
        userPreferencesRepository.preserveAccountOrder
    ) { list, preserveOrder ->
        val myDeviceId = p2pPreferences.getDeviceId()
        val filtered = list.filter { it.ownerId.isBlank() || it.ownerId == myDeviceId }
        if (preserveOrder) {
            filtered.sortedWith(compareBy({ it.bankName.lowercase() }, { it.accountLast4 }))
        } else {
            filtered
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All Subcategories for sheet
    val allSubcategories = subcategoryRepository.subcategoriesMap

    // Recently used last 10 subcategories with their parent category
    private val recentlyUsedSubcategoriesFlow: Flow<List<RecentSubcategoryInfo>> =
        transactionRepository.getRecentlyUsedSubcategories(10)

    val recentSubcategories: StateFlow<List<Pair<CategoryEntity, SubcategoryEntity>>> =
        combine(
            transactionCategories,
            allSubcategories,
            recentlyUsedSubcategoriesFlow
        ) { catList: List<CategoryEntity>, subMap: Map<Long, List<SubcategoryEntity>>, recentInfos: List<RecentSubcategoryInfo> ->
            val result = mutableListOf<Pair<CategoryEntity, SubcategoryEntity>>()
            for (info in recentInfos) {
                val cat = catList.find { it.name.equals(info.category, ignoreCase = true) }
                val sub = cat?.let { category ->
                    subMap[category.id]?.find { it.name.equals(info.subcategory, ignoreCase = true) }
                }
                if (cat != null && sub != null) {
                    result.add(Pair(cat, sub))
                }
            }
            result
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )




    // Subcategories for the selected category
    private val _transactionSubcategories =
        MutableStateFlow<List<SubcategoryEntity>>(
            emptyList()
        )
    val transactionSubcategories:
            StateFlow<List<SubcategoryEntity>> =
        _transactionSubcategories.asStateFlow()

    private val _subscriptionSubcategories =
        MutableStateFlow<List<SubcategoryEntity>>(
            emptyList()
        )
    val subscriptionSubcategories:
            StateFlow<List<SubcategoryEntity>> =
        _subscriptionSubcategories.asStateFlow()

    // Transaction attachments
    private val _transactionAttachments = MutableStateFlow<List<String>>(emptyList())
    val transactionAttachments: StateFlow<List<String>> = _transactionAttachments.asStateFlow()

    // Subscription attachments
    private val _subscriptionAttachments = MutableStateFlow<List<String>>(emptyList())
    val subscriptionAttachments: StateFlow<List<String>> = _subscriptionAttachments.asStateFlow()

    init {
        initializeData()
    }

    private fun initializeData() {
        // Load default subcategories for both tabs
        updateTransactionSubcategories("Miscellaneous")
        updateSubscriptionSubcategories("Subscription")

        // Pre-select main account
        viewModelScope.launch {
            accounts.filter { it.isNotEmpty() }.first().let { availableAccounts ->
                val mainAccountKey = sharedPrefs.getString("main_account", null)
                if (mainAccountKey != null) {
                    val mainAccount = availableAccounts.find { 
                        "${it.bankName}_${it.accountLast4}" == mainAccountKey 
                    }
                    if (mainAccount != null) {
                        _transactionUiState.update { it.copy(selectedAccount = mainAccount, currency = mainAccount.currency) }
                        _subscriptionUiState.update { it.copy(selectedAccount = mainAccount, currency = mainAccount.currency) }
                    }
                }
            }
        }
    }

    fun resetAllStates() {
        _transactionUiState.value = TransactionUiState()
        _subscriptionUiState.value = SubscriptionUiState()
        _transactionAttachments.value = emptyList()
        _subscriptionAttachments.value = emptyList()
        initializeData()
    }

    // Transaction Tab Functions
    fun updateTransactionAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        val validAmount = if (decimalCount <= 1) filtered else _transactionUiState.value.amount

        _transactionUiState.update { currentState ->
            currentState.copy(amount = validAmount, amountError = validateAmount(validAmount))
        }
        updateTransferRateCalculation()
    }

    fun updateTransactionType(type: TransactionType) {
        val newCategory = when (type) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Miscellaneous"
            TransactionType.INVESTMENT -> "Investment"
            TransactionType.CREDIT -> "Credit Bill"
            TransactionType.TRANSFER -> "Self Transfer"
            else -> _transactionUiState.value.category
        }

        _transactionUiState.update { currentState ->
            currentState.copy(
                transactionType = type,
                category = newCategory,
                subcategory = null,
                categoryError = validateCategory(newCategory)
            )
        }

        // Update subcategories for the new default category
        updateTransactionSubcategories(newCategory)
        updateTransferRateCalculation()
    }

    fun updateTransactionMerchant(merchant: String) {
        _transactionUiState.update { currentState ->
            currentState.copy(merchant = merchant, merchantError = validateMerchant(merchant))
        }
    }

    fun updateTransactionCategory(category: String, subcategory: String? = null) {
        _transactionUiState.update { currentState ->
            val updatedMerchant = if (useCategoryAsMerchant.value) {
                if (!subcategory.isNullOrBlank()) subcategory else category
            } else {
                currentState.merchant
            }
            currentState.copy(
                category = category,
                subcategory = subcategory,
                categoryError = validateCategory(category),
                merchant = updatedMerchant,
                merchantError = validateMerchant(updatedMerchant)
            )
        }

        updateTransactionSubcategories(category)
    }

    private fun updateTransactionSubcategories(category: String) {
        viewModelScope.launch {
            val cat = categories.value.find { it.name == category }
            if (cat != null) {
                subcategoryRepository.getSubcategoriesByCategoryId(cat.id).collect {
                    _transactionSubcategories.value = it
                }
            } else {
                _transactionSubcategories.value = emptyList()
            }
        }
    }

    fun updateTransactionSubcategory(subcategory: String?) {
        _transactionUiState.update { currentState ->
            val updatedMerchant = if (useCategoryAsMerchant.value) {
                if (!subcategory.isNullOrBlank()) {
                    subcategory
                } else if (currentState.category.isNotBlank()) {
                    currentState.category
                } else {
                    currentState.merchant
                }
            } else {
                currentState.merchant
            }
            currentState.copy(
                subcategory = subcategory,
                merchant = updatedMerchant,
                merchantError = validateMerchant(updatedMerchant)
            )
        }
    }

    fun updateTransactionDate(dateMillis: Long) {
        val instant = Instant.ofEpochMilli(dateMillis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val currentTime = _transactionUiState.value.date.toLocalTime()
        val newDateTime = LocalDateTime.of(localDate, currentTime)

        _transactionUiState.update { currentState -> currentState.copy(date = newDateTime) }
        updateTransferRateCalculation()
    }

    fun updateTransactionTime(hour: Int, minute: Int) {
        val currentDate = _transactionUiState.value.date.toLocalDate()
        val newDateTime = currentDate.atTime(hour, minute)

        _transactionUiState.update { currentState -> currentState.copy(date = newDateTime) }
        updateTransferRateCalculation()
    }

    fun updateTransactionNotes(notes: String) {
        _transactionUiState.update { currentState -> currentState.copy(notes = notes) }
    }

    fun updateTransactionRecurring(isRecurring: Boolean) {
        _transactionUiState.update { currentState -> currentState.copy(isRecurring = isRecurring) }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _transactionUiState.value

        val amountError = validateAmount(state.amount)
        val merchantError = validateMerchant(state.merchant)
        val categoryError = validateCategory(state.category)

        // Additional validation for Transfer transactions
        if (state.transactionType == TransactionType.TRANSFER) {
            if (state.selectedAccount == null || state.targetAccount == null) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        error = context.getString(R.string.err_source_target_required)
                    )
                }
                return
            }
            
            if (state.selectedAccount?.id == state.targetAccount?.id) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        error = context.getString(R.string.err_accounts_different)
                    )
                }
                return
            }
        }

        if (amountError != null || merchantError != null || categoryError != null) {
            _transactionUiState.update { currentState ->
                currentState.copy(
                    amountError = amountError,
                    merchantError = merchantError,
                    categoryError = categoryError
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _transactionUiState.update { it.copy(isLoading = true) }

                val amount = BigDecimal(state.amount)

                val transactionId = addTransactionUseCase.execute(
                    amount = amount,
                    merchant = state.merchant.trim(),
                    category = state.category,
                    subcategory = state.subcategory,
                    type = state.transactionType,
                    date = state.date,
                    notes = state.notes.takeIf { it.isNotBlank() },
                    isRecurring = state.isRecurring,
                    bankName = state.selectedAccount?.bankName,
                    accountLast4 = state.selectedAccount?.accountLast4,
                    currency = state.currency,
                    sourceAccountId = state.selectedAccount?.id,
                    targetAccountBankName = state.targetAccount?.bankName,
                    targetAccountLast4 = state.targetAccount?.accountLast4,
                    attachments = attachmentService.joinAttachments(_transactionAttachments.value),
                    targetAmount = state.targetAmount
                )

                // Insert items
                if (state.items.isNotEmpty() && transactionId != -1L) {
                    transactionRepository.insertTransactionItems(state.items.map {
                        it.copy(id = 0, transactionId = transactionId)
                    })
                }

                onSuccess()
            } catch (e: Exception) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.err_save_transaction)
                    )
                }
            }
        }
    }

    fun updateTransactionAccount(
        account: AccountBalanceEntity?
    ) {
        _transactionUiState.update { currentState -> 
            currentState.copy(
                selectedAccount = account,
                currency = account?.currency ?: "INR"
            ) 
        }
        updateTransferRateCalculation()
    }

    fun updateTransactionTargetAccount(
        account: AccountBalanceEntity?
    ) {
        _transactionUiState.update { currentState -> currentState.copy(targetAccount = account) }
        updateTransferRateCalculation()
    }

    private fun updateTransferRateCalculation() {
        val state = _transactionUiState.value
        if (state.transactionType != TransactionType.TRANSFER || state.selectedAccount == null || state.targetAccount == null) {
            _transactionUiState.update { it.copy(transferExchangeRate = null, targetAmount = null, isLoadingTransferRate = false) }
            return
        }

        val fromCurrency = state.selectedAccount.currency
        val toCurrency = state.targetAccount.currency
        val amount = state.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO

        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            _transactionUiState.update {
                it.copy(
                    transferExchangeRate = BigDecimal.ONE,
                    targetAmount = amount,
                    isLoadingTransferRate = false
                )
            }
            return
        }

        _transactionUiState.update { it.copy(isLoadingTransferRate = true) }

        viewModelScope.launch {
            try {
                val rate = currencyConversionService.getExchangeRate(
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    date = state.date.toLocalDate()
                )
                val calculatedTargetAmount = if (rate != null) {
                    amount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP)
                } else amount

                _transactionUiState.update {
                    it.copy(
                        transferExchangeRate = rate,
                        targetAmount = calculatedTargetAmount,
                        isLoadingTransferRate = false
                    )
                }
            } catch (e: Exception) {
                _transactionUiState.update { it.copy(isLoadingTransferRate = false) }
            }
        }
    }

    fun addTransactionItem() {
        val currentItems = _transactionUiState.value.items
        val newItem = TransactionItemEntity(
            transactionId = 0,
            name = "",
            amount = BigDecimal.ZERO,
            category = _transactionUiState.value.category
        )
        _transactionUiState.update { it.copy(items = currentItems + newItem) }
    }

    fun updateTransactionItem(index: Int, updatedItem: TransactionItemEntity) {
        val currentItems = _transactionUiState.value.items.toMutableList()
        if (index in currentItems.indices) {
            currentItems[index] = updatedItem
            _transactionUiState.update { it.copy(items = currentItems) }
            recalculateTransactionTotalFromItems()
        }
    }

    fun removeTransactionItem(index: Int) {
        val currentItems = _transactionUiState.value.items.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _transactionUiState.update { it.copy(items = currentItems) }
            recalculateTransactionTotalFromItems()
        }
    }

    private fun recalculateTransactionTotalFromItems() {
        val items = _transactionUiState.value.items
        if (items.isNotEmpty()) {
            val total = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
            _transactionUiState.update { it.copy(amount = total.stripTrailingZeros().toPlainString()) }
        }
    }

    // Attachment management for transactions
    fun addTransactionAttachment(path: String) {
        _transactionAttachments.update { it + path }
    }

    fun removeTransactionAttachment(path: String) {
        attachmentService.deleteAttachment(path)
        _transactionAttachments.update { it - path }
    }

    // Attachment management for subscriptions
    fun addSubscriptionAttachment(path: String) {
        _subscriptionAttachments.update { it + path }
    }

    fun removeSubscriptionAttachment(path: String) {
        attachmentService.deleteAttachment(path)
        _subscriptionAttachments.update { it - path }
    }

    fun saveSubcategory(
        categoryId: Long,
        name: String,
        iconResId: Int,
        iconName: String,
        color: String
    ) {
        viewModelScope.launch {
            try {
                subcategoryRepository.createSubcategory(
                    categoryId = categoryId,
                    name = name,
                    iconResId = iconResId,
                    iconName = iconName,
                    color = color
                )
                // Refresh subcategories for the active categories in the UI
                val transactionCategory = _transactionUiState.value.category
                val transactionCatObj = categories.value.find { it.name == transactionCategory }
                if (transactionCatObj?.id == categoryId) {
                    updateTransactionSubcategories(transactionCategory)
                }

                val subscriptionCategory = _subscriptionUiState.value.category
                val subscriptionCatObj = categories.value.find { it.name == subscriptionCategory }
                if (subscriptionCatObj?.id == categoryId) {
                    updateSubscriptionSubcategories(subscriptionCategory)
                }
            } catch (e: Exception) {
                Log.e("AddViewModel", "Error saving subcategory", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // NLP Quick Add — Prefill
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Applies a [ParsedTransactionDraft] from the NLP parser to pre-fill
     * the transaction form. Bank name matching is fuzzy (case-insensitive contains).
     *
     * This is idempotent — calling it multiple times with the same draft produces
     * the same result. The user can still edit any field before saving.
     */
    fun prefillFromNlpDraft(
        draft: ParsedTransactionDraft
    ) {
        // Apply amount, merchant, type, notes, and date
        _transactionUiState.update { current ->
            current.copy(
                amount = draft.amount,
                amountError = validateAmount(draft.amount),
                merchant = draft.merchant,
                merchantError = validateMerchant(draft.merchant),
                transactionType = draft.type,
                notes = draft.notes,
                date = draft.date ?: current.date
            )
        }
        if (draft.date != null) {
            updateTransferRateCalculation()
        }

        // Match bank name, account last4, or Custom ID against user's accounts
        if (draft.bankName.isNotBlank()) {
            viewModelScope.launch {
                val availableAccounts = accounts.filter { it.isNotEmpty() }.first()
                val lowerBank = draft.bankName.lowercase()
                val matched = availableAccounts.firstOrNull { account ->
                    account.customId?.lowercase() == lowerBank ||
                    account.accountLast4.lowercase() == lowerBank ||
                    account.bankName.lowercase().contains(lowerBank) ||
                    lowerBank.contains(account.bankName.lowercase())
                }
                if (matched != null) {
                    updateTransactionAccount(matched)
                }
            }
        }

        // Match category & subcategory - imitating manual selection flow
        if (draft.category.isNotBlank()) {
            viewModelScope.launch {
                val availableCategories = categories.filter { it.isNotEmpty() }.first()
                val cat = findBestMatchingCategory(availableCategories, draft.category)
                if (cat != null) {
                    val subName = if (draft.subcategory.isNotBlank()) {
                        val subList = subcategoryRepository.getSubcategoriesByCategoryId(cat.id).first()
                        val matchedSub = findBestMatchingSubcategory(subList, draft.subcategory)
                        matchedSub?.name ?: draft.subcategory
                    } else null

                    updateTransactionCategory(cat.name, subName)
                }
            }
        }
    }

    private fun findBestMatchingCategory(
        categories: List<CategoryEntity>,
        rawName: String
    ): CategoryEntity? {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return null

        // 1. Exact case-insensitive match
        categories.find { it.name.equals(trimmed, ignoreCase = true) }?.let { return it }

        // 2. Normalized match
        val normRaw = normalizeForMatching(trimmed)
        categories.find { normalizeForMatching(it.name) == normRaw }?.let { return it }

        // 3. Contains match
        categories.find {
            val normCat = normalizeForMatching(it.name)
            normCat.contains(normRaw) || normRaw.contains(normCat)
        }?.let { return it }

        return null
    }

    private fun findBestMatchingSubcategory(
        subcategories: List<SubcategoryEntity>,
        rawName: String
    ): SubcategoryEntity? {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return null

        // 1. Exact case-insensitive match
        subcategories.find { it.name.equals(trimmed, ignoreCase = true) }?.let { return it }

        // 2. Normalized match
        val normRaw = normalizeForMatching(trimmed)
        subcategories.find { normalizeForMatching(it.name) == normRaw }?.let { return it }

        // 3. Contains match
        subcategories.find {
            val normSub = normalizeForMatching(it.name)
            normSub.contains(normRaw) || normRaw.contains(normSub)
        }?.let { return it }

        return null
    }

    private fun normalizeForMatching(text: String): String {
        return text.lowercase()
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]"), "")
    }

    // Subscription Edit Loading
    fun loadSubscriptionForEdit(id: Long) {
        viewModelScope.launch {
            try {
                _subscriptionUiState.update { it.copy(isLoading = true) }
                val subscription = subscriptionRepository.getSubscriptionById(id)
                if (subscription != null) {
                    val cycle = subscription.billingCycle ?: "Monthly"
                    val isCustom = cycle.startsWith("custom_")
                    var customCount = 1
                    var customUnit = "month"
                    var customEndDate: LocalDate? = null
                    var displayCycle = cycle

                    if (isCustom) {
                        val parts = cycle.split("_")
                        customCount = parts.getOrNull(1)?.toIntOrNull() ?: 1
                        customUnit = parts.getOrNull(2) ?: "month"
                        val endDateStr = parts.getOrNull(3)
                        if (endDateStr != null && endDateStr != "forever") {
                            customEndDate = try { LocalDate.parse(endDateStr) } catch (e: Exception) { null }
                        }
                        displayCycle = "Custom"
                    }

                    _subscriptionUiState.update { state ->
                        state.copy(
                            subscriptionId = subscription.id,
                            serviceName = subscription.merchantName,
                            amount = subscription.amount.toString(),
                            billingCycle = displayCycle,
                            isCustomCycle = isCustom,
                            customCycleCount = customCount,
                            customCycleUnit = customUnit,
                            customCycleEndDate = customEndDate,
                            nextPaymentDate = subscription.nextPaymentDate ?: LocalDate.now(),
                            category = subscription.category ?: "Subscription",
                            subcategory = subscription.subcategory,
                            currency = subscription.currency,
                            notes = subscription.smsBody ?: "",
                            isLoading = false
                        )
                    }
                    
                    // Pre-select account if possible
                    accounts.filter { it.isNotEmpty() }.first().let { availableAccounts ->
                        val matchedAccount = availableAccounts.find { 
                            it.bankName == subscription.bankName
                        }
                        if (matchedAccount != null) {
                            _subscriptionUiState.update { it.copy(selectedAccount = matchedAccount) }
                        }
                    }
                    
                    // Update subcategories for the loaded category
                    updateSubscriptionSubcategories(subscription.category ?: "Subscription")
                } else {
                    _subscriptionUiState.update { it.copy(isLoading = false, error = context.getString(R.string.err_subscription_not_found)) }
                }
            } catch (e: Exception) {
                _subscriptionUiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }


    // Subscription Tab Functions
    fun updateSubscriptionService(service: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(
                serviceName = service,
                serviceError = if (service.isBlank()) context.getString(R.string.err_service_required) else null
            )
        }
    }

    fun updateSubscriptionAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        val validAmount = if (decimalCount <= 1) filtered else _subscriptionUiState.value.amount

        _subscriptionUiState.update { currentState ->
            currentState.copy(amount = validAmount, amountError = validateAmount(validAmount))
        }
    }

    fun updateSubscriptionBillingCycle(cycle: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(
                billingCycle = cycle, 
                billingCycleError = null,
                isCustomCycle = cycle == "Custom"
            )
        }
    }

    fun updateSubscriptionCustomCycleCount(count: Int) {
        _subscriptionUiState.update { it.copy(customCycleCount = count) }
    }

    fun updateSubscriptionCustomCycleUnit(unit: String) {
        _subscriptionUiState.update { it.copy(customCycleUnit = unit) }
    }

    fun updateSubscriptionCustomCycleEndDate(date: LocalDate?) {
        _subscriptionUiState.update { it.copy(customCycleEndDate = date) }
    }

    fun updateSubscriptionNextPaymentDate(dateMillis: Long) {
        val instant = Instant.ofEpochMilli(dateMillis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()

        _subscriptionUiState.update { currentState ->
            currentState.copy(nextPaymentDate = localDate)
        }
    }

    fun updateSubscriptionCategory(category: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(
                category = category,
                subcategory = null,
                categoryError = validateCategory(category)
            )
        }

        updateSubscriptionSubcategories(category)
    }

    private fun updateSubscriptionSubcategories(category: String) {
        viewModelScope.launch {
            val cat = categories.value.find { it.name == category }
            if (cat != null) {
                subcategoryRepository.getSubcategoriesByCategoryId(cat.id).collect {
                    _subscriptionSubcategories.value = it
                }
            } else {
                _subscriptionSubcategories.value = emptyList()
            }
        }
    }

    fun updateSubscriptionSubcategory(subcategory: String?) {
        _subscriptionUiState.update { currentState -> currentState.copy(subcategory = subcategory) }
    }

    fun updateSubscriptionNotes(notes: String) {
        _subscriptionUiState.update { currentState -> currentState.copy(notes = notes) }
    }

    fun updateSubscriptionAccount(
        account: AccountBalanceEntity?
    ) {
        _subscriptionUiState.update { currentState -> 
            currentState.copy(
                selectedAccount = account,
                currency = account?.currency ?: "INR"
            ) 
        }
    }

    fun saveSubscription(onSuccess: () -> Unit) {
        val state = _subscriptionUiState.value
        Log.d("AddViewModel", "saveSubscription called with state: $state")

        // Validate all fields
        val serviceError = if (state.serviceName.isBlank()) context.getString(R.string.err_service_required) else null
        val amountError = validateAmount(state.amount)
        val categoryError = validateCategory(state.category)

        Log.d(
            "AddViewModel",
            "Validation - serviceError: $serviceError, amountError: $amountError, categoryError: $categoryError"
        )

        if (serviceError != null || amountError != null || categoryError != null) {
            _subscriptionUiState.update { currentState ->
                currentState.copy(
                    serviceError = serviceError,
                    amountError = amountError,
                    categoryError = categoryError
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AddViewModel", "Starting to save subscription...")
                _subscriptionUiState.update { it.copy(isLoading = true) }

                val amount = BigDecimal(state.amount)

                val billingCycleToSave = if (state.isCustomCycle) {
                    "custom_${state.customCycleCount}_${state.customCycleUnit.lowercase()}_${state.customCycleEndDate ?: "forever"}"
                } else {
                    state.billingCycle
                }

                if (state.subscriptionId != null) {
                    // Update existing subscription
                    val existingSubscription = subscriptionRepository.getSubscriptionById(state.subscriptionId)
                    if (existingSubscription != null) {
                        val updatedSubscription = existingSubscription.copy(
                            merchantName = state.serviceName.trim(),
                            amount = amount,
                            nextPaymentDate = state.nextPaymentDate,
                            billingCycle = billingCycleToSave,
                            category = state.category,
                            subcategory = state.subcategory,
                            bankName = state.selectedAccount?.bankName,
                            currency = state.currency,
                            smsBody = state.notes.takeIf { it.isNotBlank() },
                            updatedAt = java.time.LocalDateTime.now()
                        )
                        updateSubscriptionUseCase.execute(updatedSubscription)
                        Log.d("AddViewModel", "Subscription updated successfully: ${state.subscriptionId}")
                    } else {
                        throw Exception(context.getString(R.string.err_subscription_not_found))
                    }
                } else {
                    // Create new subscription
                    val transactionDate = state.nextPaymentDate.atTime(LocalTime.now())
                    
                    addTransactionUseCase.execute(
                        amount = amount,
                        merchant = state.serviceName.trim(),
                        category = state.category,
                        subcategory = state.subcategory,
                        type = TransactionType.EXPENSE, // Subscriptions are expenses
                        date = transactionDate,
                        notes = state.notes.takeIf { it.isNotBlank() },
                        isRecurring = true, // It is part of a subscription
                        bankName = state.selectedAccount?.bankName,
                        accountLast4 = state.selectedAccount?.accountLast4,
                        currency = state.currency,
                        sourceAccountId = state.selectedAccount?.id,
                        billingCycle = billingCycleToSave,
                        createSubscription = false
                    )

                    val actualNextPaymentDate = SubscriptionUtils.calculateNextPaymentDate(state.nextPaymentDate, billingCycleToSave)
                    Log.d("AddViewModel", "DEBUG_SUBSCRIPTION: fromDate=${state.nextPaymentDate}, billingCycle=$billingCycleToSave, today=${LocalDate.now()}, result=$actualNextPaymentDate")

                    val subscriptionId =
                        addSubscriptionUseCase.execute(
                            merchantName = state.serviceName.trim(),
                            amount = amount,
                            nextPaymentDate = actualNextPaymentDate,
                            billingCycle = billingCycleToSave,
                            category = state.category,
                            subcategory = state.subcategory,
                            bankName = state.selectedAccount?.bankName,
                            autoRenewal = false, // Not implemented yet
                            paymentReminder = false, // Not implemented yet
                            currency = state.currency,
                            notes = state.notes.takeIf { it.isNotBlank() },
                            lastPaidDate = state.nextPaymentDate
                        )

                    Log.d("AddViewModel", "Subscription saved successfully with ID: $subscriptionId")
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("AddViewModel", "Error saving subscription", e)
                e.printStackTrace()
                _subscriptionUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.err_save_subscription)
                    )
                }
            } finally {
                _subscriptionUiState.update { it.copy(isLoading = false) }
            }
        }
    }
    

    // Validation helpers
    private fun validateAmount(amount: String): String? {
        return when {
            amount.isBlank() -> context.getString(R.string.err_amount_required)
            amount.toDoubleOrNull() == null -> context.getString(R.string.err_invalid_amount)
            amount.toDouble() <= 0 -> context.getString(R.string.err_amount_positive)
            else -> null
        }
    }

    private fun validateMerchant(merchant: String): String? {
        return when {
            merchant.isBlank() -> context.getString(R.string.err_merchant_required)
            merchant.length < 2 -> context.getString(R.string.err_too_short)
            else -> null
        }
    }

    private fun validateCategory(category: String): String? {
        return when {
            category.isBlank() -> context.getString(R.string.err_category_required)
            else -> null
        }
    }
}

// UI State Classes
data class AddUiState(val currentTab: Int = 0)

data class TransactionUiState(
    val amount: String = "",
    val amountError: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val merchant: String = "",
    val merchantError: String? = null,
    val category: String = "Miscellaneous",
    val subcategory: String? = null,
    val categoryError: String? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val notes: String = "",
    val isRecurring: Boolean = false,
    val selectedAccount: AccountBalanceEntity? = null,
    val targetAccount: AccountBalanceEntity? = null,
    val currency: String = "INR",
    val transferExchangeRate: BigDecimal? = null,
    val targetAmount: BigDecimal? = null,
    val isLoadingTransferRate: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<TransactionItemEntity> = emptyList()
) {
    val isValid: Boolean
        get() =
            amount.isNotBlank() &&
                    amount.toDoubleOrNull() != null &&
                    amount.toDouble() > 0 &&
                    merchant.isNotBlank() &&
                    category.isNotBlank() &&
                    amountError == null &&
                    merchantError == null &&
                    categoryError == null
}

data class SubscriptionUiState(
    val subscriptionId: Long? = null,
    val serviceName: String = "",
    val serviceError: String? = null,
    val amount: String = "",
    val amountError: String? = null,
    val billingCycle: String = "Monthly",
    val billingCycleError: String? = null,
    val nextPaymentDate: LocalDate = LocalDate.now(), // Default to today as "First Payment Date"
    val category: String = "Subscription",
    val subcategory: String? = null,
    val categoryError: String? = null,
    val selectedAccount: AccountBalanceEntity? = null,
    val currency: String = "INR",
    val notes: String = "",
    val isCustomCycle: Boolean = false,
    val customCycleCount: Int = 1,
    val customCycleUnit: String = "month",
    val customCycleEndDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() =
            serviceName.isNotBlank() &&
                    amount.isNotBlank() &&
                    amount.toDoubleOrNull() != null &&
                    amount.toDouble() > 0 &&
                    billingCycle.isNotBlank() &&
                    category.isNotBlank() &&
                    serviceError == null &&
                    amountError == null &&
                    categoryError == null
}
