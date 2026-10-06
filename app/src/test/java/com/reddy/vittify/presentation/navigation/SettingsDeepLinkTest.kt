package com.reddy.vittify.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDeepLinkTest {

    @Test
    fun parse_appearance_dynamicTheme_deepLink() {
        val target = SettingsDeepLink.parse("vittify://settings/appearance/dynamic-theme")
        assertNotNull(target)
        assertEquals("appearance", target?.page)
        assertEquals("dynamic-theme", target?.option)
        assertTrue(target?.destination is Appearance)
        assertEquals("dynamic-theme", (target?.destination as Appearance).targetOptionId)
    }

    @Test
    fun parse_appearance_aliasAndTypos() {
        // Natural link with alias "dynamic"
        val targetAlias = SettingsDeepLink.parse("settings/appearance/dynamic")
        assertNotNull(targetAlias)
        assertEquals("dynamic-theme", targetAlias?.option)

        // Typo in page "appearce"
        val targetTypo = SettingsDeepLink.parse("settings/appearce/amoled")
        assertNotNull(targetTypo)
        assertEquals("appearance", targetTypo?.page)
        assertEquals("amoled-black", targetTypo?.option)
        assertEquals("amoled-black", (targetTypo?.destination as Appearance).targetOptionId)
    }

    @Test
    fun parse_webUrl_https() {
        val target = SettingsDeepLink.parse("https://vittify.app/settings/notifications/bank-push")
        assertNotNull(target)
        assertEquals("notifications", target?.page)
        assertEquals("bank-push-notifications", target?.option)
        assertTrue(target?.destination is NotificationSettings)
        assertEquals("bank-push-notifications", (target?.destination as NotificationSettings).targetOptionId)
    }

    @Test
    fun parse_dataPrivacy_appLock() {
        val target = SettingsDeepLink.parse("vittify://settings/data-privacy/app-lock")
        assertNotNull(target)
        assertEquals("data-privacy", target?.page)
        assertEquals("app-lock", target?.option)
        assertTrue(target?.destination is DataPrivacy)
        assertEquals("app-lock", (target?.destination as DataPrivacy).targetOptionId)
    }

    @Test
    fun parse_transactions_useCategoryAsMerchant() {
        val target = SettingsDeepLink.parse("settings/transactions/use-category-as-merchant")
        assertNotNull(target)
        assertEquals("transactions", target?.page)
        assertEquals("use-category-as-merchant", target?.option)
        assertTrue(target?.destination is TransactionSettings)
        assertEquals("use-category-as-merchant", (target?.destination as TransactionSettings).targetOptionId)
    }

    @Test
    fun parse_cloudBackup_importBackup() {
        val target = SettingsDeepLink.parse("vittify://settings/backup/import-backup")
        assertNotNull(target)
        assertEquals("backup", target?.page)
        assertEquals("import-backup", target?.option)
        assertTrue(target?.destination is CloudBackup)
        assertEquals("import-backup", (target?.destination as CloudBackup).targetOptionId)
    }

    @Test
    fun parse_currency_exchangeRates() {
        val target = SettingsDeepLink.parse("settings/currency/exchange-rates")
        assertNotNull(target)
        assertEquals("currency", target?.page)
        assertEquals("exchange-rates", target?.option)
        assertTrue(target?.destination is CurrencySettings)
        assertEquals("exchange-rates", (target?.destination as CurrencySettings).targetOptionId)
    }

    @Test
    fun parse_ai_apiKey() {
        val target = SettingsDeepLink.parse("vittify://settings/ai/api-key")
        assertNotNull(target)
        assertEquals("ai", target?.page)
        assertEquals("api-key", target?.option)
        assertTrue(target?.destination is AiSettings)
        assertEquals("api-key", (target?.destination as AiSettings).targetOptionId)
    }

    @Test
    fun parse_sms_scanRange() {
        val target = SettingsDeepLink.parse("settings/sms/scan-range")
        assertNotNull(target)
        assertEquals("sms", target?.page)
        assertEquals("scan-range", target?.option)
        assertTrue(target?.destination is SmsSettings)
        assertEquals("scan-range", (target?.destination as SmsSettings).targetOptionId)
    }

    @Test
    fun parse_customization_globalSearch() {
        val target = SettingsDeepLink.parse("vittify://settings/customization/global-search")
        assertNotNull(target)
        assertEquals("customization", target?.page)
        assertEquals("global-search", target?.option)
        assertTrue(target?.destination is Customization)
        assertEquals("global-search", (target?.destination as Customization).targetOptionId)
    }

    @Test
    fun parse_topLevelSettings() {
        val target = SettingsDeepLink.parse("vittify://settings")
        assertNotNull(target)
        assertEquals("settings", target?.page)
        assertTrue(target?.destination is Settings)
    }

    @Test
    fun createUri_generatesCleanUri() {
        val uri = SettingsDeepLink.createUri("appearance", "dynamic-theme")
        assertEquals("vittify://settings/appearance/dynamic-theme", uri)

        val pageOnlyUri = SettingsDeepLink.createUri("appearance")
        assertEquals("vittify://settings/appearance", pageOnlyUri)
    }
}
