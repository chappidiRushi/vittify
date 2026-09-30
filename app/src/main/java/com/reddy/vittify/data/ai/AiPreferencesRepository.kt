package com.reddy.vittify.data.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_preferences")

@Singleton
class AiPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AiPreferencesRepo"
        private const val SECURE_PREFS_NAME = "secure_ai_preferences"
        private const val KEY_ENCRYPTED_GEMINI_API_KEY = "encrypted_gemini_api_key"
    }

    private object Keys {
        val IS_AI_ENABLED = booleanPreferencesKey("is_ai_enabled")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key") // Legacy key for migration
        val GEMINI_SELECTED_MODEL = stringPreferencesKey("gemini_selected_model")
        val GEMINI_AVAILABLE_MODELS_JSON = stringPreferencesKey("gemini_available_models_json")
        val GEMINI_ENABLED = booleanPreferencesKey("gemini_enabled")
        val GEMINI_TOTAL_REQUESTS = intPreferencesKey("gemini_total_requests")
        val GEMINI_TOTAL_TOKENS = longPreferencesKey("gemini_total_tokens")
        val GEMINI_LAST_TESTED_AT = longPreferencesKey("gemini_last_tested_at")
        val GEMINI_LAST_TEST_SUCCESS = booleanPreferencesKey("gemini_last_test_success")
        val GEMINI_LAST_TEST_MESSAGE = stringPreferencesKey("gemini_last_test_message")
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                SECURE_PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences fallback", e)
            context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _apiKeyFlow = MutableStateFlow(getEncryptedApiKey())

    private fun getEncryptedApiKey(): String {
        return securePrefs.getString(KEY_ENCRYPTED_GEMINI_API_KEY, null) ?: ""
    }

    val isAiEnabled: Flow<Boolean> = context.aiDataStore.data.map { preferences ->
        preferences[Keys.IS_AI_ENABLED] ?: true
    }

    val selectedProvider: Flow<AiProviderType> = context.aiDataStore.data.map { preferences ->
        AiProviderType.fromId(preferences[Keys.SELECTED_PROVIDER])
    }

    val geminiConfig: Flow<GeminiConfig> = combine(
        context.aiDataStore.data,
        _apiKeyFlow
    ) { preferences, apiKeyFromFlow ->
        // Check for migration from legacy plaintext DataStore key if securePrefs has no key yet
        val legacyKey = preferences[Keys.GEMINI_API_KEY]
        val effectiveApiKey = if (!legacyKey.isNullOrBlank() && apiKeyFromFlow.isBlank()) {
            val trimmedLegacy = legacyKey.trim()
            securePrefs.edit().putString(KEY_ENCRYPTED_GEMINI_API_KEY, trimmedLegacy).apply()
            _apiKeyFlow.value = trimmedLegacy
            trimmedLegacy
        } else {
            apiKeyFromFlow
        }

        val modelsJson = preferences[Keys.GEMINI_AVAILABLE_MODELS_JSON]
        val modelsList = if (modelsJson != null) {
            runCatching { Json.decodeFromString<List<String>>(modelsJson) }.getOrElse { DEFAULT_GEMINI_MODELS }
        } else {
            DEFAULT_GEMINI_MODELS
        }

        GeminiConfig(
            apiKey = effectiveApiKey,
            selectedModel = preferences[Keys.GEMINI_SELECTED_MODEL] ?: "gemini-2.5-flash",
            availableModels = modelsList.ifEmpty { DEFAULT_GEMINI_MODELS },
            isEnabled = preferences[Keys.GEMINI_ENABLED] ?: true,
            isConfigured = effectiveApiKey.isNotBlank() && (preferences[Keys.GEMINI_LAST_TEST_SUCCESS] == true),
            totalRequests = preferences[Keys.GEMINI_TOTAL_REQUESTS] ?: 0,
            totalTokens = preferences[Keys.GEMINI_TOTAL_TOKENS] ?: 0L,
            lastTestedAt = preferences[Keys.GEMINI_LAST_TESTED_AT] ?: 0L,
            lastTestSuccess = preferences[Keys.GEMINI_LAST_TEST_SUCCESS],
            lastTestMessage = preferences[Keys.GEMINI_LAST_TEST_MESSAGE]
        )
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.aiDataStore.edit { preferences ->
            preferences[Keys.IS_AI_ENABLED] = enabled
        }
    }

    suspend fun setSelectedProvider(provider: AiProviderType) {
        context.aiDataStore.edit { preferences ->
            preferences[Keys.SELECTED_PROVIDER] = provider.id
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        securePrefs.edit().putString(KEY_ENCRYPTED_GEMINI_API_KEY, trimmed).apply()
        _apiKeyFlow.value = trimmed
        context.aiDataStore.edit { preferences ->
            // Clean up plaintext key if previously set
            preferences.remove(Keys.GEMINI_API_KEY)
            // Reset test status when API key changes
            preferences.remove(Keys.GEMINI_LAST_TEST_SUCCESS)
            preferences.remove(Keys.GEMINI_LAST_TEST_MESSAGE)
        }
    }

    suspend fun setGeminiSelectedModel(model: String) {
        context.aiDataStore.edit { preferences ->
            preferences[Keys.GEMINI_SELECTED_MODEL] = model
        }
    }

    suspend fun setGeminiAvailableModels(models: List<String>) {
        context.aiDataStore.edit { preferences ->
            preferences[Keys.GEMINI_AVAILABLE_MODELS_JSON] = Json.encodeToString(models)
        }
    }

    suspend fun setGeminiEnabled(enabled: Boolean) {
        context.aiDataStore.edit { preferences ->
            preferences[Keys.GEMINI_ENABLED] = enabled
        }
    }

    suspend fun saveGeminiTestResult(success: Boolean, message: String, availableModels: List<String> = emptyList()) {
        context.aiDataStore.edit { preferences ->
            preferences[Keys.GEMINI_LAST_TESTED_AT] = System.currentTimeMillis()
            preferences[Keys.GEMINI_LAST_TEST_SUCCESS] = success
            preferences[Keys.GEMINI_LAST_TEST_MESSAGE] = message
            if (availableModels.isNotEmpty()) {
                preferences[Keys.GEMINI_AVAILABLE_MODELS_JSON] = Json.encodeToString(availableModels)
                if (preferences[Keys.GEMINI_SELECTED_MODEL] !in availableModels) {
                    preferences[Keys.GEMINI_SELECTED_MODEL] = availableModels.first()
                }
            }
        }
    }

    suspend fun recordRequestUsage(tokensUsed: Int) {
        context.aiDataStore.edit { preferences ->
            val currentRequests = preferences[Keys.GEMINI_TOTAL_REQUESTS] ?: 0
            val currentTokens = preferences[Keys.GEMINI_TOTAL_TOKENS] ?: 0L
            preferences[Keys.GEMINI_TOTAL_REQUESTS] = currentRequests + 1
            preferences[Keys.GEMINI_TOTAL_TOKENS] = currentTokens + tokensUsed
        }
    }

    suspend fun resetMetrics() {
        context.aiDataStore.edit { preferences ->
            preferences[Keys.GEMINI_TOTAL_REQUESTS] = 0
            preferences[Keys.GEMINI_TOTAL_TOKENS] = 0L
        }
    }
}
