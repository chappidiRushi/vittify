package com.reddy.vittify.data.ai

import kotlinx.serialization.Serializable

enum class AiProviderType(
    val id: String,
    val displayName: String,
    val description: String,
    val isSupported: Boolean = true
) {
    GEMINI("gemini", "Google Gemini", "Fast, multimodal & intelligent Google AI", isSupported = true);

    companion object {
        fun fromId(id: String?): AiProviderType = GEMINI
    }
}

@Serializable
data class GeminiModelInfo(
    val name: String, // e.g. "models/gemini-2.5-flash"
    val displayName: String = "", // e.g. "Gemini 2.5 Flash"
    val description: String = "",
    val supportedGenerationMethods: List<String> = emptyList()
) {
    val cleanName: String
        get() = name.removePrefix("models/")
}

@Serializable
data class GeminiListModelsResponse(
    val models: List<GeminiModelDto> = emptyList()
)

@Serializable
data class GeminiModelDto(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String> = emptyList()
)

@Serializable
data class GeminiGenerateRequest(
    val contents: List<GeminiRequestContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiRequestContent(
    val parts: List<GeminiRequestPart>
)

@Serializable
data class GeminiBlob(
    val mimeType: String,
    val data: String
)

@Serializable
data class GeminiRequestPart(
    val text: String? = null,
    val inlineData: GeminiBlob? = null
)

@Serializable
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

data class GeminiConfig(
    val apiKey: String = "",
    val selectedModel: String = "",
    val availableModels: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val isConfigured: Boolean = false,
    val totalRequests: Int = 0,
    val totalTokens: Long = 0L,
    val lastTestedAt: Long = 0L,
    val lastTestSuccess: Boolean? = null,
    val lastTestMessage: String? = null,
    val includeCategories: Boolean = true,
    val includeBankAccounts: Boolean = true,
    val customRules: String = ""
)

val DEFAULT_GEMINI_MODELS: List<String> = emptyList()

data class AiUsageMetrics(
    val totalRequests: Int = 0,
    val estimatedTokens: Long = 0L,
    val lastUsedTimestamp: Long = 0L
)

sealed interface ConnectionTestState {
    object Idle : ConnectionTestState
    object Testing : ConnectionTestState
    data class Success(val message: String, val modelsCount: Int = 0) : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}
