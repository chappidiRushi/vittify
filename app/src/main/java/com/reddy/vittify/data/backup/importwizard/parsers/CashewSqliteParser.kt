package com.reddy.vittify.data.backup.importwizard.parsers

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
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
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Parser for Cashew expense tracker SQLite backup files (.sqlite)
 */
@Singleton
class CashewSqliteParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CashewSqliteParser"
    }

    suspend fun parse(uri: Uri): ParsedImportData = withContext(Dispatchers.IO) {
        val tempDbFile = File(context.cacheDir, "cashew_import_${System.currentTimeMillis()}.sqlite")
        try {
            // Copy uri stream to temp file
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempDbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")

            val db = SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            try {
                // 1. Wallets -> Accounts (with precalculated balances from transactions)
                val walletBalances = mutableMapOf<String, BigDecimal>()
                try {
                    db.rawQuery("SELECT wallet_fk, sum(amount) as total_balance FROM transactions GROUP BY wallet_fk", null).use { cursor ->
                        val wFkIdx = cursor.getColumnIndex("wallet_fk")
                        val sumIdx = cursor.getColumnIndex("total_balance")
                        while (cursor.moveToNext()) {
                            val wFk = cursor.getString(wFkIdx) ?: continue
                            val sum = cursor.getDouble(sumIdx)
                            walletBalances[wFk] = BigDecimal.valueOf(sum).setScale(2, java.math.RoundingMode.HALF_UP)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error calculating wallet balances", e)
                }

                val accounts = mutableListOf<ImportAccountItem>()
                val walletIdToAccountMap = mutableMapOf<String, ImportAccountItem>()

                db.rawQuery(
                    "SELECT wallet_pk, name, colour, currency FROM wallets WHERE archived = 0 ORDER BY `order` ASC",
                    null
                ).use { cursor ->
                    val pkIndex = cursor.getColumnIndex("wallet_pk")
                    val nameIndex = cursor.getColumnIndex("name")
                    val colourIndex = cursor.getColumnIndex("colour")
                    val currencyIndex = cursor.getColumnIndex("currency")

                    while (cursor.moveToNext()) {
                        val walletPk = cursor.getString(pkIndex) ?: continue
                        val name = cursor.getString(nameIndex) ?: "Wallet"
                        val rawColour = cursor.getString(colourIndex)
                        val rawCurrency = cursor.getString(currencyIndex)

                        val hexColor = parseCashewColor(rawColour)
                        val currency = rawCurrency?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "INR"
                        val balance = walletBalances[walletPk] ?: BigDecimal.ZERO

                        val account = ImportAccountItem(
                            id = NanoId.generate(),
                            originalId = walletPk,
                            bankName = name,
                            accountLast4 = "",
                            accountName = name,
                            balance = balance,
                            currency = currency,
                            color = hexColor,
                            isSelected = true
                        )
                        accounts.add(account)
                        walletIdToAccountMap[walletPk] = account
                    }
                }

                // If no wallets were found, create a default "Main Wallet"
                if (accounts.isEmpty()) {
                    val defaultAccount = ImportAccountItem(
                        id = NanoId.generate(),
                        originalId = "0",
                        bankName = "Cashew Wallet",
                        accountLast4 = "",
                        accountName = "Cashew Wallet",
                        balance = BigDecimal.ZERO,
                        currency = "INR",
                        isSelected = true
                    )
                    accounts.add(defaultAccount)
                    walletIdToAccountMap["0"] = defaultAccount
                }

                // 2. Categories & Subcategories
                val rawCategories = mutableListOf<RawCashewCategory>()
                val categoryMap = mutableMapOf<String, RawCashewCategory>()

                db.rawQuery(
                    "SELECT category_pk, name, colour, icon_name, income, main_category_pk FROM categories WHERE archived = 0 ORDER BY `order` ASC",
                    null
                ).use { cursor ->
                    val pkIdx = cursor.getColumnIndex("category_pk")
                    val nameIdx = cursor.getColumnIndex("name")
                    val colourIdx = cursor.getColumnIndex("colour")
                    val iconIdx = cursor.getColumnIndex("icon_name")
                    val incomeIdx = cursor.getColumnIndex("income")
                    val mainIdx = cursor.getColumnIndex("main_category_pk")

                    while (cursor.moveToNext()) {
                        val pk = cursor.getString(pkIdx) ?: continue
                        val name = cursor.getString(nameIdx) ?: "Category"
                        val colour = cursor.getString(colourIdx)
                        val iconName = cursor.getString(iconIdx) ?: ""
                        val income = if (incomeIdx != -1) cursor.getInt(incomeIdx) == 1 else false
                        val mainPk = if (mainIdx != -1) cursor.getString(mainIdx)?.takeIf { it.isNotBlank() } else null

                        val item = RawCashewCategory(pk, name, colour, iconName, income, mainPk)
                        rawCategories.add(item)
                        categoryMap[pk] = item
                    }
                }

                val categories = rawCategories.map { raw ->
                    val parentName = raw.mainCategoryPk?.let { categoryMap[it]?.name }
                    ImportCategoryItem(
                        originalId = raw.categoryPk,
                        name = raw.name,
                        isIncome = raw.income,
                        color = parseCashewColor(raw.colour),
                        iconName = raw.iconName,
                        parentCategoryName = parentName,
                        matchStatus = CategoryMatchStatus.NEW_CATEGORY,
                        createAsNew = true,
                        isSelected = true
                    )
                }

                // 3. Transactions & Self-Transfers
                val rawTransactions = mutableListOf<RawCashewTransaction>()

                db.rawQuery(
                    """
                    SELECT transaction_pk, paired_transaction_fk, name, amount, note, category_fk, sub_category_fk, wallet_fk, income, date_created 
                    FROM transactions 
                    ORDER BY date_created DESC
                    """.trimIndent(),
                    null
                ).use { cursor ->
                    val pkIdx = cursor.getColumnIndex("transaction_pk")
                    val pairedIdx = cursor.getColumnIndex("paired_transaction_fk")
                    val nameIdx = cursor.getColumnIndex("name")
                    val amountIdx = cursor.getColumnIndex("amount")
                    val noteIdx = cursor.getColumnIndex("note")
                    val catIdx = cursor.getColumnIndex("category_fk")
                    val subCatIdx = cursor.getColumnIndex("sub_category_fk")
                    val walletIdx = cursor.getColumnIndex("wallet_fk")
                    val incomeIdx = cursor.getColumnIndex("income")
                    val dateIdx = cursor.getColumnIndex("date_created")

                    while (cursor.moveToNext()) {
                        val pk = cursor.getString(pkIdx) ?: continue
                        val pairedFk = if (pairedIdx != -1) cursor.getString(pairedIdx)?.takeIf { it.isNotBlank() } else null
                        val rawName = cursor.getString(nameIdx)?.trim()
                        val rawAmount = cursor.getDouble(amountIdx)
                        val note = cursor.getString(noteIdx)
                        val catFk = cursor.getString(catIdx)
                        val subCatFk = if (subCatIdx != -1) cursor.getString(subCatIdx) else null
                        val walletFk = cursor.getString(walletIdx)
                        val isIncome = if (incomeIdx != -1) cursor.getInt(incomeIdx) == 1 else rawAmount > 0
                        val dateEpochSec = cursor.getLong(dateIdx)

                        rawTransactions.add(
                            RawCashewTransaction(
                                pk = pk,
                                pairedFk = pairedFk,
                                name = rawName,
                                amount = rawAmount,
                                note = note,
                                categoryFk = catFk,
                                subCategoryFk = subCatFk,
                                walletFk = walletFk,
                                income = isIncome,
                                dateEpochSec = dateEpochSec
                            )
                        )
                    }
                }

                val transactions = mutableListOf<ImportTransactionItem>()
                val rawTxMap = rawTransactions.associateBy { it.pk }
                val processedPks = mutableSetOf<String>()

                rawTransactions.forEach { raw ->
                    if (raw.pk in processedPks) return@forEach

                    val isTransfer = raw.pairedFk != null || rawTransactions.any { it.pairedFk == raw.pk }
                    if (isTransfer) {
                        val paired = raw.pairedFk?.let { rawTxMap[it] } ?: rawTransactions.firstOrNull { it.pairedFk == raw.pk }
                        if (paired != null) {
                            processedPks.add(raw.pk)
                            processedPks.add(paired.pk)

                            // Identify source (outgoing) and destination (incoming)
                            val (sourceTx, destTx) = if (!raw.income || raw.amount < 0) {
                                raw to paired
                            } else {
                                paired to raw
                            }

                            val sourceAccount = walletIdToAccountMap[sourceTx.walletFk] ?: accounts.firstOrNull()
                            val destAccount = walletIdToAccountMap[destTx.walletFk] ?: accounts.firstOrNull()

                            val sourceAmount = BigDecimal.valueOf(abs(sourceTx.amount)).setScale(2, java.math.RoundingMode.HALF_UP)
                            val destAmount = BigDecimal.valueOf(abs(destTx.amount)).setScale(2, java.math.RoundingMode.HALF_UP)

                            val sourceCurrency = sourceAccount?.currency ?: "INR"
                            val destCurrency = destAccount?.currency ?: "INR"

                            val transferTitle = when {
                                !sourceTx.name.isNullOrBlank() && !sourceTx.name.contains("Transfer Out", ignoreCase = true) -> sourceTx.name
                                !destTx.name.isNullOrBlank() && !destTx.name.contains("Transfer In", ignoreCase = true) -> destTx.name
                                sourceAccount != null && destAccount != null -> "${sourceAccount.displayTitle} → ${destAccount.displayTitle}"
                                else -> "Self Transfer"
                            }

                            val dateTime = try {
                                Instant.ofEpochSecond(sourceTx.dateEpochSec)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                            } catch (e: Exception) {
                                LocalDateTime.now()
                            }

                            transactions.add(
                                ImportTransactionItem(
                                    merchantName = transferTitle,
                                    amount = sourceAmount,
                                    transactionType = TransactionType.TRANSFER,
                                    dateTime = dateTime,
                                    categoryName = "Self Transfer",
                                    subcategoryName = null,
                                    originalBankName = sourceAccount?.bankName,
                                    originalAccountLast4 = sourceAccount?.accountLast4 ?: "",
                                    mappedAccountId = sourceAccount?.id,
                                    toBankName = destAccount?.bankName,
                                    toAccountLast4 = destAccount?.accountLast4 ?: "",
                                    mappedToAccountId = destAccount?.id,
                                    targetAmount = destAmount,
                                    targetCurrency = destCurrency,
                                    currency = sourceCurrency,
                                    description = sourceTx.note.takeIf { !it.isNullOrBlank() } ?: destTx.note,
                                    isSelected = true
                                )
                            )
                            return@forEach
                        }
                    }

                    // Standard Non-Transfer Transaction
                    processedPks.add(raw.pk)
                    val matchedAccount = walletIdToAccountMap[raw.walletFk] ?: accounts.firstOrNull()
                    val categoryName = categoryMap[raw.categoryFk]?.name ?: "Other"
                    val subCategoryName = raw.subCategoryFk?.let { categoryMap[it]?.name }

                    val merchantName = if (!raw.name.isNullOrBlank()) {
                        raw.name
                    } else {
                        subCategoryName?.takeIf { it.isNotBlank() } ?: categoryName.takeIf { it.isNotBlank() } ?: "Transaction"
                    }

                    val dateTime = try {
                        Instant.ofEpochSecond(raw.dateEpochSec)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    } catch (e: Exception) {
                        LocalDateTime.now()
                    }

                    val txnType = if (raw.income) TransactionType.INCOME else TransactionType.EXPENSE
                    val amount = BigDecimal.valueOf(abs(raw.amount)).setScale(2, java.math.RoundingMode.HALF_UP)

                    transactions.add(
                        ImportTransactionItem(
                            merchantName = merchantName,
                            amount = amount,
                            transactionType = txnType,
                            dateTime = dateTime,
                            categoryName = categoryName,
                            subcategoryName = subCategoryName,
                            originalBankName = matchedAccount?.bankName,
                            originalAccountLast4 = matchedAccount?.accountLast4 ?: "",
                            mappedAccountId = matchedAccount?.id,
                            currency = matchedAccount?.currency ?: "INR",
                            description = raw.note,
                            isSelected = true
                        )
                    )
                }

                Log.d(TAG, "Parsed Cashew SQLite: ${accounts.size} wallets, ${categories.size} categories, ${transactions.size} transactions")

                ParsedImportData(
                    source = ImportSource.CASHEW_SQLITE,
                    accounts = accounts,
                    categories = categories,
                    subscriptions = emptyList(),
                    transactions = transactions,
                    rawMetadata = "Cashew SQLite Backup (${transactions.size} transactions)"
                )
            } finally {
                db.close()
            }
        } finally {
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }
        }
    }

    private fun parseCashewColor(colourStr: String?): String {
        if (colourStr.isNullOrBlank()) return "#757575"
        return try {
            if (colourStr.startsWith("0x", ignoreCase = true)) {
                // e.g., 0xff66bb6a
                val hex = colourStr.substring(2)
                if (hex.length == 8) {
                    "#" + hex.substring(2) // drop alpha
                } else {
                    "#$hex"
                }
            } else if (colourStr.startsWith("#")) {
                colourStr
            } else {
                "#$colourStr"
            }
        } catch (e: Exception) {
            "#757575"
        }
    }

    private data class RawCashewCategory(
        val categoryPk: String,
        val name: String,
        val colour: String?,
        val iconName: String,
        val income: Boolean,
        val mainCategoryPk: String?
    )

    private data class RawCashewTransaction(
        val pk: String,
        val pairedFk: String?,
        val name: String?,
        val amount: Double,
        val note: String?,
        val categoryFk: String?,
        val subCategoryFk: String?,
        val walletFk: String?,
        val income: Boolean,
        val dateEpochSec: Long
    )
}
