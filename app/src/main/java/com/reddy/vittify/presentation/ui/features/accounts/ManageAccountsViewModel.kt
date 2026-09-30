package com.reddy.vittify.presentation.ui.features.accounts

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.CardType
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CardRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.domain.usecase.AddTransactionUseCase
import com.reddy.vittify.data.database.entity.TransactionType as DBTransactionType
import com.reddy.vittify.utils.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.reddy.vittify.R
import com.reddy.vittify.utils.IconResolutionUtils
import androidx.core.content.edit

import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.ViewMode
import com.reddy.vittify.data.sync.model.filterByViewMode
import com.reddy.vittify.data.sync.model.coupleViewState

data class ManageAccountsUiState(
    val accounts: List<AccountBalanceEntity> = emptyList(),
    val hiddenAccounts: Set<String> = emptySet(),
    val balanceHistory: List<AccountBalanceEntity> = emptyList(),
    val linkedCards: Map<String, List<CardEntity>> = emptyMap(),
    val orphanedCards: List<CardEntity> = emptyList(),
    val duplicateAccountGroups: Map<String, List<AccountBalanceEntity>> = emptyMap(),
    val badAccounts: List<AccountBalanceEntity> = emptyList(),
    val issueCount: Int = 0,
    val isLoading: Boolean = false,
    val mainAccountKey: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isCoupleTrackingEnabled: Boolean = false,
    val activeViewMode: ViewMode = ViewMode.PERSONAL,
    val partnerName: String? = null,
    val myDeviceId: String = "",
    val isDeletingAccount: Boolean = false
)

data class AccountFormState(
    val bankName: String = "",
    val accountLast4: String = "",
    val balance: String = "",
    val creditLimit: String = "",
    val accountType: AccountType = AccountType.SAVINGS,
    val iconResId: Int = 0,
    val iconName: String = "",
    val currency: String = "INR",
    val customId: String = "",
    val isValid: Boolean = false,
    val errorMessage: String? = null
)

enum class AccountType {
    SAVINGS,
    CURRENT,
    CREDIT,
    WALLET
}

@HiltViewModel
class ManageAccountsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val p2pPreferences: P2pSyncPreferencesRepository
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ManageAccountsUiState())
    val uiState: StateFlow<ManageAccountsUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(AccountFormState())
    val formState: StateFlow<AccountFormState> = _formState.asStateFlow()

    val defaultCurrencyForNewAccounts: StateFlow<String> = combine(
        userPreferencesRepository.defaultCurrencyEnabled,
        userPreferencesRepository.defaultCurrencyCode
    ) { enabled, code ->
        if (enabled && !code.isNullOrBlank()) code else "INR"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    init {
        loadAccounts()
        loadHiddenAccounts()
        loadMainAccount()
        loadCards()
        initializeDefaultWallet()
        initFormCurrency()
    }

    private fun initFormCurrency() {
        viewModelScope.launch {
            userPreferencesRepository.defaultCurrencyEnabled.first().let { enabled ->
                if (enabled) {
                    userPreferencesRepository.defaultCurrencyCode.first()?.let { code ->
                        if (code.isNotBlank()) {
                            _formState.update { it.copy(currency = code) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun resolveDefaultCurrency(): String {
        if (userPreferencesRepository.defaultCurrencyEnabled.first()) {
            userPreferencesRepository.defaultCurrencyCode.first()?.let { code ->
                if (code.isNotBlank()) return code
            }
        }
        return "INR"
    }

    private fun initializeDefaultWallet() {
        viewModelScope.launch {
            val wallet = accountBalanceRepository.getLatestBalance("Cash", "wallet")
            if (wallet == null) {
                val baseCurrency = userPreferencesRepository.userPreferences.first().baseCurrency
                accountBalanceRepository.insertBalance(
                    AccountBalanceEntity(
                        bankName = "Cash",
                        accountLast4 = "wallet",
                        balance = BigDecimal.ZERO,
                        timestamp = LocalDateTime.now(),
                        isWallet = true,
                        iconResId = R.drawable.type_finance_dollar_banknote,
                        iconName = "type_finance_dollar_banknote",
                        color = "#4CAF50",
                        currency = baseCurrency
                    )
                )
            }
        }
    }

    fun setViewMode(mode: ViewMode) {
        p2pPreferences.setActiveViewMode(mode)
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            combine(
                accountBalanceRepository.getAllLatestBalances(),
                p2pPreferences.coupleViewState
            ) { accounts, coupleState ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val filteredAccounts = accounts.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { it.ownerId }
                _uiState.update { currentState ->
                    val newState = currentState.copy(
                        accounts = filteredAccounts,
                        isCoupleTrackingEnabled = coupleState.coupleEnabled,
                        activeViewMode = coupleState.viewMode,
                        partnerName = coupleState.partnerName,
                        myDeviceId = myDeviceId
                    )
                    computeDiagnostics(newState)
                }
            }.collectLatest { }
        }
    }

    private fun loadCards() {
        viewModelScope.launch {
            combine(
                cardRepository.getAllCards(),
                p2pPreferences.coupleViewState
            ) { allCards, coupleState ->
                val myDeviceId = p2pPreferences.getDeviceId()
                val filteredCards = allCards.filterByViewMode(coupleState.viewMode, coupleState.coupleEnabled, myDeviceId, coupleState.partnerDeviceId) { it.ownerId }
                // Group cards by linked account
                val linkedCardsMap = mutableMapOf<String, MutableList<CardEntity>>()
                val orphaned = mutableListOf<CardEntity>()

                for (card in filteredCards) {
                    when {
                        card.cardType == CardType.DEBIT && card.accountLast4 != null -> {
                            linkedCardsMap.getOrPut(card.accountLast4) { mutableListOf() }.add(card)
                        }
                        card.cardType == CardType.DEBIT && card.accountLast4 == null -> {
                            orphaned.add(card)
                        }
                    // Credit cards are not orphaned, they're standalone
                    }
                }

                _uiState.update { currentState ->
                    val newState = currentState.copy(linkedCards = linkedCardsMap, orphanedCards = orphaned)
                    computeDiagnostics(newState)
                }
            }.collectLatest { }
        }
    }

    private fun loadHiddenAccounts() {
        val hidden = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
        _uiState.update { currentState ->
            val newState = currentState.copy(hiddenAccounts = hidden)
            computeDiagnostics(newState)
        }
    }

    private fun computeDiagnostics(state: ManageAccountsUiState): ManageAccountsUiState {
        val accounts = state.accounts
        val hidden = state.hiddenAccounts

        // 1. Detect duplicate accounts by last4
        // 1. Detect duplicate accounts by last4 (excluding wallets)
        val duplicates = accounts
            .filter { !it.isWallet && !it.accountLast4.equals("wallet", ignoreCase = true) }
            .groupBy { it.accountLast4.trim().lowercase() }
            .filter { (last4, list) -> last4.isNotBlank() && list.size > 1 }

        // 2. Detect bad / malformed accounts
        // 2. Detect bad / malformed accounts (excluding wallets)
        val badAccounts = accounts.filter { account ->
            if (account.isWallet || account.accountLast4.equals("wallet", ignoreCase = true)) {
                return@filter false
            }
            val last4 = account.accountLast4.trim()
            val invalidLast4 = last4.length != 4 || !last4.all { it.isDigit() }
            val missingBank = account.bankName.isBlank() || account.bankName.equals("Unknown", ignoreCase = true)
            invalidLast4 || missingBank
        }

        // 3. Count total diagnostic issues
        val duplicateCount = duplicates.values.sumOf { it.size }
        val badCount = badAccounts.size
        val hiddenCount = hidden.size
        val orphanedCount = state.orphanedCards.size
        val totalIssues = duplicateCount + badCount + hiddenCount + orphanedCount

        val updatedState = state.copy(
            duplicateAccountGroups = duplicates,
            badAccounts = badAccounts,
            issueCount = totalIssues
        )

        _uiState.value = updatedState
        return updatedState
    }

    fun unhideAllAccounts() {
        sharedPrefs.edit().remove("hidden_accounts").apply()
        _uiState.update { currentState ->
            val newState = currentState.copy(
                hiddenAccounts = emptySet(),
                successMessage = "All hidden accounts restored"
            )
            computeDiagnostics(newState)
        }
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(successMessage = null) }
        }
    }

    private fun loadMainAccount() {
        val main = sharedPrefs.getString("main_account", null)
        _uiState.update { it.copy(mainAccountKey = main) }
    }

    fun setAsMainAccount(bankName: String, accountLast4: String) {
        val key = "${bankName}_${accountLast4}"
        sharedPrefs.edit { putString("main_account", key) }
        _uiState.update { it.copy(mainAccountKey = key, successMessage = "Main account set successfully") }
        viewModelScope.launch {
            // Persist this account's currency as the app-wide base currency
            val account = _uiState.value.accounts.find {
                it.bankName == bankName && it.accountLast4 == accountLast4
            }
            if (account != null) {
                userPreferencesRepository.updateBaseCurrency(account.currency)
                
                // Also update the in-built Cash wallet to match the main account's currency
                val cashWallet = accountBalanceRepository.getLatestBalance("Cash", "wallet")
                if (cashWallet != null && cashWallet.currency != account.currency) {
                    accountBalanceRepository.insertBalance(
                        cashWallet.copy(
                            currency = account.currency,
                            timestamp = LocalDateTime.now()
                        )
                    )
                }
            }
            delay(3000)
            _uiState.update { it.copy(successMessage = null) }
        }
    }

    fun updateBankName(name: String) {
        _formState.update {
            it.copy(bankName = name, isValid = validateForm(name, it.accountLast4, it.balance))
        }
    }

    fun updateAccountLast4(last4: String) {
        // Only allow 4 characters
        if (last4.length <= 4) {
            val duplicate = if (last4.length == 4) {
                _uiState.value.accounts.find {
                    it.accountLast4.trim().equals(last4.trim(), ignoreCase = true)
                }
            } else null

            val errorMsg = duplicate?.let {
                "Account number ending in '$last4' already exists (${it.bankName})"
            }

            _formState.update {
                it.copy(
                    accountLast4 = last4,
                    errorMessage = errorMsg,
                    isValid = validateForm(it.bankName, last4, it.balance) && duplicate == null
                )
            }
        }
    }

    fun updateBalance(balance: String) {
        // Only allow valid numeric input
        if (balance.isEmpty() || balance.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update {
                it.copy(
                    balance = balance,
                    isValid = validateForm(it.bankName, it.accountLast4, balance) && it.errorMessage == null
                )
            }
        }
    }

    fun updateCreditLimit(limit: String) {
        // Only allow valid numeric input
        if (limit.isEmpty() || limit.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(creditLimit = limit) }
        }
    }

    fun updateAccountType(type: AccountType) {
        _formState.update { it.copy(accountType = type) }
    }

    fun updateIcon(iconName: String) {
        val iconResId = IconResolutionUtils.nameToResId(context, iconName)
        _formState.update { it.copy(iconResId = iconResId, iconName = iconName) }
    }

    fun updateCurrency(currency: String) {
        _formState.update { it.copy(currency = currency) }
    }

    fun updateCustomId(customId: String) {
        _formState.update { it.copy(customId = customId) }
    }

    private fun validateForm(bankName: String, last4: String, balance: String): Boolean {
        return bankName.isNotBlank() &&
                last4.length == 4 &&
                balance.isNotBlank() &&
                balance.toDoubleOrNull() != null
    }

    fun addAccount(
        bankName: String,
        balance: BigDecimal,
        accountLast4: String,
        iconResId: Int,
        iconName: String,
        colorHex: String,
        isCreditCard: Boolean = false,
        isWallet: Boolean = false,
        creditLimit: BigDecimal? = null,
        currency: String = "INR",
        customId: String? = null
    ) {
        viewModelScope.launch {
            // Check for duplicates by bank+last4 as well as last4 across all banks
            val existingAccount = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
                ?: accountBalanceRepository.getAccountByLast4(accountLast4)

            if (existingAccount != null) {
                _uiState.update {
                    it.copy(
                        errorMessage = "An account with number ending in '$accountLast4' already exists (${existingAccount.bankName})"
                    )
                }
                return@launch
            }

            // 1. Create the account with ZERO initial balance record
            accountBalanceRepository.insertBalance(
                AccountBalanceEntity(
                    bankName = bankName,
                    accountLast4 = accountLast4,
                    balance = BigDecimal.ZERO,
                    creditLimit = creditLimit,
                    timestamp = LocalDateTime.now().minusSeconds(1), // Slightly earlier than the transaction
                    isCreditCard = isCreditCard,
                    isWallet = isWallet,
                    iconResId = iconResId,
                    iconName = iconName,
                    currency = currency,
                    color = colorHex,
                    customId = customId
                )
            )

            // 2. If initial balance is not zero, create an accountable transaction
            if (balance > BigDecimal.ZERO) {
                addTransactionUseCase.execute(
                    amount = balance,
                    merchant = "Initial Balance",
                    category = "Balance Correction",
                    type = if (isCreditCard) DBTransactionType.EXPENSE else DBTransactionType.INCOME,
                    date = LocalDateTime.now(),
                    bankName = bankName,
                    accountLast4 = accountLast4,
                    currency = currency,
                    createSubscription = false
                )
            }

            _uiState.update { it.copy(successMessage = "Account added successfully") }
            delay(3000)
            _uiState.update { it.copy(successMessage = null) }
        }
    }

    fun addAccount() {
        val state = _formState.value
        if (!state.isValid) return

        val creditLimit =
                if (state.accountType == AccountType.CREDIT && state.creditLimit.isNotBlank()) {
                    BigDecimal(state.creditLimit)
                } else null

        addAccount(
            bankName = state.bankName,
            balance = BigDecimal(state.balance),
            accountLast4 = state.accountLast4,
            iconResId = state.iconResId,
            iconName = state.iconName,
            colorHex = "#33B5E5", // Default or handle color
            isCreditCard = (state.accountType == AccountType.CREDIT),
            isWallet = (state.accountType == AccountType.WALLET),
            creditLimit = creditLimit,
            currency = state.currency,
            customId = state.customId.takeIf { it.isNotBlank() }
        )

        // Clear form
        viewModelScope.launch {
            _formState.value = AccountFormState(currency = resolveDefaultCurrency())
        }
    }

    fun updateAccountBalance(bankName: String, accountLast4: String, newBalance: BigDecimal) {
        viewModelScope.launch {
            val latestBalance = accountBalanceRepository.getLatestBalance(bankName, accountLast4) ?: return@launch
            val currentBalance = latestBalance.balance
            val delta = newBalance - currentBalance

            if (delta == BigDecimal.ZERO) return@launch

            val type = if (delta > BigDecimal.ZERO) DBTransactionType.INCOME else DBTransactionType.EXPENSE
            
            addTransactionUseCase.execute(
                amount = delta.abs(),
                merchant = "Balance Correction",
                category = "Balance Correction",
                type = type,
                date = LocalDateTime.now(),
                bankName = bankName,
                accountLast4 = accountLast4,
                currency = latestBalance.currency,
                createSubscription = false
            )
        }
    }

    fun updateCreditCard(
            bankName: String,
            accountLast4: String,
            newBalance: BigDecimal,
            newLimit: BigDecimal
    ) {
        viewModelScope.launch {
            val latestBalance = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
            val currentBalance = latestBalance?.balance ?: BigDecimal.ZERO
            val delta = newBalance - currentBalance

            // For Credit Cards, increasing outstanding (positive delta) is an EXPENSE
            // Decreasing outstanding (negative delta) is an INCOME

            if (delta != BigDecimal.ZERO) {
                val type = if (delta > BigDecimal.ZERO) DBTransactionType.EXPENSE else DBTransactionType.INCOME
                
                addTransactionUseCase.execute(
                    amount = delta.abs(),
                    merchant = "Balance Correction",
                    category = "Balance Correction",
                    type = type,
                    date = LocalDateTime.now(),
                    bankName = bankName,
                    accountLast4 = accountLast4,
                    currency = latestBalance?.currency ?: "INR",
                    createSubscription = false
                )
            }
            
            // If credit limit changed, we still need to update the balance record for the limit
            if (newLimit != latestBalance?.creditLimit) {
                val currentLatest = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
                accountBalanceRepository.insertBalance(
                    (currentLatest ?: AccountBalanceEntity(
                        bankName = bankName,
                        accountLast4 = accountLast4,
                        balance = newBalance,
                        timestamp = LocalDateTime.now(),
                        isCreditCard = true
                    )).copy(creditLimit = newLimit, timestamp = LocalDateTime.now())
                )
            }
        }
    }

    fun toggleAccountVisibility(bankName: String, accountLast4: String) {
        val key = "${bankName}_${accountLast4}"
        val hidden = _uiState.value.hiddenAccounts.toMutableSet()

        if (hidden.contains(key)) {
            hidden.remove(key)
        } else {
            hidden.add(key)
        }

        // Save to SharedPreferences
        sharedPrefs.edit().putStringSet("hidden_accounts", hidden).apply()

        // Update UI state
        _uiState.update { it.copy(hiddenAccounts = hidden) }
    }

    fun isAccountHidden(bankName: String, accountLast4: String): Boolean {
        val key = "${bankName}_${accountLast4}"
        return _uiState.value.hiddenAccounts.contains(key)
    }

    fun clearError() {
        _formState.update { it.copy(errorMessage = null) }
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadBalanceHistory(bankName: String, accountLast4: String) {
        viewModelScope.launch {
            val history =
                    accountBalanceRepository.getBalanceHistoryForAccount(bankName, accountLast4)
            _uiState.update { it.copy(balanceHistory = history) }
        }
    }

    fun deleteBalanceRecord(id: String, bankName: String, accountLast4: String) {
        viewModelScope.launch {
            // Check if this is the only record
            val count = accountBalanceRepository.getBalanceCountForAccount(bankName, accountLast4)
            if (count > 1) {
                accountBalanceRepository.deleteBalanceById(id)
                // Reload history and accounts
                loadBalanceHistory(bankName, accountLast4)
                loadAccounts()
            }
        }
    }

    fun updateBalanceRecord(
            id: String,
            newBalance: BigDecimal,
            bankName: String,
            accountLast4: String
    ) {
        viewModelScope.launch {
            accountBalanceRepository.updateBalanceById(id, newBalance)
            // Reload history and accounts
            loadBalanceHistory(bankName, accountLast4)
            loadAccounts()
        }
    }

    fun clearBalanceHistory() {
        _uiState.update { it.copy(balanceHistory = emptyList()) }
    }

    fun linkCardToAccount(cardId: Long, accountLast4: String) {
        viewModelScope.launch {
            try {
                Log.d(
                        "ManageAccountsViewModel",
                        "Starting to link card $cardId to account $accountLast4"
                )

                // Get card info first to know if there's a balance to copy
                val card = cardRepository.getCardById(cardId)
                val hasBalance = card?.lastBalance != null

                // Link the card to the account
                cardRepository.linkCardToAccount(cardId, accountLast4)

                // If card had a balance, copy it to the account
                if (card != null && hasBalance) {
                    try {
                        val insertedId =
                                accountBalanceRepository.insertBalanceUpdate(
                                        bankName = card.bankName,
                                        accountLast4 = accountLast4,
                                        balance = card.lastBalance!!,
                                        timestamp = card.lastBalanceDate ?: LocalDateTime.now(),
                                        smsSource = card.lastBalanceSource
                                )
                        Log.d(
                                "ManageAccountsViewModel",
                                "Balance copied to account. Insert ID: $insertedId"
                        )

                        // Show success message with balance
                        val message =
                                "Card linked successfully. Balance updated to ${CurrencyFormatter.formatCurrency(card.lastBalance)}"
                        _uiState.update { it.copy(successMessage = message) }
                    } catch (e: Exception) {
                        Log.e(
                                "ManageAccountsViewModel",
                                "Failed to copy balance: ${e.message}",
                                e
                        )
                        // Still show success for linking, but note the balance issue
                        _uiState.update {
                            it.copy(
                                    successMessage =
                                            "Card linked successfully (balance update failed)"
                            )
                        }
                    }
                } else {
                    // No balance to copy, just show link success
                    _uiState.update { it.copy(successMessage = "Card linked successfully") }
                }

                // Clear message after delay
                delay(3000)
                _uiState.update { it.copy(successMessage = null) }

                loadCards()
                loadAccounts()
            } catch (e: Exception) {
                Log.e("ManageAccountsViewModel", "Failed to link card", e)
                _uiState.update { it.copy(errorMessage = "Failed to link card: ${e.message}") }
            }
        }
    }

    fun unlinkCard(cardId: Long) {
        viewModelScope.launch {
            cardRepository.unlinkCard(cardId)
            loadCards()
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            try {
                Log.d("ManageAccountsViewModel", "Deleting card with ID: $cardId")
                cardRepository.deleteCard(cardId)
                _uiState.update { it.copy(successMessage = "Card deleted successfully") }

                // Clear message after delay
                delay(2000)
                _uiState.update { it.copy(successMessage = null) }

                loadCards()
            } catch (e: Exception) {
               Log.e("ManageAccountsViewModel", "Failed to delete card", e)
                _uiState.update { it.copy(errorMessage = "Failed to delete card: ${e.message}") }
            }
        }
    }

    fun setCardActive(cardId: Long, isActive: Boolean) {
        viewModelScope.launch {
            cardRepository.setCardActive(cardId, isActive)
            loadCards()
        }
    }

    fun deleteAccount(bankName: String, accountLast4: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            try {
                // Unlink any cards linked to this account
                val linkedCards = _uiState.value.linkedCards[accountLast4] ?: emptyList()
                linkedCards.forEach { card -> cardRepository.unlinkCard(card.id) }

                // Clean up transactions: convert transfers and delete account transactions
                val history = accountBalanceRepository.getBalanceHistoryForAccount(bankName, accountLast4)
                transactionRepository.handleAccountDeletion(bankName, accountLast4, history.map { it.id })

                // Delete all balance records for this account
                val deletedCount = accountBalanceRepository.deleteAccount(bankName, accountLast4)

                // Remove from hidden accounts if present
                val key = "${bankName}_${accountLast4}"
                val hidden = _uiState.value.hiddenAccounts.toMutableSet()
                hidden.remove(key)
                sharedPrefs.edit().putStringSet("hidden_accounts", hidden).apply()

                // Remove from main account if present
                if (_uiState.value.mainAccountKey == key) {
                    sharedPrefs.edit().remove("main_account").apply()
                }

                _uiState.update {
                    it.copy(
                            hiddenAccounts = hidden,
                            mainAccountKey = if (it.mainAccountKey == key) null else it.mainAccountKey,
                            successMessage =
                                    "Account deleted successfully ($deletedCount balance records removed)"
                    )
                }

                // Clear message after delay
                delay(3000)
                _uiState.update { it.copy(successMessage = null) }

                loadCards() // Reload cards to update UI
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete account: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isDeletingAccount = false) }
            }
        }
    }

    fun editAccount(
            oldBankName: String,
            accountLast4: String,
            newBankName: String,
            newBalance: BigDecimal,
            newCreditLimit: BigDecimal?,
            isCreditCard: Boolean,
            isWallet: Boolean,
            newIconResId: Int,
            newIconName: String,
            newColorHex: String,
            newCurrency: String = "INR",
            newCustomId: String? = null
    ) {
        viewModelScope.launch {
            try {
                // Update custom ID first (affects all history)
                accountBalanceRepository.updateAccountCustomId(oldBankName, accountLast4, newCustomId)

                val latestBalance = accountBalanceRepository.getLatestBalance(oldBankName, accountLast4)
                val currentBalance = latestBalance?.balance ?: BigDecimal.ZERO

                // Update bank name if changed
                if (newBankName != oldBankName) {
                    accountBalanceRepository.updateAccountBankName(
                        oldBankName,
                        accountLast4,
                        newBankName
                    )

                    // Update hidden accounts preference if bank name changed
                    val oldKey = "${oldBankName}_${accountLast4}"
                    val newKey = "${newBankName}_${accountLast4}"
                    val hidden = _uiState.value.hiddenAccounts.toMutableSet()
                    if (hidden.contains(oldKey)) {
                        hidden.remove(oldKey)
                        hidden.add(newKey)
                        sharedPrefs.edit { putStringSet("hidden_accounts", hidden) }
                        _uiState.update { it.copy(hiddenAccounts = hidden) }
                    }

                    // Update main account preference if bank name changed
                    if (_uiState.value.mainAccountKey == oldKey) {
                        sharedPrefs.edit { putString("main_account", newKey) }
                        _uiState.update { it.copy(mainAccountKey = newKey) }
                    }
                }

                // If balance changed, create correction transaction
                val delta = newBalance - currentBalance
                if (delta != BigDecimal.ZERO) {
                    val type = if (isCreditCard) {
                        if (delta > BigDecimal.ZERO) DBTransactionType.EXPENSE else DBTransactionType.INCOME
                    } else {
                        if (delta > BigDecimal.ZERO) DBTransactionType.INCOME else DBTransactionType.EXPENSE
                    }

                    addTransactionUseCase.execute(
                        amount = delta.abs(),
                        merchant = "Balance Correction",
                        category = "Balance Correction",
                        type = type,
                        date = LocalDateTime.now(),
                        bankName = newBankName,
                        accountLast4 = accountLast4,
                        currency = newCurrency,
                        createSubscription = false
                    )
                }

                // Update other properties (icon, color, limit, etc.) via a new balance record
                val currentLatest = accountBalanceRepository.getLatestBalance(newBankName, accountLast4)
                accountBalanceRepository.insertBalance(
                    (currentLatest ?: AccountBalanceEntity(
                        bankName = newBankName,
                        accountLast4 = accountLast4,
                        balance = newBalance,
                        timestamp = LocalDateTime.now()
                    )).copy(
                        balance = newBalance, // always use the new balance, not the stale cached one
                        creditLimit = newCreditLimit,
                        isCreditCard = isCreditCard,
                        isWallet = isWallet,
                        iconResId = newIconResId,
                        iconName = newIconName,
                        color = newColorHex,
                        currency = newCurrency,
                        timestamp = LocalDateTime.now()
                    )
                )

                _uiState.update { it.copy(successMessage = "Account updated successfully") }

                delay(3000)
                _uiState.update { it.copy(successMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update account: ${e.message}") }
            }
        }
    }

    fun mergeAccounts(targetAccount: AccountBalanceEntity, sourceAccounts: List<AccountBalanceEntity>, newBalance: BigDecimal?
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                //Reassign transactions
                sourceAccounts.forEach { source ->
                    transactionRepository.updateAccountIdForTransactions(
                        oldAccountId = source.id.toString(),
                        newAccountId = targetAccount.id.toString()
                    )
                }

                // Update target balance if requested
                if (newBalance != null) {
                    val currentLatest = accountBalanceRepository.getLatestBalance(targetAccount.bankName, targetAccount.accountLast4)
                    val currentBalance = currentLatest?.balance ?: BigDecimal.ZERO
                    val delta = newBalance - currentBalance

                    if (delta != BigDecimal.ZERO) {
                        val type = if (targetAccount.isCreditCard) {
                            if (delta > BigDecimal.ZERO) DBTransactionType.EXPENSE else DBTransactionType.INCOME
                        } else {
                            if (delta > BigDecimal.ZERO) DBTransactionType.INCOME else DBTransactionType.EXPENSE
                        }

                        addTransactionUseCase.execute(
                            amount = delta.abs(),
                            merchant = "Merge Correction",
                            category = "Balance Correction",
                            type = type,
                            date = LocalDateTime.now(),
                            bankName = targetAccount.bankName,
                            accountLast4 = targetAccount.accountLast4,
                            currency = targetAccount.currency,
                            createSubscription = false
                        )
                    }
                }

                // Delete source accounts
                sourceAccounts.forEach { source ->
                    // Unlink cards
                    val linkedCards = _uiState.value.linkedCards[source.accountLast4] ?: emptyList()
                    linkedCards.forEach { card -> cardRepository.unlinkCard(card.id) }

                    // Delete account balances
                    accountBalanceRepository.deleteAccount(source.bankName, source.accountLast4)
                }

                // Reload data
                loadAccounts()
                loadCards()

                _uiState.update {
                    it.copy(isLoading = false, successMessage = "Accounts merged successfully")
                }
                delay(3000)
                _uiState.update { it.copy(successMessage = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to merge accounts: ${e.message}"
                    )
                }
            }
        }
    }

    fun settleCreditCardBill(
        creditCard: AccountBalanceEntity,
        fromAccount: AccountBalanceEntity,
        amount: BigDecimal
    ) {
        viewModelScope.launch {
            try {
                addTransactionUseCase.execute(
                    amount = amount,
                    merchant = "Credit Card Settlement",
                    category = "Credit Card Payment",
                    type = DBTransactionType.TRANSFER,
                    date = LocalDateTime.now(),
                    bankName = fromAccount.bankName,
                    accountLast4 = fromAccount.accountLast4,
                    currency = creditCard.currency,
                    targetAccountBankName = creditCard.bankName,
                    targetAccountLast4 = creditCard.accountLast4,
                    createSubscription = false
                )

                _uiState.update {
                    it.copy(successMessage = "Credit card bill settled successfully")
                }
                delay(3000)
                _uiState.update { it.copy(successMessage = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to settle credit card bill: ${e.message}")
                }
            }
        }
    }
}

