package com.reddy.vittify.data.backup.importwizard.parsers

import android.content.Context
import android.net.Uri
import android.util.Log
import com.opencsv.CSVReader
import com.reddy.vittify.data.backup.importwizard.CategoryMatchStatus
import com.reddy.vittify.data.backup.importwizard.ImportAccountItem
import com.reddy.vittify.data.backup.importwizard.ImportCategoryItem
import com.reddy.vittify.data.backup.importwizard.ImportSource
import com.reddy.vittify.data.backup.importwizard.ImportTransactionItem
import com.reddy.vittify.data.backup.importwizard.ParsedImportData
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.utils.NanoId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvImportParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CsvImportParser"
    }

    suspend fun parse(uri: Uri): ParsedImportData = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open CSV stream at $uri")

        val accounts = mutableListOf<ImportAccountItem>()
        val categories = mutableListOf<ImportCategoryItem>()
        val transactions = mutableListOf<ImportTransactionItem>()

        InputStreamReader(inputStream).use { reader ->
            CSVReader(reader).use { csvReader ->
                val lines = csvReader.readAll()
                if (lines.isEmpty()) return@withContext ParsedImportData(ImportSource.CSV)

                // Header finding: skip any initial comments (starting with #)
                var headerIndex = -1
                var headers: Map<String, Int> = emptyMap()

                for (i in lines.indices) {
                    val firstCol = lines[i].firstOrNull()?.trim() ?: ""
                    if (firstCol.startsWith("#") || firstCol.isBlank()) continue
                    // Found header row
                    headerIndex = i
                    headers = lines[i].mapIndexed { idx, col ->
                        col.trim().lowercase(Locale.ROOT) to idx
                    }.toMap()
                    break
                }

                if (headerIndex == -1 || headerIndex >= lines.size - 1) {
                    return@withContext ParsedImportData(ImportSource.CSV)
                }

                val dateCol = headers["date"] ?: 0
                val timeCol = headers["time"]
                val merchantCol = headers["merchant"] ?: headers["merchant / title"] ?: headers["payee"] ?: headers["title"] ?: 2
                val catCol = headers["category"] ?: 3
                val subcatCol = headers["subcategory"]
                val typeCol = headers["type"] ?: 4
                val amountCol = headers["amount"] ?: 5
                val bankCol = headers["bank"] ?: 6
                val accCol = headers["account"] ?: 7
                val descCol = headers["description"] ?: headers["note"] ?: 9

                for (rowIdx in (headerIndex + 1) until lines.size) {
                    val row = lines[rowIdx]
                    if (row.isEmpty() || row.all { it.isBlank() } || row[0].trim().startsWith("#")) continue

                    val dateStr = row.getOrNull(dateCol)?.trim() ?: ""
                    val timeStr = timeCol?.let { row.getOrNull(it)?.trim() } ?: "12:00:00"
                    val rawMerchant = row.getOrNull(merchantCol)?.trim() ?: ""
                    val catStr = row.getOrNull(catCol)?.trim()?.ifBlank { "Other" } ?: "Other"
                    val subcatStr = subcatCol?.let { row.getOrNull(it)?.trim() }?.takeIf { it.isNotBlank() }
                    val typeStr = row.getOrNull(typeCol)?.trim()?.uppercase(Locale.ROOT) ?: "EXPENSE"
                    val amountStr = row.getOrNull(amountCol)?.trim()?.replace(",", "") ?: "0"
                    val bankStr = row.getOrNull(bankCol)?.trim() ?: ""
                    val accNumStr = row.getOrNull(accCol)?.trim() ?: ""
                    val descStr = descCol?.let { row.getOrNull(it)?.trim() }?.takeIf { it.isNotBlank() }

                    val merchant = if (rawMerchant.isNotBlank()) {
                        rawMerchant
                    } else {
                        subcatStr?.takeIf { it.isNotBlank() } ?: catStr.ifBlank { "Transaction" }
                    }

                    val amount = try {
                        BigDecimal(amountStr).abs()
                    } catch (e: Exception) {
                        BigDecimal.ZERO
                    }

                    val dateTime = try {
                        val cleanDate = dateStr.take(10)
                        val cleanTime = if (timeStr.length >= 5) timeStr.take(8) else "12:00:00"
                        LocalDateTime.parse("${cleanDate}T${cleanTime}")
                    } catch (e: Exception) {
                        LocalDateTime.now()
                    }

                    val txnType = when {
                        typeStr.contains("INCOME") -> TransactionType.INCOME
                        typeStr.contains("TRANSFER") -> TransactionType.TRANSFER
                        typeStr.contains("CREDIT") -> TransactionType.CREDIT
                        typeStr.contains("INVEST") -> TransactionType.INVESTMENT
                        typeStr.contains("BALANCE") -> TransactionType.BALANCE_UPDATE
                        else -> TransactionType.EXPENSE
                    }

                    transactions.add(
                        ImportTransactionItem(
                            merchantName = merchant,
                            amount = amount,
                            transactionType = txnType,
                            dateTime = dateTime,
                            categoryName = catStr,
                            subcategoryName = subcatStr,
                            originalBankName = bankStr.ifBlank { null },
                            originalAccountLast4 = if (accNumStr.length <= 4 && accNumStr.all { it.isDigit() }) accNumStr else "",
                            description = descStr,
                            isSelected = true
                        )
                    )
                }
            }
        }

        // Infer Accounts
        val bankGroups = transactions.mapNotNull { it.originalBankName }.filter { it.isNotBlank() }.distinct()
        bankGroups.forEach { bankName ->
            accounts.add(
                ImportAccountItem(
                    id = NanoId.generate(),
                    bankName = bankName,
                    accountLast4 = "",
                    accountName = bankName,
                    balance = BigDecimal.ZERO,
                    currency = "INR",
                    isSelected = true
                )
            )
        }

        if (accounts.isEmpty()) {
            accounts.add(
                ImportAccountItem(
                    id = NanoId.generate(),
                    bankName = "Primary Account",
                    accountLast4 = "",
                    accountName = "Primary Account",
                    balance = BigDecimal.ZERO,
                    currency = "INR",
                    isSelected = true
                )
            )
        }

        val accountMap = accounts.associateBy { it.bankName.lowercase(Locale.ROOT) }
        val mappedTransactions = transactions.map { txn ->
            val mapped = txn.originalBankName?.lowercase(Locale.ROOT)?.let { accountMap[it] } ?: accounts.firstOrNull()
            txn.copy(mappedAccountId = mapped?.id)
        }

        // Infer Categories
        val distinctCats = transactions.map { it.categoryName to it.subcategoryName }.distinct()
        distinctCats.forEach { (catName, subcatName) ->
            if (categories.none { it.name.equals(catName, ignoreCase = true) && it.parentCategoryName == null }) {
                categories.add(
                    ImportCategoryItem(
                        name = catName,
                        parentCategoryName = null,
                        matchStatus = CategoryMatchStatus.NEW_CATEGORY,
                        createAsNew = true,
                        isSelected = true
                    )
                )
            }
            if (!subcatName.isNullOrBlank() && categories.none { it.name.equals(subcatName, ignoreCase = true) && it.parentCategoryName.equals(catName, ignoreCase = true) }) {
                categories.add(
                    ImportCategoryItem(
                        name = subcatName,
                        parentCategoryName = catName,
                        matchStatus = CategoryMatchStatus.NEW_CATEGORY,
                        createAsNew = true,
                        isSelected = true
                    )
                )
            }
        }

        Log.d(TAG, "Parsed CSV: ${accounts.size} accounts, ${categories.size} categories, ${mappedTransactions.size} transactions")

        ParsedImportData(
            source = ImportSource.CSV,
            accounts = accounts,
            categories = categories,
            subscriptions = emptyList(),
            transactions = mappedTransactions,
            rawMetadata = "CSV Import (${mappedTransactions.size} transactions)"
        )
    }
}
