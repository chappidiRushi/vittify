package com.reddy.vittify.data.ai

import kotlinx.serialization.Serializable

enum class AiProviderType(
    val id: String,
    val displayName: String,
    val description: String,
    val isSupported: Boolean = true
) {
    GEMINI("gemini", "Google Gemini AI", "Fast, multimodal & intelligent Google AI", isSupported = true),
    OPENAI("openai", "OpenAI (ChatGPT)", "GPT-4o & GPT-4o-mini models", isSupported = false),
    CLAUDE("claude", "Anthropic Claude", "Claude 3.5 Sonnet & Haiku models", isSupported = false),
    DEEPSEEK("deepseek", "DeepSeek AI", "Reasoning & coding models", isSupported = false);

    companion object {
        fun fromId(id: String?): AiProviderType =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: GEMINI
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

data class GeminiConfig(
    val apiKey: String = "",
    val selectedModel: String = "gemini-2.5-flash",
    val availableModels: List<String> = DEFAULT_GEMINI_MODELS,
    val isEnabled: Boolean = true,
    val isConfigured: Boolean = false,
    val totalRequests: Int = 0,
    val totalTokens: Long = 0L,
    val lastTestedAt: Long = 0L,
    val lastTestSuccess: Boolean? = null,
    val lastTestMessage: String? = null
)

val DEFAULT_GEMINI_MODELS = listOf(
    "gemini-2.5-flash",
    "gemini-2.5-pro",
    "gemini-2.0-flash",
    "gemini-1.5-flash",
    "gemini-1.5-pro"
)

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
