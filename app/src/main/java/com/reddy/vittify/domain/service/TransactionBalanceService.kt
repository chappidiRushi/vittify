package com.reddy.vittify.domain.service

import android.util.Log
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.domain.model.BalanceCalculator
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized service for all balance mutations tied to transactions.
 * Single entry point for applying, updating, or reverting balance effects on Bank Accounts.
 */
@Singleton
class TransactionBalanceService @Inject constructor(
    private val accountBalanceRepository: AccountBalanceRepository,
    private val currencyConversionService: CurrencyConversionService? = null
) {
    companion object {
        private const val TAG = "TransactionBalanceService"
    }

    /**
     * Resolves the primary/source account associated with a transaction.
     */
    suspend fun resolvePrimaryAccount(transaction: TransactionEntity): AccountBalanceEntity? {
        val id = transaction.accountId?.ifBlank { null } ?: transaction.fromAccountId?.ifBlank { null }
        if (id != null) {
            val account = accountBalanceRepository.getAccountById(id)
            if (account != null) return account
        }
        val last4 = transaction.fromAccount?.ifBlank { null }
        if (last4 != null) {
            return accountBalanceRepository.getAccountByLast4(last4)
        }
        return null
    }

    /**
     * Resolves the target/destination account associated with a transfer transaction.
     */
    suspend fun resolveTargetAccount(transaction: TransactionEntity): AccountBalanceEntity? {
        val id = transaction.toAccountId?.ifBlank { null }
        if (id != null) {
            val account = accountBalanceRepository.getAccountById(id)
            if (account != null) return account
        }
        val last4 = transaction.toAccount?.ifBlank { null }
        if (last4 != null) {
            return accountBalanceRepository.getAccountByLast4(last4)
        }
        return null
    }

    /**
     * Returns the transaction amount converted to the currency of the specified account.
     */
    private suspend fun getAmountForAccount(
        transaction: TransactionEntity,
        account: AccountBalanceEntity,
        isTargetAccount: Boolean = false
    ): BigDecimal {
        if (isTargetAccount && transaction.targetAmount != null) {
            return transaction.targetAmount
        }
        val txnCurrency = transaction.currency
        val accCurrency = account.currency
        if (txnCurrency.isBlank() || accCurrency.isBlank() || txnCurrency.equals(accCurrency, ignoreCase = true)) {
            return transaction.amount
        }
        return currencyConversionService?.convertAmount(
            amount = transaction.amount,
            fromCurrency = txnCurrency,
            toCurrency = accCurrency,
            date = transaction.dateTime.toLocalDate()
        ) ?: transaction.amount
    }

    /**
     * Calculates the signed balance effect of a transaction on a specific account.
     * Positive = balance increases (or debt increases for credit cards).
     * Negative = balance decreases (or debt decreases for credit cards).
     * Returns BigDecimal.ZERO if the account is not affected by this transaction.
     */
    private suspend fun calculateEffect(
        account: AccountBalanceEntity,
        transaction: TransactionEntity
    ): BigDecimal {
        if (transaction.transactionType == TransactionType.BALANCE_UPDATE || transaction.isDeleted) {
            return BigDecimal.ZERO
        }

        if (transaction.transactionType == TransactionType.TRANSFER) {
            val fromAcc = resolvePrimaryAccount(transaction)
            val toAcc = resolveTargetAccount(transaction)

            val isSource = (fromAcc?.id == account.id)
            val isDestination = (toAcc?.id == account.id)

            return when {
                isSource && isDestination -> BigDecimal.ZERO // Self-transfer has no net effect
                isSource -> {
                    val amount = getAmountForAccount(transaction, account, isTargetAccount = false)
                    BalanceCalculator.balanceEffect(
                        amount = amount,
                        transactionType = TransactionType.TRANSFER,
                        isCreditCard = account.isCreditCard,
                        direction = BalanceCalculator.TransferDirection.SOURCE
                    )
                }
                isDestination -> {
                    val amount = getAmountForAccount(transaction, account, isTargetAccount = true)
                    BalanceCalculator.balanceEffect(
                        amount = amount,
                        transactionType = TransactionType.TRANSFER,
                        isCreditCard = account.isCreditCard,
                        direction = BalanceCalculator.TransferDirection.DESTINATION
                    )
                }
                else -> BigDecimal.ZERO
            }
        } else {
            val primaryAcc = resolvePrimaryAccount(transaction)
            if (primaryAcc?.id == account.id) {
                val amount = getAmountForAccount(transaction, account, isTargetAccount = false)
                return BalanceCalculator.balanceEffect(
                    amount = amount,
                    transactionType = transaction.transactionType,
                    isCreditCard = account.isCreditCard,
                    direction = BalanceCalculator.TransferDirection.NONE
                )
            }
            return BigDecimal.ZERO
        }
    }

    /**
     * Applies balance effects for a newly inserted transaction.
     */
    suspend fun applyTransaction(
        transaction: TransactionEntity,
        transactionId: Long,
        explicitBalance: BigDecimal? = null,
        creditLimit: BigDecimal? = null,
        smsSource: String? = null
    ) {
        // Skip BALANCE_UPDATE type — these come through insertBalanceUpdate path
        if (transaction.transactionType == TransactionType.BALANCE_UPDATE || transaction.isDeleted) {
            return
        }

        val primaryAccount = resolvePrimaryAccount(transaction)

        if (primaryAccount == null && transaction.transactionType != TransactionType.TRANSFER) {
            Log.d(TAG, "No account associated with transaction $transactionId; skipping balance update")
            return
        }

        if (transaction.transactionType == TransactionType.TRANSFER) {
            applyTransfer(
                transaction = transaction,
                primaryAccount = primaryAccount,
                explicitBalance = explicitBalance
            )
        } else if (primaryAccount != null) {
            applyNonTransfer(
                transaction = transaction,
                account = primaryAccount,
                explicitBalance = explicitBalance
            )
        }
    }

    suspend fun revertTransaction(transactionId: Long) {
        // Balances are stored directly on AccountBalanceEntity
    }

    /**
     * Updates account balances when a transaction is edited.
     * Computes the net difference per affected account (new effect minus old effect)
     * and applies only the difference. If an account is changed from A to B:
     * - Account A's old effect is reverted (credited back for expenses).
     * - Account B's new effect is applied (debited for expenses).
     * If the amount is changed on the same account:
     * - If new amount > old amount, difference is debited (or debt increased for CC).
     * - If new amount < old amount, difference is credited (or debt reduced for CC).
     */
    suspend fun updateTransaction(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ) {
        if (oldTransaction.transactionType == TransactionType.BALANCE_UPDATE &&
            newTransaction.transactionType == TransactionType.BALANCE_UPDATE) {
            return
        }

        // Collect all unique accounts that could be affected by old or new transaction
        val accountsMap = mutableMapOf<String, AccountBalanceEntity>()

        suspend fun collectAccounts(txn: TransactionEntity) {
            resolvePrimaryAccount(txn)?.let { primary ->
                val fresh = accountBalanceRepository.getAccountById(primary.id) ?: primary
                accountsMap[fresh.id] = fresh
            }
            if (txn.transactionType == TransactionType.TRANSFER) {
                resolveTargetAccount(txn)?.let { target ->
                    val fresh = accountBalanceRepository.getAccountById(target.id) ?: target
                    accountsMap[fresh.id] = fresh
                }
            }
        }

        collectAccounts(oldTransaction)
        collectAccounts(newTransaction)

        for ((_, account) in accountsMap) {
            val oldEffect = calculateEffect(account, oldTransaction)
            val newEffect = calculateEffect(account, newTransaction)
            val delta = newEffect - oldEffect

            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                val updatedBalance = account.balance + delta
                accountBalanceRepository.updateBalance(account.copy(balance = updatedBalance))
            }
        }
    }

    suspend fun recalculateAffectedAccounts(transaction: TransactionEntity) {
        if (transaction.isDeleted) {
            revertTransactionBalance(transaction)
        } else {
            applyTransaction(transaction, transaction.id)
        }
    }

    private suspend fun revertTransactionBalance(transaction: TransactionEntity) {
        if (transaction.transactionType == TransactionType.BALANCE_UPDATE) {
            return
        }

        if (transaction.transactionType == TransactionType.TRANSFER) {
            val fromAcc = resolvePrimaryAccount(transaction)
            if (fromAcc != null) {
                val sourceAmount = getAmountForAccount(transaction, fromAcc, isTargetAccount = false)
                val effect = BalanceCalculator.balanceEffect(
                    amount = sourceAmount,
                    transactionType = TransactionType.TRANSFER,
                    isCreditCard = fromAcc.isCreditCard,
                    direction = BalanceCalculator.TransferDirection.SOURCE
                )
                accountBalanceRepository.updateBalance(fromAcc.copy(balance = fromAcc.balance - effect))
            }

            val toAcc = resolveTargetAccount(transaction)
            if (toAcc != null && toAcc.id != fromAcc?.id) {
                val targetAmount = getAmountForAccount(transaction, toAcc, isTargetAccount = true)
                val effect = BalanceCalculator.balanceEffect(
                    amount = targetAmount,
                    transactionType = TransactionType.TRANSFER,
                    isCreditCard = toAcc.isCreditCard,
                    direction = BalanceCalculator.TransferDirection.DESTINATION
                )
                accountBalanceRepository.updateBalance(toAcc.copy(balance = toAcc.balance - effect))
            }
        } else {
            val primaryAccount = resolvePrimaryAccount(transaction)
            if (primaryAccount != null) {
                val amount = getAmountForAccount(transaction, primaryAccount, isTargetAccount = false)
                val effect = BalanceCalculator.balanceEffect(
                    amount = amount,
                    transactionType = transaction.transactionType,
                    isCreditCard = primaryAccount.isCreditCard,
                    direction = BalanceCalculator.TransferDirection.NONE
                )
                accountBalanceRepository.updateBalance(primaryAccount.copy(balance = primaryAccount.balance - effect))
            }
        }
    }

    private suspend fun applyNonTransfer(
        transaction: TransactionEntity,
        account: AccountBalanceEntity,
        explicitBalance: BigDecimal?
    ) {
        if (explicitBalance != null) {
            accountBalanceRepository.updateBalance(account.copy(balance = explicitBalance))
            return
        }

        val amount = getAmountForAccount(transaction, account, isTargetAccount = false)
        val updatedBalance = BalanceCalculator.apply(
            currentBalance = account.balance,
            amount = amount,
            transactionType = transaction.transactionType,
            isCreditCard = account.isCreditCard,
            direction = BalanceCalculator.TransferDirection.NONE
        )

        accountBalanceRepository.updateBalance(account.copy(balance = updatedBalance))
    }

    private suspend fun applyTransfer(
        transaction: TransactionEntity,
        primaryAccount: AccountBalanceEntity?,
        explicitBalance: BigDecimal?
    ) {
        // 1. Source Account
        val fromAcc = primaryAccount ?: resolvePrimaryAccount(transaction)

        if (fromAcc != null) {
            val sourceAmount = getAmountForAccount(transaction, fromAcc, isTargetAccount = false)
            val sourceBalance = explicitBalance ?: BalanceCalculator.apply(
                currentBalance = fromAcc.balance,
                amount = sourceAmount,
                transactionType = TransactionType.TRANSFER,
                isCreditCard = fromAcc.isCreditCard,
                direction = BalanceCalculator.TransferDirection.SOURCE
            )
            accountBalanceRepository.updateBalance(fromAcc.copy(balance = sourceBalance))
        }

        // 2. Destination Account
        val toAcc = resolveTargetAccount(transaction)

        if (toAcc != null && toAcc.id != fromAcc?.id) {
            val targetAmount = getAmountForAccount(transaction, toAcc, isTargetAccount = true)
            val targetBalance = BalanceCalculator.apply(
                currentBalance = toAcc.balance,
                amount = targetAmount,
                transactionType = TransactionType.TRANSFER,
                isCreditCard = toAcc.isCreditCard,
                direction = BalanceCalculator.TransferDirection.DESTINATION
            )
            accountBalanceRepository.updateBalance(toAcc.copy(balance = targetBalance))
        }
    }
}
