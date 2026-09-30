package com.reddy.vittify.presentation.ui.features.analytics

import com.reddy.vittify.presentation.ui.components.BalancePoint
import com.reddy.vittify.presentation.common.TimePeriod
import com.reddy.vittify.presentation.common.TransactionTypeFilter
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Internal state for combining all filter parameters.
 * Used in reactive Flow to trigger data reload when any filter changes.
 */
data class FilterState(
    val period: TimePeriod,
    val customRange: Pair<LocalDate, LocalDate>?,
    val typeFilter: Set<TransactionTypeFilter>,
    val currency: String?
)

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val categoryBreakdown: List<CategoryData> = emptyList(),
    val topMerchants: List<MerchantData> = emptyList(),
    val transactionCount: Int = 0,
    val averageAmount: BigDecimal = BigDecimal.ZERO,
    val topCategory: String? = null,
    val topCategoryPercentage: Float = 0f,
    val currency: String = "INR",
    val baseCurrency: String = "INR",
    val isLoading: Boolean = true,
    val spendingTrend: List<BalancePoint> = emptyList(),
    val convertedMerchantAmounts: Map<String, BigDecimal> = emptyMap(),
    val isCoupleTrackingEnabled: Boolean = false,
    val activeViewMode: com.reddy.vittify.data.sync.ViewMode = com.reddy.vittify.data.sync.ViewMode.PERSONAL,
    val partnerName: String? = null,
    val myDeviceId: String = ""
)

data class CategoryData(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

data class MerchantData(
    val name: String,
    val amount: BigDecimal,
    val transactionCount: Int,
    val isSubscription: Boolean,
    val categoryName: String? = null,
    val subcategoryName: String? = null,
    val accountIconName: String? = null
)
