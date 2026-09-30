package com.reddy.vittify.presentation.ui.features.subscriptions

import com.reddy.vittify.data.database.entity.SubscriptionEntity
import java.math.BigDecimal

data class SubscriptionsUiState(
    val activeSubscriptions: List<SubscriptionEntity> = emptyList(),
    val totalMonthlyAmount: BigDecimal = BigDecimal.ZERO,
    val totalYearlyAmount: BigDecimal = BigDecimal.ZERO,
    val targetCurrency: String = "INR",
    val isLoading: Boolean = true,
    val lastHiddenSubscription: SubscriptionEntity? = null,
    val selectedSubscription: SubscriptionEntity? = null,
    val convertedAmounts: Map<Long, BigDecimal> = emptyMap(),
    val conversionFailureCount: Int = 0,
    val isCoupleTrackingEnabled: Boolean = false,
    val activeViewMode: com.reddy.vittify.data.sync.ViewMode = com.reddy.vittify.data.sync.ViewMode.PERSONAL,
    val partnerName: String? = null,
    val myDeviceId: String = ""
)
