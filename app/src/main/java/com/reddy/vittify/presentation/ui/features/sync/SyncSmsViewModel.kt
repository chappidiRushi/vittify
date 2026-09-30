package com.reddy.vittify.presentation.ui.features.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.manager.SmsScanner
import com.reddy.vittify.data.manager.SmsTransactionProcessor
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.firstOrNull
import com.reddy.vittify.data.database.entity.AccountBalanceEntity

data class UnmappedBankPrompt(
    val bankName: String,
    val accountLast4: String,
    val accountType: String,
    val currency: String,
    val latestBalance: BigDecimal
)

data class SyncSmsUiState(
    val isScanning: Boolean = false,
    val scanCompleted: Boolean = false,
    val isImporting: Boolean = false,
    val importCompleted: Boolean = false,
    val totalSmsCount: Int = 0,
    val scannedSmsCount: Int = 0,
    val discoveredMessages: List<SyncMessageItem> = emptyList(),
    val isEditingSelection: Boolean = false,
    val importedCount: Int = 0,
    val importProgress: Int = 0,
    val hasPermission: Boolean = true,
    val forceResync: Boolean = false,
    val unmappedBankPrompt: UnmappedBankPrompt? = null,
    val unmappedBankQueue: List<UnmappedBankPrompt> = emptyList(),
    val errorMessage: String? = null
) {
    val selectedCount: Int get() = discoveredMessages.count { it.isSelected }
    val totalDiscoveredCount: Int get() = discoveredMessages.size

    val canSelectBetween: Boolean get() {
        val selectedIndices = discoveredMessages.mapIndexedNotNull { index, item ->
            if (item.isSelected) index else null
        }
        return selectedIndices.size == 2 && (selectedIndices[1] - selectedIndices[0] > 1)
    }

    val selectBetweenCount: Int get() {
        val selectedIndices = discoveredMessages.mapIndexedNotNull { index, item ->
            if (item.isSelected) index else null
        }
        return if (selectedIndices.size == 2) {
            selectedIndices[1] - selectedIndices[0] + 1
        } else 0
    }
}

@HiltViewModel
class SyncSmsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsScanner: SmsScanner,
    private val smsTransactionProcessor: SmsTransactionProcessor,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val forceResyncArg: Boolean = savedStateHandle.get<Boolean>("forceResync") ?: false

    private val _uiState = MutableStateFlow(SyncSmsUiState())
    val uiState: StateFlow<SyncSmsUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var importJob: Job? = null

    init {
        _uiState.update { it.copy(forceResync = forceResyncArg) }
        checkPermissionStatus()
    }

    fun checkPermissionStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.update { it.copy(hasPermission = hasPermission) }
    }

    fun toggleForceResync() {
        _uiState.update { it.copy(forceResync = !it.forceResync) }
    }

    fun setForceResync(value: Boolean) {
        _uiState.update { it.copy(forceResync = value) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            startScan()
        }
    }

    fun startScan(forceResync: Boolean? = null) {
        val isForceResync = forceResync ?: _uiState.value.forceResync
        scanJob?.cancel()
        _uiState.update {
            it.copy(
                forceResync = isForceResync,
                isScanning = true,
                scanCompleted = false,
                importCompleted = false,
                isImporting = false,
                isEditingSelection = false,
                scannedSmsCount = 0,
                totalSmsCount = 0,
                discoveredMessages = emptyList(),
                errorMessage = null
            )
        }

        scanJob = viewModelScope.launch {
            try {
                val results = smsScanner.scanMessages(forceResync = isForceResync) { scanned, total ->
                    _uiState.update {
                        it.copy(
                            scannedSmsCount = scanned,
                            totalSmsCount = total
                        )
                    }
                }

                val existingAccounts = accountBalanceRepository.getAllLatestBalances().firstOrNull() ?: emptyList()
                val existingKeys = existingAccounts.map { "${it.bankName.trim().lowercase()}:::${it.accountLast4.trim()}" }.toSet()

                val missing = results.mapNotNull { item ->
                    val pt = item.parsedTransaction
                    val bank = pt.bankName.trim()
                    val last4 = pt.accountLast4?.trim() ?: ""
                    if (bank.isNotBlank() && (last4.isNotBlank() || pt.isMobileWallet)) {
                        val key = "${bank.lowercase()}:::$last4"
                        if (!existingKeys.contains(key)) {
                            UnmappedBankPrompt(
                                bankName = bank,
                                accountLast4 = last4,
                                accountType = if (pt.isFromCard) "CREDIT" else if (pt.isMobileWallet) "WALLET" else "SAVINGS",
                                currency = pt.currency.ifBlank { "INR" },
                                latestBalance = pt.balance ?: BigDecimal.ZERO
                            )
                        } else null
                    } else null
                }.distinctBy { "${it.bankName.lowercase()}:::${it.accountLast4}" }

                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanCompleted = true,
                        discoveredMessages = results,
                        unmappedBankQueue = missing,
                        unmappedBankPrompt = missing.firstOrNull()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanCompleted = true,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun acceptAddUnmappedAccount() {
        val prompt = _uiState.value.unmappedBankPrompt ?: return
        viewModelScope.launch {
            val entity = AccountBalanceEntity(
                id = UUID.randomUUID().toString(),
                bankName = prompt.bankName,
                accountLast4 = prompt.accountLast4,
                accountName = prompt.bankName,
                balance = prompt.latestBalance,
                currency = prompt.currency,
                isCreditCard = prompt.accountType == "CREDIT",
                isWallet = prompt.accountType == "WALLET",
                color = "#33B5E5",
                iconName = "",
                iconResId = 0,
                timestamp = LocalDateTime.now()
            )
            accountBalanceRepository.insertBalance(entity)

            val remaining = _uiState.value.unmappedBankQueue.drop(1)
            _uiState.update {
                it.copy(
                    unmappedBankQueue = remaining,
                    unmappedBankPrompt = remaining.firstOrNull()
                )
            }
        }
    }

    fun dismissUnmappedAccountPrompt() {
        val remaining = _uiState.value.unmappedBankQueue.drop(1)
        _uiState.update {
            it.copy(
                unmappedBankQueue = remaining,
                unmappedBankPrompt = remaining.firstOrNull()
            )
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.update {
            it.copy(
                isScanning = false,
                scanCompleted = false
            )
        }
    }

    fun resetToReady() {
        scanJob?.cancel()
        _uiState.update {
            it.copy(
                isScanning = false,
                scanCompleted = false,
                importCompleted = false,
                isImporting = false,
                isEditingSelection = false,
                scannedSmsCount = 0,
                totalSmsCount = 0,
                discoveredMessages = emptyList(),
                errorMessage = null
            )
        }
    }

    fun toggleEditMode(editing: Boolean) {
        _uiState.update { it.copy(isEditingSelection = editing) }
    }

    fun toggleItemSelection(id: String) {
        _uiState.update { state ->
            state.copy(
                discoveredMessages = state.discoveredMessages.map { item ->
                    if (item.id == id) item.copy(isSelected = !item.isSelected) else item
                }
            )
        }
    }

    fun toggleMonthSelection(itemIds: List<String>) {
        _uiState.update { state ->
            val monthItems = state.discoveredMessages.filter { it.id in itemIds }
            val allSelected = monthItems.isNotEmpty() && monthItems.all { it.isSelected }
            val targetState = !allSelected
            state.copy(
                discoveredMessages = state.discoveredMessages.map { item ->
                    if (item.id in itemIds) item.copy(isSelected = targetState) else item
                }
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                discoveredMessages = state.discoveredMessages.map { it.copy(isSelected = true) }
            )
        }
    }

    fun unselectAll() {
        _uiState.update { state ->
            state.copy(
                discoveredMessages = state.discoveredMessages.map { it.copy(isSelected = false) }
            )
        }
    }

    fun selectBetweenTwoSelected() {
        _uiState.update { state ->
            val selectedIndices = state.discoveredMessages.mapIndexedNotNull { index, item ->
                if (item.isSelected) index else null
            }
            if (selectedIndices.size == 2) {
                val start = minOf(selectedIndices[0], selectedIndices[1])
                val end = maxOf(selectedIndices[0], selectedIndices[1])
                state.copy(
                    discoveredMessages = state.discoveredMessages.mapIndexed { index, item ->
                        if (index in start..end) item.copy(isSelected = true) else item
                    }
                )
            } else {
                state
            }
        }
    }

    fun importSelectedMessages() {
        val selectedItems = _uiState.value.discoveredMessages.filter { it.isSelected }
        if (selectedItems.isEmpty()) return

        importJob?.cancel()
        _uiState.update {
            it.copy(
                isImporting = true,
                importCompleted = false,
                importProgress = 0
            )
        }

        importJob = viewModelScope.launch {
            var successCount = 0
            for ((index, item) in selectedItems.withIndex()) {
                try {
                    val result = smsTransactionProcessor.saveParsedTransaction(
                        item.parsedTransaction,
                        item.smsBody
                    )
                    if (result.success) {
                        successCount++
                    }
                } catch (_: Exception) {}

                _uiState.update { it.copy(importProgress = index + 1) }
            }

            try {
                userPreferencesRepository.setLastScanTimestamp(System.currentTimeMillis())
            } catch (_: Exception) {}

            _uiState.update {
                it.copy(
                    isImporting = false,
                    importCompleted = true,
                    importedCount = successCount,
                    isEditingSelection = false
                )
            }
        }
    }
}

