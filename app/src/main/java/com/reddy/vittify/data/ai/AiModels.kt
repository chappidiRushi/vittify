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

enum class AiModelTier {
    RECOMMENDED,  // High free tier quota (15 RPM), fast, stable
    PRO,          // Strict free rate limit (2 RPM), often hits token limits or requires billing
    EXPERIMENTAL, // Preview / Experimental model
    OTHER         // Standard / Other model
}

data class ClassifiedAiModel(
    val id: String,
    val displayName: String,
    val tier: AiModelTier,
    val isRecommended: Boolean,
    val subtitle: String
)

object GeminiModelHelper {
    const val DEFAULT_MODEL = "gemini-1.5-flash"

    val DEFAULT_MODELS: List<String> = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-1.5-flash-8b",
        "gemini-2.5-pro",
        "gemini-1.5-pro"
    )

    fun classify(modelName: String, displayName: String? = null): ClassifiedAiModel {
        val clean = modelName.removePrefix("models/").trim()
        val lower = clean.lowercase()

        val tier = when {
            (lower.contains("flash") || lower.contains("flash-lite")) &&
                !lower.contains("exp") && !lower.contains("preview") -> AiModelTier.RECOMMENDED
            lower.contains("exp") || lower.contains("preview") || lower.contains("thinking") -> AiModelTier.EXPERIMENTAL
            lower.contains("pro") -> AiModelTier.PRO
            else -> AiModelTier.OTHER
        }

        val subtitle = when (tier) {
            AiModelTier.RECOMMENDED -> "Free tier friendly (15 RPM) • Fast & reliable"
            AiModelTier.PRO -> "Strict free quota (2 RPM) • May require paid billing"
            AiModelTier.EXPERIMENTAL -> "Preview model • Quotas & access may fluctuate"
            AiModelTier.OTHER -> "General Gemini model"
        }

        val cleanDisplayName = displayName?.takeIf { it.isNotBlank() } ?: clean

        return ClassifiedAiModel(
            id = clean,
            displayName = cleanDisplayName,
            tier = tier,
            isRecommended = tier == AiModelTier.RECOMMENDED,
            subtitle = subtitle
        )
    }

    fun sortModels(modelNames: List<String>): List<ClassifiedAiModel> {
        val classified = modelNames.map { classify(it) }
        return classified.sortedWith(
            compareBy<ClassifiedAiModel> {
                when (it.tier) {
                    AiModelTier.RECOMMENDED -> 0
                    AiModelTier.PRO -> 1
                    AiModelTier.EXPERIMENTAL -> 2
                    AiModelTier.OTHER -> 3
                }
            }.thenBy {
                // Deprioritize versioned snapshots (-001, -002, etc.) compared to canonical
                if (it.id.matches(Regex(""".*-\d{3}$"""))) 1 else 0
            }.thenBy {
                // Prioritize 2.5 > 2.0 > 1.5
                when {
                    it.id.contains("2.5") -> 0
                    it.id.contains("2.0") -> 1
                    it.id.contains("1.5") -> 2
                    else -> 3
                }
            }.thenBy { it.id }
        )
    }
}

val DEFAULT_GEMINI_MODELS: List<String> = GeminiModelHelper.DEFAULT_MODELS

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
