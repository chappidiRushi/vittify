package com.reddy.vittify.data.ai

import android.util.Base64
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

interface AiProvider {
    val providerType: AiProviderType
    suspend fun fetchAvailableModels(apiKey: String): Result<List<GeminiModelInfo>>
    suspend fun testConnection(apiKey: String, model: String): Result<ConnectionTestResult>
    suspend fun generateContent(apiKey: String, model: String, prompt: String): Result<AiGenerationResult>
    suspend fun generateContentWithImage(
        apiKey: String,
        model: String,
        prompt: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<AiGenerationResult>
}

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val message: String,
    val availableModels: List<String> = emptyList()
)

data class AiGenerationResult(
    val text: String,
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val totalTokens: Int = 0
)

@Singleton
class GeminiAiProvider @Inject constructor() : AiProvider {
    override val providerType: AiProviderType = AiProviderType.GEMINI

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
                explicitNulls = false
            })
        }
    }

    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    override suspend fun fetchAvailableModels(apiKey: String): Result<List<GeminiModelInfo>> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API Key cannot be empty"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.get("$BASE_URL/models") {
                    parameter("key", apiKey.trim())
                    header("User-Agent", "Vittify-AndroidApp/1.0")
                }

                val status = response.status
                val bodyText = response.body<String>()

                if (status.value in 200..299) {
                    val json = Json { ignoreUnknownKeys = true }
                    val listResponse = json.decodeFromString<GeminiListModelsResponse>(bodyText)
                    
                    val filteredModels = listResponse.models
                        .filter { dto ->
                            val clean = dto.name.removePrefix("models/").lowercase()
                            val supportsGeneration = dto.supportedGenerationMethods.isEmpty() || 
                                dto.supportedGenerationMethods.contains("generateContent")

                            supportsGeneration &&
                                clean.contains("gemini") &&
                                !clean.contains("embedding") &&
                                !clean.contains("vision") &&
                                !clean.contains("bison") &&
                                !clean.contains("aqa") &&
                                !clean.contains("imagen")
                        }
                        .map { dto ->
                            GeminiModelInfo(
                                name = dto.name,
                                displayName = dto.displayName ?: dto.name.removePrefix("models/"),
                                description = dto.description ?: "",
                                supportedGenerationMethods = dto.supportedGenerationMethods
                            )
                        }

                    val sortedModels = if (filteredModels.isNotEmpty()) {
                        val sortedClassified = GeminiModelHelper.sortModels(filteredModels.map { it.cleanName }).map { it.id }
                        val orderMap = sortedClassified.withIndex().associate { it.value to it.index }
                        filteredModels.sortedBy { orderMap[it.cleanName] ?: Int.MAX_VALUE }
                    } else {
                        GeminiModelHelper.DEFAULT_MODELS.map { GeminiModelInfo(name = it, displayName = it) }
                    }

                    Result.success(sortedModels)
                } else {
                    val errorMessage = extractErrorMessage(bodyText, status.value)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun testConnection(apiKey: String, model: String): Result<ConnectionTestResult> {
        if (apiKey.isBlank()) {
            return Result.success(
                ConnectionTestResult(
                    isSuccess = false,
                    message = "API key is missing. Please enter your Google Gemini API key.",
                    availableModels = emptyList()
                )
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val modelsResult = fetchAvailableModels(apiKey)
                
                modelsResult.fold(
                    onSuccess = { models ->
                        val cleanModelNames = models.map { it.cleanName }
                        val targetModel = model.trim().removePrefix("models/").ifBlank {
                            cleanModelNames.firstOrNull { it.contains("flash", ignoreCase = true) && !it.contains("exp", ignoreCase = true) }
                                ?: cleanModelNames.firstOrNull()
                                ?: GeminiModelHelper.DEFAULT_MODEL
                        }

                        // Test actual generation with the target model to verify quotas and access
                        val testGenResult = generateContent(apiKey, targetModel, "Test connection. Reply with OK.")
                        testGenResult.fold(
                            onSuccess = {
                                val classified = GeminiModelHelper.classify(targetModel)
                                val notice = when (classified.tier) {
                                    AiModelTier.RECOMMENDED -> "Recommended for free tier (15 RPM)."
                                    AiModelTier.PRO -> "Note: Pro model has strict free rate limits (2 RPM)."
                                    AiModelTier.EXPERIMENTAL -> "Note: Experimental model."
                                    else -> ""
                                }
                                val fullMsg = "Successfully verified! Model '$targetModel' is working. $notice (${cleanModelNames.size} models loaded)"
                                Result.success(
                                    ConnectionTestResult(
                                        isSuccess = true,
                                        message = fullMsg.trim(),
                                        availableModels = cleanModelNames
                                    )
                                )
                            },
                            onFailure = { genError ->
                                val errorText = genError.message ?: ""
                                val friendlyMessage = when {
                                    errorText.contains("429") || errorText.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || errorText.contains("quota", ignoreCase = true) -> {
                                        "Model '$targetModel' quota or rate limit exceeded. Free tier accounts frequently run out of quota on this model. Please switch to a Recommended Flash model like 'gemini-1.5-flash' or 'gemini-2.0-flash'."
                                    }
                                    errorText.contains("403") || errorText.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                                        "Access denied for '$targetModel'. Your API key does not have permission for this model (may require paid billing). Switch to a Recommended model like 'gemini-1.5-flash'."
                                    }
                                    errorText.contains("404") || errorText.contains("NOT_FOUND", ignoreCase = true) -> {
                                        "Model '$targetModel' was not found or is discontinued. Please select a Recommended model from the list."
                                    }
                                    else -> {
                                        "Model '$targetModel' verification failed: $errorText. Try selecting a Recommended Flash model like 'gemini-1.5-flash'."
                                    }
                                }
                                Result.success(
                                    ConnectionTestResult(
                                        isSuccess = false,
                                        message = friendlyMessage,
                                        availableModels = cleanModelNames
                                    )
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        Result.success(
                            ConnectionTestResult(
                                isSuccess = false,
                                message = error.message ?: "Failed to connect to Gemini API. Check your key & network connection.",
                                availableModels = emptyList()
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                Result.success(
                    ConnectionTestResult(
                        isSuccess = false,
                        message = e.localizedMessage ?: "Connection error occurred.",
                        availableModels = emptyList()
                    )
                )
            }
        }
    }

    override suspend fun generateContent(apiKey: String, model: String, prompt: String): Result<AiGenerationResult> {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Gemini API key is required"))
        }

        val cleanModel = model.trim().removePrefix("models/").ifBlank { "gemini-1.5-flash" }

        return withContext(Dispatchers.IO) {
            try {
                val requestUrl = "$BASE_URL/models/$cleanModel:generateContent"

                val requestBody = GeminiGenerateRequest(
                    contents = listOf(
                        GeminiRequestContent(
                            parts = listOf(GeminiRequestPart(text = prompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1f
                    )
                )

                val response: HttpResponse = client.post(requestUrl) {
                    parameter("key", trimmedKey)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                val bodyText = response.body<String>()
                if (response.status.value in 200..299) {
                    val json = Json { ignoreUnknownKeys = true }
                    val jsonElement = json.parseToJsonElement(bodyText).jsonObject
                    
                    val text = jsonElement["candidates"]
                        ?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: ""

                    val usageMetadata = jsonElement["usageMetadata"]?.jsonObject
                    val promptTokens = usageMetadata?.get("promptTokenCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val candidateTokens = usageMetadata?.get("candidatesTokenCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val totalTokens = usageMetadata?.get("totalTokenCount")?.jsonPrimitive?.content?.toIntOrNull() ?: (promptTokens + candidateTokens)

                    Result.success(
                        AiGenerationResult(
                            text = text,
                            promptTokens = promptTokens,
                            candidateTokens = candidateTokens,
                            totalTokens = totalTokens
                        )
                    )
                } else {
                    val errorMessage = extractErrorMessage(bodyText, response.status.value)
                    Log.e("GeminiAiProvider", "generateContent HTTP error: $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("GeminiAiProvider", "generateContent request failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun generateContentWithImage(
        apiKey: String,
        model: String,
        prompt: String,
        imageBytes: ByteArray,
        mimeType: String
    ): Result<AiGenerationResult> {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Gemini API key is required"))
        }

        val cleanModel = model.trim().removePrefix("models/").ifBlank { "gemini-1.5-flash" }

        return withContext(Dispatchers.IO) {
            try {
                val base64Data = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                val requestBody = GeminiGenerateRequest(
                    contents = listOf(
                        GeminiRequestContent(
                            parts = listOf(
                                GeminiRequestPart(
                                    inlineData = GeminiBlob(
                                        mimeType = mimeType,
                                        data = base64Data
                                    )
                                ),
                                GeminiRequestPart(text = prompt)
                            )
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1f
                    )
                )

                var response: HttpResponse = client.post("$BASE_URL/models/$cleanModel:generateContent") {
                    parameter("key", trimmedKey)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                var bodyText = response.body<String>()

                // Fallback 1: If responseMimeType is not supported by the model, retry without it
                if (response.status.value in 400..499 && bodyText.contains("responseMimeType", ignoreCase = true)) {
                    val fallbackBody = GeminiGenerateRequest(
                        contents = requestBody.contents,
                        generationConfig = GeminiGenerationConfig(temperature = 0.1f)
                    )
                    response = client.post("$BASE_URL/models/$cleanModel:generateContent") {
                        parameter("key", trimmedKey)
                        contentType(ContentType.Application.Json)
                        setBody(fallbackBody)
                    }
                    bodyText = response.body<String>()
                }

                // Fallback 2: If the custom model is not found or unsupported for vision, retry with default flash model
                if (response.status.value in 400..499 && cleanModel != "gemini-1.5-flash" && cleanModel != "gemini-2.5-flash") {
                    Log.w("GeminiAiProvider", "Model $cleanModel failed (${response.status.value}), attempting fallback to gemini-1.5-flash")
                    response = client.post("$BASE_URL/models/gemini-1.5-flash:generateContent") {
                        parameter("key", trimmedKey)
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                    }
                    bodyText = response.body<String>()
                }

                if (response.status.value in 200..299) {
                    val json = Json { ignoreUnknownKeys = true }
                    val jsonElement = json.parseToJsonElement(bodyText).jsonObject

                    val text = jsonElement["candidates"]
                        ?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: ""

                    val usageMetadata = jsonElement["usageMetadata"]?.jsonObject
                    val promptTokens = usageMetadata?.get("promptTokenCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val candidateTokens = usageMetadata?.get("candidatesTokenCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val totalTokens = usageMetadata?.get("totalTokenCount")?.jsonPrimitive?.content?.toIntOrNull() ?: (promptTokens + candidateTokens)

                    Result.success(
                        AiGenerationResult(
                            text = text,
                            promptTokens = promptTokens,
                            candidateTokens = candidateTokens,
                            totalTokens = totalTokens
                        )
                    )
                } else {
                    val errorMessage = extractErrorMessage(bodyText, response.status.value)
                    Log.e("GeminiAiProvider", "generateContentWithImage HTTP error: $errorMessage, body: $bodyText")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("GeminiAiProvider", "generateContentWithImage request failed", e)
                Result.failure(e)
            }
        }
    }

    private fun extractErrorMessage(bodyText: String, statusCode: Int): String {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val obj = json.parseToJsonElement(bodyText).jsonObject
            val errorObj = obj["error"]?.jsonObject
            val message = errorObj?.get("message")?.jsonPrimitive?.content
            if (!message.isNullOrBlank()) {
                "Gemini API Error ($statusCode): $message"
            } else {
                "HTTP $statusCode: Failed to communicate with Gemini API"
            }
        } catch (_: Exception) {
            "HTTP $statusCode: Gemini API Request Failed"
        }
    }
}
