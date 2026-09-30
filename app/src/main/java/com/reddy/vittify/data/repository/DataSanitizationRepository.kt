package com.reddy.vittify.data.repository

import com.reddy.vittify.data.database.VittifyDatabase
import com.reddy.vittify.data.database.dao.*
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.model.*
import com.reddy.vittify.domain.model.BalanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSanitizationRepository @Inject constructor(
    private val database: VittifyDatabase,
    private val transactionDao: TransactionDao,
    private val accountBalanceDao: AccountBalanceDao,
    private val cardDao: CardDao,
    private val subscriptionDao: SubscriptionDao,
    private val budgetDao: BudgetDao,
    private val unrecognizedSmsDao: UnrecognizedSmsDao,
    private val transactionRepository: dagger.Lazy<TransactionRepository>
) {

    suspend fun runFullAudit(
        myDeviceId: String,
        partnerUserId: String?,
        pairedDeviceIds: Set<String>,
        hiddenAccountKeys: Set<String>
    ): DataHealthAudit = withContext(Dispatchers.IO) {
        // 1. Fetch raw data
        val allTransactions = transactionDao.getAllTransactionsIncludingDeleted().first()
        val activeTransactions = allTransactions.filter { !it.isDeleted }
        val allAccounts = accountBalanceDao.getAllBalances().first()
        val allCards = cardDao.getAllCards().first()
        val allSubscriptions = subscriptionDao.getAllSubscriptions().first()
        val allBudgets = budgetDao.getAllBudgets().first()
        val allUnrecognizedSms = unrecognizedSmsDao.getAllUnrecognizedSms().first()

        val knownOwnerIds = buildSet {
            if (myDeviceId.isNotBlank()) add(myDeviceId)
            if (!partnerUserId.isNullOrBlank()) add(partnerUserId)
            addAll(pairedDeviceIds.filter { it.isNotBlank() })
        }

        val accountMapById = allAccounts.associateBy { it.id }
        val accountLast4Set = allAccounts.map { it.accountLast4.trim().lowercase() }.toSet()

        // 2. Ownership Issues
        val unassignedTxns = activeTransactions.filter { it.ownerId.isBlank() }
        val unassignedAccs = allAccounts.filter { it.ownerId.isBlank() }
        val unassignedCards = allCards.filter { it.ownerId.isBlank() }
        val unassignedSubs = allSubscriptions.filter { it.ownerId.isBlank() }
        val unassignedBudgets = allBudgets.filter { it.ownerId.isBlank() }

        val foreignTxns = activeTransactions.filter { it.ownerId.isNotBlank() && it.ownerId !in knownOwnerIds }
        val foreignAccs = allAccounts.filter { it.ownerId.isNotBlank() && it.ownerId !in knownOwnerIds }
        val foreignCards = allCards.filter { it.ownerId.isNotBlank() && it.ownerId !in knownOwnerIds }
        val foreignSubs = allSubscriptions.filter { it.ownerId.isNotBlank() && it.ownerId !in knownOwnerIds }
        val foreignBudgets = allBudgets.filter { it.ownerId.isNotBlank() && it.ownerId !in knownOwnerIds }

        val ownerMismatchedTxns = activeTransactions.filter { tx ->
            val linkedAcc = tx.accountId?.let { accountMapById[it] }
            linkedAcc != null && linkedAcc.ownerId.isNotBlank() && tx.ownerId.isNotBlank() && linkedAcc.ownerId != tx.ownerId
        }

        val ownershipIssue = OwnershipIssue(
            unassignedTransactions = unassignedTxns,
            unassignedAccounts = unassignedAccs,
            unassignedCards = unassignedCards,
            unassignedSubscriptions = unassignedSubs,
            unassignedBudgets = unassignedBudgets,
            foreignTransactions = foreignTxns,
            foreignAccounts = foreignAccs,
            foreignCards = foreignCards,
            foreignSubscriptions = foreignSubs,
            foreignBudgets = foreignBudgets,
            ownerMismatchedTransactions = ownerMismatchedTxns
        )

        // 3. Account Health Issues
        val duplicateAccountGroups = allAccounts
            .filter { !it.isWallet && !it.accountLast4.equals("wallet", ignoreCase = true) }
            .groupBy { it.accountLast4.trim().lowercase() }
            .filter { (last4, list) -> last4.isNotBlank() && list.size > 1 }

        val badAccounts = allAccounts.filter { acc ->
            if (acc.isWallet || acc.accountLast4.equals("wallet", ignoreCase = true)) false
            else {
                val last4 = acc.accountLast4.trim()
                val invalidLast4 = last4.length != 4 || !last4.all { it.isDigit() }
                val missingBank = acc.bankName.isBlank() || acc.bankName.equals("Unknown", ignoreCase = true)
                invalidLast4 || missingBank
            }
        }

        val orphanedCards = allCards.filter { card ->
            if (card.cardType == com.reddy.vittify.data.database.entity.CardType.DEBIT) {
                val cardAccLast4 = card.accountLast4?.trim()?.lowercase() ?: ""
                cardAccLast4.isBlank() || cardAccLast4 !in accountLast4Set
            } else {
                false
            }
        }

        val hiddenAccounts = allAccounts.filter { acc ->
            hiddenAccountKeys.contains("${acc.bankName}_${acc.accountLast4}")
        }

        // Account Discrepancies
        val discrepancies = mutableListOf<AccountDiscrepancy>()
        val ghostAccounts = mutableListOf<AccountBalanceEntity>()

        allAccounts.forEach { acc ->
            if (acc.isWallet || acc.accountLast4.equals("wallet", ignoreCase = true)) {
                // Wallets like Cash are valid built-in system accounts, not ghost accounts
                return@forEach
            }

            val accTxns = transactionDao.getTransactionsByAccount(acc.bankName, acc.accountLast4)
                .first()
                .filter { !it.isDeleted && it.transactionType != TransactionType.BALANCE_UPDATE }

            if (accTxns.isEmpty() && acc.balance.compareTo(BigDecimal.ZERO) == 0) {
                ghostAccounts.add(acc)
            } else if (accTxns.isNotEmpty()) {
                val chronological = accTxns.sortedBy { it.dateTime }
                val auditBalances = BalanceCalculator.calculateAuditBalances(
                    transactions = chronological,
                    accountLast4 = acc.accountLast4,
                    isCreditCard = acc.isCreditCard,
                    currentBalance = acc.balance
                )
                val diff = auditBalances.expectedBalance - acc.balance
                if (diff.compareTo(BigDecimal.ZERO) != 0) {
                    discrepancies.add(
                        AccountDiscrepancy(
                            account = acc,
                            currentBalance = acc.balance,
                            expectedBalance = auditBalances.expectedBalance,
                            discrepancy = diff
                        )
                    )
                }
            }
        }

        val accountHealthIssue = AccountHealthIssue(
            duplicateAccountGroups = duplicateAccountGroups,
            badAccounts = badAccounts,
            orphanedCards = orphanedCards,
            hiddenAccounts = hiddenAccounts,
            discrepancies = discrepancies,
            ghostAccounts = ghostAccounts
        )

        // 4. Transaction Anomalies
        val duplicateTxnGroups = findDuplicateTransactions(activeTransactions)

        val orphanedTxns = activeTransactions.filter { tx ->
            val accId = tx.accountId
            accId != null && accId.isNotBlank() && accId !in accountMapById
        }

        val corruptedTxns = activeTransactions.filter { tx ->
            tx.amount <= BigDecimal.ZERO
        }

        val blankMerchantTxns = activeTransactions.filter { tx ->
            val m = tx.merchantName.trim().lowercase()
            m.isBlank() || m == "unknown" || m == "null" || m == "n/a"
        }

        val now = LocalDateTime.now()
        val extremeDateTxns = activeTransactions.filter { tx ->
            tx.dateTime.isAfter(now.plusDays(1)) || tx.dateTime.year < 2000
        }

        val transactionAnomalyIssue = TransactionAnomalyIssue(
            duplicateGroups = duplicateTxnGroups,
            orphanedTransactions = orphanedTxns,
            corruptedTransactions = corruptedTxns,
            blankMerchantTransactions = blankMerchantTxns,
            extremeDateTransactions = extremeDateTxns
        )

        // 5. System / Junk Issues
        val sampleTxnCount = allTransactions.count { it.isSample }
        val sampleAccCount = allAccounts.count { it.isSample }
        val sampleCardCount = allCards.count { it.isSample }
        val sampleSubCount = allSubscriptions.count { it.isSample }
        val sampleBudgetCount = allBudgets.count { it.isSample }
        val archivedCount = allTransactions.count { it.isDeleted }

        val systemJunkIssue = SystemJunkIssue(
            sampleTransactionsCount = sampleTxnCount,
            sampleAccountsCount = sampleAccCount,
            sampleCardsCount = sampleCardCount,
            sampleSubscriptionsCount = sampleSubCount,
            sampleBudgetsCount = sampleBudgetCount,
            unrecognizedSmsCount = allUnrecognizedSms.size,
            archivedTransactionsCount = archivedCount
        )

        // 6. Calculate Overall Health Score
        val totalScanned = activeTransactions.size + allAccounts.size + allCards.size + allSubscriptions.size + allBudgets.size
        val totalIssues = ownershipIssue.totalIssuesCount +
                accountHealthIssue.totalIssuesCount +
                transactionAnomalyIssue.totalIssuesCount +
                systemJunkIssue.totalSampleCount

        // Deductions weighted by severity
        val penalty = (accountHealthIssue.discrepancies.size * 12) +
                (accountHealthIssue.duplicateAccountGroups.size * 10) +
                (accountHealthIssue.badAccounts.size * 8) +
                (transactionAnomalyIssue.corruptedTransactions.size * 10) +
                (transactionAnomalyIssue.totalDuplicateTxnCount * 5) +
                (ownershipIssue.totalIssuesCount * 3) +
                (transactionAnomalyIssue.orphanedTransactions.size * 4) +
                (systemJunkIssue.totalSampleCount * 2)

        val healthScore = (100 - penalty).coerceIn(0, 100)

        DataHealthAudit(
            healthScore = healthScore,
            totalRecordsScanned = totalScanned,
            totalIssuesFound = totalIssues,
            ownership = ownershipIssue,
            accounts = accountHealthIssue,
            transactions = transactionAnomalyIssue,
            system = systemJunkIssue
        )
    }

    private fun findDuplicateTransactions(transactions: List<TransactionEntity>): List<DuplicateTransactionGroup> {
        val groups = mutableListOf<DuplicateTransactionGroup>()
        val seen = mutableSetOf<Long>()

        for (i in transactions.indices) {
            val current = transactions[i]
            if (current.id in seen) continue

            val duplicates = mutableListOf(current)
            for (j in i + 1 until transactions.size) {
                val other = transactions[j]
                if (other.id in seen) continue

                val sameAmount = current.amount.compareTo(other.amount) == 0
                val sameMerchant = current.merchantName.equals(other.merchantName, ignoreCase = true)
                val sameType = current.transactionType == other.transactionType
                val withinWindow = kotlin.math.abs(Duration.between(current.dateTime, other.dateTime).toMinutes()) <= 5

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

    // --- Sanitization Mutation Methods ---

    suspend fun claimAllUnassigned(myDeviceId: String): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            val txns = transactionDao.updateOwnerIdForBlank(myDeviceId)
            val accs = accountBalanceDao.updateOwnerIdForBlank(myDeviceId)
            val cards = cardDao.updateOwnerIdForBlank(myDeviceId)
            val subs = subscriptionDao.updateOwnerIdForBlank(myDeviceId)
            val budgets = budgetDao.updateOwnerIdForBlank(myDeviceId)
            val total = txns + accs + cards + subs + budgets
            SanitizationOperationResult(true, "Claimed $total unassigned records as yours", total)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to claim unassigned records: ${e.message}")
        }
    }

    suspend fun claimAccount(accountId: String, myDeviceId: String): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            accountBalanceDao.updateOwnerIdForAccount(accountId, myDeviceId)
            SanitizationOperationResult(true, "Account claimed to your device", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to claim account: ${e.message}")
        }
    }

    suspend fun claimCard(cardId: Long, myDeviceId: String): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            cardDao.updateOwnerIdForCard(cardId, myDeviceId)
            SanitizationOperationResult(true, "Card claimed to your device", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to claim card: ${e.message}")
        }
    }

    suspend fun deleteCard(cardId: Long): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            cardDao.deleteCardById(cardId)
            SanitizationOperationResult(true, "Card removed", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to delete card: ${e.message}")
        }
    }

    suspend fun reassignForeignRecords(fromOwnerId: String, toOwnerId: String): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            val txns = transactionDao.updateOwnerIdForOwner(fromOwnerId, toOwnerId)
            val accs = accountBalanceDao.updateOwnerIdForOwner(fromOwnerId, toOwnerId)
            val cards = cardDao.updateOwnerIdForOwner(fromOwnerId, toOwnerId)
            val subs = subscriptionDao.updateOwnerIdForOwner(fromOwnerId, toOwnerId)
            val budgets = budgetDao.updateOwnerIdForOwner(fromOwnerId, toOwnerId)
            val total = txns + accs + cards + subs + budgets
            SanitizationOperationResult(true, "Reassigned $total records", total)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to reassign records: ${e.message}")
        }
    }

    suspend fun alignTransactionOwnersWithAccounts(): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            val aligned = transactionDao.alignTransactionOwnersWithAccounts()
            SanitizationOperationResult(true, "Aligned $aligned transactions with their bank account owners", aligned)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to align owners: ${e.message}")
        }
    }

    suspend fun autoDeduplicateAll(duplicateGroups: List<DuplicateTransactionGroup>): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            // Keep the first record in each group, delete the subsequent ones
            val redundantIds = duplicateGroups.flatMap { group ->
                group.transactions.drop(1).map { it.id }
            }
            if (redundantIds.isNotEmpty()) {
                transactionDao.deleteTransactionsByIds(redundantIds)
            }
            SanitizationOperationResult(true, "Removed ${redundantIds.size} duplicate transactions", redundantIds.size)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to deduplicate: ${e.message}")
        }
    }

    suspend fun deleteCorruptedTransactions(): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            val deleted = transactionDao.deleteZeroAmountTransactions()
            SanitizationOperationResult(true, "Deleted $deleted corrupted zero-amount transactions", deleted)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to delete corrupted transactions: ${e.message}")
        }
    }

    suspend fun purgeTrashTransactions(): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteAllArchivedTransactions()
            SanitizationOperationResult(true, "Archived trash purged permanently", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to purge trash: ${e.message}")
        }
    }

    suspend fun purgeSampleData(): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteSampleTransactions()
            accountBalanceDao.deleteSampleBalances()
            cardDao.deleteSampleCards()
            subscriptionDao.deleteSampleSubscriptions()
            budgetDao.deleteSampleBudgets()
            SanitizationOperationResult(true, "All demo and sample data removed successfully", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to remove sample data: ${e.message}")
        }
    }

    suspend fun purgeUnrecognizedSms(): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            val purged = unrecognizedSmsDao.purgeAll()
            SanitizationOperationResult(true, "Purged $purged unrecognized messages", purged)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to purge unrecognized SMS: ${e.message}")
        }
    }

    suspend fun fixAccountDiscrepancy(accountId: String, expectedBalance: BigDecimal): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            accountBalanceDao.updateBalanceById(accountId, expectedBalance)
            SanitizationOperationResult(true, "Account balance updated to match verified ledger", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to fix balance: ${e.message}")
        }
    }

    suspend fun fixAllAccountDiscrepancies(discrepancies: List<AccountDiscrepancy>): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            var count = 0
            discrepancies.forEach { disc ->
                accountBalanceDao.updateBalanceById(disc.account.id, disc.expectedBalance)
                count++
            }
            SanitizationOperationResult(true, "Updated $count account balances to match verified ledgers", count)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to fix balances: ${e.message}")
        }
    }

    suspend fun mergeAccounts(
        targetAccount: AccountBalanceEntity,
        sourceAccounts: List<AccountBalanceEntity>,
        newBalance: BigDecimal?
    ): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            sourceAccounts.forEach { source ->
                transactionDao.updateAccountIdForTransactions(
                    oldAccountId = source.id,
                    newAccountId = targetAccount.id
                )
                accountBalanceDao.deleteAccount(source.bankName, source.accountLast4)
            }
            if (newBalance != null) {
                accountBalanceDao.updateBalanceById(targetAccount.id, newBalance)
            }
            SanitizationOperationResult(true, "Accounts successfully merged into ${targetAccount.bankName}", sourceAccounts.size)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to merge accounts: ${e.message}")
        }
    }

    suspend fun deleteAccount(account: AccountBalanceEntity): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            transactionRepository.get().handleAccountDeletion(account.bankName, account.accountLast4, listOf(account.id))
            accountBalanceDao.deleteAccount(account.bankName, account.accountLast4)
            SanitizationOperationResult(true, "Account and associated transactions handled", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to delete account: ${e.message}")
        }
    }

    suspend fun deleteTransaction(transactionId: Long): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteTransactionById(transactionId)
            SanitizationOperationResult(true, "Transaction deleted", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to delete transaction: ${e.message}")
        }
    }

    suspend fun unlinkOrphanedTransactions(transactionIds: List<Long> = emptyList()): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            val count = if (transactionIds.isNotEmpty()) {
                transactionDao.unlinkTransactionsByIds(transactionIds)
            } else {
                val allTxns = transactionDao.getAllTransactionsIncludingDeleted().first()
                val allAccs = accountBalanceDao.getAllBalances().first()
                val accIds = allAccs.map { it.id }.toSet()
                val orphaned = allTxns.filter { tx ->
                    val accId = tx.accountId
                    accId != null && accId.isNotBlank() && accId !in accIds
                }.map { it.id }
                if (orphaned.isNotEmpty()) {
                    transactionDao.unlinkTransactionsByIds(orphaned)
                } else 0
            }
            SanitizationOperationResult(true, "Unlinked $count transactions from deleted accounts", count)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to unlink transactions: ${e.message}")
        }
    }

    suspend fun deleteOrphanedTransactions(transactionIds: List<Long>): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteTransactionsByIds(transactionIds)
            SanitizationOperationResult(true, "Deleted ${transactionIds.size} orphaned transactions", transactionIds.size)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to delete orphaned transactions: ${e.message}")
        }
    }

    suspend fun autoResolveUnknownMerchants(transactions: List<TransactionEntity> = emptyList()): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            val targets = if (transactions.isNotEmpty()) transactions else {
                val all = transactionDao.getAllTransactionsIncludingDeleted().first().filter { !it.isDeleted }
                all.filter { tx ->
                    val m = tx.merchantName.trim().lowercase()
                    m.isBlank() || m == "unknown" || m == "null" || m == "n/a"
                }
            }
            var updated = 0
            targets.forEach { tx ->
                val fallback = when {
                    !tx.subcategory.isNullOrBlank() -> tx.subcategory
                    !tx.category.isBlank() && !tx.category.equals("Miscellaneous", ignoreCase = true) -> "${tx.category} Expense"
                    else -> "General Expense"
                }
                transactionDao.updateMerchantName(tx.id, fallback)
                updated++
            }
            SanitizationOperationResult(true, "Resolved $updated unknown merchant names", updated)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to resolve merchant names: ${e.message}")
        }
    }

    suspend fun updateMerchantName(transactionId: Long, newName: String): SanitizationOperationResult = withContext(Dispatchers.IO) {
        try {
            transactionDao.updateMerchantName(transactionId, newName)
            SanitizationOperationResult(true, "Updated merchant name", 1)
        } catch (e: Exception) {
            SanitizationOperationResult(false, "Failed to update merchant name: ${e.message}")
        }
    }
}
