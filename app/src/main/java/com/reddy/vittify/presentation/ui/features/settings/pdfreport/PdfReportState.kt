package com.reddy.vittify.presentation.ui.features.settings.pdfreport

import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import java.io.File
import java.time.LocalDate

data class PdfReportUiState(
    val accounts: List<AccountBalanceEntity> = emptyList(),
    /** Keys are "bankName|accountLast4" */
    val selectedAccountKeys: Set<String> = emptySet(),
    val dateRangeOption: DateRangeOption = DateRangeOption.CURRENT_MONTH,
    val customStartDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val customEndDate: LocalDate = LocalDate.now(),
    val isGenerating: Boolean = false,
    val generatedFile: File? = null,
    val errorMessage: String? = null,
)

enum class DateRangeOption(val label: String) {
    CURRENT_MONTH("Current Month"),
    PREVIOUS_MONTH("Previous Month"),
    CURRENT_YEAR("Current Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}

fun AccountBalanceEntity.accountKey(): String = "$bankName|$accountLast4"
