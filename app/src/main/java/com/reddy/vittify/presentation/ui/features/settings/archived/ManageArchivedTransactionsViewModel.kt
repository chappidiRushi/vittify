package com.reddy.vittify.presentation.ui.features.settings.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.CurrencyRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.worker.ArchiveAutoDeleteScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ManageArchivedTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val currencyRepository: CurrencyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val archiveAutoDeleteScheduler: ArchiveAutoDeleteScheduler
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _lastRestoredTransactions = MutableStateFlow<List<TransactionEntity>?>(null)
    val lastRestoredTransactions: StateFlow<List<TransactionEntity>?> = _lastRestoredTransactions.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        // Run cleanup on opening archived transactions
        viewModelScope.launch {
            try {
                val enabled = userPreferencesRepository.getAutoDeleteArchivedEnabled()
                if (enabled) {
                    val days = userPreferencesRepository.getAutoDeleteArchivedDays()
                    transactionRepository.autoDeleteArchivedTransactions(days)
                }
            } catch (_: Exception) {
            }
        }
    }

    private val archivedTransactionsFlow = transactionRepository.getArchivedTransactions()
    private val categoriesMapFlow = categoryRepository.categories.combine(categoryRepository.getAllCategories()) { _, list ->
        list.associateBy { it.name }
    }
    private val subcategoriesMapFlow = subcategoryRepository.getAllSubcategories().combine(categoryRepository.categories) { list, _ ->
        list.associateBy { it.name }
    }
    private val accountsMapFlow = accountBalanceRepository.getAllLatestBalances()

    private val selectionFlow = combine(_isSelectionMode, _selectedIds) { isMode, ids ->
        isMode to ids
    }

    private val autoDeleteConfigFlow = combine(
        userPreferencesRepository.autoDeleteArchivedEnabled,
        userPreferencesRepository.autoDeleteArchivedDays
    ) { enabled, days ->
        enabled to days
    }

    val uiState: StateFlow<ManageArchivedTransactionsUiState> = combine(
        archivedTransactionsFlow,
        _searchQuery,
        selectionFlow,
        autoDeleteConfigFlow,
        currencyRepository.effectiveBaseCurrencyCode
    ) { txList, query, (selectionMode, selected), (autoDelEnabled, autoDelDays), baseCurr ->
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

        val total = filtered.fold(BigDecimal.ZERO) { acc: BigDecimal, item: TransactionEntity ->
            acc.add(item.amount)
        }

        ManageArchivedTransactionsUiState(
            transactions = txList,
            filteredTransactions = filtered,
            isLoading = false,
            searchQuery = query,
            isSelectionMode = selectionMode,
            selectedIds = selected,
            autoDeleteEnabled = autoDelEnabled,
            autoDeleteDays = autoDelDays,
            baseCurrency = baseCurr,
            totalAmount = total
        )
    }.combine(categoriesMapFlow) { state, catMap ->
        state.copy(categoriesMap = catMap)
    }.combine(subcategoriesMapFlow) { state, subMap ->
        state.copy(subcategoriesMap = subMap)
    }.combine(accountsMapFlow) { state, accList ->
        state.copy(accountsMap = accList.associateBy { it.id })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageArchivedTransactionsUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedIds.value = emptySet()
        }
    }

    fun toggleTransactionSelection(transactionId: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(transactionId)) {
            current.remove(transactionId)
        } else {
            current.add(transactionId)
        }
        _selectedIds.value = current
        if (current.isEmpty() && _isSelectionMode.value) {
            // Keep selection mode active or allow manual cancel
        }
    }

    fun selectAll() {
        val allIds = uiState.value.filteredTransactions.map { it.id }.toSet()
        _selectedIds.value = allIds
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun restoreTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                transactionRepository.restoreArchivedTransaction(transaction)
                _lastRestoredTransactions.value = listOf(transaction)
            } catch (_: Exception) {
            }
        }
    }

    fun restoreSelectedTransactions() {
        val selected = _selectedIds.value
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val transactionsToRestore = uiState.value.transactions.filter { selected.contains(it.id) }
            try {
                transactionRepository.restoreArchivedTransactions(transactionsToRestore)
                _lastRestoredTransactions.value = transactionsToRestore
                _selectedIds.value = emptySet()
                _isSelectionMode.value = false
            } catch (_: Exception) {
            }
        }
    }

    fun undoRestore(transactions: List<TransactionEntity>) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransactions(transactions, hardDelete = false)
            } catch (_: Exception) {
            }
        }
    }

    fun clearLastRestored() {
        _lastRestoredTransactions.value = null
    }

    fun deleteTransactionPermanently(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                transactionRepository.permanentlyDeleteArchivedTransaction(transaction)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteSelectedPermanently() {
        val selected = _selectedIds.value
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val transactionsToDelete = uiState.value.transactions.filter { selected.contains(it.id) }
            try {
                transactionRepository.permanentlyDeleteArchivedTransactions(transactionsToDelete)
                _selectedIds.value = emptySet()
                _isSelectionMode.value = false
            } catch (_: Exception) {
            }
        }
    }

    fun deleteAllPermanently() {
        viewModelScope.launch {
            try {
                transactionRepository.permanentlyDeleteAllArchivedTransactions()
                _selectedIds.value = emptySet()
                _isSelectionMode.value = false
            } catch (_: Exception) {
            }
        }
    }

    fun updateAutoDeleteSettings(enabled: Boolean, days: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateAutoDeleteArchivedEnabled(enabled)
            userPreferencesRepository.updateAutoDeleteArchivedDays(days)
            if (enabled) {
                transactionRepository.autoDeleteArchivedTransactions(days)
                archiveAutoDeleteScheduler.schedulePeriodicCleanup()
            }
        }
    }
}

