package com.reddy.vittify.data.model

import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.BudgetEntity
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import java.math.BigDecimal

data class AccountDiscrepancy(
    val account: AccountBalanceEntity,
    val currentBalance: BigDecimal,
    val expectedBalance: BigDecimal,
    val discrepancy: BigDecimal
)

data class DuplicateTransactionGroup(
    val key: String,
    val transactions: List<TransactionEntity>,
    val amount: BigDecimal,
    val merchantName: String
)

data class OwnershipIssue(
    val unassignedTransactions: List<TransactionEntity> = emptyList(),
    val unassignedAccounts: List<AccountBalanceEntity> = emptyList(),
    val unassignedCards: List<CardEntity> = emptyList(),
    val unassignedSubscriptions: List<SubscriptionEntity> = emptyList(),
    val unassignedBudgets: List<BudgetEntity> = emptyList(),
    val foreignTransactions: List<TransactionEntity> = emptyList(),
    val foreignAccounts: List<AccountBalanceEntity> = emptyList(),
    val foreignCards: List<CardEntity> = emptyList(),
    val foreignSubscriptions: List<SubscriptionEntity> = emptyList(),
    val foreignBudgets: List<BudgetEntity> = emptyList(),
    val ownerMismatchedTransactions: List<TransactionEntity> = emptyList()
) {
    val totalUnassignedCount: Int
        get() = unassignedTransactions.size + unassignedAccounts.size + unassignedCards.size + unassignedSubscriptions.size + unassignedBudgets.size

    val totalForeignCount: Int
        get() = foreignTransactions.size + foreignAccounts.size + foreignCards.size + foreignSubscriptions.size + foreignBudgets.size

    val totalIssuesCount: Int
        get() = totalUnassignedCount + totalForeignCount + ownerMismatchedTransactions.size
}

data class AccountHealthIssue(
    val duplicateAccountGroups: Map<String, List<AccountBalanceEntity>> = emptyMap(),
    val badAccounts: List<AccountBalanceEntity> = emptyList(),
    val orphanedCards: List<CardEntity> = emptyList(),
    val hiddenAccounts: List<AccountBalanceEntity> = emptyList(),
    val discrepancies: List<AccountDiscrepancy> = emptyList(),
    val ghostAccounts: List<AccountBalanceEntity> = emptyList()
) {
    val totalIssuesCount: Int
        get() = duplicateAccountGroups.values.sumOf { it.size } +
                badAccounts.size +
                orphanedCards.size +
                hiddenAccounts.size +
                discrepancies.size +
                ghostAccounts.size
}

data class TransactionAnomalyIssue(
    val duplicateGroups: List<DuplicateTransactionGroup> = emptyList(),
    val orphanedTransactions: List<TransactionEntity> = emptyList(),
    val corruptedTransactions: List<TransactionEntity> = emptyList(), // <= 0
    val blankMerchantTransactions: List<TransactionEntity> = emptyList(),
    val extremeDateTransactions: List<TransactionEntity> = emptyList()
) {
    val totalDuplicateTxnCount: Int
        get() = duplicateGroups.sumOf { (it.transactions.size - 1).coerceAtLeast(0) }

    val totalIssuesCount: Int
        get() = totalDuplicateTxnCount +
                orphanedTransactions.size +
                corruptedTransactions.size +
                blankMerchantTransactions.size +
                extremeDateTransactions.size
}

data class SystemJunkIssue(
    val sampleTransactionsCount: Int = 0,
    val sampleAccountsCount: Int = 0,
    val sampleCardsCount: Int = 0,
    val sampleSubscriptionsCount: Int = 0,
    val sampleBudgetsCount: Int = 0,
    val unrecognizedSmsCount: Int = 0,
    val archivedTransactionsCount: Int = 0
) {
    val totalSampleCount: Int
        get() = sampleTransactionsCount + sampleAccountsCount + sampleCardsCount + sampleSubscriptionsCount + sampleBudgetsCount

    val totalIssuesCount: Int
        get() = totalSampleCount + unrecognizedSmsCount + archivedTransactionsCount
}

data class DataHealthAudit(
    val healthScore: Int = 100, // 0 - 100
    val totalRecordsScanned: Int = 0,
    val totalIssuesFound: Int = 0,
    val ownership: OwnershipIssue = OwnershipIssue(),
    val accounts: AccountHealthIssue = AccountHealthIssue(),
    val transactions: TransactionAnomalyIssue = TransactionAnomalyIssue(),
    val system: SystemJunkIssue = SystemJunkIssue()
)

data class SanitizationOperationResult(
    val success: Boolean,
    val message: String,
    val affectedCount: Int = 0
)
