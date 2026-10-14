package com.reddy.vittify.data.backup.importwizard

import androidx.annotation.StringRes
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.utils.NanoId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Supported import/restore sources
 */
enum class ImportSource(
    val title: String,
    val description: String,
    val fileExtensions: List<String>,
    val mimeTypes: List<String>,
    val isBeta: Boolean = false
) {
    VITTIFY(
        title = "Vittify Backup",
        description = "Restore from an encrypted or standard Vittify JSON/ZIP backup",
        fileExtensions = listOf("json", "zip"),
        mimeTypes = listOf("application/json", "application/zip", "application/octet-stream", "*/*"),
        isBeta = false
    ),
    EXCEL(
        title = "Excel Spreadsheet",
        description = "Import multi-sheet .xlsx workbook (Accounts, Categories, Transactions, Subscriptions)",
        fileExtensions = listOf("xlsx"),
        mimeTypes = listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "*/*"),
        isBeta = true
    ),
    CASHEW_SQLITE(
        title = "Cashew Backup (SQLite)",
        description = "Import wallets, categories, and transactions from Cashew backup .sqlite database",
        fileExtensions = listOf("sqlite", "db", "sql"),
        mimeTypes = listOf("application/x-sqlite3", "application/vnd.sqlite3", "application/sql", "application/octet-stream", "*/*"),
        isBeta = true
    ),
    CSV(
        title = "CSV File",
        description = "Import transactions from standard CSV format",
        fileExtensions = listOf("csv"),
        mimeTypes = listOf("text/csv", "text/comma-separated-values", "application/csv", "*/*"),
        isBeta = true
    )
}

/**
 * Match status for imported categories compared to in-app categories
 */
enum class CategoryMatchStatus {
    EXACT_MATCH,
    SUGGESTED_MATCH,
    NEW_CATEGORY
}

/**
 * Parsed Account model for preview & selection
 */
data class ImportAccountItem(
    val id: String = NanoId.generate(),
    val originalId: String? = null,
    val bankName: String,
    val accountLast4: String,
    val accountName: String? = null,
    val balance: BigDecimal = BigDecimal.ZERO,
    val currency: String = "INR",
    val isCreditCard: Boolean = false,
    val isWallet: Boolean = false,
    val color: String = "#33B5E5",
    val isSelected: Boolean = true
) {
    val displayTitle: String
        get() = accountName?.takeIf { it.isNotBlank() } ?: bankName

    val displaySubtitle: String
        get() = buildString {
            if (accountLast4.isNotBlank()) append("•••• ").append(accountLast4)
            if (isCreditCard) append(" • Credit Card")
            else if (isWallet) append(" • Wallet")
        }
}

/**
 * Parsed Category model for preview & mapping
 */
data class ImportCategoryItem(
    val id: Long = 0,
    val originalId: String? = null,
    val name: String,
    val isIncome: Boolean = false,
    val color: String = "#757575",
    val iconName: String = "",
    val parentCategoryName: String? = null,
    val matchStatus: CategoryMatchStatus = CategoryMatchStatus.NEW_CATEGORY,
    val mappedCategoryId: Long? = null,
    val mappedCategoryName: String? = null,
    val mappedSubcategoryId: Long? = null,
    val mappedSubcategoryName: String? = null,
    val createAsSubcategoryUnderParent: Boolean = false,
    val createAsNew: Boolean = true,
    val isSelected: Boolean = true
)

/**
 * Parsed Subscription model for preview & selection
 */
data class ImportSubscriptionItem(
    val id: Long = 0,
    val merchantName: String,
    val amount: BigDecimal,
    val billingCycle: String = "MONTHLY",
    val nextPaymentDate: LocalDate? = null,
    val categoryName: String? = null,
    val bankName: String? = null,
    val currency: String = "INR",
    val isSelected: Boolean = true
)

/**
 * Parsed Transaction model for preview & selection
 */
data class ImportTransactionItem(
    val id: Long = 0,
    val merchantName: String,
    val amount: BigDecimal,
    val transactionType: TransactionType,
    val dateTime: LocalDateTime,
    val categoryName: String,
    val subcategoryName: String? = null,
    val originalBankName: String? = null,
    val originalAccountLast4: String? = null,
    val mappedAccountId: String? = null,
    val toBankName: String? = null,
    val toAccountLast4: String? = null,
    val mappedToAccountId: String? = null,
    val targetAmount: BigDecimal? = null,
    val targetCurrency: String? = null,
    val currency: String = "INR",
    val description: String? = null,
    val isSelected: Boolean = true
)

/**
 * Unified container for all parsed items from any source
 */
data class ParsedImportData(
    val source: ImportSource,
    val accounts: List<ImportAccountItem> = emptyList(),
    val categories: List<ImportCategoryItem> = emptyList(),
    val subscriptions: List<ImportSubscriptionItem> = emptyList(),
    val transactions: List<ImportTransactionItem> = emptyList(),
    val rawMetadata: String? = null
)

/**
 * Helper to track item range selection (selecting start and end, then clicking "Select Range")
 */
data class RangeSelectionTracker(
    val firstIndex: Int? = null,
    val secondIndex: Int? = null
) {
    val isRangeReady: Boolean
        get() = firstIndex != null && secondIndex != null && firstIndex != secondIndex

    val rangeIndices: IntRange?
        get() = if (isRangeReady) {
            val min = minOf(firstIndex!!, secondIndex!!)
            val max = maxOf(firstIndex, secondIndex)
            min..max
        } else null

    val rangeCount: Int
        get() = rangeIndices?.let { it.last - it.first + 1 } ?: 0

    fun withToggled(index: Int): RangeSelectionTracker {
        return when {
            firstIndex == null -> copy(firstIndex = index)
            firstIndex == index -> copy(firstIndex = null)
            secondIndex == null -> copy(secondIndex = index)
            secondIndex == index -> copy(secondIndex = null)
            else -> copy(firstIndex = secondIndex, secondIndex = index)
        }
    }

    fun reset(): RangeSelectionTracker = RangeSelectionTracker()
}
