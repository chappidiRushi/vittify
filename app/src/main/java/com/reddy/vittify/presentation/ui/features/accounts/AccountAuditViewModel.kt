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

/**
 * A single row in the bank-statement ledger view.
 *
 * @param rowNumber   1-based sequential row number
 * @param transaction The underlying transaction
 * @param paid        Debit amount (money going out). Null if this is a credit/income.
 * @param income      Credit amount (money coming in). Null if this is a debit/expense.
 * @param balance     Running account balance after this transaction
 */
data class LedgerRow(
    val rowNumber: Int,
    val transaction: TransactionEntity,
    val paid: BigDecimal?,
    val income: BigDecimal?,
    val balance: BigDecimal
)

data class DuplicateGroup(
    val transactions: List<TransactionEntity>,
    val amount: BigDecimal,
    val merchantName: String
)

data class AccountAuditUiState(
    val isLoading: Boolean = true,
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

    fun runAudit(bankName: String, accountLast4: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                bankName = bankName,
                accountLast4 = accountLast4
            )
        }

        viewModelScope.launch {
            try {
                // 1. Get the current (latest) balance record
                val latestBalance = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
                val currentBalance = latestBalance?.balance ?: BigDecimal.ZERO
                val currency = latestBalance?.currency ?: "INR"
                val isCreditCard = latestBalance?.isCreditCard ?: false
                val creditLimit = latestBalance?.creditLimit

                // 2. Get all transactions for this account
                val transactions = transactionRepository
                    .getTransactionsByAccount(bankName, accountLast4)
                    .first()
                    .filter { !it.isDeleted && it.transactionType != TransactionType.BALANCE_UPDATE }

                // 3. Compute totals
                val totalIncome = transactions
                    .filter { 
                        it.transactionType == TransactionType.INCOME || 
                        it.transactionType == TransactionType.CREDIT
                    }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

                val totalExpenses = transactions
                    .filter { 
                        it.transactionType == TransactionType.EXPENSE || 
                        it.transactionType == TransactionType.INVESTMENT
                    }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

                val totalTransfersIn = transactions
                    .filter {
                        it.transactionType == TransactionType.TRANSFER &&
                        BalanceCalculator.resolveTransferDirection(accountLast4, it.fromAccount, it.toAccount) == BalanceCalculator.TransferDirection.DESTINATION
                    }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

                val totalTransfersOut = transactions
                    .filter {
                        it.transactionType == TransactionType.TRANSFER &&
                        BalanceCalculator.resolveTransferDirection(accountLast4, it.fromAccount, it.toAccount) == BalanceCalculator.TransferDirection.SOURCE
                    }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

                // 4. Calculate initial and expected balance dynamically
                val chronologicalTransactions = transactions.sortedBy { it.dateTime }
                val auditBalances = BalanceCalculator.calculateAuditBalances(
                    transactions = chronologicalTransactions,
                    accountLast4 = accountLast4,
                    isCreditCard = isCreditCard,
                    currentBalance = currentBalance
                )
                val initialBalance = auditBalances.initialBalance
                val expectedBalance = auditBalances.expectedBalance

                val discrepancy = expectedBalance - currentBalance

                // 6. Find suspected duplicates (same amount, same merchant, within 5 minutes)
                val duplicateSuspects = findDuplicates(transactions)

                // 7. Build chronological ledger rows with running balance
                val ledgerRows = buildLedgerRows(
                    transactions = chronologicalTransactions,
                    initialBalance = initialBalance,
                    accountLast4 = accountLast4,
                    isCreditCard = isCreditCard
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currency = currency,
                        isCreditCard = isCreditCard,
                        creditLimit = creditLimit,
                        initialBalance = initialBalance,
                        currentBalance = currentBalance,
                        expectedBalance = expectedBalance,
                        discrepancy = discrepancy,
                        isHealthy = discrepancy.compareTo(BigDecimal.ZERO) == 0,
                        totalIncome = totalIncome,
                        totalExpenses = totalExpenses,
                        totalTransfersIn = totalTransfersIn,
                        totalTransfersOut = totalTransfersOut,
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
     * Builds chronologically ordered bank-statement ledger rows with a running balance.
     */
    private fun buildLedgerRows(
        transactions: List<TransactionEntity>,
        initialBalance: BigDecimal,
        accountLast4: String,
        isCreditCard: Boolean
    ): List<LedgerRow> {
        var runningBalance = initialBalance
        return transactions.mapIndexed { index, tx ->
            val direction = if (tx.transactionType == TransactionType.TRANSFER) {
                BalanceCalculator.resolveTransferDirection(
                    accountLast4,
                    tx.fromAccount,
                    tx.toAccount
                )
            } else {
                BalanceCalculator.TransferDirection.NONE
            }

            val effect = BalanceCalculator.balanceEffect(
                amount = tx.amount,
                transactionType = tx.transactionType,
                isCreditCard = isCreditCard,
                direction = direction
            )

            runningBalance = runningBalance + effect

            // Positive effect = balance increased = money came IN = Income column
            // Negative effect = balance decreased = money went OUT = Paid column
            // For credit cards: positive effect = debt increased = Paid (spent), negative = debt decreased = Income (repaid)
            val (paid, income) = if (isCreditCard) {
                when {
                    effect > BigDecimal.ZERO -> effect to null         // debt increased = Paid/Spent
                    effect < BigDecimal.ZERO -> null to effect.abs()   // debt decreased = Income/Repaid
                    else -> null to null
                }
            } else {
                when {
                    effect > BigDecimal.ZERO -> null to effect          // balance increased = Income
                    effect < BigDecimal.ZERO -> effect.abs() to null    // balance decreased = Paid
                    else -> null to null
                }
            }

            LedgerRow(
                rowNumber = index + 1,
                transaction = tx,
                paid = paid,
                income = income,
                balance = runningBalance
            )
        }
    }

    /**
     * Find transactions that look like duplicates: same amount, same merchant name,
     * within a 5-minute window, excluding those already marked as intentional.
     */
    private fun findDuplicates(transactions: List<TransactionEntity>): List<DuplicateGroup> {
        val nonDismissed = transactions.filter { !it.isDuplicateDismissed }
        val groups = mutableListOf<DuplicateGroup>()
        val seen = mutableSetOf<Long>()

        for (i in nonDismissed.indices) {
            if (nonDismissed[i].id in seen) continue

            val current = nonDismissed[i]
            val duplicates = mutableListOf(current)

            for (j in i + 1 until nonDismissed.size) {
                val other = nonDismissed[j]
                if (other.id in seen) continue

                val sameAmount = current.amount.compareTo(other.amount) == 0
                val sameMerchant = current.merchantName.equals(other.merchantName, ignoreCase = true)
                val sameType = current.transactionType == other.transactionType
                val withinWindow = kotlin.math.abs(
                    java.time.Duration.between(current.dateTime, other.dateTime).toMinutes()
                ) <= 5

                if (sameAmount && sameMerchant && sameType && withinWindow) {
                    duplicates.add(other)
                    seen.add(other.id)
                }
            }

            if (duplicates.size > 1) {
                seen.add(current.id)
                groups.add(
                    DuplicateGroup(
                        transactions = duplicates,
                        amount = current.amount,
                        merchantName = current.merchantName
                    )
                )
            }
        }

        return groups
    }

    /**
     * Creates a balance correction transaction to fix the discrepancy.
     */
    fun fixDiscrepancyWithCorrection() {
        val state = _uiState.value
        if (state.discrepancy.compareTo(BigDecimal.ZERO) == 0) return

        viewModelScope.launch {
            try {
                val latestBalance = accountBalanceRepository.getLatestBalance(state.bankName, state.accountLast4)
                if (latestBalance != null) {
                    accountBalanceRepository.updateBalanceById(latestBalance.id, state.expectedBalance)
                    _uiState.update { it.copy(successMessage = "Balance fixed successfully") }
                    // Re-run audit to reflect updated state
                    runAudit(state.bankName, state.accountLast4)
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
                runAudit(state.bankName, state.accountLast4)
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
                runAudit(state.bankName, state.accountLast4)
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
                runAudit(state.bankName, state.accountLast4)
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

                val latestBalance = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
                val isCreditCard = latestBalance?.isCreditCard ?: false
                val transactions = transactionRepository
                    .getTransactionsByAccount(bankName, accountLast4)
                    .first()
                    .filter { !it.isDeleted && it.transactionType != TransactionType.BALANCE_UPDATE }

                val chronologicalTransactions = transactions.sortedBy { it.dateTime }
                val auditBalances = BalanceCalculator.calculateAuditBalances(
                    transactions = chronologicalTransactions,
                    accountLast4 = accountLast4,
                    isCreditCard = isCreditCard,
                    currentBalance = latestBalance?.balance ?: BigDecimal.ZERO
                )
                val expectedBalance = auditBalances.expectedBalance

                if (latestBalance != null) {
                    accountBalanceRepository.updateBalanceById(latestBalance.id, expectedBalance)
                }

                _uiState.update { it.copy(successMessage = "Balances recalculated from scratch") }

                // Re-run audit
                runAudit(bankName, accountLast4)
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
