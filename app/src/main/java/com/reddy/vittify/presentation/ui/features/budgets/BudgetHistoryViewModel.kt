package com.reddy.vittify.presentation.ui.features.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.BudgetEntity
import com.reddy.vittify.data.database.entity.BudgetPeriod
import com.reddy.vittify.data.database.entity.BudgetType
import com.reddy.vittify.data.repository.BudgetRepository
import com.reddy.vittify.presentation.ui.components.BalancePoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class BudgetPeriodHistory(
    val name: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val amount: BigDecimal,
    val spent: BigDecimal,
    val currency: String
) {
    val percentUsed: Float get() = if (amount > BigDecimal.ZERO) {
        (spent.toFloat() / amount.toFloat()).coerceIn(0f, 1.1f)
    } else 0f
    
    val isOverBudget: Boolean get() = spent > amount
}

data class BudgetHistoryUiState(
    val isLoading: Boolean = false,
    val budget: BudgetEntity? = null,
    val periods: List<BudgetPeriodHistory> = emptyList(),
    val chartPoints: List<BalancePoint> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BudgetHistoryViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetHistoryUiState())
    val uiState: StateFlow<BudgetHistoryUiState> = _uiState.asStateFlow()

    fun loadBudgetHistory(budgetId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val budget = budgetRepository.getBudgetById(budgetId)
                if (budget != null) {
                    val periods = calculateHistoricalPeriods(budget)
                    val historyWithSpending = periods.map { period ->
                        val spending = calculateSpendingForPeriod(budget, period.first, period.second)
                        BudgetPeriodHistory(
                            name = formatPeriodName(period.first, period.second, budget.periodType),
                            startDate = period.first,
                            endDate = period.second,
                            amount = budget.amount,
                            spent = spending,
                            currency = budget.currency
                        )
                    }

                    val chartPoints = historyWithSpending.reversed().map {
                        BalancePoint(
                            timestamp = it.startDate,
                            balance = it.spent,
                            currency = it.currency
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            budget = budget,
                            periods = historyWithSpending,
                            chartPoints = chartPoints
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Budget not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load history") }
            }
        }
    }

    private fun calculateHistoricalPeriods(budget: BudgetEntity): List<Pair<LocalDateTime, LocalDateTime>> {
        val periods = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
        val now = LocalDateTime.now()
        val creationDate = budget.createdAt
        
        // If Custom (one-time), only show the original period
        if (budget.periodType == BudgetPeriod.CUSTOM) {
            return listOf(budget.startDate to budget.endDate)
        }

        var currentStart = budget.startDate
        var currentEnd = budget.endDate

        // Add current period
        periods.add(currentStart to currentEnd)

        // Go backwards until creation month
        val creationYearMonth = YearMonth.from(creationDate)
        
        while (true) {
            val prevStart = when (budget.periodType) {
                BudgetPeriod.DAILY -> currentStart.minusDays(1)
                BudgetPeriod.WEEKLY -> currentStart.minusWeeks(1)
                BudgetPeriod.MONTHLY -> currentStart.minusMonths(1)
                BudgetPeriod.YEARLY -> currentStart.minusYears(1)
                else -> break
            }
            
            val prevEnd = when (budget.periodType) {
                BudgetPeriod.DAILY -> prevStart
                BudgetPeriod.WEEKLY -> prevStart.plusDays(6).withHour(23).withMinute(59).withSecond(59)
                BudgetPeriod.MONTHLY -> prevStart.withDayOfMonth(prevStart.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                BudgetPeriod.YEARLY -> prevStart.plusYears(1).minusDays(1).withHour(23).withMinute(59).withSecond(59)
                else -> break
            }

            if (YearMonth.from(prevStart).isBefore(creationYearMonth)) {
                break
            }

            periods.add(prevStart to prevEnd)
            currentStart = prevStart
            currentEnd = prevEnd
            
            // Safety break to prevent infinite loops (back up to 2 years)
            if (periods.size > 24) break
        }

        return periods
    }

    private suspend fun calculateSpendingForPeriod(
        budget: BudgetEntity,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): BigDecimal {
        // Create a temporary budget entity for calculation
        val tempBudget = budget.copy(
            startDate = startDate,
            endDate = endDate
        )
        val spending = budgetRepository.getBudgetWithSpending(tempBudget)
        return spending.currentSpending
    }

    private fun formatPeriodName(start: LocalDateTime, end: LocalDateTime, periodType: BudgetPeriod): String {
        val now = LocalDateTime.now()
        val isCurrentPeriod = (now.isAfter(start) || now.isEqual(start)) && (now.isBefore(end) || now.isEqual(end))
        
        if (isCurrentPeriod) return "Current Period"
        
        return when (periodType) {
            BudgetPeriod.MONTHLY -> {
                if (start.year == now.year) {
                    start.format(DateTimeFormatter.ofPattern("MMMM d"))
                } else {
                    start.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                }
            }
            BudgetPeriod.WEEKLY -> {
                "${start.format(DateTimeFormatter.ofPattern("MMM d"))} - ${end.format(DateTimeFormatter.ofPattern("MMM d"))}"
            }
            BudgetPeriod.DAILY -> {
                start.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
            BudgetPeriod.YEARLY -> {
                start.format(DateTimeFormatter.ofPattern("yyyy"))
            }
            BudgetPeriod.CUSTOM -> {
                "${start.format(DateTimeFormatter.ofPattern("MMM d"))} - ${end.format(DateTimeFormatter.ofPattern("MMM d"))}"
            }
        }
    }
}
