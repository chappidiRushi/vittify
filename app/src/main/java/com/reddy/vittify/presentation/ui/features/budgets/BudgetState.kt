package com.reddy.vittify.presentation.ui.features.budgets

import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.repository.BudgetWithSpending
import com.reddy.vittify.data.repository.CategoryLimitWithSpending
import com.reddy.vittify.data.database.entity.BudgetPeriod
import com.reddy.vittify.data.database.entity.BudgetTrackType
import com.reddy.vittify.data.database.entity.BudgetType
import com.reddy.vittify.data.database.entity.TransactionEntity
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import java.time.format.DateTimeFormatter

data class BudgetUiState(
    val isLoading: Boolean = true,
    val budgets: List<BudgetWithSpending> = emptyList(),
    val selectedBudget: BudgetWithSpending? = null,
    val categoryLimitsWithSpending: List<CategoryLimitWithSpending> = emptyList(),
    val selectedBudgetTransactions: List<TransactionEntity> = emptyList(),
    val allAccounts: List<AccountBalanceEntity> = emptyList(),
    val convertedAmounts: Map<Long, BigDecimal> = emptyMap(),
    val splitTransactionIds: Set<Long> = emptySet(),
    val baseCurrency: String = "INR",
    val error: String? = null,
    val isCoupleTrackingEnabled: Boolean = false,
    val activeViewMode: com.reddy.vittify.data.sync.ViewMode = com.reddy.vittify.data.sync.ViewMode.PERSONAL,
    val partnerName: String? = null,
    val myDeviceId: String = ""
)

data class EditBudgetState(
    val budgetId: Long? = null,
    val name: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val year: Int = LocalDateTime.now().year,
    val month: Int = LocalDateTime.now().monthValue,
    val startDate: LocalDateTime = LocalDateTime.now(),
    val endDate: LocalDateTime = LocalDateTime.now().plusMonths(1).minusDays(1),
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val trackType: BudgetTrackType = BudgetTrackType.ALL_TRANSACTIONS,
    val budgetType: BudgetType = BudgetType.EXPENSE,
    val accountIds: List<String> = emptyList(),
    val color: String = "#4CAF50",
    val currency: String = "INR",
    val categoryLimits: List<EditCategoryLimit> = emptyList()
) {
    val isNewBudget: Boolean get() = budgetId == null
    
    fun getDefaultName(): String {
        return when (periodType) {
            BudgetPeriod.MONTHLY -> {
                val monthName = startDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                "$monthName ${startDate.year}"
            }
            BudgetPeriod.YEARLY -> "${startDate.year} Budget"
            BudgetPeriod.DAILY -> {
                val dateStr = startDate.format(DateTimeFormatter.ofPattern("MMM d"))
                "$dateStr Budget"
            }
            BudgetPeriod.WEEKLY -> {
                val startStr = startDate.format(DateTimeFormatter.ofPattern("MMM d"))
                "Weekly ($startStr)"
            }
            BudgetPeriod.CUSTOM -> "Custom Budget"
        }
    }
}

data class EditCategoryLimit(
    val id: Long? = null,
    val categoryName: String,
    val limitAmount: BigDecimal
)
