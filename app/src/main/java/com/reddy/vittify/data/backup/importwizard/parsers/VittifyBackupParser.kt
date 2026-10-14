package com.reddy.vittify.data.backup.importwizard.parsers

import android.net.Uri
import com.reddy.vittify.data.backup.BackupImporter
import com.reddy.vittify.data.backup.importwizard.CategoryMatchStatus
import com.reddy.vittify.data.backup.importwizard.ImportAccountItem
import com.reddy.vittify.data.backup.importwizard.ImportCategoryItem
import com.reddy.vittify.data.backup.importwizard.ImportSource
import com.reddy.vittify.data.backup.importwizard.ImportSubscriptionItem
import com.reddy.vittify.data.backup.importwizard.ImportTransactionItem
import com.reddy.vittify.data.backup.importwizard.ParsedImportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VittifyBackupParser @Inject constructor(
    private val backupImporter: BackupImporter
) {
    suspend fun parse(uri: Uri): ParsedImportData = withContext(Dispatchers.IO) {
        val backup = backupImporter.readBackup(uri)
            ?: throw IllegalArgumentException("Could not read or parse Vittify backup file")

        val db = backup.database

        // 1. Accounts
        val accounts = (db.accountBalances ?: emptyList()).map { acc ->
            ImportAccountItem(
                id = acc.id,
                originalId = acc.id,
                bankName = acc.bankName,
                accountLast4 = acc.accountLast4,
                accountName = acc.accountName,
                balance = acc.balance,
                currency = acc.currency,
                isCreditCard = acc.isCreditCard,
                isWallet = acc.isWallet,
                color = acc.color,
                isSelected = true
            )
        }
        val accountMap = accounts.associateBy { it.id }

        // 2. Categories & Subcategories
        val categories = mutableListOf<ImportCategoryItem>()
        val catIdToNameMap = mutableMapOf<Long, String>()

        (db.categories ?: emptyList()).forEach { cat ->
            catIdToNameMap[cat.id] = cat.name
            categories.add(
                ImportCategoryItem(
                    id = cat.id,
                    originalId = cat.id.toString(),
                    name = cat.name,
                    isIncome = cat.isIncome,
                    color = cat.color,
                    iconName = cat.iconName,
                    parentCategoryName = null,
                    matchStatus = CategoryMatchStatus.NEW_CATEGORY,
                    createAsNew = true,
                    isSelected = true
                )
            )
        }

        (db.subcategories ?: emptyList()).forEach { sub ->
            val parentName = catIdToNameMap[sub.categoryId]
            categories.add(
                ImportCategoryItem(
                    id = sub.id,
                    originalId = sub.id.toString(),
                    name = sub.name,
                    isIncome = false,
                    color = sub.color,
                    iconName = sub.iconName,
                    parentCategoryName = parentName,
                    matchStatus = CategoryMatchStatus.NEW_CATEGORY,
                    createAsNew = true,
                    isSelected = true
                )
            )
        }

        // 3. Subscriptions
        val subscriptions = (db.subscriptions ?: emptyList()).map { sub ->
            ImportSubscriptionItem(
                id = sub.id,
                merchantName = sub.merchantName,
                amount = sub.amount,
                billingCycle = sub.billingCycle ?: "MONTHLY",
                nextPaymentDate = sub.nextPaymentDate,
                categoryName = sub.category,
                bankName = sub.bankName,
                currency = sub.currency,
                isSelected = true
            )
        }

        // 4. Transactions
        val transactions = (db.transactions ?: emptyList()).map { txn ->
            val mappedAccount = txn.accountId?.let { accountMap[it] }
            ImportTransactionItem(
                id = txn.id,
                merchantName = txn.merchantName,
                amount = txn.amount,
                transactionType = txn.transactionType,
                dateTime = txn.dateTime,
                categoryName = txn.category,
                subcategoryName = txn.subcategory,
                originalBankName = mappedAccount?.bankName ?: txn.fromAccount,
                originalAccountLast4 = mappedAccount?.accountLast4 ?: "",
                mappedAccountId = txn.accountId,
                currency = txn.currency,
                description = txn.description,
                isSelected = true
            )
        }

        ParsedImportData(
            source = ImportSource.VITTIFY,
            accounts = accounts,
            categories = categories,
            subscriptions = subscriptions,
            transactions = transactions,
            rawMetadata = "Vittify Backup v${backup.metadata.appVersion} (${backup.metadata.statistics.totalTransactions} transactions)"
        )
    }
}
