package com.reddy.vittify.presentation.ui.features.transactions

import com.reddy.vittify.utils.SubscriptionUtils

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.database.dao.RecentSubcategoryInfo
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.data.repository.CurrencyRepository
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.MerchantMappingRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.SubscriptionState
import com.reddy.vittify.data.service.AttachmentService
import com.reddy.vittify.domain.service.TransactionBalanceService
import com.reddy.vittify.core.Constants
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.TransactionItemEntity
import com.reddy.vittify.utils.CurrencyFormatter
import com.reddy.vittify.utils.DeviceEncryption
import com.reddy.vittify.utils.capitalizeFirst
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import android.net.Uri
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import com.reddy.vittify.data.nlp.ParsedTransactionDraft
import com.reddy.vittify.data.nlp.ReceiptTransactionParser
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val currencyRepository: CurrencyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val transactionBalanceService: TransactionBalanceService,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val receiptTransactionParser: ReceiptTransactionParser,
    val attachmentService: AttachmentService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    // Receipt scanning states
    private val _isScanningReceipt = MutableStateFlow(false)
    val isScanningReceipt: StateFlow<Boolean> = _isScanningReceipt.asStateFlow()

    private val _receiptScanError = MutableStateFlow<String?>(null)
    val receiptScanError: StateFlow<String?> = _receiptScanError.asStateFlow()

    private val _showAiNotConfiguredDialog = MutableStateFlow(false)
    val showAiNotConfiguredDialog: StateFlow<Boolean> = _showAiNotConfiguredDialog.asStateFlow()

    val directFieldEditing: StateFlow<Boolean> = userPreferencesRepository.directFieldEditing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                _uiState.update { it.copy(
                    isAmoledMode = prefs.isAmoledMode,
                    darkThemeConfig = prefs.isDarkThemeEnabled
                ) }
            }
        }
    }

    // Categories should be based on transaction type
    val categories: StateFlow<List<CategoryEntity>> = _uiState.map { state ->
        val transaction = state.editableTransaction ?: state.transaction
        val txType = transaction?.transactionType ?: TransactionType.EXPENSE
        CategoryType.fromTransactionType(txType)
    }.distinctUntilChanged()
    .flatMapLatest { catType ->
        if (catType != null) {
            categoryRepository.getCategoriesByType(catType)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Available accounts for linking (excluding hidden accounts and filtered by transaction owner)
    private val sharedPrefs =
        context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    val availableAccounts: StateFlow<List<AccountBalanceEntity>> = combine(
        accountBalanceRepository.getAllLatestBalances(),
        userPreferencesRepository.preserveAccountOrder,
        _uiState
    ) { balances, preserveOrder, state ->
        val hiddenAccounts =
            sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
        val transaction = state.editableTransaction ?: state.transaction
        val myDeviceId = p2pPreferences.getDeviceId()
        val isPartnerTxn = transaction != null && transaction.ownerId.isNotBlank() && transaction.ownerId != myDeviceId

        val filtered = balances
            .filter { balance ->
                val key = "${balance.bankName}_${balance.accountLast4}"
                !hiddenAccounts.contains(key)
            }
            .filter { balance ->
                if (isPartnerTxn) {
                    balance.ownerId.isNotBlank() && balance.ownerId != myDeviceId
                } else {
                    balance.ownerId.isBlank() || balance.ownerId == myDeviceId
                }
            }
            .distinctBy { "${it.bankName}_${it.accountLast4}" }

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

    val selectedAccount: StateFlow<AccountBalanceEntity?> = combine(
        _uiState,
        availableAccounts
    ) { state, accounts ->
        val transaction = state.editableTransaction ?: state.transaction
        if (transaction == null) return@combine null
        accounts.find {
            (transaction.accountId != null && it.id == transaction.accountId) ||
            (transaction.fromAccountId != null && it.id == transaction.fromAccountId) ||
            (transaction.fromAccount != null && it.accountLast4 == transaction.fromAccount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val targetAccount: StateFlow<AccountBalanceEntity?> = combine(
        _uiState,
        availableAccounts
    ) { state, accounts ->
        val transaction = state.editableTransaction ?: state.transaction
        if (transaction == null) return@combine null
        accounts.find { it.id == transaction.toAccountId || it.accountLast4 == transaction.toAccount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    val allSubcategories: StateFlow<Map<Long, List<SubcategoryEntity>>> =
        subcategoryRepository.subcategoriesMap

    private val recentlyUsedSubcategoriesFlow: Flow<List<RecentSubcategoryInfo>> =
        transactionRepository.getRecentlyUsedSubcategories(10)

    val recentSubcategories: StateFlow<List<Pair<CategoryEntity, SubcategoryEntity>>> =
        combine(
            categories,
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

    // Attachment state for edit mode
    private val _editableAttachments = MutableStateFlow<List<String>>(emptyList())
    val editableAttachments: StateFlow<List<String>> = _editableAttachments.asStateFlow()

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId)
            val items = transactionRepository.getItemsForTransactionSync(transactionId)
            _uiState.update { it.copy(
                transaction = transaction,
                transactionItems = items
            ) }
            val accountIconName = transaction?.accountId?.let { accId ->
                accountBalanceRepository.getAccountById(accId)?.iconName
            }
            _uiState.update { it.copy(accountIconName = accountIconName) }
            transaction?.let {
                determinePrimaryCurrency(it)
                calculateConvertedAmount(it)
                findLinkedSubscription(it)
            }
        }
    }

    private suspend fun findLinkedSubscription(transaction: TransactionEntity) {
        if (transaction.isRecurring) {
            val linked = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )
            _uiState.update { it.copy(subscription = linked) }
        } else {
            _uiState.update { it.copy(subscription = null) }
        }
    }

    private suspend fun determinePrimaryCurrency(transaction: TransactionEntity) {
        val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()
        _uiState.update { it.copy(primaryCurrency = effectiveCurrency) }
    }

    private suspend fun calculateConvertedAmount(transaction: TransactionEntity) {
        val primaryCurrency = _uiState.value.primaryCurrency
        if (transaction.currency.isNotEmpty() && !transaction.currency.equals(
                primaryCurrency,
                ignoreCase = true
            )
        ) {
            // Convert the amount to the primary currency
            val converted = currencyConversionService.convertAmount(
                amount = transaction.amount,
                fromCurrency = transaction.currency,
                toCurrency = primaryCurrency
            )
            _uiState.update { it.copy(convertedAmount = converted) }
        } else {
            // No conversion needed if currencies are the same
            _uiState.update { it.copy(convertedAmount = null) }
        }
    }

    fun enterEditMode() {
        val currentTransaction = _uiState.value.transaction
        val billingCycle = currentTransaction?.billingCycle
        
        var isCustom = false
        var count = 1
        var unit = "month"
        var endDate: LocalDate? = null

        if (billingCycle?.startsWith("custom_") == true) {
            isCustom = true
            val parts = billingCycle.split("_")
            count = parts.getOrNull(1)?.toIntOrNull() ?: 1
            unit = parts.getOrNull(2) ?: "month"
            val endDateStr = parts.getOrNull(3)
            if (endDateStr != null && endDateStr != "forever") {
                endDate = try { LocalDate.parse(endDateStr) } catch (e: Exception) { null }
            }
        }

        _uiState.update { state ->
            state.copy(
                editableTransaction = state.transaction?.copy(),
                isEditMode = true,
                errorMessage = null,
                isCustomCycle = isCustom,
                customCycleCount = count,
                customCycleUnit = unit,
                customCycleEndDate = endDate,
                editableItems = state.transactionItems.map { it.copy() }
            )
        }
        // Initialize editable attachments from transaction
        _uiState.value.transaction?.let { txn ->
            _editableAttachments.value = attachmentService.parseAttachments(txn.attachments)
        }

        // Load count of other transactions from same merchant (always, not just for SMS)
        _uiState.value.transaction?.let { txn ->
            viewModelScope.launch {
                val matches = transactionRepository.getTransactionsByMerchantContains(
                    txn.merchantName,
                    txn.id
                )
                _uiState.update { it.copy(
                    existingTransactionCount = matches.size,
                    matchedTransactions = matches,
                    selectedMatchIds = matches.map { m -> m.id }.toSet()
                ) }
            }
        }
    }

    fun exitEditMode() {
        _uiState.update {
            it.copy(
                editableTransaction = null,
                isEditMode = false,
                errorMessage = null,
                applyToAllFromMerchant = false,
                updateExistingTransactions = false,
                existingTransactionCount = 0,
                editableItems = emptyList()
            )
        }
        _editableAttachments.value = emptyList()
    }

    fun toggleApplyToAllFromMerchant() {
        _uiState.update { it.copy(applyToAllFromMerchant = !it.applyToAllFromMerchant) }
    }

    fun toggleUpdateExistingTransactions() {
        _uiState.update { it.copy(updateExistingTransactions = !it.updateExistingTransactions) }
    }

    fun showMatchPreviewSheet() {
        _uiState.update { it.copy(showMatchPreviewSheet = true) }
    }

    fun hideMatchPreviewSheet() {
        _uiState.update { it.copy(showMatchPreviewSheet = false) }
    }

    fun toggleMatchSelection(transactionId: Long) {
        val current = _uiState.value.selectedMatchIds
        val updated = if (current.contains(transactionId)) current - transactionId
                      else current + transactionId
        _uiState.update { it.copy(selectedMatchIds = updated) }
    }

    fun selectAllMatches() {
        val allIds = _uiState.value.matchedTransactions.map { it.id }.toSet()
        _uiState.update { it.copy(selectedMatchIds = allIds) }
    }

    fun deselectAllMatches() {
        _uiState.update { it.copy(selectedMatchIds = emptySet()) }
    }

    /** Adds a transaction found via the Transactions search screen to the match preview list. */
    fun addTransactionToMatchList(transaction: TransactionEntity) {
        val current = _uiState.value.matchedTransactions
        if (current.any { it.id == transaction.id }) return
        _uiState.update { state ->
            state.copy(
                matchedTransactions = current + transaction,
                selectedMatchIds = state.selectedMatchIds + transaction.id,
                matchSearchQuery = "",
                matchSearchResults = emptyList()
            )
        }
    }

    fun updateMatchSearchQuery(query: String) {
        _uiState.update { it.copy(matchSearchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(matchSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            try {
                val results = transactionRepository.searchTransactionsList(query)
                val existingIds = _uiState.value.matchedTransactions.map { it.id }.toSet()
                val currentId = _uiState.value.transaction?.id ?: -1L
                val filtered = results.filter { it.id !in existingIds && it.id != currentId }
                _uiState.update { it.copy(matchSearchResults = filtered) }
            } catch (e: Exception) {
                // Ignore search errors
            }
        }
    }

    /** Applies the current editable category to only the user-selected transactions. */
    fun applyToSelectedMatches() {
        val state = _uiState.value
        val category = state.editableTransaction?.category ?: return
        val ids = state.selectedMatchIds
        if (ids.isEmpty()) return

        viewModelScope.launch {
            try {
                val toUpdate = state.matchedTransactions
                    .filter { it.id in ids }
                    .map { it.copy(category = category) }
                toUpdate.forEach { transactionRepository.updateTransaction(it) }
                _uiState.update { it.copy(showMatchPreviewSheet = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update transactions: ${e.message}") }
            }
        }
    }

    fun updateMerchantName(name: String) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(merchantName = name)) }
        validateMerchantName(name)
    }

    fun updateAmount(amountStr: String) {
        val amount = amountStr.toBigDecimalOrNull()
        if (amount != null && amount > BigDecimal.ZERO) {
            _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(amount = amount), errorMessage = null) }
        } else if (amountStr.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = "Amount must be a positive number") }
        }
    }

    fun updateTransactionType(type: TransactionType) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(transactionType = type)) }

        // Auto-select category based on type
        val newCategory = when (type) {
            TransactionType.INCOME -> "Salary"
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Miscellaneous"
            TransactionType.CREDIT -> "Credit Bill"
            TransactionType.TRANSFER -> {
                val targetAccount = _uiState.value.editableTransaction?.toAccount
                if (targetAccount == "wallet") "Cash Withdrawal" else "Self Transfer"
            }

            TransactionType.INVESTMENT -> "Investment"
            TransactionType.BALANCE_UPDATE -> "Miscellaneous"
        }

        updateCategory(newCategory)
    }

    fun addAttachment(path: String) {
        _editableAttachments.update { it + path }
    }

    fun removeAttachment(path: String) {
        attachmentService.deleteAttachment(path)
        _editableAttachments.update { it - path }
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
            } catch (e: Exception) {
                Log.e("TransactionDetailVM", "Error saving subcategory", e)
            }
        }
    }

    fun updateCategory(category: String, subcategory: String? = null) {
        viewModelScope.launch {
            val useCatAsMerchant = userPreferencesRepository.useCategoryAsMerchant.first()
            _uiState.update { state ->
                val current = state.editableTransaction
                val effectiveSub = subcategory ?: if (current?.category != category) null else current?.subcategory
                val newMerchant = if (useCatAsMerchant) {
                    val preferred = effectiveSub?.takeIf { it.isNotBlank() } ?: category
                    if (preferred.isNotBlank()) preferred else (current?.merchantName ?: "")
                } else {
                    current?.merchantName ?: ""
                }
                state.copy(
                    editableTransaction = current?.copy(
                        category = category.ifEmpty { "Miscellaneous" },
                        subcategory = effectiveSub,
                        merchantName = newMerchant
                    )
                )
            }
        }
    }

    fun updateSubcategory(subcategory: String?) {
        viewModelScope.launch {
            val useCatAsMerchant = userPreferencesRepository.useCategoryAsMerchant.first()
            _uiState.update { state ->
                val current = state.editableTransaction
                val newMerchant = if (useCatAsMerchant && !subcategory.isNullOrBlank()) {
                    subcategory
                } else if (useCatAsMerchant && !current?.category.isNullOrBlank()) {
                    current!!.category
                } else {
                    current?.merchantName ?: ""
                }
                state.copy(
                    editableTransaction = current?.copy(
                        subcategory = subcategory,
                        merchantName = newMerchant
                    )
                )
            }
        }
    }

    fun updateDateTime(dateTime: LocalDateTime) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(dateTime = dateTime)) }
    }

    fun updateDescription(description: String?) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(description = if (description.isNullOrEmpty()) null else description)) }
    }

    fun updateRecurringStatus(isRecurring: Boolean) {
        _uiState.update { state ->
            val current = state.editableTransaction
            state.copy(
                editableTransaction = current?.copy(
                    isRecurring = isRecurring,
                    billingCycle = if (isRecurring) current.billingCycle ?: "Monthly" else null
                ),
                isCustomCycle = if (!isRecurring) false else state.isCustomCycle
            )
        }
    }

    fun updateBillingCycle(cycle: String) {
        _uiState.update { it.copy(
            editableTransaction = it.editableTransaction?.copy(billingCycle = cycle),
            isCustomCycle = cycle == "Custom"
        ) }
    }

    fun updateSubscriptionCustomCycleCount(count: Int) {
        _uiState.update { it.copy(customCycleCount = count) }
    }

    fun updateSubscriptionCustomCycleUnit(unit: String) {
        _uiState.update { it.copy(customCycleUnit = unit) }
    }

    fun updateSubscriptionCustomCycleEndDate(date: LocalDate?) {
        _uiState.update { it.copy(customCycleEndDate = date) }
    }

    fun updateAccountNumber(accountNumber: String?) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(fromAccount = if (accountNumber.isNullOrEmpty()) null else accountNumber)) }
    }

    fun updateTransactionAccount(account: AccountBalanceEntity?) {
        _uiState.update { state ->
            val current = state.editableTransaction
            state.copy(
                editableTransaction = current?.copy(
                    accountId = account?.id,
                    fromAccountId = account?.id,
                    fromAccount = account?.accountLast4,
                    currency = account?.currency ?: current.currency
                ),
                accountIconName = account?.iconName // Update the icon name in state too
            )
        }
    }

    fun updateTransactionTargetAccount(account: AccountBalanceEntity?) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(toAccount = account?.accountLast4, toAccountId = account?.id)) }

        // Update category if type is TRANSFER
        _uiState.value.editableTransaction?.let { txn ->
            if (txn.transactionType == TransactionType.TRANSFER) {
                val newCategory = if (account?.accountLast4 == "wallet") {
                    "Cash Withdrawal"
                } else {
                    "Self Transfer"
                }
                updateCategory(newCategory)
            }
        }
    }

    fun updateCurrency(currency: String) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(currency = currency)) }
        // Recalculate converted amount when currency changes
        _uiState.value.editableTransaction?.let { transaction ->
            viewModelScope.launch {
                calculateConvertedAmount(transaction)
            }
        }
    }

    fun addEditableItem() {
        val currentItems = _uiState.value.editableItems
        val transactionId = _uiState.value.transaction?.id ?: 0L
        val newItem = TransactionItemEntity(
            transactionId = transactionId,
            name = "",
            amount = BigDecimal.ZERO,
            category = _uiState.value.editableTransaction?.category ?: "Miscellaneous"
        )
        _uiState.update { it.copy(editableItems = currentItems + newItem) }
    }

    fun updateEditableItem(index: Int, updatedItem: TransactionItemEntity) {
        val currentItems = _uiState.value.editableItems.toMutableList()
        if (index in currentItems.indices) {
            currentItems[index] = updatedItem
            _uiState.update { it.copy(editableItems = currentItems) }
            recalculateTotalFromItems()
        }
    }

    fun removeEditableItem(index: Int) {
        val currentItems = _uiState.value.editableItems.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _uiState.update { it.copy(editableItems = currentItems) }
            recalculateTotalFromItems()
        }
    }

    private fun recalculateTotalFromItems() {
        val items = _uiState.value.editableItems
        if (items.isNotEmpty()) {
            val total = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
            _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(amount = total)) }
        }
    }

    fun saveChanges() {
        val state = _uiState.value
        val toSave = state.editableTransaction ?: return

        // Validate before saving
        if (toSave.merchantName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Merchant name is required") }
            return
        }

        if (toSave.amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Amount must be positive") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // Handle custom cycle serialization
                val finalBillingCycle = if (state.isCustomCycle) {
                    val endDateStr = state.customCycleEndDate?.toString() ?: "forever"
                    "custom_${state.customCycleCount}_${state.customCycleUnit}_$endDateStr"
                } else toSave.billingCycle

                val originalTransaction = state.transaction ?: transactionRepository.getTransactionById(toSave.id)

                // For transfer transactions, recalculate target amount if different currencies
                val finalTargetAmount = if (toSave.transactionType == TransactionType.TRANSFER) {
                    val toAcc = toSave.toAccountId?.let { accountBalanceRepository.getAccountById(it) }
                        ?: toSave.toAccount?.let { accountBalanceRepository.getAccountByLast4(it) }
                    if (toAcc != null && !toAcc.currency.equals(toSave.currency, ignoreCase = true)) {
                        currencyConversionService.convertAmount(
                            amount = toSave.amount,
                            fromCurrency = toSave.currency,
                            toCurrency = toAcc.currency,
                            date = toSave.dateTime.toLocalDate()
                        )
                    } else {
                        toSave.amount
                    }
                } else null

                // Normalize merchant name before saving
                // Also lazily assign a UUID if this is an old transaction that doesn't have one yet
                val stableUuid = if (toSave.uuid.isBlank()) UUID.randomUUID().toString() else toSave.uuid
                val normalizedTransaction = toSave.copy(
                    merchantName = normalizeMerchantName(toSave.merchantName),
                    billingCycle = finalBillingCycle,
                    targetAmount = finalTargetAmount,
                    updatedAt = LocalDateTime.now(),
                    uuid = stableUuid,
                    attachments = attachmentService.joinAttachments(_editableAttachments.value)
                )

                // Handle Balance Updates before saving to repository
                if (originalTransaction != null) {
                    transactionBalanceService.updateTransaction(originalTransaction, normalizedTransaction)
                }

                transactionRepository.updateTransaction(normalizedTransaction)

                // Update items
                transactionRepository.deleteItemsForTransaction(normalizedTransaction.id)
                if (state.editableItems.isNotEmpty()) {
                    transactionRepository.insertTransactionItems(state.editableItems.map { 
                        it.copy(id = 0, transactionId = normalizedTransaction.id) 
                    })
                }

                // Sync with subscriptions if recurring
                syncSubscriptionForTransaction(normalizedTransaction)

                // Save merchant mapping if checkbox is checked
                if (state.applyToAllFromMerchant) {
                    merchantMappingRepository.setMapping(
                        normalizedTransaction.merchantName,
                        normalizedTransaction.category
                    )
                }

                // Update existing transactions if checkbox is checked (fuzzy/contains match)
                if (state.updateExistingTransactions) {
                    transactionRepository.updateCategoryAndSubcategoryForMerchantContains(
                        normalizedTransaction.merchantName,
                        normalizedTransaction.category,
                        normalizedTransaction.subcategory
                    )
                }

                _uiState.update {
                    it.copy(
                        transaction = normalizedTransaction,
                        transactionItems = state.editableItems,
                        saveSuccess = true,
                        isEditMode = false,
                        editableTransaction = null,
                        errorMessage = null,
                        applyToAllFromMerchant = false,
                        updateExistingTransactions = false,
                        existingTransactionCount = 0,
                        matchedTransactions = emptyList(),
                        selectedMatchIds = emptySet(),
                        showMatchPreviewSheet = false
                    )
                }
                findLinkedSubscription(normalizedTransaction) // Refresh linked subscription
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save changes: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun cancelEdit() {
        exitEditMode()
    }

    fun quickUpdateCategory(category: String, subcategory: String?) {
        val currentTxn = _uiState.value.transaction ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val useCatAsMerchant = userPreferencesRepository.useCategoryAsMerchant.first()
                val newMerchant = if (useCatAsMerchant) {
                    val preferred = subcategory?.takeIf { it.isNotBlank() } ?: category
                    if (preferred.isNotBlank()) preferred else currentTxn.merchantName
                } else {
                    currentTxn.merchantName
                }
                val updated = currentTxn.copy(
                    category = category,
                    subcategory = subcategory,
                    merchantName = newMerchant,
                    uuid = if (currentTxn.uuid.isBlank()) UUID.randomUUID().toString() else currentTxn.uuid,
                    updatedAt = LocalDateTime.now()
                )
                transactionRepository.updateTransaction(updated)
                syncSubscriptionForTransaction(updated)
                _uiState.update { it.copy(transaction = updated) }
                findLinkedSubscription(updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update category") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun quickUpdateMerchant(merchant: String) {
        val currentTxn = _uiState.value.transaction ?: return
        if (merchant.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updated = currentTxn.copy(
                    merchantName = merchant.trim(),
                    uuid = if (currentTxn.uuid.isBlank()) UUID.randomUUID().toString() else currentTxn.uuid,
                    updatedAt = LocalDateTime.now()
                )
                transactionRepository.updateTransaction(updated)
                syncSubscriptionForTransaction(updated)
                _uiState.update { it.copy(transaction = updated) }
                findLinkedSubscription(updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update merchant") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun quickUpdateDate(date: LocalDate) {
        val currentTxn = _uiState.value.transaction ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val newDateTime = LocalDateTime.of(date, currentTxn.dateTime.toLocalTime())
                val updated = currentTxn.copy(
                    dateTime = newDateTime,
                    uuid = if (currentTxn.uuid.isBlank()) UUID.randomUUID().toString() else currentTxn.uuid,
                    updatedAt = LocalDateTime.now()
                )
                transactionRepository.updateTransaction(updated)
                syncSubscriptionForTransaction(updated)
                _uiState.update { it.copy(transaction = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update date") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun quickUpdateTime(hour: Int, minute: Int) {
        val currentTxn = _uiState.value.transaction ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val newDateTime = LocalDateTime.of(currentTxn.dateTime.toLocalDate(), LocalTime.of(hour, minute))
                val updated = currentTxn.copy(
                    dateTime = newDateTime,
                    uuid = if (currentTxn.uuid.isBlank()) UUID.randomUUID().toString() else currentTxn.uuid,
                    updatedAt = LocalDateTime.now()
                )
                transactionRepository.updateTransaction(updated)
                syncSubscriptionForTransaction(updated)
                _uiState.update { it.copy(transaction = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update time") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun quickUpdateType(type: TransactionType) {
        val currentTxn = _uiState.value.transaction ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updated = currentTxn.copy(
                    transactionType = type,
                    uuid = if (currentTxn.uuid.isBlank()) UUID.randomUUID().toString() else currentTxn.uuid,
                    updatedAt = LocalDateTime.now()
                )
                transactionBalanceService.updateTransaction(currentTxn, updated)
                transactionRepository.updateTransaction(updated)
                syncSubscriptionForTransaction(updated)
                _uiState.update { it.copy(transaction = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update transaction type") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun quickUpdateAccount(account: AccountBalanceEntity?) {
        val currentTxn = _uiState.value.transaction ?: return
        if (account == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updated = currentTxn.copy(
                    accountId = account.id,
                    fromAccountId = account.id,
                    fromAccount = account.accountLast4,
                    currency = account.currency,
                    uuid = if (currentTxn.uuid.isBlank()) UUID.randomUUID().toString() else currentTxn.uuid,
                    updatedAt = LocalDateTime.now()
                )
                transactionBalanceService.updateTransaction(currentTxn, updated)
                transactionRepository.updateTransaction(updated)
                syncSubscriptionForTransaction(updated)
                _uiState.update { it.copy(
                    transaction = updated,
                    accountIconName = account.iconName
                ) }
                findLinkedSubscription(updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update account") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun quickUpdateTargetAccount(account: AccountBalanceEntity?) {
        val currentTxn = _uiState.value.transaction ?: return
        if (account == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val targetAmount = if (!account.currency.equals(currentTxn.currency, ignoreCase = true)) {
                    currencyConversionService.convertAmount(
                        amount = currentTxn.amount,
                        fromCurrency = currentTxn.currency,
                        toCurrency = account.currency,
                        date = currentTxn.dateTime.toLocalDate()
                    )
                } else {
                    currentTxn.amount
                }
                val updated = currentTxn.copy(
                    toAccountId = account.id,
                    toAccount = account.accountLast4,
                    targetAmount = targetAmount,
                    uuid = if (currentTxn.uuid.isBlank()) UUID.randomUUID().toString() else currentTxn.uuid,
                    updatedAt = LocalDateTime.now()
                )
                transactionBalanceService.updateTransaction(currentTxn, updated)
                transactionRepository.updateTransaction(updated)
                syncSubscriptionForTransaction(updated)
                _uiState.update { it.copy(transaction = updated) }
                findLinkedSubscription(updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update target account") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    private fun validateMerchantName(name: String) {
        _uiState.update { it.copy(errorMessage = if (name.isBlank()) "Merchant name is required" else null) }
    }

    /**
     * Normalizes merchant name to consistent format.
     * Converts all-caps to proper case, preserves already mixed case.
     */
    private fun normalizeMerchantName(name: String): String {
        val trimmed = name.trim()

        // If it's all uppercase, convert to proper case
        return if (trimmed == trimmed.uppercase()) {
            trimmed.lowercase().split(" ").joinToString(" ") { word ->
                word.capitalizeFirst()
            }
        } else {
            // Already has mixed case, keep as is
            trimmed
        }
    }

    fun getReportUrl(): String {
        val txn = _uiState.value.transaction ?: return ""

        val smsBody = txn.smsBody ?: "Transaction: ${txn.merchantName} - ${txn.amount}"
        val sender = txn.smsSender ?: "Unknown Sender"
        val bank = txn.accountId?.let { id -> availableAccounts.value.find { it.id == id }?.bankName } ?: "Manual"

        val issueTitle = "[Parsing Issue] ${txn.merchantName} - ${txn.amount}"
        val issueBody = """
            ### Transaction Details
            - **Merchant:** ${txn.merchantName}
            - **Amount:** ${txn.amount} ${txn.currency}
            - **Type:** ${txn.transactionType}
            - **Bank:** $bank
            - **Sender:** $sender
            
            ### Original SMS
            ```
            $smsBody
            ```
            
            ### Expected Behavior
            _Describe what was wrong (e.g., wrong category, wrong date, etc.)_
        """.trimIndent()

        val encodedTitle = URLEncoder.encode(issueTitle, "UTF-8")
        val encodedBody = URLEncoder.encode(issueBody, "UTF-8")

        return "https://github.com/chappidiRushi/Vittify/issues/new?title=$encodedTitle&body=$encodedBody"
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            _uiState.value.transaction?.let { txn ->
                _uiState.update { it.copy(isDeleting = true, showDeleteDialog = false) }

                try {
                    transactionRepository.deleteTransaction(txn)
                    _uiState.update { it.copy(deleteSuccess = true) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Failed to delete transaction") }
                } finally {
                    _uiState.update { it.copy(isDeleting = false) }
                }
            }
        }
    }

    fun duplicateTransaction() {
        viewModelScope.launch {
            _uiState.value.transaction?.let { txn ->
                try {
                    val duplicate = txn.copy(
                        id = 0,
                        uuid = UUID.randomUUID().toString(),
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                    transactionRepository.insertTransaction(duplicate)
                    _uiState.update { it.copy(duplicateSuccess = true) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Failed to duplicate transaction") }
                }
            }
        }
    }

    fun clearDuplicateSuccess() {
        _uiState.update { it.copy(duplicateSuccess = false) }
    }

    private suspend fun syncSubscriptionForTransaction(transaction: TransactionEntity) {
        val bankName = transaction.accountId?.let { id -> accountBalanceRepository.getAccountById(id)?.bankName } ?: "Manual"
        if (transaction.isRecurring) {
            val existing = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )

            val nextPaymentDate = SubscriptionUtils.calculateNextPaymentDate(
                (transaction.dateTime ?: LocalDateTime.now()).toLocalDate(),
                transaction.billingCycle
            )

            val subscription = existing?.copy(
                amount = transaction.amount,
                nextPaymentDate = nextPaymentDate,
                category = transaction.category,
                subcategory = transaction.subcategory,
                bankName = bankName,
                currency = transaction.currency,
                billingCycle = transaction.billingCycle,
                state = SubscriptionState.ACTIVE,
                updatedAt = LocalDateTime.now()
            )
                ?: SubscriptionEntity(
                    merchantName = transaction.merchantName,
                    amount = transaction.amount,
                    nextPaymentDate = nextPaymentDate,
                    state = SubscriptionState.ACTIVE,
                    bankName = bankName,
                    category = transaction.category,
                    subcategory = transaction.subcategory,
                    currency = transaction.currency,
                    billingCycle = transaction.billingCycle,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            subscriptionRepository.insertSubscription(subscription)
        } else {
            // Find existing matching subscription and hide it
            val existing = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )
            if (existing != null) {
                subscriptionRepository.hideSubscription(existing.id)
            }
        }
    }




    private suspend fun findAccountByLast4(last4: String): AccountBalanceEntity? {
        return accountBalanceRepository.getAllLatestBalances().first()
            .find { it.accountLast4 == last4 }
    }

    /**
     * Checks AI configuration before initiating receipt scanning.
     */
    fun onScanReceiptClicked(onLaunchScanner: () -> Unit) {
        viewModelScope.launch {
            if (receiptTransactionParser.isAiConfigured()) {
                onLaunchScanner()
            } else {
                _showAiNotConfiguredDialog.value = true
            }
        }
    }

    /**
     * Analyzes a scanned receipt image with Gemini AI and auto-fills transaction fields & items in edit mode.
     */
    fun processScannedReceipt(uri: Uri) {
        viewModelScope.launch {
            _isScanningReceipt.value = true
            _receiptScanError.value = null
            try {
                val currentId = _uiState.value.editableTransaction?.id ?: _uiState.value.transaction?.id ?: 0L
                val savedPath = attachmentService.saveAttachment(uri, currentId)
                val savedFile = savedPath?.let { java.io.File(context.filesDir, it) }
                val parseResult = receiptTransactionParser.parseReceipt(uri, savedFile)
                parseResult.fold(
                    onSuccess = { draft ->
                        applyReceiptDraft(draft, savedPath)
                        _isScanningReceipt.value = false
                    },
                    onFailure = { error ->
                        Log.e("TransactionDetailVM", "Receipt scan failed", error)
                        if (savedPath != null) {
                            addAttachment(savedPath)
                        }
                        _receiptScanError.value = error.message ?: "Failed to read receipt"
                        _isScanningReceipt.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e("TransactionDetailVM", "Unexpected error scanning receipt", e)
                _receiptScanError.value = e.message ?: "Failed to read receipt"
                _isScanningReceipt.value = false
            }
        }
    }

    fun applyReceiptDraft(draft: ParsedTransactionDraft, attachmentPath: String? = null) {
        val currentTxn = _uiState.value.editableTransaction ?: _uiState.value.transaction ?: return
        val currentId = currentTxn.id

        if (!_uiState.value.isEditMode) {
            enterEditMode()
        }

        val parsedAmount = draft.amount.toBigDecimalOrNull()
        val parsedDate = draft.date ?: currentTxn.dateTime

        _uiState.update { state ->
            val cur = state.editableTransaction ?: currentTxn
            state.copy(
                editableTransaction = cur.copy(
                    amount = parsedAmount ?: cur.amount,
                    merchantName = draft.merchant.ifBlank { cur.merchantName },
                    dateTime = parsedDate,
                    transactionType = draft.type,
                    description = if (draft.notes.isNotBlank()) draft.notes else cur.description
                )
            )
        }

        // Match bank name / account
        if (draft.bankName.isNotBlank()) {
            viewModelScope.launch {
                val accounts = availableAccounts.filter { it.isNotEmpty() }.first()
                val lowerBank = draft.bankName.lowercase()
                val matched = accounts.firstOrNull { account ->
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

        // Match category & subcategory
        if (draft.category.isNotBlank()) {
            viewModelScope.launch {
                val availableCats = categoryRepository.getAllCategories().first()
                val cat = availableCats.firstOrNull {
                    it.name.equals(draft.category, ignoreCase = true) ||
                    it.name.contains(draft.category, ignoreCase = true)
                }
                if (cat != null) {
                    val subName = if (draft.subcategory.isNotBlank()) {
                        val subList = subcategoryRepository.getSubcategoriesByCategoryId(cat.id).first()
                        subList.firstOrNull {
                            it.name.equals(draft.subcategory, ignoreCase = true) ||
                            it.name.contains(draft.subcategory, ignoreCase = true)
                        }?.name ?: draft.subcategory
                    } else null

                    updateCategory(cat.name, subName)
                }
            }
        }

        // Populate items
        if (draft.items.isNotEmpty()) {
            viewModelScope.launch {
                val availableCats = categoryRepository.getAllCategories().first()
                val convertedItems = draft.items.map { itemDraft ->
                    val cat = availableCats.firstOrNull {
                        it.name.equals(itemDraft.category, ignoreCase = true) ||
                        it.name.contains(itemDraft.category, ignoreCase = true)
                    }
                    val catName = cat?.name ?: itemDraft.category.ifBlank { "Miscellaneous" }
                    val subName = if (cat != null && itemDraft.subcategory.isNotBlank()) {
                        val subList = subcategoryRepository.getSubcategoriesByCategoryId(cat.id).first()
                        subList.firstOrNull {
                            it.name.equals(itemDraft.subcategory, ignoreCase = true) ||
                            it.name.contains(itemDraft.subcategory, ignoreCase = true)
                        }?.name ?: itemDraft.subcategory
                    } else itemDraft.subcategory.ifBlank { null }

                    TransactionItemEntity(
                        transactionId = currentId,
                        name = itemDraft.name,
                        amount = itemDraft.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        category = catName,
                        subcategory = subName
                    )
                }
                _uiState.update { it.copy(editableItems = convertedItems) }
            }
        }

        if (attachmentPath != null) {
            addAttachment(attachmentPath)
        }
    }

    fun clearReceiptScanError() {
        _receiptScanError.value = null
    }

    fun dismissAiNotConfiguredDialog() {
        _showAiNotConfiguredDialog.value = false
    }
}
