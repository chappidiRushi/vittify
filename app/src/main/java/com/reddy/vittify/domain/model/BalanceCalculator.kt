package com.reddy.vittify.domain.model

import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.model.DuplicateTransactionGroup
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

data class LedgerRow(
    val rowNumber: Int,
    val transaction: TransactionEntity,
    val paid: BigDecimal?,
    val income: BigDecimal?,
    val balance: BigDecimal
)

data class AccountAuditSummary(
    val initialBalance: BigDecimal,
    val expectedBalance: BigDecimal,
    val discrepancy: BigDecimal,
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val totalTransfersIn: BigDecimal,
    val totalTransfersOut: BigDecimal,
    val isHealthy: Boolean,
    val chronologicalTransactions: List<TransactionEntity>
)

/**
 * Single source of truth for balance calculations.
 *
 * This object contains pure functions that determine how a transaction affects
 * an account balance. All balance math in the app should flow through here.
 *
 * Design principles:
 * - No dependencies — pure math only
 * - Explicit direction for transfers (no fuzzy matching)
 * - Credit card balance = outstanding debt (positive = owed)
 */
object BalanceCalculator {

    /**
     * Direction of money flow for a transfer transaction relative to an account.
     */
    enum class TransferDirection {
        /** Money is leaving this account (balance decreases for savings, debt decreases for CC). */
        SOURCE,
        /** Money is entering this account (balance increases for savings, debt increases for CC). */
        DESTINATION,
        /** Not a transfer, or direction cannot be determined — no balance change. */
        NONE
    }

    /**
     * Calculates the new balance after applying a transaction.
     *
     * For regular accounts (savings/current/wallet):
     * - INCOME / CREDIT → balance increases
     * - EXPENSE / INVESTMENT → balance decreases
     * - TRANSFER as SOURCE → balance decreases
     * - TRANSFER as DESTINATION → balance increases
     *
     * For credit cards (balance = outstanding debt):
     * - EXPENSE / INVESTMENT → debt increases (spent more)
     * - INCOME / CREDIT → debt decreases (repayment)
     * - TRANSFER as DESTINATION → debt decreases (receiving a payment towards bill)
     * - TRANSFER as SOURCE → debt increases (cash advance or similar)
     *
     * @param currentBalance The account's balance before this transaction.
     * @param amount The transaction amount (always positive).
     * @param transactionType The type of transaction.
     * @param isCreditCard Whether this is a credit card account (balance = outstanding debt).
     * @param direction For TRANSFER transactions, the direction relative to this account.
     *                  Ignored for non-TRANSFER types.
     * @return The new balance after applying the transaction.
     */
    fun apply(
        currentBalance: BigDecimal,
        amount: BigDecimal,
        transactionType: TransactionType,
        isCreditCard: Boolean,
        direction: TransferDirection = TransferDirection.NONE
    ): BigDecimal {
        if (transactionType == TransactionType.BALANCE_UPDATE) {
            return currentBalance
        }

        if (transactionType == TransactionType.TRANSFER) {
            return applyTransfer(currentBalance, amount, isCreditCard, direction)
        }

        return if (isCreditCard) {
            applyCreditCard(currentBalance, amount, transactionType)
        } else {
            applyRegular(currentBalance, amount, transactionType)
        }
    }

    /**
     * Resolves the transfer direction for a given account in a transfer transaction.
     *
     * Uses suffix matching: if the account's last-4 appears as a suffix of `fromAccount`
     * or `toAccount`, the direction is determined. If both or neither match, returns NONE.
     *
     * @param accountLast4 The last-4 digits of the account to resolve direction for.
     * @param fromAccount The source account identifier from the transaction.
     * @param toAccount The destination account identifier from the transaction.
     * @return The resolved transfer direction.
     */
    /**
     * Resolves the transfer direction for a given account in a transfer transaction.
     *
     * Uses suffix matching: if the account's last-4 appears as a suffix of `fromAccount`
     * or `toAccount`, the direction is determined. If both or neither match, returns NONE.
     *
     * @param accountLast4 The last-4 digits of the account to resolve direction for.
     * @param fromAccount The source account identifier from the transaction.
     * @param toAccount The destination account identifier from the transaction.
     * @return The resolved transfer direction.
     */
    fun resolveTransferDirection(
        accountLast4: String,
        fromAccount: String?,
        toAccount: String?
    ): TransferDirection {
        val isSource = isAccountMatch(accountLast4, fromAccount)
        val isDestination = isAccountMatch(accountLast4, toAccount)

        return when {
            isSource && !isDestination -> TransferDirection.SOURCE
            isDestination && !isSource -> TransferDirection.DESTINATION
            // Both match (self-transfer) or neither match — no balance change
            else -> TransferDirection.NONE
        }
    }

    /**
     * Resolves transfer direction using full account context (ID, bank name, last-4, and nickname).
     * Handles accounts without last-4 (e.g. wallets or imported accounts), cross-account transfers,
     * and explicit fromAccountId / toAccountId relations.
     */
    fun resolveTransferDirection(
        accountId: String?,
        bankName: String?,
        accountLast4: String?,
        accountName: String?,
        transaction: TransactionEntity
    ): TransferDirection {
        if (transaction.transactionType != TransactionType.TRANSFER) {
            return TransferDirection.NONE
        }

        // 1. Direct ID matching (most reliable)
        val isSourceById = accountId != null && (
            transaction.fromAccountId == accountId ||
            (transaction.fromAccountId == null && transaction.accountId == accountId)
        )
        val isDestById = accountId != null && transaction.toAccountId == accountId

        if (isSourceById || isDestById) {
            return when {
                isSourceById && !isDestById -> TransferDirection.SOURCE
                isDestById && !isSourceById -> TransferDirection.DESTINATION
                else -> TransferDirection.NONE
            }
        }

        // 2. Account last-4 suffix matching (if last-4 is present and not blank)
        val cleanLast4 = accountLast4?.trim() ?: ""
        if (cleanLast4.isNotEmpty()) {
            val isSourceByLast4 = isAccountMatch(cleanLast4, transaction.fromAccount)
            val isDestByLast4 = isAccountMatch(cleanLast4, transaction.toAccount)

            if (isSourceByLast4 || isDestByLast4) {
                return when {
                    isSourceByLast4 && !isDestByLast4 -> TransferDirection.SOURCE
                    isDestByLast4 && !isSourceByLast4 -> TransferDirection.DESTINATION
                    else -> TransferDirection.NONE
                }
            }
        }

        // 3. Bank name & Account name matching
        val isSourceByName = (!bankName.isNullOrBlank() && transaction.fromAccount?.trim().equals(bankName.trim(), ignoreCase = true)) ||
                (!accountName.isNullOrBlank() && transaction.fromAccount?.trim().equals(accountName.trim(), ignoreCase = true))
        val isDestByName = (!bankName.isNullOrBlank() && transaction.toAccount?.trim().equals(bankName.trim(), ignoreCase = true)) ||
                (!accountName.isNullOrBlank() && transaction.toAccount?.trim().equals(accountName.trim(), ignoreCase = true))

        return when {
            isSourceByName && !isDestByName -> TransferDirection.SOURCE
            isDestByName && !isSourceByName -> TransferDirection.DESTINATION
            else -> TransferDirection.NONE
        }
    }

    /**
     * Returns the effective amount for a transaction relative to an account and its direction.
     * For transfers entering an account (DESTINATION), uses targetAmount if available.
     * Otherwise uses amount.
     */
    fun getEffectiveAmount(
        transaction: TransactionEntity,
        direction: TransferDirection
    ): BigDecimal {
        return if (direction == TransferDirection.DESTINATION && transaction.targetAmount != null) {
            transaction.targetAmount
        } else {
            transaction.amount
        }
    }

    /**
     * Calculates the net balance effect of a transaction (signed).
     * Positive = balance increases, Negative = balance decreases.
     *
     * Useful for audit calculations where you need the signed delta.
     */
    fun balanceEffect(
        amount: BigDecimal,
        transactionType: TransactionType,
        isCreditCard: Boolean,
        direction: TransferDirection = TransferDirection.NONE
    ): BigDecimal {
        if (transactionType == TransactionType.BALANCE_UPDATE) {
            return BigDecimal.ZERO
        }

        if (transactionType == TransactionType.TRANSFER) {
            return transferEffect(amount, isCreditCard, direction)
        }

        return if (isCreditCard) {
            creditCardEffect(amount, transactionType)
        } else {
            regularEffect(amount, transactionType)
        }
    }

    data class AuditBalances(
        val initialBalance: BigDecimal,
        val expectedBalance: BigDecimal
    )

    /**
     * Calculates the initial (starting) balance and expected current balance for an account
     * based on its transaction history and optional anchor balance points.
     */
    fun calculateAuditBalances(
        transactions: List<TransactionEntity>,
        accountLast4: String,
        isCreditCard: Boolean,
        currentBalance: BigDecimal,
        accountId: String? = null,
        bankName: String? = null,
        accountName: String? = null
    ): AuditBalances {
        val chronologicalTransactions = transactions.sortedBy { it.dateTime }
        if (chronologicalTransactions.isEmpty()) {
            return AuditBalances(
                initialBalance = currentBalance,
                expectedBalance = currentBalance
            )
        }

        fun getDirection(t: TransactionEntity): TransferDirection {
            return if (t.transactionType == TransactionType.TRANSFER) {
                if (accountId != null || bankName != null) {
                    resolveTransferDirection(accountId, bankName, accountLast4, accountName, t)
                } else {
                    resolveTransferDirection(accountLast4, t.fromAccount, t.toAccount)
                }
            } else {
                TransferDirection.NONE
            }
        }

        val lastWithBalanceAfter = chronologicalTransactions.lastOrNull { it.balanceAfter != null }
        return if (lastWithBalanceAfter != null) {
            val anchorBalance = lastWithBalanceAfter.balanceAfter!!
            val anchorIndex = chronologicalTransactions.indexOf(lastWithBalanceAfter)

            val afterAnchorEffects = chronologicalTransactions.drop(anchorIndex + 1).fold(BigDecimal.ZERO) { acc, t ->
                val direction = getDirection(t)
                val effAmount = getEffectiveAmount(t, direction)
                acc + balanceEffect(effAmount, t.transactionType, isCreditCard, direction)
            }
            val expectedBalance = anchorBalance + afterAnchorEffects

            val upToAnchorEffects = chronologicalTransactions.take(anchorIndex + 1).fold(BigDecimal.ZERO) { acc, t ->
                val direction = getDirection(t)
                val effAmount = getEffectiveAmount(t, direction)
                acc + balanceEffect(effAmount, t.transactionType, isCreditCard, direction)
            }
            val initialBalance = anchorBalance - upToAnchorEffects

            AuditBalances(
                initialBalance = initialBalance,
                expectedBalance = expectedBalance
            )
        } else {
            val totalNetEffect = chronologicalTransactions.fold(BigDecimal.ZERO) { acc, t ->
                val direction = getDirection(t)
                val effAmount = getEffectiveAmount(t, direction)
                acc + balanceEffect(effAmount, t.transactionType, isCreditCard, direction)
            }

            AuditBalances(
                initialBalance = BigDecimal.ZERO,
                expectedBalance = totalNetEffect
            )
        }
    }

    /**
     * Unified single-source-of-truth method to compute audit balances and totals for any account.
     * Accurately handles transfers, cross-currency amounts, credit cards, and ledger chronology.
     */
    fun computeAccountAudit(
        account: AccountBalanceEntity,
        transactions: List<TransactionEntity>
    ): AccountAuditSummary {
        val chronological = transactions.sortedBy { it.dateTime }

        fun resolveDirection(tx: TransactionEntity): TransferDirection {
            return resolveTransferDirection(
                accountId = account.id.ifBlank { null },
                bankName = account.bankName,
                accountLast4 = account.accountLast4,
                accountName = account.accountName,
                transaction = tx
            )
        }

        val totalIncome = chronological
            .filter { it.transactionType == TransactionType.INCOME || it.transactionType == TransactionType.CREDIT }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

        val totalExpenses = chronological
            .filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.INVESTMENT }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

        val totalTransfersIn = chronological
            .filter {
                it.transactionType == TransactionType.TRANSFER &&
                resolveDirection(it) == TransferDirection.DESTINATION
            }
            .fold(BigDecimal.ZERO) { acc, t ->
                acc + getEffectiveAmount(t, TransferDirection.DESTINATION)
            }

        val totalTransfersOut = chronological
            .filter {
                it.transactionType == TransactionType.TRANSFER &&
                resolveDirection(it) == TransferDirection.SOURCE
            }
            .fold(BigDecimal.ZERO) { acc, t ->
                acc + getEffectiveAmount(t, TransferDirection.SOURCE)
            }

        val auditBalances = calculateAuditBalances(
            transactions = chronological,
            accountLast4 = account.accountLast4,
            isCreditCard = account.isCreditCard,
            currentBalance = account.balance,
            accountId = account.id.ifBlank { null },
            bankName = account.bankName,
            accountName = account.accountName
        )

        val discrepancy = auditBalances.expectedBalance - account.balance

        return AccountAuditSummary(
            initialBalance = auditBalances.initialBalance,
            expectedBalance = auditBalances.expectedBalance,
            discrepancy = discrepancy,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            totalTransfersIn = totalTransfersIn,
            totalTransfersOut = totalTransfersOut,
            isHealthy = discrepancy.compareTo(BigDecimal.ZERO) == 0,
            chronologicalTransactions = chronological
        )
    }

    /**
     * Builds chronological bank-statement ledger rows with a running balance.
     */
    fun buildLedgerRows(
        account: AccountBalanceEntity,
        chronologicalTransactions: List<TransactionEntity>,
        initialBalance: BigDecimal
    ): List<LedgerRow> {
        var runningBalance = initialBalance
        return chronologicalTransactions.mapIndexed { index, tx ->
            val direction = if (tx.transactionType == TransactionType.TRANSFER) {
                resolveTransferDirection(
                    accountId = account.id.ifBlank { null },
                    bankName = account.bankName,
                    accountLast4 = account.accountLast4,
                    accountName = account.accountName,
                    transaction = tx
                )
            } else {
                TransferDirection.NONE
            }

            val effAmount = getEffectiveAmount(tx, direction)

            val effect = balanceEffect(
                amount = effAmount,
                transactionType = tx.transactionType,
                isCreditCard = account.isCreditCard,
                direction = direction
            )

            runningBalance = runningBalance + effect

            val (paid, income) = if (account.isCreditCard) {
                when {
                    effect > BigDecimal.ZERO -> effect to null
                    effect < BigDecimal.ZERO -> null to effect.abs()
                    else -> null to null
                }
            } else {
                when {
                    effect > BigDecimal.ZERO -> null to effect
                    effect < BigDecimal.ZERO -> effect.abs() to null
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
     * Single source of truth for detecting suspected duplicate transactions:
     * - Filters out transactions already marked as intentional (isDuplicateDismissed == true)
     * - Matches identical amount, merchant name (case-insensitive), transaction type, within 5 minutes
     */
    fun findDuplicateTransactions(transactions: List<TransactionEntity>): List<DuplicateTransactionGroup> {
        val nonDismissed = transactions.filter { !it.isDuplicateDismissed }
        val groups = mutableListOf<DuplicateTransactionGroup>()
        val seen = mutableSetOf<Long>()

        for (i in nonDismissed.indices) {
            val current = nonDismissed[i]
            if (current.id in seen) continue

            val duplicates = mutableListOf(current)
            for (j in i + 1 until nonDismissed.size) {
                val other = nonDismissed[j]
                if (other.id in seen) continue

                val sameAmount = current.amount.compareTo(other.amount) == 0
                val sameMerchant = current.merchantName.equals(other.merchantName, ignoreCase = true)
                val sameType = current.transactionType == other.transactionType
                val withinWindow = kotlin.math.abs(
                    Duration.between(current.dateTime, other.dateTime).toMinutes()
                ) <= 5

                if (sameAmount && sameMerchant && sameType && withinWindow) {
                    duplicates.add(other)
                    seen.add(other.id)
                }
            }

            if (duplicates.size > 1) {
                seen.add(current.id)
                groups.add(
                    DuplicateTransactionGroup(
                        key = "${current.merchantName}_${current.amount}_${current.id}",
                        transactions = duplicates,
                        amount = current.amount,
                        merchantName = current.merchantName
                    )
                )
            }
        }
        return groups
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun applyRegular(
        currentBalance: BigDecimal,
        amount: BigDecimal,
        type: TransactionType
    ): BigDecimal = when (type) {
        TransactionType.INCOME, TransactionType.CREDIT -> currentBalance + amount
        TransactionType.EXPENSE, TransactionType.INVESTMENT -> currentBalance - amount
        else -> currentBalance
    }

    private fun applyCreditCard(
        currentBalance: BigDecimal,
        amount: BigDecimal,
        type: TransactionType
    ): BigDecimal = when (type) {
        // Spending increases outstanding debt
        TransactionType.EXPENSE, TransactionType.INVESTMENT, TransactionType.CREDIT -> currentBalance + amount
        // Repayments reduce outstanding debt
        TransactionType.INCOME -> currentBalance - amount
        else -> currentBalance
    }

    private fun applyTransfer(
        currentBalance: BigDecimal,
        amount: BigDecimal,
        isCreditCard: Boolean,
        direction: TransferDirection
    ): BigDecimal = when (direction) {
        TransferDirection.SOURCE -> {
            if (isCreditCard) {
                // Cash advance or similar — debt increases
                currentBalance + amount
            } else {
                // Money leaving savings/current
                currentBalance - amount
            }
        }
        TransferDirection.DESTINATION -> {
            if (isCreditCard) {
                // Bill payment — debt decreases
                currentBalance - amount
            } else {
                // Money arriving in savings/current
                currentBalance + amount
            }
        }
        TransferDirection.NONE -> currentBalance
    }

    private fun regularEffect(amount: BigDecimal, type: TransactionType): BigDecimal = when (type) {
        TransactionType.INCOME, TransactionType.CREDIT -> amount
        TransactionType.EXPENSE, TransactionType.INVESTMENT -> amount.negate()
        else -> BigDecimal.ZERO
    }

    private fun creditCardEffect(amount: BigDecimal, type: TransactionType): BigDecimal = when (type) {
        TransactionType.EXPENSE, TransactionType.INVESTMENT, TransactionType.CREDIT -> amount
        TransactionType.INCOME -> amount.negate()
        else -> BigDecimal.ZERO
    }

    private fun transferEffect(
        amount: BigDecimal,
        isCreditCard: Boolean,
        direction: TransferDirection
    ): BigDecimal = when (direction) {
        TransferDirection.SOURCE -> {
            if (isCreditCard) amount else amount.negate()
        }
        TransferDirection.DESTINATION -> {
            if (isCreditCard) amount.negate() else amount
        }
        TransferDirection.NONE -> BigDecimal.ZERO
    }

    /**
     * Checks if [accountLast4] matches [candidate] using suffix comparison.
     * Returns true if either string ends with the other (digit-normalized).
     */
    private fun isAccountMatch(accountLast4: String, candidate: String?): Boolean {
        if (candidate == null) return false
        val clean = candidate.trim()
        val acct = accountLast4.trim()
        if (clean.isEmpty() || acct.isEmpty()) return false

        // Direct equality
        if (clean.equals(acct, ignoreCase = true)) return true

        // Suffix matching: either direction
        if (clean.endsWith(acct, ignoreCase = true) || acct.endsWith(clean, ignoreCase = true)) {
            return true
        }

        // Digit-only suffix matching for cases like "XX1234" vs "1234"
        val digits1 = clean.filter { it.isDigit() }
        val digits2 = acct.filter { it.isDigit() }
        if (digits1.isNotEmpty() && digits2.isNotEmpty()) {
            if (digits1.endsWith(digits2) || digits2.endsWith(digits1)) return true
        }

        return false
    }

    /**
     * Calculates the start date for portfolio balance history.
     * Takes the least of maxDays (default 180 days) or the duration since the first transaction date.
     *
     * @param firstTransactionDate The date of the user's first transaction (using transaction dateTime).
     * @param endDate The end date for the history range (defaults to today).
     * @param maxDays Maximum number of days of history to include (default 180).
     * @return The starting LocalDate for computing portfolio balance history.
     */
    fun calculatePortfolioStartDate(
        firstTransactionDate: LocalDate?,
        endDate: LocalDate = LocalDate.now(),
        maxDays: Long = 180
    ): LocalDate {
        val defaultStartDate = endDate.minusDays(maxDays)
        if (firstTransactionDate == null) {
            return defaultStartDate
        }
        val candidate = if (firstTransactionDate.isAfter(defaultStartDate)) {
            firstTransactionDate
        } else {
            defaultStartDate
        }
        return if (candidate.isAfter(endDate)) endDate else candidate
    }
}
