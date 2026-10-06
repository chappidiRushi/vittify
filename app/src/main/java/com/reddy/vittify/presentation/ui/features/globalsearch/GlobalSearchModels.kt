package com.reddy.vittify.presentation.ui.features.globalsearch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.preferences.GlobalSearchPreferences
import com.reddy.vittify.presentation.navigation.*
import com.reddy.vittify.presentation.ui.icons.*
import com.reddy.vittify.presentation.ui.theme.*

data class GlobalSearchSettingItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val keywords: List<String>,
    val iconVector: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val targetSettingId: String = id,
    val parentSection: String? = null,
    val destination: Any? = null,
    val deepLinkUri: String? = null
)

data class GlobalSearchPageItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val keywords: List<String>,
    val iconVector: ImageVector,
    val destination: Any
)

data class GlobalSearchGroupedResults(
    val accounts: List<AccountBalanceEntity> = emptyList(),
    val settings: List<GlobalSearchSettingItem> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val pages: List<GlobalSearchPageItem> = emptyList()
) {
    val isEmpty: Boolean
        get() = accounts.isEmpty() && settings.isEmpty() && transactions.isEmpty() && pages.isEmpty()

    val totalCount: Int
        get() = accounts.size + settings.size + transactions.size + pages.size
}

data class GlobalSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val preferences: GlobalSearchPreferences = GlobalSearchPreferences(),
    val results: GlobalSearchGroupedResults = GlobalSearchGroupedResults()
)

object GlobalSearchIndex {

    val allSettings = listOf(
        GlobalSearchSettingItem(
            id = "profile",
            title = "Profile",
            subtitle = "User account, profile photo, and name",
            keywords = listOf("profile", "user", "name", "avatar", "photo", "picture", "account", "display name", "personal info"),
            iconVector = Icons.Rounded.Person,
            iconTint = blue_dark,
            iconBg = blue_light,
            destination = Profile,
            deepLinkUri = "settings/profile"
        ),
        GlobalSearchSettingItem(
            id = "appearance",
            title = "Appearance",
            subtitle = "Theme, color scheme, amoled mode, typography",
            keywords = listOf("appearance", "theme", "dark", "light", "amoled", "font", "accent", "color", "style", "nav", "monet", "custom font", "pure black"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            destination = Appearance(),
            deepLinkUri = "settings/appearance"
        ),
        GlobalSearchSettingItem(
            id = "language",
            title = "Language",
            subtitle = "App language and display translation",
            keywords = listOf("language", "locale", "translate", "translation", "english", "hindi", "spanish", "french", "german", "display language"),
            iconVector = Icons.Rounded.Translate,
            iconTint = blue_dark,
            iconBg = blue_light,
            deepLinkUri = "settings/language"
        ),
        GlobalSearchSettingItem(
            id = "currency",
            title = "Currency",
            subtitle = "Base currency, default currency, unified currency",
            keywords = listOf("currency", "rates", "inr", "usd", "eur", "gbp", "base currency", "exchange", "money", "foreign exchange", "forex"),
            iconVector = Iconax.DollarCircle,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            destination = CurrencySettings(),
            deepLinkUri = "settings/currency"
        ),
        GlobalSearchSettingItem(
            id = "notifications",
            title = "Notifications",
            subtitle = "Transaction alerts and daily summary reminders",
            keywords = listOf("notification", "notifications", "alerts", "reminder", "daily summary", "push", "scheduled notifications", "sound"),
            iconVector = Iconax.NotificationBing,
            iconTint = green_dark,
            iconBg = green_light,
            destination = NotificationSettings(),
            deepLinkUri = "settings/notifications"
        ),
        GlobalSearchSettingItem(
            id = "accounts",
            title = "Accounts",
            subtitle = "Manage bank accounts and credit cards",
            keywords = listOf("accounts", "bank", "credit card", "cards", "wallet", "balances", "manage accounts", "savings", "cash", "bank balance"),
            iconVector = Icons.Rounded.AccountBalance,
            iconTint = red_dark,
            iconBg = red_light,
            destination = ManageAccounts,
            deepLinkUri = "settings/accounts"
        ),
        GlobalSearchSettingItem(
            id = "categories",
            title = "Categories",
            subtitle = "Manage expense and income categories",
            keywords = listOf("categories", "category", "subcategory", "icon", "food", "travel", "shopping", "bills", "custom category", "tag"),
            iconVector = Iconax.Box2,
            iconTint = purple_dark,
            iconBg = purple_light,
            destination = Categories,
            deepLinkUri = "settings/categories"
        ),
        GlobalSearchSettingItem(
            id = "budgets",
            title = "Budgets",
            subtitle = "Spending limits and monthly budget tracking",
            keywords = listOf("budget", "budgets", "spending limit", "limit", "target", "monthly", "burn rate", "cap", "monthly budget"),
            iconVector = Iconax.Status,
            iconTint = green_dark,
            iconBg = green_light,
            destination = Budgets(),
            deepLinkUri = "settings/budgets"
        ),
        GlobalSearchSettingItem(
            id = "rules",
            title = "Smart Rules",
            subtitle = "Auto-categorize transactions with rules",
            keywords = listOf("rules", "smart rules", "auto categorize", "conditions", "filter", "automation", "triggers", "keywords", "rule"),
            iconVector = Iconax.Fireworks7,
            iconTint = orange_dark,
            iconBg = orange_light,
            destination = Rules,
            deepLinkUri = "settings/rules"
        ),
        GlobalSearchSettingItem(
            id = "transaction_settings",
            title = "Transaction Preferences",
            subtitle = "Default transaction type, clear notes, default account",
            keywords = listOf("transaction settings", "preferences", "default type", "default account", "clear notes", "auto keypad", "expense behavior", "transaction options"),
            iconVector = Iconax.ReceiptItem,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            destination = TransactionSettings(),
            deepLinkUri = "settings/transactions"
        ),
        GlobalSearchSettingItem(
            id = "sms",
            title = "SMS Settings",
            subtitle = "SMS sync, parser options, and permissions",
            keywords = listOf("sms", "messages", "sync sms", "scanner", "parser", "unrecognized", "sms range", "auto scan", "text messages", "bank sms"),
            iconVector = Iconax.Clock,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            destination = SmsSettings(),
            deepLinkUri = "settings/sms"
        ),
        GlobalSearchSettingItem(
            id = "couple_tracker",
            title = "Couple Tracker",
            subtitle = "Track joint spending and pair with your partner",
            keywords = listOf("couple", "couple tracker", "partner", "wife", "husband", "pair", "qr pair", "household", "shared finances", "joint budget"),
            iconVector = Icons.Rounded.People,
            iconTint = red_dark,
            iconBg = red_light,
            destination = CoupleTracker,
            deepLinkUri = "settings/couple-tracker"
        ),
        GlobalSearchSettingItem(
            id = "webhooks",
            title = "Webhooks",
            subtitle = "Configure automated webhook sync endpoints",
            keywords = listOf("webhook", "webhooks", "api", "endpoint", "automated sync", "post", "payload", "home assistant", "n8n", "byoapi"),
            iconVector = Icons.Rounded.Webhook,
            iconTint = purple_dark,
            iconBg = purple_light,
            destination = Webhooks,
            deepLinkUri = "settings/webhooks"
        ),
        GlobalSearchSettingItem(
            id = "pdf_report",
            title = "Transaction Report PDF",
            subtitle = "Generate and export PDF transaction statements",
            keywords = listOf("pdf", "report", "statement", "export pdf", "document", "print", "monthly statement", "financial report", "download statement"),
            iconVector = Iconax.ImportArrow01,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            destination = PdfReport,
            deepLinkUri = "settings/pdf-report"
        ),
        GlobalSearchSettingItem(
            id = "cloud_backup",
            title = "Backup & Sync",
            subtitle = "Google Drive and WebDAV cloud backups",
            keywords = listOf("backup", "cloud", "sync", "drive", "google drive", "webdav", "restore", "encrypted backup", "passphrase", "snapshot"),
            iconVector = Icons.Rounded.CloudSync,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            destination = CloudBackup(),
            deepLinkUri = "settings/backup"
        ),
        GlobalSearchSettingItem(
            id = "archived_transactions",
            title = "Archived Transactions",
            subtitle = "View, restore, or permanently remove deleted transactions",
            keywords = listOf("archive", "archived", "trash", "restore", "deleted", "bin", "recycle bin", "unarchive", "recover"),
            iconVector = Iconax.Bag,
            iconTint = red_dark,
            iconBg = red_light,
            destination = ManageArchivedTransactions,
            deepLinkUri = "settings/archived-transactions"
        ),
        GlobalSearchSettingItem(
            id = "data_privacy",
            title = "Data Privacy",
            subtitle = "Security, app lock, and data export",
            keywords = listOf("privacy", "security", "app lock", "biometric", "fingerprint", "face unlock", "export", "import", "data", "delete all data", "clear data"),
            iconVector = Iconax.SecuritySafe,
            iconTint = blue_dark,
            iconBg = blue_light,
            destination = DataPrivacy(),
            deepLinkUri = "settings/data-privacy"
        ),
        GlobalSearchSettingItem(
            id = "ai",
            title = "AI Integration",
            subtitle = "Gemini API key, custom models, and tokens",
            keywords = listOf("ai", "gemini", "model", "api", "key", "token", "artificial intelligence", "prompt", "llm", "generative"),
            iconVector = Icons.Filled.AutoAwesome,
            iconTint = purple_dark,
            iconBg = purple_light,
            destination = AiSettings(),
            deepLinkUri = "settings/ai"
        ),
        GlobalSearchSettingItem(
            id = "customization",
            title = "Customization",
            subtitle = "Global search toggles and result limits",
            keywords = listOf("customization", "customize", "search", "global search", "toggles", "limits", "footer nav", "floating nav", "widgets order"),
            iconVector = Icons.Rounded.Tune,
            iconTint = green_dark,
            iconBg = green_light,
            destination = Customization(),
            deepLinkUri = "settings/customization"
        ),
        GlobalSearchSettingItem(
            id = "about",
            title = "About",
            subtitle = "App version, licenses, and credits",
            keywords = listOf("about", "version", "licenses", "source", "credits", "vittify", "app info", "faq", "guides"),
            iconVector = Icons.Rounded.Info,
            iconTint = orange_dark,
            iconBg = orange_light,
            destination = About,
            deepLinkUri = "settings/about"
        ),
        GlobalSearchSettingItem(
            id = "developer",
            title = "Developer Options",
            subtitle = "Tests and experimental developer features",
            keywords = listOf("developer", "developer options", "debug", "sample data", "simulate sms", "experimental"),
            iconVector = Iconax.CodeCircle,
            iconTint = grey_dark,
            iconBg = grey_light,
            destination = DeveloperOptions,
            deepLinkUri = "settings/developer"
        ),

        // ─── APPEARANCE SUB-OPTIONS ──────────────────────────────────────────
        GlobalSearchSettingItem(
            id = "appearance_dynamic_theme",
            title = "Dynamic Theming (Monet)",
            subtitle = "Derive color palette from device wallpaper",
            keywords = listOf("dynamic theme", "monet", "material you", "wallpaper colors", "system theme", "palette", "dynamic"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "dynamic-theme"),
            deepLinkUri = "settings/appearance/dynamic-theme"
        ),
        GlobalSearchSettingItem(
            id = "appearance_theme_mode",
            title = "Theme Mode",
            subtitle = "Switch between Light, Dark, or System mode",
            keywords = listOf("theme mode", "dark mode", "light mode", "system mode", "night mode", "dark theme"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "theme-mode"),
            deepLinkUri = "settings/appearance/theme-mode"
        ),
        GlobalSearchSettingItem(
            id = "appearance_amoled_black",
            title = "AMOLED Pure Black",
            subtitle = "True pitch black background for OLED screens",
            keywords = listOf("amoled", "pure black", "pitch black", "oled", "true black", "battery saver"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "amoled-black"),
            deepLinkUri = "settings/appearance/amoled-black"
        ),
        GlobalSearchSettingItem(
            id = "appearance_curated_palette",
            title = "Curated Expressive Themes",
            subtitle = "Select curated color themes like Latte, Macchiato, or Rose Pine",
            keywords = listOf("curated themes", "palettes", "latte", "macchiato", "rose pine", "accent color", "curated palette"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "curated-palette"),
            deepLinkUri = "settings/appearance/curated-palette"
        ),
        GlobalSearchSettingItem(
            id = "appearance_custom_seed",
            title = "Custom Seed Color",
            subtitle = "Pick a custom primary accent color seed",
            keywords = listOf("custom color", "seed color", "color picker", "hex color", "primary accent"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "custom-seed"),
            deepLinkUri = "settings/appearance/custom-seed"
        ),
        GlobalSearchSettingItem(
            id = "appearance_navigation_style",
            title = "Navigation Style",
            subtitle = "Toggle between docked bar and floating navigation footer",
            keywords = listOf("navigation style", "floating nav", "docked nav", "navigation bar", "footer nav", "bottom bar"),
            iconVector = Icons.Rounded.Tune,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "navigation-style"),
            deepLinkUri = "settings/appearance/navigation-style"
        ),
        GlobalSearchSettingItem(
            id = "appearance_hide_nav_labels",
            title = "Hide Navigation Labels",
            subtitle = "Show only icons in navigation bar for a cleaner look",
            keywords = listOf("hide nav labels", "hide labels", "icon only", "clean nav", "nav bar text"),
            iconVector = Icons.Rounded.Tune,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "hide-nav-labels"),
            deepLinkUri = "settings/appearance/hide-nav-labels"
        ),
        GlobalSearchSettingItem(
            id = "appearance_hide_pill_indicator",
            title = "Hide Pill Indicator",
            subtitle = "Hide the active pill indicator behind nav icons",
            keywords = listOf("hide pill indicator", "nav pill", "indicator", "pill shape"),
            iconVector = Icons.Rounded.Tune,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "hide-pill-indicator"),
            deepLinkUri = "settings/appearance/hide-pill-indicator"
        ),
        GlobalSearchSettingItem(
            id = "appearance_profile_switcher_footer",
            title = "Footer Profile Switcher",
            subtitle = "Show quick profile/couple switcher directly in footer",
            keywords = listOf("profile switcher footer", "couple switcher", "footer profile", "avatar footer"),
            iconVector = Icons.Rounded.Person,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "profile-switcher-footer"),
            deepLinkUri = "settings/appearance/profile-switcher-footer"
        ),
        GlobalSearchSettingItem(
            id = "appearance_app_logo",
            title = "App Logo & Launcher Icon",
            subtitle = "Choose custom launcher icon and app logo styling",
            keywords = listOf("app logo", "launcher icon", "app icon", "logo style", "homescreen icon"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "app-logo"),
            deepLinkUri = "settings/appearance/app-logo"
        ),
        GlobalSearchSettingItem(
            id = "appearance_blur_effects",
            title = "Glassmorphism & Blur Effects",
            subtitle = "Enable haze and glassmorphic translucent effects",
            keywords = listOf("blur effects", "glassmorphism", "haze", "translucent", "glass", "blur"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "blur-effects"),
            deepLinkUri = "settings/appearance/blur-effects"
        ),
        GlobalSearchSettingItem(
            id = "appearance_playful_animation",
            title = "Playful Spring Animations",
            subtitle = "Enable bouncy physical spring motion and expressive interactions",
            keywords = listOf("playful animations", "spring physics", "motion", "bouncy", "reduced motion", "expressive"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "playful-animation"),
            deepLinkUri = "settings/appearance/playful-animation"
        ),
        GlobalSearchSettingItem(
            id = "appearance_background_effects",
            title = "Canvas Background Effects",
            subtitle = "Subtle organic particle motion on canvas surfaces",
            keywords = listOf("background effects", "canvas effects", "particles", "ambient background"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "background-effects"),
            deepLinkUri = "settings/appearance/background-effects"
        ),
        GlobalSearchSettingItem(
            id = "appearance_app_font",
            title = "App Font & Typography",
            subtitle = "Customize typography with SN Pro, Inter, or System font",
            keywords = listOf("app font", "typography", "sn pro", "font family", "custom font", "inter", "system font"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "app-font"),
            deepLinkUri = "settings/appearance/app-font"
        ),
        GlobalSearchSettingItem(
            id = "appearance_surface_geometry",
            title = "Surface Geometry & Shapes",
            subtitle = "Material 3 Expressive squircle curvature and border radius",
            keywords = listOf("surface geometry", "squircle", "corner radius", "shapes", "geometry tokens"),
            iconVector = Icons.Rounded.Palette,
            iconTint = orange_dark,
            iconBg = orange_light,
            targetSettingId = "appearance",
            parentSection = "Appearance",
            destination = Appearance(targetOptionId = "surface-geometry"),
            deepLinkUri = "settings/appearance/surface-geometry"
        ),

        // ─── TRANSACTION PREFERENCES SUB-OPTIONS ─────────────────────────────
        GlobalSearchSettingItem(
            id = "transaction_use_category_as_merchant",
            title = "Use Category Name as Merchant",
            subtitle = "Default merchant name to category if unspecified",
            keywords = listOf("category as merchant", "default merchant", "fallback merchant"),
            iconVector = Iconax.ReceiptItem,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "transaction_settings",
            parentSection = "Transaction Preferences",
            destination = TransactionSettings(targetOptionId = "use-category-as-merchant"),
            deepLinkUri = "settings/transactions/use-category-as-merchant"
        ),
        GlobalSearchSettingItem(
            id = "transaction_use_category_as_merchant_sms",
            title = "Category as Merchant for SMS",
            subtitle = "Use parsed category as merchant for SMS transactions",
            keywords = listOf("sms merchant", "category as merchant sms", "bank sms name"),
            iconVector = Iconax.ReceiptItem,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "transaction_settings",
            parentSection = "Transaction Preferences",
            destination = TransactionSettings(targetOptionId = "use-category-as-merchant-sms"),
            deepLinkUri = "settings/transactions/use-category-as-merchant-sms"
        ),
        GlobalSearchSettingItem(
            id = "transaction_preserve_account_order",
            title = "Preserve Account Order",
            subtitle = "Keep manual drag-and-drop order for accounts in selectors",
            keywords = listOf("preserve account order", "account order", "sort accounts", "reorder accounts"),
            iconVector = Iconax.ReceiptItem,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "transaction_settings",
            parentSection = "Transaction Preferences",
            destination = TransactionSettings(targetOptionId = "preserve-account-order"),
            deepLinkUri = "settings/transactions/preserve-account-order"
        ),
        GlobalSearchSettingItem(
            id = "transaction_direct_field_editing",
            title = "Direct Field Editing",
            subtitle = "Quick edit transaction fields directly without opening full form",
            keywords = listOf("direct field editing", "inline edit", "quick edit", "field editing"),
            iconVector = Iconax.ReceiptItem,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "transaction_settings",
            parentSection = "Transaction Preferences",
            destination = TransactionSettings(targetOptionId = "direct-field-editing"),
            deepLinkUri = "settings/transactions/direct-field-editing"
        ),

        // ─── NOTIFICATIONS SUB-OPTIONS ───────────────────────────────────────
        GlobalSearchSettingItem(
            id = "notification_bank_push_notifications",
            title = "Bank Push Notifications",
            subtitle = "Detect and parse transactions from incoming notification listener",
            keywords = listOf("bank push notifications", "push notification listener", "notification service", "instant alert detection"),
            iconVector = Iconax.NotificationBing,
            iconTint = green_dark,
            iconBg = green_light,
            targetSettingId = "notifications",
            parentSection = "Notifications",
            destination = NotificationSettings(targetOptionId = "bank-push-notifications"),
            deepLinkUri = "settings/notifications/bank-push-notifications"
        ),
        GlobalSearchSettingItem(
            id = "notification_remind_transactions",
            title = "Unprocessed Transaction Reminders",
            subtitle = "Daily reminders to review unverified or pending transactions",
            keywords = listOf("remind transactions", "daily reminder", "unprocessed", "pending review", "daily alerts"),
            iconVector = Iconax.NotificationBing,
            iconTint = green_dark,
            iconBg = green_light,
            targetSettingId = "notifications",
            parentSection = "Notifications",
            destination = NotificationSettings(targetOptionId = "remind-transactions"),
            deepLinkUri = "settings/notifications/remind-transactions"
        ),
        GlobalSearchSettingItem(
            id = "notification_alert_time",
            title = "Daily Reminder Time",
            subtitle = "Time of day to receive transaction reminders",
            keywords = listOf("reminder time", "alert time", "daily schedule", "notification time"),
            iconVector = Iconax.NotificationBing,
            iconTint = green_dark,
            iconBg = green_light,
            targetSettingId = "notifications",
            parentSection = "Notifications",
            destination = NotificationSettings(targetOptionId = "alert-time"),
            deepLinkUri = "settings/notifications/alert-time"
        ),
        GlobalSearchSettingItem(
            id = "notification_upcoming_alerts",
            title = "Upcoming Subscription Alerts",
            subtitle = "Notify before recurring bills and subscriptions renew",
            keywords = listOf("upcoming subscription alerts", "bill reminders", "renewal alerts", "subscription notifications"),
            iconVector = Iconax.NotificationBing,
            iconTint = green_dark,
            iconBg = green_light,
            targetSettingId = "notifications",
            parentSection = "Notifications",
            destination = NotificationSettings(targetOptionId = "upcoming-alerts"),
            deepLinkUri = "settings/notifications/upcoming-alerts"
        ),

        // ─── DATA PRIVACY SUB-OPTIONS ────────────────────────────────────────
        GlobalSearchSettingItem(
            id = "privacy_app_lock",
            title = "Biometric App Lock",
            subtitle = "Secure Vittify with fingerprint, face unlock, or device PIN",
            keywords = listOf("app lock", "biometric", "fingerprint", "face unlock", "pin", "screen lock", "security"),
            iconVector = Iconax.SecuritySafe,
            iconTint = blue_dark,
            iconBg = blue_light,
            targetSettingId = "data_privacy",
            parentSection = "Data Privacy",
            destination = DataPrivacy(targetOptionId = "app-lock"),
            deepLinkUri = "settings/data-privacy/app-lock"
        ),
        GlobalSearchSettingItem(
            id = "privacy_lock_timeout",
            title = "Lock Timeout",
            subtitle = "Require authentication immediately or after background delay",
            keywords = listOf("lock timeout", "timeout", "grace period", "auto lock delay"),
            iconVector = Iconax.SecuritySafe,
            iconTint = blue_dark,
            iconBg = blue_light,
            targetSettingId = "data_privacy",
            parentSection = "Data Privacy",
            destination = DataPrivacy(targetOptionId = "lock-timeout"),
            deepLinkUri = "settings/data-privacy/lock-timeout"
        ),
        GlobalSearchSettingItem(
            id = "privacy_data_sanitization",
            title = "Data Sanitization & Health",
            subtitle = "Audit database integrity, orphan accounts, and claim unowned data",
            keywords = listOf("data sanitization", "health audit", "clean data", "orphan records", "fix database"),
            iconVector = Iconax.SecuritySafe,
            iconTint = blue_dark,
            iconBg = blue_light,
            targetSettingId = "data_privacy",
            parentSection = "Data Privacy",
            destination = DataPrivacy(targetOptionId = "data-sanitization"),
            deepLinkUri = "settings/data-privacy/data-sanitization"
        ),
        GlobalSearchSettingItem(
            id = "privacy_export_backup",
            title = "Export Data Backup",
            subtitle = "Export encrypted JSON database backup to device storage",
            keywords = listOf("export backup", "backup to file", "save backup", "export database", "download backup"),
            iconVector = Iconax.SecuritySafe,
            iconTint = blue_dark,
            iconBg = blue_light,
            targetSettingId = "data_privacy",
            parentSection = "Data Privacy",
            destination = DataPrivacy(targetOptionId = "export-backup"),
            deepLinkUri = "settings/data-privacy/export-backup"
        ),
        GlobalSearchSettingItem(
            id = "privacy_import_backup",
            title = "Import Data Backup",
            subtitle = "Restore Vittify data from local backup file",
            keywords = listOf("import backup", "restore backup", "upload backup", "import database"),
            iconVector = Iconax.SecuritySafe,
            iconTint = blue_dark,
            iconBg = blue_light,
            targetSettingId = "data_privacy",
            parentSection = "Data Privacy",
            destination = DataPrivacy(targetOptionId = "import-backup"),
            deepLinkUri = "settings/data-privacy/import-backup"
        ),
        GlobalSearchSettingItem(
            id = "privacy_import_pdf",
            title = "Import Bank Statement PDF",
            subtitle = "Import and parse PDF statements directly into timeline",
            keywords = listOf("import pdf", "statement import", "pdf parser", "bank statement pdf"),
            iconVector = Iconax.SecuritySafe,
            iconTint = blue_dark,
            iconBg = blue_light,
            targetSettingId = "data_privacy",
            parentSection = "Data Privacy",
            destination = DataPrivacy(targetOptionId = "import-pdf"),
            deepLinkUri = "settings/data-privacy/import-pdf"
        ),
        GlobalSearchSettingItem(
            id = "privacy_delete_all_data",
            title = "Delete All Data",
            subtitle = "Permanently erase all accounts, transactions, and settings",
            keywords = listOf("delete all data", "clear data", "reset app", "wipe data", "factory reset"),
            iconVector = Iconax.SecuritySafe,
            iconTint = blue_dark,
            iconBg = blue_light,
            targetSettingId = "data_privacy",
            parentSection = "Data Privacy",
            destination = DataPrivacy(targetOptionId = "delete-all-data"),
            deepLinkUri = "settings/data-privacy/delete-all-data"
        ),

        // ─── BACKUP & SYNC SUB-OPTIONS ───────────────────────────────────────
        GlobalSearchSettingItem(
            id = "backup_google_drive",
            title = "Google Drive Backup",
            subtitle = "Automated cloud backup to personal Google Drive app data",
            keywords = listOf("google drive", "gdrive", "drive backup", "cloud sync"),
            iconVector = Icons.Rounded.CloudSync,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "cloud_backup",
            parentSection = "Backup & Sync",
            destination = CloudBackup(targetOptionId = "google-drive"),
            deepLinkUri = "settings/backup/google-drive"
        ),
        GlobalSearchSettingItem(
            id = "backup_webdav",
            title = "WebDAV Cloud Sync",
            subtitle = "Self-hosted Nextcloud, ownCloud, or custom WebDAV backup",
            keywords = listOf("webdav", "nextcloud", "owncloud", "self hosted", "webdav backup"),
            iconVector = Icons.Rounded.CloudSync,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "cloud_backup",
            parentSection = "Backup & Sync",
            destination = CloudBackup(targetOptionId = "webdav"),
            deepLinkUri = "settings/backup/webdav"
        ),
        GlobalSearchSettingItem(
            id = "backup_auto_schedule",
            title = "Auto Backup Frequency",
            subtitle = "Daily, weekly, or manual background backup cadence",
            keywords = listOf("auto backup schedule", "backup frequency", "daily backup", "scheduled backup"),
            iconVector = Icons.Rounded.CloudSync,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "cloud_backup",
            parentSection = "Backup & Sync",
            destination = CloudBackup(targetOptionId = "auto-backup-schedule"),
            deepLinkUri = "settings/backup/auto-backup-schedule"
        ),
        GlobalSearchSettingItem(
            id = "backup_passphrase",
            title = "Backup Encryption Passphrase",
            subtitle = "End-to-end encryption passphrase for cloud backups",
            keywords = listOf("backup passphrase", "encryption key", "cloud security", "e2ee", "password"),
            iconVector = Icons.Rounded.CloudSync,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "cloud_backup",
            parentSection = "Backup & Sync",
            destination = CloudBackup(targetOptionId = "backup-passphrase"),
            deepLinkUri = "settings/backup/backup-passphrase"
        ),

        // ─── CURRENCY SUB-OPTIONS ────────────────────────────────────────────
        GlobalSearchSettingItem(
            id = "currency_unified_currency",
            title = "Unified Base Currency",
            subtitle = "Convert all foreign accounts and transactions to base currency",
            keywords = listOf("unified currency", "base currency", "convert currency", "single currency"),
            iconVector = Iconax.DollarCircle,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "currency",
            parentSection = "Currency",
            destination = CurrencySettings(targetOptionId = "unified-currency"),
            deepLinkUri = "settings/currency/unified-currency"
        ),
        GlobalSearchSettingItem(
            id = "currency_picker",
            title = "Default Currency",
            subtitle = "Select default currency symbol and code (e.g. INR, USD, EUR)",
            keywords = listOf("select currency", "currency picker", "inr", "usd", "eur", "gbp", "default currency"),
            iconVector = Iconax.DollarCircle,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "currency",
            parentSection = "Currency",
            destination = CurrencySettings(targetOptionId = "currency-picker"),
            deepLinkUri = "settings/currency/currency-picker"
        ),
        GlobalSearchSettingItem(
            id = "currency_exchange_rates",
            title = "Exchange Rates & Forex",
            subtitle = "Fetch and update live foreign exchange rates",
            keywords = listOf("exchange rates", "forex", "fx rates", "conversion rates", "live rates"),
            iconVector = Iconax.DollarCircle,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "currency",
            parentSection = "Currency",
            destination = CurrencySettings(targetOptionId = "exchange-rates"),
            deepLinkUri = "settings/currency/exchange-rates"
        ),

        // ─── AI INTEGRATION SUB-OPTIONS ──────────────────────────────────────
        GlobalSearchSettingItem(
            id = "ai_enable_ai",
            title = "Enable AI Features",
            subtitle = "Enable Google Gemini natural language processing & auto-categorization",
            keywords = listOf("enable ai", "gemini ai", "artificial intelligence", "smart parsing"),
            iconVector = Icons.Filled.AutoAwesome,
            iconTint = purple_dark,
            iconBg = purple_light,
            targetSettingId = "ai",
            parentSection = "AI Integration",
            destination = AiSettings(targetOptionId = "enable-ai"),
            deepLinkUri = "settings/ai/enable-ai"
        ),
        GlobalSearchSettingItem(
            id = "ai_api_key",
            title = "Gemini API Key",
            subtitle = "Provide your custom Google Gemini API key",
            keywords = listOf("gemini api key", "byok", "api token", "ai key"),
            iconVector = Icons.Filled.AutoAwesome,
            iconTint = purple_dark,
            iconBg = purple_light,
            targetSettingId = "ai",
            parentSection = "AI Integration",
            destination = AiSettings(targetOptionId = "api-key"),
            deepLinkUri = "settings/ai/api-key"
        ),
        GlobalSearchSettingItem(
            id = "ai_model",
            title = "AI Model Selection",
            subtitle = "Select Gemini model (e.g. Gemini 2.0 Flash, 1.5 Flash)",
            keywords = listOf("ai model", "gemini flash", "gemini pro", "model selection", "llm"),
            iconVector = Icons.Filled.AutoAwesome,
            iconTint = purple_dark,
            iconBg = purple_light,
            targetSettingId = "ai",
            parentSection = "AI Integration",
            destination = AiSettings(targetOptionId = "ai-model"),
            deepLinkUri = "settings/ai/ai-model"
        ),

        // ─── SMS SETTINGS SUB-OPTIONS ────────────────────────────────────────
        GlobalSearchSettingItem(
            id = "sms_scan_range",
            title = "SMS Scan Range",
            subtitle = "Configure how many months of SMS history to scan",
            keywords = listOf("sms range", "scan months", "history range", "sms scan duration"),
            iconVector = Iconax.Clock,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "sms",
            parentSection = "SMS Settings",
            destination = SmsSettings(targetOptionId = "scan-range"),
            deepLinkUri = "settings/sms/scan-range"
        ),
        GlobalSearchSettingItem(
            id = "sms_scan_all_time",
            title = "Scan All SMS Messages",
            subtitle = "Scan all SMS messages without time cutoff",
            keywords = listOf("scan all time", "full scan", "scan all messages"),
            iconVector = Iconax.Clock,
            iconTint = cyan_dark,
            iconBg = cyan_light,
            targetSettingId = "sms",
            parentSection = "SMS Settings",
            destination = SmsSettings(targetOptionId = "scan-all-time"),
            deepLinkUri = "settings/sms/scan-all-time"
        ),

        // ─── CUSTOMIZATION SUB-OPTIONS ───────────────────────────────────────
        GlobalSearchSettingItem(
            id = "customization_global_search",
            title = "Global Search Preferences",
            subtitle = "Configure categories, result counts, and search sources",
            keywords = listOf("global search settings", "search toggles", "results limit", "search config"),
            iconVector = Icons.Rounded.Tune,
            iconTint = green_dark,
            iconBg = green_light,
            targetSettingId = "customization",
            parentSection = "Customization",
            destination = Customization(targetOptionId = "global-search"),
            deepLinkUri = "settings/customization/global-search"
        )
    )

    val allPages = listOf(
        GlobalSearchPageItem(
            id = "home",
            title = "Home",
            subtitle = "Financial timeline and recent overview",
            keywords = listOf("home", "dashboard", "timeline", "overview", "recent", "main", "start", "net worth", "summary"),
            iconVector = Icons.Rounded.Home,
            destination = Home
        ),
        GlobalSearchPageItem(
            id = "analytics",
            title = "Analytics",
            subtitle = "Spending breakdowns and visual charts",
            keywords = listOf("analytics", "charts", "graphs", "insights", "spending", "stats", "visualize", "breakdown", "trends", "reports"),
            iconVector = Icons.Rounded.BarChart,
            destination = Analytics
        ),
        GlobalSearchPageItem(
            id = "transactions",
            title = "Transactions",
            subtitle = "All transaction history and filtering",
            keywords = listOf("transactions", "history", "expenses", "income", "records", "all transactions", "list", "filter", "search transactions"),
            iconVector = Iconax.ReceiptItem,
            destination = Transactions()
        ),
        GlobalSearchPageItem(
            id = "add_transaction",
            title = "Add Transaction",
            subtitle = "Log an expense, income, or transfer manually",
            keywords = listOf("add", "new transaction", "log expense", "add income", "record spending", "new payment", "create transaction", "quick add", "transfer"),
            iconVector = Icons.Rounded.Add,
            destination = AddTransaction()
        ),
        GlobalSearchPageItem(
            id = "budgets",
            title = "Budgets",
            subtitle = "Budget targets and spending status",
            keywords = listOf("budgets", "budget", "limits", "targets", "monthly", "category budgets", "burn rate", "spending ceiling"),
            iconVector = Iconax.Status,
            destination = Budgets()
        ),
        GlobalSearchPageItem(
            id = "subscriptions",
            title = "Subscriptions",
            subtitle = "Recurring bills and subscriptions",
            keywords = listOf("subscriptions", "recurring", "bills", "renewal", "autopay", "monthly services", "memberships", "netflix", "spotify"),
            iconVector = Iconax.Clock,
            destination = Subscriptions
        ),
        GlobalSearchPageItem(
            id = "manage_accounts",
            title = "Manage Accounts",
            subtitle = "Bank accounts, credit cards, and balances",
            keywords = listOf("accounts", "bank", "cards", "wallet", "balances", "manage accounts", "savings account", "credit limit"),
            iconVector = Icons.Rounded.AccountBalance,
            destination = ManageAccounts
        ),
        GlobalSearchPageItem(
            id = "add_account",
            title = "Add New Account",
            subtitle = "Add a new bank, credit card, or wallet",
            keywords = listOf("add account", "new bank", "new card", "create account", "link bank", "add credit card", "wallet"),
            iconVector = Icons.Rounded.AddCard,
            destination = AddAccount
        ),
        GlobalSearchPageItem(
            id = "categories",
            title = "Categories",
            subtitle = "Category icon and label management",
            keywords = listOf("categories", "category", "icons", "tags", "manage categories", "food", "travel", "shopping", "subcategories"),
            iconVector = Iconax.Box2,
            destination = Categories
        ),
        GlobalSearchPageItem(
            id = "rules",
            title = "Smart Rules",
            subtitle = "Automated transaction categorization rules",
            keywords = listOf("rules", "smart rules", "auto categorize", "automation", "filters", "keywords", "auto tag"),
            iconVector = Iconax.Fireworks7,
            destination = Rules
        ),
        GlobalSearchPageItem(
            id = "create_rule",
            title = "Create Smart Rule",
            subtitle = "Define a new automated categorization rule",
            keywords = listOf("create rule", "new rule", "add rule", "auto categorize rule", "match merchant", "filter condition"),
            iconVector = Icons.Rounded.Add,
            destination = CreateRule()
        ),
        GlobalSearchPageItem(
            id = "sync_sms",
            title = "Scan / Sync SMS",
            subtitle = "Scan device SMS inbox for bank transactions",
            keywords = listOf("sync sms", "scan sms", "resync", "read messages", "import sms", "refresh transactions", "bank messages"),
            iconVector = Icons.Rounded.Sync,
            destination = SyncSms()
        ),
        GlobalSearchPageItem(
            id = "unrecognized_sms",
            title = "Unrecognized SMS",
            subtitle = "Review and convert unrecognized bank alerts",
            keywords = listOf("unrecognized", "missed sms", "failed parsing", "unknown bank", "pending review", "parser report"),
            iconVector = Iconax.MessageQuestion,
            destination = UnrecognizedSms
        ),
        GlobalSearchPageItem(
            id = "couple_tracker",
            title = "Couple Tracker",
            subtitle = "Shared finances and partner spending mode",
            keywords = listOf("couple", "partner", "couple tracker", "wife", "husband", "pair devices", "household", "shared finances"),
            iconVector = Icons.Rounded.People,
            destination = CoupleTracker
        ),
        GlobalSearchPageItem(
            id = "p2p_device_sync",
            title = "P2P Device Sync",
            subtitle = "Local peer-to-peer encrypted sync without cloud",
            keywords = listOf("p2p sync", "device sync", "local sync", "webrtc", "offline sync", "pair phones", "direct sync"),
            iconVector = Icons.Rounded.Sync,
            destination = P2pDeviceSync
        ),
        GlobalSearchPageItem(
            id = "settings",
            title = "Settings",
            subtitle = "App settings and configuration hub",
            keywords = listOf("settings", "preferences", "config", "options", "setup"),
            iconVector = Iconax.Setting2,
            destination = Settings()
        ),
        GlobalSearchPageItem(
            id = "appearance",
            title = "Appearance",
            subtitle = "Theme, amoled mode, and accent colors",
            keywords = listOf("appearance", "theme", "dark mode", "amoled", "font", "accent", "colors", "monet", "custom font"),
            iconVector = Icons.Rounded.Palette,
            destination = Appearance()
        ),
        GlobalSearchPageItem(
            id = "customization",
            title = "Customization",
            subtitle = "Global search and UI customization",
            keywords = listOf("customization", "customize", "global search", "toggles", "widgets", "reorder widgets", "nav labels"),
            iconVector = Icons.Rounded.Tune,
            destination = Customization()
        ),
        GlobalSearchPageItem(
            id = "ai_settings",
            title = "AI & Gemini Settings",
            subtitle = "Configure Google Gemini API key and AI models",
            keywords = listOf("ai", "gemini", "api key", "tokens", "artificial intelligence", "byok", "smart insights"),
            iconVector = Icons.Filled.AutoAwesome,
            destination = AiSettings()
        ),
        GlobalSearchPageItem(
            id = "currency_settings",
            title = "Currency Settings",
            subtitle = "Base currency, exchange rates, and symbols",
            keywords = listOf("currency", "exchange rates", "base currency", "forex", "inr", "usd", "eur", "conversion"),
            iconVector = Iconax.DollarCircle,
            destination = CurrencySettings()
        ),
        GlobalSearchPageItem(
            id = "profile",
            title = "Profile",
            subtitle = "Edit user profile name and avatar",
            keywords = listOf("profile", "user", "name", "avatar", "photo", "display name"),
            iconVector = Icons.Rounded.Person,
            destination = Profile
        ),
        GlobalSearchPageItem(
            id = "sms_settings",
            title = "SMS Settings",
            subtitle = "SMS sync range and parser setup",
            keywords = listOf("sms", "messages", "sync sms", "parser", "sms settings", "scan duration"),
            iconVector = Iconax.Clock,
            destination = SmsSettings()
        ),
        GlobalSearchPageItem(
            id = "data_privacy",
            title = "Data Privacy",
            subtitle = "App lock, biometrics, and database export",
            keywords = listOf("privacy", "security", "app lock", "biometrics", "fingerprint", "face unlock", "export data", "delete data"),
            iconVector = Iconax.SecuritySafe,
            destination = DataPrivacy()
        ),
        GlobalSearchPageItem(
            id = "cloud_backup",
            title = "Backup & Sync",
            subtitle = "Google Drive and WebDAV cloud backups",
            keywords = listOf("backup", "cloud", "sync", "drive", "google drive", "webdav", "restore", "snapshot", "passphrase"),
            iconVector = Icons.Rounded.CloudSync,
            destination = CloudBackup()
        ),
        GlobalSearchPageItem(
            id = "data_sanitization",
            title = "Data Sanitization & Health",
            subtitle = "Audit data health, claim unowned data, and fix accounts",
            keywords = listOf("sanitize", "clean", "hygiene", "audit", "diagnostics", "unowned", "foreign", "health", "fix"),
            iconVector = Icons.Rounded.Security,
            destination = DataSanitization()
        ),
        GlobalSearchPageItem(
            id = "manage_archived_transactions",
            title = "Archived Transactions",
            subtitle = "View, restore, or permanently remove archived transactions",
            keywords = listOf("archive", "archived", "trash", "restore", "deleted", "bin", "recycle", "recover"),
            iconVector = Iconax.Bag,
            destination = ManageArchivedTransactions
        ),
        GlobalSearchPageItem(
            id = "pdf_report",
            title = "PDF Report",
            subtitle = "Export transaction statement as PDF",
            keywords = listOf("pdf", "report", "export statement", "statement", "print", "financial summary", "download statement"),
            iconVector = Iconax.ImportArrow01,
            destination = PdfReport
        ),
        GlobalSearchPageItem(
            id = "notifications",
            title = "Notification Settings",
            subtitle = "Alert timings and scan reminders",
            keywords = listOf("notifications", "alerts", "reminders", "daily summary", "push alerts", "scheduled"),
            iconVector = Iconax.NotificationBing,
            destination = NotificationSettings()
        ),
        GlobalSearchPageItem(
            id = "webhooks",
            title = "Webhooks",
            subtitle = "Webhook endpoints and sync profiles",
            keywords = listOf("webhooks", "webhook", "endpoints", "api", "automate", "home assistant", "n8n"),
            iconVector = Icons.Rounded.Webhook,
            destination = Webhooks
        ),
        GlobalSearchPageItem(
            id = "transaction_settings",
            title = "Transaction Settings",
            subtitle = "Configure default transaction behavior",
            keywords = listOf("transaction settings", "defaults", "clear notes", "auto keypad", "default bank"),
            iconVector = Iconax.ReceiptItem,
            destination = TransactionSettings()
        ),
        GlobalSearchPageItem(
            id = "about",
            title = "About Vittify",
            subtitle = "Version, developer options, and licenses",
            keywords = listOf("about", "version", "licenses", "developer", "credits", "source code", "vittify info"),
            iconVector = Icons.Rounded.Info,
            destination = About
        ),
        GlobalSearchPageItem(
            id = "faq",
            title = "Frequently Asked Questions (FAQ)",
            subtitle = "Answers to common questions about tracking and privacy",
            keywords = listOf("faq", "help", "questions", "answers", "how does it work", "troubleshooting", "support", "banks supported", "privacy faq"),
            iconVector = Icons.Rounded.HelpOutline,
            destination = Faq
        ),
        GlobalSearchPageItem(
            id = "guides",
            title = "Guides & Tutorials",
            subtitle = "Step-by-step feature walkthroughs and workflows",
            keywords = listOf("guides", "tutorials", "walkthrough", "how to use", "instructions", "manual", "tips", "setup guide", "getting started"),
            iconVector = Iconax.DocumentText2,
            destination = Guides
        ),
        GlobalSearchPageItem(
            id = "privacy_policy",
            title = "Privacy Policy",
            subtitle = "100% on-device guarantee and data commitments",
            keywords = listOf("privacy policy", "privacy", "zero tracking", "gdpr", "on device", "data safety", "security manifesto"),
            iconVector = Icons.Rounded.Security,
            destination = PrivacyPolicy
        ),
        GlobalSearchPageItem(
            id = "terms_of_service",
            title = "Terms of Service",
            subtitle = "Rules, terms, and software disclaimers",
            keywords = listOf("terms of service", "terms", "conditions", "legal", "disclaimer", "license terms", "agreement"),
            iconVector = Icons.Rounded.Gavel,
            destination = TermsOfService
        ),
        GlobalSearchPageItem(
            id = "credits",
            title = "Credits & Acknowledgments",
            subtitle = "Cashiro fork, open-source libraries, and contributors",
            keywords = listOf("credits", "acknowledgments", "cashiro", "open source", "authors", "contributors", "libraries", "fork"),
            iconVector = Icons.Rounded.Favorite,
            destination = Credits
        ),
        GlobalSearchPageItem(
            id = "licenses",
            title = "Open Source Licenses",
            subtitle = "Notices for third-party open-source libraries",
            keywords = listOf("licenses", "open source", "software licenses", "apache", "mit", "bsd", "third party libraries"),
            iconVector = Iconax.Status,
            destination = Licenses
        ),
        GlobalSearchPageItem(
            id = "developer_options",
            title = "Developer Options",
            subtitle = "Database inspection, logs, and debug tools",
            keywords = listOf("developer options", "debug", "database inspector", "test notifications", "experimental", "dev tools"),
            iconVector = Iconax.CodeCircle,
            destination = DeveloperOptions
        )
    )
}

