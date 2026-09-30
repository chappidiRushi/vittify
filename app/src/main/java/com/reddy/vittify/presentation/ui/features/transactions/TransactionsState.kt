package com.reddy.vittify.presentation.ui.features.transactions

import androidx.annotation.StringRes
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.presentation.common.TimePeriod
import com.reddy.vittify.presentation.common.TransactionTypeFilter
import java.math.BigDecimal

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val groupedTransactions: Map<DateGroup, List<TransactionEntity>> = emptyMap(),
    val convertedAmounts: Map<Long, BigDecimal> = emptyMap(),
    val splitTransactionIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isCoupleTrackingEnabled: Boolean = false,
    val activeViewMode: com.reddy.vittify.data.sync.ViewMode = com.reddy.vittify.data.sync.ViewMode.PERSONAL,
    val partnerName: String? = null,
    val myDeviceId: String = "",
    val userName: String = "User"
)

data class FilterParams(
    val query: String,
    val period: TimePeriod,
    val category: String?,
    val typeFilter: TransactionTypeFilter
)

enum class DateGroup(@StringRes val labelRes: Int) {
    TODAY(R.string.today_lbl),
    YESTERDAY(R.string.yesterday_lbl),
    THIS_WEEK(R.string.this_week_lbl),
    EARLIER(R.string.earlier_lbl)
}

enum class SortOption(@StringRes val labelRes: Int) {
    DATE_NEWEST(R.string.sort_date_newest),
    DATE_OLDEST(R.string.sort_date_oldest),
    AMOUNT_HIGHEST(R.string.sort_amount_highest),
    AMOUNT_LOWEST(R.string.sort_amount_lowest),
    MERCHANT_AZ(R.string.sort_merchant_az),
    MERCHANT_ZA(R.string.sort_merchant_za)
}

data class FilteredTotals(
    val income: BigDecimal = BigDecimal.ZERO,
    val expenses: BigDecimal = BigDecimal.ZERO,
    val credit: BigDecimal = BigDecimal.ZERO,
    val transfer: BigDecimal = BigDecimal.ZERO,
    val investment: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0
)
