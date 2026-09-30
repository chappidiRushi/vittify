package com.reddy.vittify.presentation.ui.features.settings.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.ai.AiPreferencesRepository
import com.reddy.vittify.data.ai.AiProviderType
import com.reddy.vittify.data.ai.ConnectionTestState
import com.reddy.vittify.data.ai.DEFAULT_GEMINI_MODELS
import com.reddy.vittify.data.ai.GeminiAiProvider
import com.reddy.vittify.data.ai.GeminiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiSettingsUiState(
    val isAiEnabled: Boolean = true,
    val selectedProvider: AiProviderType = AiProviderType.GEMINI,
    val geminiConfig: GeminiConfig = GeminiConfig(),
    val testState: ConnectionTestState = ConnectionTestState.Idle,
    val isFetchingModels: Boolean = false,
    val showGuideDialog: Boolean = false
)

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val aiPreferencesRepository: AiPreferencesRepository,
    private val geminiAiProvider: GeminiAiProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                aiPreferencesRepository.isAiEnabled,
                aiPreferencesRepository.selectedProvider,
                aiPreferencesRepository.geminiConfig
            ) { isEnabled, provider, geminiConfig ->
                AiSettingsUiState(
                    isAiEnabled = isEnabled,
                    selectedProvider = provider,
                    geminiConfig = geminiConfig
                )
            }.collectLatest { state ->
                _uiState.update { current ->
                    current.copy(
                        isAiEnabled = state.isAiEnabled,
                        selectedProvider = state.selectedProvider,
                        geminiConfig = state.geminiConfig
                    )
                }
            }
        }
    }

    fun onToggleAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiPreferencesRepository.setAiEnabled(enabled)
        }
    }

    fun onSelectProvider(provider: AiProviderType) {
        viewModelScope.launch {
            aiPreferencesRepository.setSelectedProvider(provider)
        }
    }

    fun onApiKeyChanged(apiKey: String) {
        viewModelScope.launch {
            aiPreferencesRepository.setGeminiApiKey(apiKey)
            _uiState.update { it.copy(testState = ConnectionTestState.Idle) }
        }
    }

    fun onModelSelected(model: String) {
        viewModelScope.launch {
            aiPreferencesRepository.setGeminiSelectedModel(model)
        }
    }

    fun onToggleGeminiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiPreferencesRepository.setGeminiEnabled(enabled)
        }
    }

    fun onFetchModels() {
        val apiKey = _uiState.value.geminiConfig.apiKey
        if (apiKey.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true) }
            val result = geminiAiProvider.fetchAvailableModels(apiKey)
            result.fold(
                onSuccess = { models ->
                    val cleanNames = models.map { it.cleanName }
                    if (cleanNames.isNotEmpty()) {
                        aiPreferencesRepository.setGeminiAvailableModels(cleanNames)
                    }
                    _uiState.update { it.copy(isFetchingModels = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isFetchingModels = false) }
                }
            )
        }
    }

    fun onTestConnection() {
        val apiKey = _uiState.value.geminiConfig.apiKey
        val selectedModel = _uiState.value.geminiConfig.selectedModel

        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(testState = ConnectionTestState.Error("Please enter your Gemini API key first."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(testState = ConnectionTestState.Testing, isFetchingModels = true) }

            val result = geminiAiProvider.testConnection(apiKey, selectedModel)
            
            result.fold(
                onSuccess = { testResult ->
                    if (testResult.isSuccess) {
                        val availableModels = testResult.availableModels.ifEmpty { DEFAULT_GEMINI_MODELS }
                        aiPreferencesRepository.saveGeminiTestResult(
                            success = true,
                            message = testResult.message,
                            availableModels = availableModels
                        )
                        _uiState.update {
                            it.copy(
                                testState = ConnectionTestState.Success(
                                    message = testResult.message,
                                    modelsCount = availableModels.size
                                ),
                                isFetchingModels = false
                            )
                        }
                    } else {
                        aiPreferencesRepository.saveGeminiTestResult(
                            success = false,
                            message = testResult.message
                        )
                        _uiState.update {
                            it.copy(
                                testState = ConnectionTestState.Error(testResult.message),
                                isFetchingModels = false
                            )
                        }
                    }
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: "Failed to connect to Gemini API"
                    aiPreferencesRepository.saveGeminiTestResult(success = false, message = msg)
                    _uiState.update {
                        it.copy(
                            testState = ConnectionTestState.Error(msg),
                            isFetchingModels = false
                        )
                    }
                }
            )
        }
    }

    fun onResetMetrics() {
        viewModelScope.launch {
            aiPreferencesRepository.resetMetrics()
        }
    }

    fun onToggleGuideDialog(show: Boolean) {
        _uiState.update { it.copy(showGuideDialog = show) }
    }
}
