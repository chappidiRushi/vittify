package com.reddy.vittify.data.ai

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
                            dto.supportedGenerationMethods.isEmpty() || 
                            dto.supportedGenerationMethods.contains("generateContent")
                        }
                        .map { dto ->
                            GeminiModelInfo(
                                name = dto.name,
                                displayName = dto.displayName ?: dto.name.removePrefix("models/"),
                                description = dto.description ?: "",
                                supportedGenerationMethods = dto.supportedGenerationMethods
                            )
                        }

                    if (filteredModels.isNotEmpty()) {
                        Result.success(filteredModels)
                    } else {
                        // Fallback list if filtering returns empty
                        Result.success(DEFAULT_GEMINI_MODELS.map { GeminiModelInfo(name = "models/$it", displayName = it) })
                    }
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
                    availableModels = DEFAULT_GEMINI_MODELS
                )
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val modelsResult = fetchAvailableModels(apiKey)
                
                modelsResult.fold(
                    onSuccess = { models ->
                        val cleanModelNames = models.map { it.cleanName }
                        Result.success(
                            ConnectionTestResult(
                                isSuccess = true,
                                message = "Successfully connected to Gemini API! ${cleanModelNames.size} models available.",
                                availableModels = if (cleanModelNames.isNotEmpty()) cleanModelNames else DEFAULT_GEMINI_MODELS
                            )
                        )
                    },
                    onFailure = { error ->
                        Result.success(
                            ConnectionTestResult(
                                isSuccess = false,
                                message = error.message ?: "Failed to connect to Gemini API. Check your key & network connection.",
                                availableModels = DEFAULT_GEMINI_MODELS
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                Result.success(
                    ConnectionTestResult(
                        isSuccess = false,
                        message = e.localizedMessage ?: "Connection error occurred.",
                        availableModels = DEFAULT_GEMINI_MODELS
                    )
                )
            }
        }
    }

    override suspend fun generateContent(apiKey: String, model: String, prompt: String): Result<AiGenerationResult> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Gemini API key is required"))
        }

        val cleanModel = model.removePrefix("models/")

        return withContext(Dispatchers.IO) {
            try {
                val requestUrl = "$BASE_URL/models/$cleanModel:generateContent"
                
                @Serializable
                data class TextPart(val text: String)
                @Serializable
                data class Content(val parts: List<TextPart>)
                @Serializable
                data class GeminiRequest(val contents: List<Content>)

                val requestBody = GeminiRequest(
                    contents = listOf(Content(parts = listOf(TextPart(prompt))))
                )

                val response: HttpResponse = client.post(requestUrl) {
                    parameter("key", apiKey.trim())
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
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
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
