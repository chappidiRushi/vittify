package com.reddy.vittify.presentation.ui.features.home

import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.nlp.NlpParsingMode
import com.reddy.vittify.data.repository.BudgetWithSpending
import com.reddy.vittify.presentation.ui.components.BalancePoint
import java.math.BigDecimal

data class HomeUiState(
    val currentMonthTotal: BigDecimal = BigDecimal.ZERO,
    val currentYearTotal: BigDecimal = BigDecimal.ZERO,
    val currentMonthIncome: BigDecimal = BigDecimal.ZERO,
    val currentMonthExpenses: BigDecimal = BigDecimal.ZERO,
    val currentYearExpenses: BigDecimal = BigDecimal.ZERO,
    val currentMonthCreditCard: BigDecimal = BigDecimal.ZERO,
    val currentMonthTransfer: BigDecimal = BigDecimal.ZERO,
    val currentMonthInvestment: BigDecimal = BigDecimal.ZERO,
    val lastMonthTotal: BigDecimal = BigDecimal.ZERO,
    val lastMonthIncome: BigDecimal = BigDecimal.ZERO,
    val lastMonthExpenses: BigDecimal = BigDecimal.ZERO,
    val monthlyChange: BigDecimal = BigDecimal.ZERO,
    val monthlyChangePercent: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val upcomingSubscriptions: List<SubscriptionEntity> = emptyList(),
    val upcomingSubscriptionsTotal: BigDecimal = BigDecimal.ZERO,
    val upcomingSubscriptionsCurrency: String = "INR",
    val accountBalances: List<AccountBalanceEntity> = emptyList(),
    val creditCards: List<AccountBalanceEntity> = emptyList(),
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val totalAvailableCredit: BigDecimal = BigDecimal.ZERO,
    val selectedCurrency: String = "INR",
    val availableCurrencies: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val showBreakdownDialog: Boolean = false,
    val userName: String = "User",
    val profileImageUri: Uri? = null,
    val profileBackgroundColor: Color = Color.Transparent,
    val unreadUpdatesCount: Int = 0,
    val activeBudgets: List<BudgetWithSpending> = emptyList(),
    val balanceHistory: List<BalancePoint> = emptyList(),
    val transactionHeatmap: Map<java.time.LocalDate, Int> = emptyMap(),
    val convertedAmounts: Map<Long, BigDecimal> = emptyMap(),
    val splitTransactionIds: Set<Long> = emptySet(),
    val baseCurrency: String = "INR",
    val nlpIsProcessing: Boolean = false,
    val nlpParsingMode: NlpParsingMode? = null,
    val nlpError: String? = null,
    val isCoupleTrackingEnabled: Boolean = false,
    val activeViewMode: com.reddy.vittify.data.sync.ViewMode = com.reddy.vittify.data.sync.ViewMode.COMBINED,
    val partnerName: String? = null,
    val partnerUserId: String? = null,
    val myUserId: String = "",
)
