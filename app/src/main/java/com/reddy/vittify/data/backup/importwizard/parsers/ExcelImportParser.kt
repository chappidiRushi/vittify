package com.reddy.vittify.data.backup.importwizard.parsers

import android.content.Context
import android.net.Uri
import android.util.Log
import com.reddy.vittify.data.backup.importwizard.CategoryMatchStatus
import com.reddy.vittify.data.backup.importwizard.ImportAccountItem
import com.reddy.vittify.data.backup.importwizard.ImportCategoryItem
import com.reddy.vittify.data.backup.importwizard.ImportSource
import com.reddy.vittify.data.backup.importwizard.ImportSubscriptionItem
import com.reddy.vittify.data.backup.importwizard.ImportTransactionItem
import com.reddy.vittify.data.backup.importwizard.ParsedImportData
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.utils.NanoId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelImportParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ExcelImportParser"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    suspend fun parse(uri: Uri): ParsedImportData = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Failed to open Excel file at $uri")

        val workbook = inputStream.use { stream ->
            WorkbookFactory.create(stream)
        }

        try {
            val accounts = mutableListOf<ImportAccountItem>()
            val categories = mutableListOf<ImportCategoryItem>()
            val subscriptions = mutableListOf<ImportSubscriptionItem>()
            val transactions = mutableListOf<ImportTransactionItem>()

            // 1. Parse Bank Accounts Sheet
            val accountsSheet = workbook.findSheet("bank accounts", "accounts", "wallets")
            if (accountsSheet != null) {
                parseAccountsSheet(accountsSheet, accounts)
            }

            // 2. Parse Categories Sheet if present
            val categoriesSheet = workbook.findSheet("categories", "category list")
            if (categoriesSheet != null) {
                parseCategoriesSheet(categoriesSheet, categories)
            }

            // 3. Parse Subcategories Sheet if present
            val subcategoriesSheet = workbook.findSheet("subcategories", "subcategory list", "sub-categories")
            if (subcategoriesSheet != null) {
                parseSubcategoriesSheet(subcategoriesSheet, categories)
            }

            // 4. Parse Subscriptions Sheet
            val subscriptionsSheet = workbook.findSheet("subscriptions", "recurring")
            if (subscriptionsSheet != null) {
                parseSubscriptionsSheet(subscriptionsSheet, subscriptions)
            }

            // 5. Parse Transactions Sheet
            val transactionsSheet = workbook.findSheet("transactions", "ledger", "expenses")
                ?: if (workbook.numberOfSheets > 1) workbook.getSheetAt(workbook.numberOfSheets - 1) else workbook.getSheetAt(0)

            if (transactionsSheet != null) {
                parseTransactionsSheet(transactionsSheet, transactions, accounts)
            }

            // 4. If accounts were not explicitly in an accounts sheet, infer from transactions
            if (accounts.isEmpty()) {
                val inferredBankNames = transactions.mapNotNull { it.originalBankName }.filter { it.isNotBlank() }.distinct()
                inferredBankNames.forEach { bankName ->
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
            }

            // If still no accounts, add a default Cash Account
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

            // Link transactions to account IDs
            val accountMap = accounts.associateBy { it.bankName.lowercase(Locale.ROOT) }
            val linkedTransactions = transactions.map { txn ->
                val mappedAcc = txn.originalBankName?.lowercase(Locale.ROOT)?.let { accountMap[it] } ?: accounts.firstOrNull()
                txn.copy(mappedAccountId = mappedAcc?.id)
            }

            // 5. Infer categories from transactions & subscriptions if no explicit sheet
            val foundCategories = (transactions.map { it.categoryName to it.subcategoryName } +
                    subscriptions.mapNotNull { it.categoryName?.let { c -> c to null } })
                .filter { it.first.isNotBlank() }
                .distinct()

            foundCategories.forEach { (catName, subcatName) ->
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

            Log.d(TAG, "Parsed Excel: ${accounts.size} accounts, ${categories.size} categories, ${subscriptions.size} subscriptions, ${transactions.size} transactions")

            ParsedImportData(
                source = ImportSource.EXCEL,
                accounts = accounts,
                categories = categories,
                subscriptions = subscriptions,
                transactions = linkedTransactions,
                rawMetadata = "Excel Workbook (${transactions.size} transactions)"
            )
        } finally {
            workbook.close()
        }
    }

    private fun parseAccountsSheet(sheet: Sheet, outAccounts: MutableList<ImportAccountItem>) {
        val headerRow = sheet.getRow(0) ?: return
        val headers = (0 until headerRow.lastCellNum).associate { col ->
            headerRow.getCell(col).asString().lowercase(Locale.ROOT).trim() to col
        }

        val bankCol = headers["bank name"] ?: headers["bank"] ?: headers["institution"] ?: 0
        val accNameCol = headers["account name / last 4"] ?: headers["account name"] ?: headers["account"] ?: 1
        val balanceCol = headers["balance"] ?: headers["initial balance"] ?: 2
        val currencyCol = headers["currency"] ?: 3
        val typeCol = headers["account type"] ?: headers["type"] ?: 4

        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val bankName = row.getCell(bankCol).asString().trim()
            if (bankName.isBlank()) continue

            val accName = row.getCell(accNameCol).asString().trim()
            val balance = row.getCell(balanceCol).asBigDecimal()
            val currency = row.getCell(currencyCol).asString().trim().ifBlank { "INR" }
            val typeStr = row.getCell(typeCol).asString().uppercase(Locale.ROOT)

            val isCredit = typeStr.contains("CREDIT")
            val isWallet = typeStr.contains("WALLET") || typeStr.contains("CASH")

            outAccounts.add(
                ImportAccountItem(
                    id = NanoId.generate(),
                    bankName = bankName,
                    accountLast4 = if (accName.length <= 4 && accName.all { it.isDigit() }) accName else "",
                    accountName = if (accName.isNotBlank()) accName else bankName,
                    balance = balance,
                    currency = currency,
                    isCreditCard = isCredit,
                    isWallet = isWallet,
                    isSelected = true
                )
            )
        }
    }

    private fun parseSubscriptionsSheet(sheet: Sheet, outSubscriptions: MutableList<ImportSubscriptionItem>) {
        val headerRow = sheet.getRow(0) ?: return
        val headers = (0 until headerRow.lastCellNum).associate { col ->
            headerRow.getCell(col).asString().lowercase(Locale.ROOT).trim() to col
        }

        val merchantCol = headers["merchant"] ?: headers["title"] ?: headers["service"] ?: 0
        val amountCol = headers["amount"] ?: 1
        val cycleCol = headers["billing cycle"] ?: headers["cycle"] ?: headers["frequency"] ?: 2
        val dateCol = headers["next payment date"] ?: headers["next date"] ?: headers["due date"] ?: 3
        val catCol = headers["category"] ?: 4
        val bankCol = headers["bank"] ?: headers["account"] ?: 5

        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val merchant = row.getCell(merchantCol).asString().trim()
            if (merchant.isBlank()) continue

            val amount = row.getCell(amountCol).asBigDecimal()
            val cycle = row.getCell(cycleCol).asString().uppercase(Locale.ROOT).ifBlank { "MONTHLY" }
            val nextDate = row.getCell(dateCol).asLocalDate()
            val category = row.getCell(catCol).asString().trim()
            val bank = row.getCell(bankCol).asString().trim()

            outSubscriptions.add(
                ImportSubscriptionItem(
                    merchantName = merchant,
                    amount = amount,
                    billingCycle = cycle,
                    nextPaymentDate = nextDate,
                    categoryName = category.takeIf { it.isNotBlank() },
                    bankName = bank.takeIf { it.isNotBlank() },
                    isSelected = true
                )
            )
        }
    }

    private fun parseCategoriesSheet(sheet: Sheet, outCategories: MutableList<ImportCategoryItem>) {
        val headerRow = sheet.getRow(0) ?: return
        val headers = (0 until headerRow.lastCellNum).associate { col ->
            headerRow.getCell(col).asString().lowercase(Locale.ROOT).trim() to col
        }

        val nameCol = headers["category name"] ?: headers["name"] ?: headers["category"] ?: 0
        val typeCol = headers["type"] ?: 1
        val colorCol = headers["color"] ?: 2

        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val name = row.getCell(nameCol).asString().trim()
            if (name.isBlank()) continue

            val typeStr = row.getCell(typeCol).asString().uppercase(Locale.ROOT)
            val colorStr = row.getCell(colorCol).asString().trim().ifBlank { "#757575" }
            val isIncome = typeStr.contains("INCOME")

            if (outCategories.none { it.name.equals(name, ignoreCase = true) && it.parentCategoryName == null }) {
                outCategories.add(
                    ImportCategoryItem(
                        name = name,
                        isIncome = isIncome,
                        color = colorStr,
                        parentCategoryName = null,
                        createAsNew = true,
                        isSelected = true
                    )
                )
            }
        }
    }

    private fun parseSubcategoriesSheet(sheet: Sheet, outCategories: MutableList<ImportCategoryItem>) {
        val headerRow = sheet.getRow(0) ?: return
        val headers = (0 until headerRow.lastCellNum).associate { col ->
            headerRow.getCell(col).asString().lowercase(Locale.ROOT).trim() to col
        }

        val catCol = headers["category"] ?: headers["parent"] ?: 0
        val subcatCol = headers["subcategory name"] ?: headers["subcategory"] ?: headers["name"] ?: 1

        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val parentName = row.getCell(catCol).asString().trim()
            val subcatName = row.getCell(subcatCol).asString().trim()
            if (subcatName.isBlank() || parentName.isBlank()) continue

            if (outCategories.none { it.name.equals(parentName, ignoreCase = true) && it.parentCategoryName == null }) {
                outCategories.add(
                    ImportCategoryItem(
                        name = parentName,
                        parentCategoryName = null,
                        createAsNew = true,
                        isSelected = true
                    )
                )
            }

            if (outCategories.none { it.name.equals(subcatName, ignoreCase = true) && it.parentCategoryName.equals(parentName, ignoreCase = true) }) {
                outCategories.add(
                    ImportCategoryItem(
                        name = subcatName,
                        parentCategoryName = parentName,
                        createAsNew = true,
                        isSelected = true
                    )
                )
            }
        }
    }

    private fun parseTransactionsSheet(sheet: Sheet, outTransactions: MutableList<ImportTransactionItem>, accounts: List<ImportAccountItem>) {
        val headerRow = sheet.getRow(0) ?: return
        val headers = (0 until headerRow.lastCellNum).associate { col ->
            headerRow.getCell(col).asString().lowercase(Locale.ROOT).trim() to col
        }

        val dateCol = headers["date"] ?: 0
        val timeCol = headers["time"] ?: 1
        val merchantCol = headers["merchant"] ?: headers["merchant / title"] ?: headers["title"] ?: headers["payee"] ?: 2
        val catCol = headers["category"] ?: 3
        val subcatCol = headers["subcategory"] ?: 4
        val typeCol = headers["type"] ?: headers["transaction type"] ?: 5
        val amountCol = headers["amount"] ?: 6
        val bankCol = headers["bank"] ?: headers["bank / account"] ?: 7
        val noteCol = headers["note"] ?: headers["description"] ?: 8

        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val merchant = row.getCell(merchantCol).asString().trim()
            val amount = row.getCell(amountCol).asBigDecimal()
            if (merchant.isBlank() && amount == BigDecimal.ZERO) continue

            val dateCell = row.getCell(dateCol)
            val timeCell = row.getCell(timeCol)
            val dateTime = parseDateTime(dateCell, timeCell)

            val category = row.getCell(catCol).asString().trim().ifBlank { "Other" }
            val subcategory = row.getCell(subcatCol).asString().trim().takeIf { it.isNotBlank() }
            val typeStr = row.getCell(typeCol).asString().uppercase(Locale.ROOT).trim()
            val bank = row.getCell(bankCol).asString().trim().takeIf { it.isNotBlank() }
            val note = row.getCell(noteCol).asString().trim().takeIf { it.isNotBlank() }

            val txnType = when {
                typeStr.contains("INCOME") -> TransactionType.INCOME
                typeStr.contains("TRANSFER") -> TransactionType.TRANSFER
                typeStr.contains("CREDIT") -> TransactionType.CREDIT
                typeStr.contains("INVEST") -> TransactionType.INVESTMENT
                typeStr.contains("BALANCE") -> TransactionType.BALANCE_UPDATE
                else -> TransactionType.EXPENSE
            }

            val finalMerchant = if (merchant.isNotBlank()) {
                merchant
            } else {
                subcategory?.takeIf { it.isNotBlank() } ?: category.ifBlank { "Transaction" }
            }

            outTransactions.add(
                ImportTransactionItem(
                    merchantName = finalMerchant,
                    amount = amount.abs(),
                    transactionType = txnType,
                    dateTime = dateTime,
                    categoryName = category,
                    subcategoryName = subcategory,
                    originalBankName = bank,
                    originalAccountLast4 = "",
                    mappedAccountId = null,
                    description = note,
                    isSelected = true
                )
            )
        }
    }

    private fun parseDateTime(dateCell: Cell?, timeCell: Cell?): LocalDateTime {
        if (dateCell != null && dateCell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
            val date = dateCell.dateCellValue
            return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        }

        val dateStr = dateCell?.asString()?.trim() ?: ""
        val timeStr = timeCell?.asString()?.trim() ?: "12:00:00"

        return try {
            if (dateStr.isNotBlank()) {
                val cleanDate = dateStr.take(10)
                val cleanTime = if (timeStr.length >= 5) timeStr.take(8) else "12:00:00"
                LocalDateTime.parse("${cleanDate}T${cleanTime}")
            } else {
                LocalDateTime.now()
            }
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }

    private fun Sheet.findSheet(vararg names: String): Sheet? = null // helper below
    private fun org.apache.poi.ss.usermodel.Workbook.findSheet(vararg names: String): Sheet? {
        for (name in names) {
            for (i in 0 until numberOfSheets) {
                if (getSheetName(i).trim().equals(name, ignoreCase = true)) {
                    return getSheetAt(i)
                }
            }
        }
        return null
    }

    private fun Cell?.asString(): String {
        if (this == null) return ""
        return when (cellType) {
            CellType.STRING -> stringCellValue ?: ""
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(this)) {
                    dateCellValue.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
                } else {
                    val num = numericCellValue
                    if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
                }
            }
            CellType.BOOLEAN -> booleanCellValue.toString()
            CellType.FORMULA -> {
                try { stringCellValue } catch (e: Exception) { numericCellValue.toString() }
            }
            else -> ""
        }
    }

    private fun Cell?.asBigDecimal(): BigDecimal {
        if (this == null) return BigDecimal.ZERO
        return try {
            when (cellType) {
                CellType.NUMERIC -> BigDecimal.valueOf(numericCellValue)
                CellType.STRING -> BigDecimal(stringCellValue.replace(",", "").trim())
                CellType.FORMULA -> BigDecimal.valueOf(numericCellValue)
                else -> BigDecimal.ZERO
            }
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun Cell?.asLocalDate(): LocalDate? {
        if (this == null) return null
        return try {
            if (cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(this)) {
                dateCellValue.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            } else {
                val str = asString().trim()
                if (str.isNotBlank()) LocalDate.parse(str.take(10)) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
