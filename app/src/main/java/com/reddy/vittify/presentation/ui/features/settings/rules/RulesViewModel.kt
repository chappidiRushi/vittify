package com.reddy.vittify.presentation.ui.features.settings.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.domain.model.rule.TransactionRule
import com.reddy.vittify.domain.repository.RuleRepository
import com.reddy.vittify.domain.service.RuleTemplateService
import com.reddy.vittify.domain.usecase.ApplyRulesToPastTransactionsUseCase
import com.reddy.vittify.domain.usecase.BatchApplyResult
import com.reddy.vittify.domain.usecase.GetCategoriesUseCase
import com.reddy.vittify.domain.usecase.InitializeRuleTemplatesUseCase
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val ruleTemplateService: RuleTemplateService,
    private val initializeRuleTemplatesUseCase: InitializeRuleTemplatesUseCase,
    private val applyRulesToPastTransactionsUseCase: ApplyRulesToPastTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    val rules: StateFlow<List<TransactionRule>> = ruleRepository.getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Categories for selection sheet
    val categories = getCategoriesUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All subcategories map for selection sheet
    val allSubcategories = subcategoryRepository.subcategoriesMap

    val accounts = accountBalanceRepository.getAllLatestBalances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        initializeRules()
    }

    private fun initializeRules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Initialize default rule templates if none exist
                initializeRuleTemplatesUseCase()
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            } finally {
            _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleRule(ruleId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                ruleRepository.setRuleActive(ruleId, isActive)
            } catch (e: Exception) {
                // Log error
                e.printStackTrace()
            }
        }
    }

    fun createRule(rule: TransactionRule) {
        viewModelScope.launch {
            try {
                ruleRepository.insertRule(rule)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            try {
                ruleRepository.deleteRule(ruleId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateRule(rule: TransactionRule) {
        viewModelScope.launch {
            try {
                ruleRepository.updateRule(rule)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getRuleApplicationCount(ruleId: String): Flow<Int> = flow {
        emit(ruleRepository.getRuleApplicationCount(ruleId))
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Force reset to default templates
                initializeRuleTemplatesUseCase(forceReset = true)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
            _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun applyRuleToPastTransactions(
        rule: TransactionRule,
        applyToUncategorizedOnly: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(batchApplyProgress = 0 to 0) }
            _uiState.update { it.copy(batchApplyResult = null) }

            try {
                val result = if (applyToUncategorizedOnly) {
                    applyRulesToPastTransactionsUseCase.applyRuleToUncategorizedTransactions(
                        rule = rule,
                        onProgress = { processed, total ->
                            _uiState.update { it.copy(batchApplyProgress = processed to total) }
                        }
                    )
                } else {
                    applyRulesToPastTransactionsUseCase.applyRuleToAllTransactions(
                        rule = rule,
                        onProgress = { processed, total ->
                            _uiState.update { it.copy(batchApplyProgress = processed to total) }
                        }
                    )
                }
                _uiState.update { it.copy(batchApplyResult = result) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(batchApplyResult = BatchApplyResult(
                    totalProcessed = 0,
                    totalUpdated = 0,
                    errors = listOf("Error: ${e.message}")
                )) }
            } finally {
            _uiState.update { it.copy(isLoading = false) }
                _uiState.update { it.copy(batchApplyProgress = null) }
            }
        }
    }

    fun applyAllRulesToPastTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(batchApplyProgress = 0 to 0) }
            _uiState.update { it.copy(batchApplyResult = null) }

            try {
                val result = applyRulesToPastTransactionsUseCase.applyAllActiveRulesToTransactions(
                    onProgress = { processed, total ->
                        _uiState.update { it.copy(batchApplyProgress = processed to total) }
                    }
                )
                _uiState.update { it.copy(batchApplyResult = result) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(batchApplyResult = BatchApplyResult(
                    totalProcessed = 0,
                    totalUpdated = 0,
                    errors = listOf("Error: ${e.message}")
                )) }
            } finally {
            _uiState.update { it.copy(isLoading = false) }
                _uiState.update { it.copy(batchApplyProgress = null) }
            }
        }
    }

    fun clearBatchApplyResult() {
        _uiState.update { it.copy(batchApplyResult = null) }
    }

    fun saveSubcategory(
        categoryId: Long,
        name: String,
        iconResId: Int,
        iconName: String,
        color: String
    ) {
        viewModelScope.launch {
            try {
                subcategoryRepository.createSubcategory(
                    categoryId = categoryId,
                    name = name,
                    iconResId = iconResId,
                    iconName = iconName,
                    color = color
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
