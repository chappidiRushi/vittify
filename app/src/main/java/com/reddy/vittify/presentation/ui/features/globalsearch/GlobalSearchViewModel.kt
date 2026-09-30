package com.reddy.vittify.presentation.ui.features.globalsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.preferences.GlobalSearchPreferences
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<GlobalSearchPreferences> =
        userPreferencesRepository.globalSearchPreferences
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = GlobalSearchPreferences()
            )

    private val allAccounts = accountBalanceRepository.getAllLatestBalances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)

    val uiState: StateFlow<GlobalSearchUiState> = combine(
        _searchQuery,
        preferences,
        _isSearching,
        allAccounts
    ) { query, prefs, isSearching, accounts ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            GlobalSearchUiState(
                query = query,
                isSearching = isSearching,
                preferences = prefs,
                results = GlobalSearchGroupedResults()
            )
        } else {
            val matchingAccounts = if (prefs.includeAccounts) {
                accounts.filter { account ->
                    account.bankName.contains(trimmed, ignoreCase = true) ||
                            (account.accountName?.contains(trimmed, ignoreCase = true) == true) ||
                            account.accountLast4.contains(trimmed)
                }.take(prefs.resultsPerType)
            } else emptyList()

            val matchingSettings = if (prefs.includeSettings) {
                GlobalSearchIndex.allSettings.filter { setting ->
                    setting.title.contains(trimmed, ignoreCase = true) ||
                            setting.subtitle.contains(trimmed, ignoreCase = true) ||
                            setting.keywords.any { it.contains(trimmed, ignoreCase = true) }
                }.take(prefs.resultsPerType)
            } else emptyList()

            val matchingPages = if (prefs.includePages) {
                GlobalSearchIndex.allPages.filter { page ->
                    page.title.contains(trimmed, ignoreCase = true) ||
                            page.subtitle.contains(trimmed, ignoreCase = true) ||
                            page.keywords.any { it.contains(trimmed, ignoreCase = true) }
                }.take(prefs.resultsPerType)
            } else emptyList()

            // Transactions will be populated via the debounce pipeline
            GlobalSearchUiState(
                query = query,
                isSearching = isSearching,
                preferences = prefs,
                results = GlobalSearchGroupedResults(
                    accounts = matchingAccounts,
                    settings = matchingSettings,
                    transactions = emptyList(),
                    pages = matchingPages
                )
            )
        }
    }.combine(
        _searchQuery
            .debounce(150)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                flow {
                    val trimmed = query.trim()
                    if (trimmed.isEmpty() || !preferences.value.includeTransactions) {
                        emit(emptyList<TransactionEntity>())
                    } else {
                        val transactions = transactionRepository.searchTransactionsForGlobal(
                            query = trimmed,
                            limit = preferences.value.resultsPerType
                        )
                        emit(transactions)
                    }
                }
            }
    ) { state, transactions ->
        if (state.query.trim().isEmpty()) {
            state.copy(results = GlobalSearchGroupedResults())
        } else {
            state.copy(
                results = state.results.copy(
                    transactions = if (state.preferences.includeTransactions) transactions else emptyList()
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlobalSearchUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onQueryChange(query: String) = onSearchQueryChange(query)

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun toggleAccounts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateGlobalSearchAccounts(enabled)
        }
    }

    fun toggleSettings(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateGlobalSearchSettings(enabled)
        }
    }

    fun toggleTransactions(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateGlobalSearchTransactions(enabled)
        }
    }

    fun togglePages(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateGlobalSearchPages(enabled)
        }
    }

    fun updateResultsPerType(count: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateGlobalSearchResultsPerType(count)
        }
    }
}

