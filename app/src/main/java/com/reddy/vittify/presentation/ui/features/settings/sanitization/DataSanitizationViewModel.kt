package com.reddy.vittify.presentation.ui.features.settings.sanitization

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.model.AccountDiscrepancy
import com.reddy.vittify.data.model.DataHealthAudit
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.DataSanitizationRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.worker.ArchiveAutoDeleteScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class DataSanitizationViewModel @Inject constructor(
    private val sanitizationRepository: DataSanitizationRepository,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val archiveAutoDeleteScheduler: ArchiveAutoDeleteScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DataSanitizationUiState())
    val uiState: StateFlow<DataSanitizationUiState> = _uiState.asStateFlow()

    private val _archivedSearchQuery = MutableStateFlow("")
    private val _isArchivedSelectionMode = MutableStateFlow(false)
    private val _archivedSelectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _lastRestoredTransactions = MutableStateFlow<List<TransactionEntity>?>(null)

    init {
        // Run auto-delete cleanup on opening if enabled
        viewModelScope.launch {
            try {
                val enabled = userPreferencesRepository.getAutoDeleteArchivedEnabled()
                if (enabled) {
                    val days = userPreferencesRepository.getAutoDeleteArchivedDays()
                    transactionRepository.autoDeleteArchivedTransactions(days)
                }
            } catch (_: Exception) {}
        }

        val selectionFlow = combine(_isArchivedSelectionMode, _archivedSelectedIds) { isMode, ids ->
            isMode to ids
        }

        val autoDeleteConfigFlow = combine(
            userPreferencesRepository.autoDeleteArchivedEnabled,
            userPreferencesRepository.autoDeleteArchivedDays
        ) { enabled, days ->
            enabled to days
        }

        // Collect archived transactions and auto-delete settings
        viewModelScope.launch {
            combine(
                transactionRepository.getArchivedTransactions(),
                _archivedSearchQuery,
                selectionFlow,
                autoDeleteConfigFlow,
                _lastRestoredTransactions
            ) { txList, query, (selMode, selIds), (autoDelEnabled, autoDelDays), lastRestored ->
                val filtered = if (query.isBlank()) {
                    txList
                } else {
                    val q = query.trim().lowercase()
                    txList.filter { tx ->
                        tx.merchantName.lowercase().contains(q) ||
                                tx.category.lowercase().contains(q) ||
                                (tx.subcategory?.lowercase()?.contains(q) == true) ||
                                (tx.description?.lowercase()?.contains(q) == true) ||
                                tx.amount.toPlainString().contains(q)
                    }
                }
                _uiState.update { current ->
                    current.copy(
                        allArchivedTransactions = txList,
                        archivedTransactions = filtered,
                        archivedSearchQuery = query,
                        isArchivedSelectionMode = selMode,
                        archivedSelectedIds = selIds,
                        autoDeleteArchivedEnabled = autoDelEnabled,
                        autoDeleteArchivedDays = autoDelDays,
                        lastRestoredTransactions = lastRestored
                    )
                }
            }.collect {}
        }

        // Initial scan
        runFullAudit()
    }

    fun selectTab(tab: SanitizationTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleSection(sectionId: String) {
        _uiState.update { current ->
            val next = if (current.expandedSectionId == sectionId) null else sectionId
            current.copy(expandedSectionId = next)
        }
    }

    fun runFullAudit() {
        _uiState.update { it.copy(isScanning = true) }
        viewModelScope.launch {
            try {
                val myDeviceId = p2pPreferences.getDeviceId()
                val partnerUserId = p2pPreferences.getPartnerUserId()
                val partnerName = p2pPreferences.getPartnerName()
                val isCoupleTracking = p2pPreferences.isCoupleTrackingEnabled()
                val pairedDeviceIds = p2pPreferences.getPairedDevices().map { it.deviceId }.toSet()
                val hiddenKeys = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()

                val audit = sanitizationRepository.runFullAudit(
                    myDeviceId = myDeviceId,
                    partnerUserId = partnerUserId,
                    pairedDeviceIds = pairedDeviceIds,
                    hiddenAccountKeys = hiddenKeys
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isScanning = false,
                        healthAudit = audit,
                        myDeviceId = myDeviceId,
                        partnerUserId = partnerUserId,
                        partnerName = partnerName,
                        isCoupleTrackingEnabled = isCoupleTracking
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isScanning = false,
                        errorMessage = "Audit failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 1-Tap Quick Sanitize: Safely addresses all unambiguous, non-destructive issues:
     * - Claims unassigned records to current device
     * - Aligns transaction owners with bank accounts
     * - Auto-deduplicates suspected duplicate transactions
     * - Cleans corrupted 0.00 entries
     */
    fun quickSanitizeAll() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val myDeviceId = p2pPreferences.getDeviceId()
                sanitizationRepository.claimAllUnassigned(myDeviceId)
                sanitizationRepository.alignTransactionOwnersWithAccounts()

                val duplicates = _uiState.value.healthAudit.transactions.duplicateGroups
                if (duplicates.isNotEmpty()) {
                    sanitizationRepository.autoDeduplicateAll(duplicates)
                }

                val corrupted = _uiState.value.healthAudit.transactions.corruptedTransactions
                if (corrupted.isNotEmpty()) {
                    sanitizationRepository.deleteCorruptedTransactions()
                }

                val orphaned = _uiState.value.healthAudit.transactions.orphanedTransactions
                if (orphaned.isNotEmpty()) {
                    sanitizationRepository.unlinkOrphanedTransactions(orphaned.map { it.id })
                }

                val blankMerchants = _uiState.value.healthAudit.transactions.blankMerchantTransactions
                if (blankMerchants.isNotEmpty()) {
                    sanitizationRepository.autoResolveUnknownMerchants(blankMerchants)
                }

                _uiState.update { it.copy(userMessage = "Quick sanitization completed!") }
                runFullAudit()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Sanitization error: ${e.message}") }
            }
        }
    }

    // --- Ownership Operations ---

    fun claimAllUnassigned() {
        viewModelScope.launch {
            val myDeviceId = p2pPreferences.getDeviceId()
            val res = sanitizationRepository.claimAllUnassigned(myDeviceId)
            handleOpResult(res)
        }
    }

    fun claimAccount(accountId: String) {
        viewModelScope.launch {
            val myDeviceId = p2pPreferences.getDeviceId()
            val res = sanitizationRepository.claimAccount(accountId, myDeviceId)
            handleOpResult(res)
        }
    }

    fun claimCard(cardId: Long) {
        viewModelScope.launch {
            val myDeviceId = p2pPreferences.getDeviceId()
            val res = sanitizationRepository.claimCard(cardId, myDeviceId)
            handleOpResult(res)
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            val res = sanitizationRepository.deleteCard(cardId)
            handleOpResult(res)
        }
    }

    fun reassignForeignRecords(fromOwnerId: String, toOwnerId: String) {
        viewModelScope.launch {
            val res = sanitizationRepository.reassignForeignRecords(fromOwnerId, toOwnerId)
            handleOpResult(res)
        }
    }

    fun alignOwnersWithAccounts() {
        viewModelScope.launch {
            val res = sanitizationRepository.alignTransactionOwnersWithAccounts()
            handleOpResult(res)
        }
    }

    // --- Transaction Operations ---

    fun autoDeduplicateTransactions() {
        viewModelScope.launch {
            val groups = _uiState.value.healthAudit.transactions.duplicateGroups
            val res = sanitizationRepository.autoDeduplicateAll(groups)
            handleOpResult(res)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            val res = sanitizationRepository.deleteTransaction(transactionId)
            handleOpResult(res)
        }
    }

    fun markDuplicateDismissed(transactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.markDuplicateDismissed(transactionId, true)
                _uiState.update { it.copy(userMessage = "Marked as intentional (not duplicate)") }
                runFullAudit()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update: ${e.message}") }
            }
        }
    }

    fun markDuplicateGroupDismissed(group: com.reddy.vittify.data.model.DuplicateTransactionGroup) {
        viewModelScope.launch {
            try {
                val ids = group.transactions.map { it.id }
                transactionRepository.markDuplicatesDismissed(ids, true)
                _uiState.update { it.copy(userMessage = "All marked as intentional (not duplicate)") }
                runFullAudit()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update: ${e.message}") }
            }
        }
    }

    fun deleteCorruptedTransactions() {
        viewModelScope.launch {
            val res = sanitizationRepository.deleteCorruptedTransactions()
            handleOpResult(res)
        }
    }

    // --- Account Health Operations ---

    fun fixAccountDiscrepancy(accountId: String, expectedBalance: BigDecimal) {
        viewModelScope.launch {
            val res = sanitizationRepository.fixAccountDiscrepancy(accountId, expectedBalance)
            handleOpResult(res)
        }
    }

    fun fixAllAccountDiscrepancies() {
        viewModelScope.launch {
            val discrepancies = _uiState.value.healthAudit.accounts.discrepancies
            val res = sanitizationRepository.fixAllAccountDiscrepancies(discrepancies)
            handleOpResult(res)
        }
    }

    fun unhideAccount(bankName: String, accountLast4: String) {
        val key = "${bankName}_${accountLast4}"
        val current = sharedPrefs.getStringSet("hidden_accounts", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(key)
        sharedPrefs.edit().putStringSet("hidden_accounts", current).apply()
        _uiState.update { it.copy(userMessage = "Account unhidden") }
        runFullAudit()
    }

    fun unhideAllAccounts() {
        sharedPrefs.edit().remove("hidden_accounts").apply()
        _uiState.update { it.copy(userMessage = "All accounts unhidden") }
        runFullAudit()
    }

    fun mergeAccounts(targetAccount: AccountBalanceEntity, sourceAccounts: List<AccountBalanceEntity>, newBalance: BigDecimal?) {
        viewModelScope.launch {
            val res = sanitizationRepository.mergeAccounts(targetAccount, sourceAccounts, newBalance)
            handleOpResult(res)
        }
    }

    fun deleteAccount(account: AccountBalanceEntity) {
        viewModelScope.launch {
            val res = sanitizationRepository.deleteAccount(account)
            handleOpResult(res)
        }
    }

    // --- Transaction Operations ---

    fun unlinkOrphanedTransactions() {
        viewModelScope.launch {
            val orphaned = _uiState.value.healthAudit.transactions.orphanedTransactions
            val res = sanitizationRepository.unlinkOrphanedTransactions(orphaned.map { it.id })
            handleOpResult(res)
        }
    }

    fun unlinkSingleTransaction(transactionId: Long) {
        viewModelScope.launch {
            val res = sanitizationRepository.unlinkOrphanedTransactions(listOf(transactionId))
            handleOpResult(res)
        }
    }

    fun deleteOrphanedTransactions() {
        viewModelScope.launch {
            val orphaned = _uiState.value.healthAudit.transactions.orphanedTransactions
            val res = sanitizationRepository.deleteOrphanedTransactions(orphaned.map { it.id })
            handleOpResult(res)
        }
    }

    fun autoResolveUnknownMerchants() {
        viewModelScope.launch {
            val blanks = _uiState.value.healthAudit.transactions.blankMerchantTransactions
            val res = sanitizationRepository.autoResolveUnknownMerchants(blanks)
            handleOpResult(res)
        }
    }

    fun updateMerchantName(transactionId: Long, newName: String) {
        viewModelScope.launch {
            val res = sanitizationRepository.updateMerchantName(transactionId, newName)
            handleOpResult(res)
        }
    }

    // --- System / Junk Operations ---

    fun purgeSampleData() {
        viewModelScope.launch {
            val res = sanitizationRepository.purgeSampleData()
            userPreferencesRepository.setSampleDataSeeded(false)
            handleOpResult(res)
        }
    }

    fun purgeUnrecognizedSms() {
        viewModelScope.launch {
            val res = sanitizationRepository.purgeUnrecognizedSms()
            handleOpResult(res)
        }
    }

    fun purgeTrashTransactions() {
        viewModelScope.launch {
            val res = sanitizationRepository.purgeTrashTransactions()
            handleOpResult(res)
        }
    }

    // --- Archived Tab Actions (Moved from ManageArchivedTransactionsViewModel) ---

    fun onArchivedSearchQueryChanged(query: String) {
        _archivedSearchQuery.value = query
    }

    fun toggleArchivedSelectionMode() {
        _isArchivedSelectionMode.update { !it }
        _archivedSelectedIds.value = emptySet()
    }

    fun toggleSelectArchivedTransaction(id: Long) {
        _archivedSelectedIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun selectAllArchived() {
        val allIds = _uiState.value.archivedTransactions.map { it.id }.toSet()
        _archivedSelectedIds.value = allIds
    }

    fun clearArchivedSelection() {
        _archivedSelectedIds.value = emptySet()
    }

    fun restoreSelectedArchivedTransactions() {
        viewModelScope.launch {
            val selected = _archivedSelectedIds.value.toList()
            if (selected.isNotEmpty()) {
                val toRestore = _uiState.value.archivedTransactions.filter { it.id in selected }
                transactionRepository.restoreArchivedTransactions(toRestore)
                _archivedSelectedIds.value = emptySet()
                _isArchivedSelectionMode.value = false
                _lastRestoredTransactions.value = toRestore
                _uiState.update { it.copy(userMessage = "${toRestore.size} transactions restored") }
                runFullAudit()
            }
        }
    }

    fun restoreSingleArchivedTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val toRestore = listOf(transaction)
            transactionRepository.restoreArchivedTransactions(toRestore)
            _lastRestoredTransactions.value = toRestore
            _uiState.update { it.copy(userMessage = "Transaction restored") }
            runFullAudit()
        }
    }

    fun undoRestore(restoredList: List<TransactionEntity>) {
        viewModelScope.launch {
            transactionRepository.deleteTransactions(restoredList, hardDelete = false)
            _uiState.update { it.copy(userMessage = "Restoration undone") }
            runFullAudit()
        }
    }

    fun clearLastRestored() {
        _lastRestoredTransactions.value = null
    }

    fun permanentlyDeleteSelectedArchived() {
        viewModelScope.launch {
            val selected = _archivedSelectedIds.value.toList()
            if (selected.isNotEmpty()) {
                val toDelete = _uiState.value.archivedTransactions.filter { it.id in selected }
                transactionRepository.permanentlyDeleteArchivedTransactions(toDelete)
                _archivedSelectedIds.value = emptySet()
                _isArchivedSelectionMode.value = false
                _uiState.update { it.copy(userMessage = "${toDelete.size} transactions permanently removed") }
                runFullAudit()
            }
        }
    }

    fun permanentlyDeleteSingleArchived(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.permanentlyDeleteArchivedTransactions(listOf(transaction))
            _uiState.update { it.copy(userMessage = "Transaction permanently removed") }
            runFullAudit()
        }
    }

    fun emptyArchivedTrash() {
        viewModelScope.launch {
            transactionRepository.permanentlyDeleteAllArchivedTransactions()
            _archivedSelectedIds.value = emptySet()
            _isArchivedSelectionMode.value = false
            _uiState.update { it.copy(userMessage = "Archive emptied") }
            runFullAudit()
        }
    }

    fun updateAutoDeleteSettings(enabled: Boolean, days: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateAutoDeleteArchivedEnabled(enabled)
            userPreferencesRepository.updateAutoDeleteArchivedDays(days)
            if (enabled) {
                transactionRepository.autoDeleteArchivedTransactions(days)
                archiveAutoDeleteScheduler.schedulePeriodicCleanup()
            } else {
                androidx.work.WorkManager.getInstance(context).cancelUniqueWork(com.reddy.vittify.worker.ArchiveAutoDeleteWorker.WORK_NAME)
            }
            _uiState.update {
                it.copy(
                    autoDeleteArchivedEnabled = enabled,
                    autoDeleteArchivedDays = days,
                    userMessage = "Auto-delete policy updated"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }

    private fun handleOpResult(result: com.reddy.vittify.data.model.SanitizationOperationResult) {
        if (result.success) {
            _uiState.update { it.copy(userMessage = result.message) }
            runFullAudit()
        } else {
            _uiState.update { it.copy(errorMessage = result.message) }
        }
    }
}
