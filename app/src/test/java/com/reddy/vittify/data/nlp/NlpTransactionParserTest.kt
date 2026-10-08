package com.reddy.vittify.data.nlp

import com.reddy.vittify.data.ai.AiProviderType
import com.reddy.vittify.data.ai.GeminiConfig
import com.reddy.vittify.data.ai.GeminiModelInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiConfigTest {

    @Test
    fun testGeminiConfigDefaults() {
        val config = GeminiConfig()
        assertTrue(config.availableModels.isEmpty())
        assertEquals("", config.selectedModel)
        assertTrue(config.isEnabled)
        assertTrue(config.includeCategories)
        assertTrue(config.includeBankAccounts)
        assertEquals("", config.customRules)
    }

    @Test
    fun testGeminiModelInfoCleanName() {
        val modelWithPrefix = GeminiModelInfo(name = "models/gemini-2.5-flash")
        assertEquals("gemini-2.5-flash", modelWithPrefix.cleanName)

        val modelWithoutPrefix = GeminiModelInfo(name = "gemini-1.5-flash")
        assertEquals("gemini-1.5-flash", modelWithoutPrefix.cleanName)
    }

    @Test
    fun testAiProviderTypeDefault() {
        assertEquals(AiProviderType.GEMINI, AiProviderType.fromId(null))
        assertEquals(AiProviderType.GEMINI, AiProviderType.fromId("gemini"))
        assertEquals(AiProviderType.GEMINI, AiProviderType.fromId("unknown"))
    }
}

