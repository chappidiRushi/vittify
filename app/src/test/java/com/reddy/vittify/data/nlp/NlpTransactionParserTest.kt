package com.reddy.vittify.data.nlp

import com.reddy.vittify.data.ai.AiModelTier
import com.reddy.vittify.data.ai.AiProviderType
import com.reddy.vittify.data.ai.GeminiConfig
import com.reddy.vittify.data.ai.GeminiModelHelper
import com.reddy.vittify.data.ai.GeminiModelInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testGeminiModelClassification() {
        val flash = GeminiModelHelper.classify("models/gemini-1.5-flash")
        assertEquals("gemini-1.5-flash", flash.id)
        assertEquals(AiModelTier.RECOMMENDED, flash.tier)
        assertTrue(flash.isRecommended)

        val flash20 = GeminiModelHelper.classify("gemini-2.0-flash")
        assertEquals(AiModelTier.RECOMMENDED, flash20.tier)
        assertTrue(flash20.isRecommended)

        val flash8b = GeminiModelHelper.classify("gemini-1.5-flash-8b")
        assertEquals(AiModelTier.RECOMMENDED, flash8b.tier)
        assertTrue(flash8b.isRecommended)

        val pro = GeminiModelHelper.classify("gemini-1.5-pro")
        assertEquals(AiModelTier.PRO, pro.tier)
        assertFalse(pro.isRecommended)

        val exp = GeminiModelHelper.classify("gemini-2.0-flash-exp")
        assertEquals(AiModelTier.EXPERIMENTAL, exp.tier)
        assertFalse(exp.isRecommended)

        val preview = GeminiModelHelper.classify("gemini-2.0-flash-lite-preview-02-05")
        assertEquals(AiModelTier.EXPERIMENTAL, preview.tier)
        assertFalse(preview.isRecommended)
    }

    @Test
    fun testGeminiModelSorting() {
        val unsorted = listOf(
            "gemini-1.5-pro",
            "gemini-2.0-flash-exp",
            "gemini-1.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-flash-001"
        )
        val sorted = GeminiModelHelper.sortModels(unsorted)
        val sortedIds = sorted.map { it.id }

        // Recommended flash models should be first
        assertEquals("gemini-2.0-flash", sortedIds[0])
        assertEquals("gemini-1.5-flash", sortedIds[1])
        assertEquals("gemini-1.5-flash-001", sortedIds[2])

        // Pro models next
        assertEquals("gemini-2.5-pro", sortedIds[3])
        assertEquals("gemini-1.5-pro", sortedIds[4])

        // Experimental last
        assertEquals("gemini-2.0-flash-exp", sortedIds[5])
    }
}

