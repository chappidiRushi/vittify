package com.reddy.vittify.presentation.ui.features.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.domain.model.BalanceCalculator
import com.reddy.vittify.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

typealias LedgerRow = com.reddy.vittify.domain.model.LedgerRow
typealias DuplicateGroup = com.reddy.vittify.data.model.DuplicateTransactionGroup

data class AccountAuditUiState(
    val isLoading: Boolean = true,
    val accountId: String = "",
    val bankName: String = "",
    val accountLast4: String = "",
    val currency: String = "INR",
    val isCreditCard: Boolean = false,
    val creditLimit: BigDecimal? = null,
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    val expectedBalance: BigDecimal = BigDecimal.ZERO,
    val discrepancy: BigDecimal = BigDecimal.ZERO,
    val isHealthy: Boolean = true,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpenses: BigDecimal = BigDecimal.ZERO,
    val totalTransfersIn: BigDecimal = BigDecimal.ZERO,
    val totalTransfersOut: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0,
    val duplicateSuspects: List<DuplicateGroup> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    /** Chronological (oldest → newest) bank-statement rows with running balance. */
    val ledgerRows: List<LedgerRow> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AccountAuditViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountAuditUiState())
    val uiState: StateFlow<AccountAuditUiState> = _uiState.asStateFlow()

    fun runAudit(
        accountId: String? = null,
        bankName: String = "",
        accountLast4: String = ""
    ) {
        _uiState.update {
            it.copy(
                isLoading = true,
                accountId = accountId ?: it.accountId,
                bankName = bankName.ifBlank { it.bankName },
                accountLast4 = accountLast4.ifBlank { it.accountLast4 }
            )
        }

        viewModelScope.launch {
            try {
                // 1. Prioritize fetching by account unique ID; fallback to bankName + accountLast4
                val latestBalance = if (!accountId.isNullOrBlank()) {
                    accountBalanceRepository.getAccountById(accountId)
                } else {
                    val allAccounts = accountBalanceRepository.getAllLatestBalances().first()
                    allAccounts.firstOrNull {
                        it.bankName.equals(bankName, ignoreCase = true) &&
                        (accountLast4.isBlank() || it.accountLast4 == accountLast4)
                    } ?: accountBalanceRepository.getLatestBalance(bankName, accountLast4)
                }

                val resolvedAccountId = latestBalance?.id ?: accountId ?: ""
                val resolvedBankName = latestBalance?.bankName ?: bankName
                val resolvedLast4 = latestBalance?.accountLast4 ?: accountLast4

                val account = latestBalance ?: AccountBalanceEntity(
                    id = resolvedAccountId,
                    bankName = resolvedBankName,
                    accountLast4 = resolvedLast4,
                    balance = BigDecimal.ZERO
                )

                val currency = account.currency
                val isCreditCard = account.isCreditCard
                val creditLimit = account.creditLimit
                val currentBalance = account.balance

                // 2. Get all transactions for this account using unified single source of truth
                val transactions = transactionRepository.getTransactionsForAccount(
                    accountId = account.id,
                    bankName = account.bankName,
                    accountLast4 = account.accountLast4
                )

                // 3. Compute audit summary using unified BalanceCalculator single source of truth
                val audit = BalanceCalculator.computeAccountAudit(account, transactions)

                // 4. Find suspected duplicates using unified method
                val duplicateSuspects = BalanceCalculator.findDuplicateTransactions(transactions)

                // 5. Build chronological ledger rows with running balance
                val ledgerRows = BalanceCalculator.buildLedgerRows(
                    account = account,
                    chronologicalTransactions = audit.chronologicalTransactions,
                    initialBalance = audit.initialBalance
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        accountId = resolvedAccountId,
                        bankName = resolvedBankName,
                        accountLast4 = resolvedLast4,
                        currency = currency,
                        isCreditCard = isCreditCard,
                        creditLimit = creditLimit,
                        initialBalance = audit.initialBalance,
                        currentBalance = currentBalance,
                        expectedBalance = audit.expectedBalance,
                        discrepancy = audit.discrepancy,
                        isHealthy = audit.isHealthy,
                        totalIncome = audit.totalIncome,
                        totalExpenses = audit.totalExpenses,
                        totalTransfersIn = audit.totalTransfersIn,
                        totalTransfersOut = audit.totalTransfersOut,
                        transactionCount = transactions.size,
                        duplicateSuspects = duplicateSuspects,
                        allTransactions = transactions,
                        ledgerRows = ledgerRows
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Audit failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Creates a balance correction transaction to fix the discrepancy.
     */
    fun fixDiscrepancyWithCorrection() {
        val state = _uiState.value
        if (state.discrepancy.compareTo(BigDecimal.ZERO) == 0) return

        viewModelScope.launch {
            try {
                val latestBalance = if (state.accountId.isNotBlank()) {
                    accountBalanceRepository.getAccountById(state.accountId)
                } else {
                    val allAccounts = accountBalanceRepository.getAllLatestBalances().first()
                    allAccounts.firstOrNull {
                        it.bankName.equals(state.bankName, ignoreCase = true) &&
                        (state.accountLast4.isBlank() || it.accountLast4 == state.accountLast4)
                    } ?: accountBalanceRepository.getLatestBalance(state.bankName, state.accountLast4)
                }

                if (latestBalance != null) {
                    accountBalanceRepository.updateBalanceById(latestBalance.id, state.expectedBalance)
                    _uiState.update { it.copy(successMessage = "Balance fixed successfully") }
                    // Re-run audit to reflect updated state
                    runAudit(accountId = latestBalance.id, bankName = latestBalance.bankName, accountLast4 = latestBalance.accountLast4)
                } else {
                    _uiState.update { it.copy(errorMessage = "No balance record found to update") }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to fix balance: ${e.message}")
                }
            }
        }
    }

    /**
     * Deletes a suspected duplicate transaction.
     */
    fun deleteDuplicateTransaction(transactionId: Long) {
        deleteTransaction(transactionId, "Duplicate removed")
    }

    /**
     * Marks a suspected duplicate transaction as intentional (not duplicate).
     */
    fun markDuplicateDismissed(transactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.markDuplicateDismissed(transactionId, true)
                _uiState.update { it.copy(successMessage = "Marked as intentional") }

                val state = _uiState.value
                runAudit(accountId = state.accountId, bankName = state.bankName, accountLast4 = state.accountLast4)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update: ${e.message}")
                }
            }
        }
    }

    /**
     * Marks all transactions in a duplicate group as intentional (not duplicate).
     */
    fun markGroupAsNotDuplicate(group: DuplicateGroup) {
        viewModelScope.launch {
            try {
                val ids = group.transactions.map { it.id }
                transactionRepository.markDuplicatesDismissed(ids, true)
                _uiState.update { it.copy(successMessage = "All marked as intentional") }

                val state = _uiState.value
                runAudit(accountId = state.accountId, bankName = state.bankName, accountLast4 = state.accountLast4)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update: ${e.message}")
                }
            }
        }
    }

    /**
     * Deletes any transaction (like a bogus balance correction).
     */
    fun deleteTransaction(transactionId: Long, successMsg: String = "Transaction removed") {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransactionById(transactionId)
                _uiState.update { it.copy(successMessage = successMsg) }

                // Re-run audit
                val state = _uiState.value
                runAudit(accountId = state.accountId, bankName = state.bankName, accountLast4 = state.accountLast4)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete: ${e.message}")
                }
            }
        }
    }

    /**
     * Triggers a full balance recalculation from scratch using transaction history.
     */
    fun recalculateBalances() {
        val state = _uiState.value
        val bankName = state.bankName
        val accountLast4 = state.accountLast4

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val latestBalance = if (state.accountId.isNotBlank()) {
                    accountBalanceRepository.getAccountById(state.accountId)
                } else {
                    val allAccounts = accountBalanceRepository.getAllLatestBalances().first()
                    allAccounts.firstOrNull {
                        it.bankName.equals(bankName, ignoreCase = true) &&
                        (accountLast4.isBlank() || it.accountLast4 == accountLast4)
                    } ?: accountBalanceRepository.getLatestBalance(bankName, accountLast4)
                }

                if (latestBalance != null) {
                    val transactions = transactionRepository.getTransactionsForAccount(
                        accountId = latestBalance.id,
                        bankName = latestBalance.bankName,
                        accountLast4 = latestBalance.accountLast4
                    )
                    val audit = BalanceCalculator.computeAccountAudit(latestBalance, transactions)
                    accountBalanceRepository.updateBalanceById(latestBalance.id, audit.expectedBalance)
                }

                _uiState.update { it.copy(successMessage = "Balances recalculated from scratch") }

                // Re-run audit
                runAudit(accountId = latestBalance?.id, bankName = latestBalance?.bankName ?: bankName, accountLast4 = latestBalance?.accountLast4 ?: accountLast4)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Recalculation failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
