package com.reddy.vittify.presentation.navigation

import android.net.Uri

/**
 * Encapsulates the resolved target for a settings navigation or deep link.
 */
data class SettingsDeepLinkTarget(
    val page: String,
    val option: String? = null,
    val destination: Any
)

/**
 * Central parser and URL builder for settings deep links.
 * Supports natural and easy to remember URLs:
 * - vittify://settings/{page}/{option} (e.g. vittify://settings/appearance/dynamic-theme)
 * - vittify://settings/{page} (e.g. vittify://settings/appearance)
 * - vittify://settings
 * - https://vittify.app/settings/{page}/{option}
 * - settings/{page}/{option}
 */
object SettingsDeepLink {

    const val SCHEME = "vittify"
    const val HOST = "settings"
    const val WEB_HOST = "vittify.app"

    fun createUri(page: String, option: String? = null): String {
        val cleanPage = normalizePageSlug(page)
        return if (!option.isNullOrBlank()) {
            val cleanOption = normalizeOptionSlug(cleanPage, option)
            "$SCHEME://$HOST/$cleanPage/$cleanOption"
        } else {
            "$SCHEME://$HOST/$cleanPage"
        }
    }

    fun parse(uri: Uri?): SettingsDeepLinkTarget? {
        if (uri == null) return null
        return parse(uri.toString())
    }

    fun parse(uriString: String?): SettingsDeepLinkTarget? {
        if (uriString.isNullOrBlank()) return null
        val trimmed = uriString.trim()

        val parsedUri = try {
            Uri.parse(trimmed)
        } catch (_: Exception) {
            null
        }

        var segments: List<String> = when {
            parsedUri != null && parsedUri.scheme.equals(SCHEME, ignoreCase = true) &&
                    parsedUri.host.equals(HOST, ignoreCase = true) -> {
                // vittify://settings/{page}/{option}
                parsedUri.pathSegments
            }
            parsedUri != null && parsedUri.scheme.equals(SCHEME, ignoreCase = true) &&
                    parsedUri.host.equals("app", ignoreCase = true) -> {
                // vittify://app/settings/{page}/{option}
                val pathSegs = parsedUri.pathSegments
                if (pathSegs.firstOrNull()?.equals("settings", ignoreCase = true) == true) {
                    pathSegs.drop(1)
                } else {
                    pathSegs
                }
            }
            parsedUri != null && (parsedUri.scheme.equals("http", ignoreCase = true) || parsedUri.scheme.equals("https", ignoreCase = true)) &&
                    (parsedUri.host.equals(WEB_HOST, ignoreCase = true) || parsedUri.host?.endsWith("vittify.app") == true) -> {
                // https://vittify.app/settings/{page}/{option}
                val pathSegs = parsedUri.pathSegments
                if (pathSegs.firstOrNull()?.equals("settings", ignoreCase = true) == true) {
                    pathSegs.drop(1)
                } else {
                    pathSegs
                }
            }
            else -> emptyList()
        }

        // If URI parsing yielded no segments, fall back to robust string stripping
        if (segments.isEmpty() && !trimmed.equals("vittify://settings", ignoreCase = true) && !trimmed.equals("settings", ignoreCase = true)) {
            val withoutScheme = trimmed
                .removePrefix("https://vittify.app/")
                .removePrefix("http://vittify.app/")
                .removePrefix("vittify://settings/")
                .removePrefix("vittify://settings")
                .removePrefix("vittify://app/settings/")
                .removePrefix("vittify://app/settings")
                .removePrefix("vittify://")
                .removePrefix("/settings/")
                .removePrefix("settings/")
                .removePrefix("/")
                .split("?")[0]
                .trim()

            val rawParts = withoutScheme.split("/").map { it.trim() }.filter { it.isNotBlank() }
            segments = if (rawParts.firstOrNull()?.equals("settings", ignoreCase = true) == true) {
                rawParts.drop(1)
            } else {
                rawParts
            }
        }

        return resolveTarget(segments)
    }

    private fun resolveTarget(segments: List<String>): SettingsDeepLinkTarget {
        if (segments.isEmpty()) {
            return SettingsDeepLinkTarget(
                page = "settings",
                option = null,
                destination = Settings()
            )
        }

        val rawPage = segments[0]
        val rawOption = segments.getOrNull(1)

        val page = normalizePageSlug(rawPage)
        val option = if (!rawOption.isNullOrBlank()) normalizeOptionSlug(page, rawOption) else null

        val destination: Any = when (page) {
            "appearance" -> Appearance(targetOptionId = option)
            "transactions" -> TransactionSettings(targetOptionId = option)
            "notifications" -> NotificationSettings(targetOptionId = option)
            "data-privacy" -> DataPrivacy(targetOptionId = option)
            "backup" -> CloudBackup(targetOptionId = option)
            "currency" -> CurrencySettings(targetOptionId = option)
            "customization" -> Customization(targetOptionId = option)
            "ai" -> AiSettings(targetOptionId = option)
            "sms" -> SmsSettings(targetOptionId = option)
            "profile" -> Profile
            "accounts" -> ManageAccounts
            "categories" -> Categories
            "budgets" -> Budgets()
            "rules" -> Rules
            "couple-tracker" -> CoupleTracker
            "p2p-sync" -> P2pDeviceSync
            "webhooks" -> Webhooks
            "pdf-report" -> PdfReport
            "archived-transactions" -> ManageArchivedTransactions
            "data-sanitization" -> DataSanitization(initialTab = option)
            "about" -> About
            "licenses" -> Licenses
            "faq" -> Faq
            "guides" -> Guides
            "developer" -> DeveloperOptions
            else -> Settings(targetSettingId = rawPage)
        }

        return SettingsDeepLinkTarget(
            page = page,
            option = option,
            destination = destination
        )
    }

    fun normalizePageSlug(page: String): String {
        val clean = page.lowercase().trim().replace("_", "-")
        return when (clean) {
            "appearance", "appearce", "theme", "themes", "styling", "ui" -> "appearance"
            "transactions", "transaction", "transaction-settings", "transaction-preferences" -> "transactions"
            "notifications", "notification", "alerts", "reminders" -> "notifications"
            "data-privacy", "dataprivacy", "privacy", "security", "security-safe" -> "data-privacy"
            "backup", "cloud-backup", "cloudbackup", "sync-backup", "gdrive", "webdav" -> "backup"
            "currency", "currencies", "exchange-rates" -> "currency"
            "customization", "customize", "custom" -> "customization"
            "ai", "ai-settings", "gemini", "intelligence" -> "ai"
            "sms", "sms-settings", "messages" -> "sms"
            "profile", "user", "account" -> "profile"
            "accounts", "manage-accounts", "bank-accounts" -> "accounts"
            "categories", "category", "tags" -> "categories"
            "budgets", "budget", "spending-limits" -> "budgets"
            "rules", "smart-rules", "automation" -> "rules"
            "couple-tracker", "couple", "partner" -> "couple-tracker"
            "p2p-sync", "p2p", "device-sync" -> "p2p-sync"
            "webhooks", "webhook", "api" -> "webhooks"
            "pdf-report", "pdf", "statement" -> "pdf-report"
            "archived-transactions", "archive", "archived", "trash" -> "archived-transactions"
            "data-sanitization", "sanitization", "data-health" -> "data-sanitization"
            "about", "info", "version" -> "about"
            "licenses", "license", "open-source" -> "licenses"
            "faq", "help", "questions" -> "faq"
            "guides", "guide", "tutorials" -> "guides"
            "developer", "developer-options", "dev" -> "developer"
            else -> clean
        }
    }

    fun normalizeOptionSlug(page: String, option: String): String {
        val clean = option.lowercase().trim().replace("_", "-")
        return when (page) {
            "appearance" -> when (clean) {
                "dynamic-theme", "dynamic", "wallpaper", "monet", "dynamic-wallpaper" -> "dynamic-theme"
                "theme-mode", "dark-mode", "light-mode", "system-mode", "theme" -> "theme-mode"
                "amoled-black", "amoled-mode", "amoled", "pure-black", "pitch-black" -> "amoled-black"
                "curated-palette", "curated-theme", "curated", "palette", "palettes", "accent", "colors" -> "curated-palette"
                "custom-seed", "seed-color", "seed", "color-picker" -> "custom-seed"
                "navigation-style", "floating-nav", "nav-style", "navigation-bar", "docked-nav" -> "navigation-style"
                "hide-nav-labels", "nav-labels", "hide-labels" -> "hide-nav-labels"
                "hide-pill-indicator", "pill-indicator", "hide-indicator" -> "hide-pill-indicator"
                "profile-switcher-footer", "footer-profile", "footer-switcher" -> "profile-switcher-footer"
                "app-logo", "app-icon", "launcher-icon", "logo", "icon" -> "app-logo"
                "blur-effects", "blur", "glassmorphism", "haze" -> "blur-effects"
                "playful-animation", "playful-animations", "animations", "motion", "spring" -> "playful-animation"
                "background-effects", "canvas-effects", "canvas", "particles" -> "background-effects"
                "app-font", "font", "fonts", "typography", "typeface", "custom-font" -> "app-font"
                "surface-geometry", "geometry", "spacing", "corner-radius", "tokens" -> "surface-geometry"
                else -> clean
            }
            "transactions" -> when (clean) {
                "use-category-as-merchant", "category-as-merchant" -> "use-category-as-merchant"
                "use-category-as-merchant-sms", "category-sms" -> "use-category-as-merchant-sms"
                "preserve-account-order", "account-order" -> "preserve-account-order"
                "direct-field-editing", "field-editing", "inline-editing" -> "direct-field-editing"
                else -> clean
            }
            "notifications" -> when (clean) {
                "bank-push-notifications", "bank-push", "notification-access", "push-notifications", "push-notification", "push" -> "bank-push-notifications"
                "remind-transactions", "scan-alert", "reminder", "unprocessed" -> "remind-transactions"
                "alert-time", "reminder-time", "time" -> "alert-time"
                "upcoming-alerts", "upcoming-subscriptions", "subscriptions-alert", "upcoming" -> "upcoming-alerts"
                else -> clean
            }
            "data-privacy" -> when (clean) {
                "app-lock", "biometrics", "biometric-lock", "fingerprint", "face-unlock" -> "app-lock"
                "lock-timeout", "timeout" -> "lock-timeout"
                "data-sanitization", "sanitization", "hygiene" -> "data-sanitization"
                "export-backup", "export-data", "export", "backup-export" -> "export-backup"
                "import-backup", "import-data", "import", "backup-import" -> "import-backup"
                "import-pdf", "pdf-import", "pdf-statement" -> "import-pdf"
                "delete-all-data", "clear-data", "reset-data" -> "delete-all-data"
                else -> clean
            }
            "backup" -> when (clean) {
                "google-drive", "drive", "gdrive" -> "google-drive"
                "webdav", "nextcloud", "owncloud" -> "webdav"
                "auto-backup-schedule", "backup-schedule", "schedule" -> "auto-backup-schedule"
                "backup-passphrase", "passphrase", "encryption" -> "backup-passphrase"
                else -> clean
            }
            "currency" -> when (clean) {
                "unified-currency", "base-currency" -> "unified-currency"
                "currency-picker", "select-currency" -> "currency-picker"
                "exchange-rates", "rates", "forex" -> "exchange-rates"
                "convert-historical", "historical" -> "convert-historical"
                else -> clean
            }
            "ai" -> when (clean) {
                "enable-ai", "ai-enabled" -> "enable-ai"
                "api-key", "gemini-key", "key" -> "api-key"
                "ai-model", "model-selection", "model" -> "ai-model"
                "token-limit", "tokens" -> "token-limit"
                "temperature" -> "temperature"
                else -> clean
            }
            "sms" -> when (clean) {
                "scan-range", "sms-range", "months" -> "scan-range"
                "scan-all-time", "all-time" -> "scan-all-time"
                "unrecognized-sms", "unrecognized" -> "unrecognized-sms"
                else -> clean
            }
            "customization" -> when (clean) {
                "global-search", "global-search-settings", "search" -> "global-search"
                else -> clean
            }
            else -> clean
        }
    }
}
