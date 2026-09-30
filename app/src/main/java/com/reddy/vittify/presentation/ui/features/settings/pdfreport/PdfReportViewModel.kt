package com.reddy.vittify.presentation.ui.features.settings.pdfreport

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.pdf.TransactionPdfGenerator
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class PdfReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val pdfGenerator: TransactionPdfGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfReportUiState())
    val uiState: StateFlow<PdfReportUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            val accounts = accountBalanceRepository.getAllLatestBalances().first()
            val allKeys = accounts.map { it.accountKey() }.toSet()
            _uiState.update { it.copy(accounts = accounts, selectedAccountKeys = allKeys) }
        }
    }

    fun toggleAccount(key: String) {
        _uiState.update { state ->
            val updated = state.selectedAccountKeys.toMutableSet()
            if (updated.contains(key)) updated.remove(key) else updated.add(key)
            state.copy(selectedAccountKeys = updated)
        }
    }

    fun selectAllAccounts() {
        _uiState.update { it.copy(selectedAccountKeys = it.accounts.map { a -> a.accountKey() }.toSet()) }
    }

    fun deselectAllAccounts() {
        _uiState.update { it.copy(selectedAccountKeys = emptySet()) }
    }

    fun setDateRange(option: DateRangeOption) {
        _uiState.update { it.copy(dateRangeOption = option) }
    }

    fun setCustomStartDate(date: LocalDate) {
        _uiState.update { it.copy(customStartDate = date) }
    }

    fun setCustomEndDate(date: LocalDate) {
        _uiState.update { it.copy(customEndDate = date) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearGeneratedFile() {
        _uiState.update { it.copy(generatedFile = null) }
    }

    fun generatePdf() {
        val state = _uiState.value
        if (state.selectedAccountKeys.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one account.") }
            return
        }
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (startDate, endDate) = resolveDateRange(state)
                val selectedAccounts = state.accounts.filter { state.selectedAccountKeys.contains(it.accountKey()) }

                val allTx = transactionRepository.getTransactionsBetweenDates(startDate, endDate).first()
                val filteredTx = allTx.filter { tx ->
                    selectedAccounts.any { acc ->
                        acc.id == tx.accountId
                    } || tx.accountId == null
                }.filter { it.transactionType != TransactionType.BALANCE_UPDATE }

                val outputDir = File(context.cacheDir, "pdf_reports").also { it.mkdirs() }
                val fileName = "vittify_report_${System.currentTimeMillis()}.pdf"
                val outputFile = File(outputDir, fileName)

                pdfGenerator.generate(
                    outputFile = outputFile,
                    accounts = selectedAccounts,
                    transactions = filteredTx,
                    startDate = startDate,
                    endDate = endDate,
                    reportTitle = "Transaction Report"
                )

                _uiState.update { it.copy(isGenerating = false, generatedFile = outputFile) }
            } catch (e: Exception) {
                Log.e("PdfReportViewModel", "PDF generation failed", e)
                _uiState.update { it.copy(isGenerating = false, errorMessage = "Failed to generate PDF: ${e.message}") }
            }
        }
    }

    fun sharePdf() {
        val file = _uiState.value.generatedFile ?: return
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Vittify Transaction Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("PdfReportViewModel", "Share failed", e)
            _uiState.update { it.copy(errorMessage = "Could not share PDF: ${e.message}") }
        }
    }

    fun downloadPdf() {
        val file = _uiState.value.generatedFile ?: return
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, file.name)
            file.copyTo(destFile, overwrite = true)
            _uiState.update { it.copy(errorMessage = "PDF downloaded to Downloads folder") }
        } catch (e: Exception) {
            Log.e("PdfReportViewModel", "Download failed", e)
            _uiState.update { it.copy(errorMessage = "Could not download PDF: ${e.message}") }
        }
    }

    private fun resolveDateRange(state: PdfReportUiState): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (state.dateRangeOption) {
            DateRangeOption.CURRENT_MONTH -> today.withDayOfMonth(1) to today
            DateRangeOption.PREVIOUS_MONTH -> {
                val prev = today.minusMonths(1)
                prev.withDayOfMonth(1) to prev.withDayOfMonth(prev.lengthOfMonth())
            }
            DateRangeOption.CURRENT_YEAR -> today.withDayOfYear(1) to today
            DateRangeOption.ALL_TIME -> LocalDate.of(2000, 1, 1) to today
            DateRangeOption.CUSTOM -> state.customStartDate to state.customEndDate
        }
    }
}
