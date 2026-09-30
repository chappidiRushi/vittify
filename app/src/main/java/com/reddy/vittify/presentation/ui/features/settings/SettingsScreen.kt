package com.reddy.vittify.presentation.ui.features.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.R
import com.reddy.vittify.core.Constants
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.FoldableSection
import com.reddy.vittify.presentation.ui.components.LanguageSelectionBottomSheet
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.ProfileAvatarImage
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.icons.BagTimer
import com.reddy.vittify.presentation.ui.icons.Box2
import com.reddy.vittify.presentation.ui.icons.Clock
import com.reddy.vittify.presentation.ui.icons.DollarCircle
import com.reddy.vittify.presentation.ui.icons.Fireworks7
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.ImportArrow01
import com.reddy.vittify.presentation.ui.icons.NotificationBing
import com.reddy.vittify.presentation.ui.icons.SecuritySafe
import com.reddy.vittify.presentation.ui.icons.Status
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifySpacing
import com.reddy.vittify.presentation.ui.theme.blue_dark
import com.reddy.vittify.presentation.ui.theme.blue_light
import com.reddy.vittify.presentation.ui.theme.cyan_dark
import com.reddy.vittify.presentation.ui.theme.cyan_light
import com.reddy.vittify.presentation.ui.theme.green_dark
import com.reddy.vittify.presentation.ui.theme.green_light
import com.reddy.vittify.presentation.ui.theme.orange_dark
import com.reddy.vittify.presentation.ui.theme.orange_light
import com.reddy.vittify.presentation.ui.theme.purple_dark
import com.reddy.vittify.presentation.ui.theme.purple_light
import com.reddy.vittify.presentation.ui.theme.red_dark
import com.reddy.vittify.presentation.ui.theme.red_light
import com.reddy.vittify.presentation.ui.theme.yellow_dark
import com.reddy.vittify.presentation.ui.theme.yellow_light
import com.reddy.vittify.presentation.ui.theme.grey_dark
import com.reddy.vittify.presentation.ui.theme.grey_light
import com.reddy.vittify.presentation.ui.icons.CodeCircle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay

@Composable
private fun Modifier.settingItemHighlight(
    id: String,
    requester: BringIntoViewRequester?,
    highlightedId: String?,
    shape: androidx.compose.ui.graphics.Shape
): Modifier {
    val isHighlighted = id == highlightedId
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "setting_highlight_border"
    )
    return this
        .then(if (requester != null) Modifier.bringIntoViewRequester(requester) else Modifier)
        .border(if (isHighlighted) 2.dp else 0.dp, borderColor, shape)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    targetSettingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToCustomization: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSms: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToWebhooks: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToDataPrivacy: () -> Unit = {},
    onNavigateToCloudBackup: () -> Unit = {},
    onNavigateToP2pSync: () -> Unit = {},
    onNavigateToCoupleTracker: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToCurrency: () -> Unit = {},
    onNavigateToPdfReport: () -> Unit = {},
    onNavigateToAi: () -> Unit = {},
    onNavigateToManageArchivedTransactions: () -> Unit = {},
    onNavigateToTransactionSettings: () -> Unit = {},
    onNavigateToDeveloper: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val bringIntoViewRequesters = remember {
        mapOf(
            "profile" to BringIntoViewRequester(),
            "appearance" to BringIntoViewRequester(),
            "language" to BringIntoViewRequester(),
            "currency" to BringIntoViewRequester(),
            "notifications" to BringIntoViewRequester(),
            "accounts" to BringIntoViewRequester(),
            "categories" to BringIntoViewRequester(),
            "budgets" to BringIntoViewRequester(),
            "rules" to BringIntoViewRequester(),
            "transaction_settings" to BringIntoViewRequester(),
            "sms" to BringIntoViewRequester(),
            "couple_tracker" to BringIntoViewRequester(),
            "webhooks" to BringIntoViewRequester(),
            "pdf_report" to BringIntoViewRequester(),
            "cloud_backup" to BringIntoViewRequester(),
            "archived_transactions" to BringIntoViewRequester(),
            "data_privacy" to BringIntoViewRequester(),
            "ai" to BringIntoViewRequester(),
            "customization" to BringIntoViewRequester(),
            "about" to BringIntoViewRequester(),
            "developer" to BringIntoViewRequester()
        )
    }

    var highlightedSettingId by remember { mutableStateOf<String?>(null) }
    var isPersonalizationExpanded by remember { mutableStateOf(true) }
    var isFinancesExpanded by remember { mutableStateOf(true) }
    var isDataSyncExpanded by remember { mutableStateOf(true) }
    var isSystemSecurityExpanded by remember { mutableStateOf(true) }

    val bottomNavPadding = LocalBottomNavPadding.current
    val density = LocalDensity.current

    LaunchedEffect(targetSettingId) {
        targetSettingId?.let { id ->
            when (id) {
                "profile", "appearance", "language", "currency", "notifications" -> isPersonalizationExpanded = true
                "accounts", "budgets", "categories", "rules", "transaction_settings", "sms", "couple_tracker", "webhooks" -> isFinancesExpanded = true
                "pdf_report", "cloud_backup", "archived_transactions" -> isDataSyncExpanded = true
                "data_privacy", "ai", "customization", "about", "developer" -> isSystemSecurityExpanded = true
            }
            delay(350)
            bringIntoViewRequesters[id]?.bringIntoView()
            val extraBottomPx = with(density) { (bottomNavPadding + 32.dp).toPx() }
            bringIntoViewRequesters[id]?.bringIntoView(
                Rect(0f, 0f, 1000f, 200f + extraBottomPx)
            )
            highlightedSettingId = id
            delay(2500)
            highlightedSettingId = null
        }
    }

    val userPreferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle(initialValue = null)
    val isWebhookModeEnabled = userPreferences?.isWebhookModeEnabled == true
    val googleDriveEmail by settingsViewModel.googleDriveEmail.collectAsStateWithLifecycle()
    var showLanguageBottomSheet by remember { mutableStateOf(false) }

    val currentLanguageCode = remember(androidx.compose.ui.platform.LocalConfiguration.current) {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            locales.get(0)?.language ?: "en"
        } else {
            java.util.Locale.getDefault().language
        }
    }
    val currentLanguageName = remember(currentLanguageCode) {
        when (currentLanguageCode) {
            "en" -> "English"
            "af" -> "Afrikaans"
            "ar" -> "العربية"
            "az" -> "Azərbaycan"
            "bg" -> "Български"
            "bn" -> "বাংলা"
            "bo" -> "བོད་སྐད་"
            "ca" -> "Català"
            "cs" -> "Čeština"
            "da" -> "Dansk"
            "de" -> "Deutsch"
            "dz" -> "རྫོང་ཁ་"
            "el" -> "Ελληνικά"
            "es" -> "Español"
            "fa" -> "فارسی"
            "fr" -> "Français"
            "gu" -> "ગુજરાતી"
            "haw" -> "ʻŌlelo Hawaiʻi"
            "he" -> "עברית"
            "hi" -> "हिन्दी"
            "hr" -> "Hrvatski"
            "hu" -> "Magyar"
            "id" -> "Bahasa Indonesia"
            "is" -> "Íslenska"
            "it" -> "Italiano"
            "ja" -> "日本語"
            "kab" -> "Taqbaylit"
            "kn" -> "ಕನ್ನಡ"
            "ks" -> "कश्मीरी"
            "la" -> "Latina"
            "ml" -> "മലയാളം"
            "mr" -> "मराठी"
            "ne" -> "नेपाली"
            "nl" -> "Nederlands"
            "no" -> "Norsk"
            "ny" -> "Chichewa"
            "or" -> "ଓଡ଼ିଆ"
            "os" -> "Ирон"
            "pa" -> "ਪੰਜਾਬੀ"
            "pl" -> "Polski"
            "pt" -> "Português"
            "ro" -> "Română"
            "ru" -> "Русский"
            "sk" -> "Slovenčina"
            "sl" -> "Slovenščina"
            "sv" -> "Svenska"
            "ta" -> "தமிழ்"
            "te" -> "తెలుగు"
            "th" -> "ไทย"
            "tk" -> "Türkmençe"
            "tr" -> "Türkçe"
            "uk" -> "Українська"
            "ur" -> "اردو"
            "uz" -> "Oʻzbekcha"
            "val" -> "Valencian"
            "vi" -> "Tiếng Việt"
            "zh" -> "中文"
            else -> java.util.Locale.forLanguageTag(currentLanguageCode).displayName
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.settings),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = VittifySpacing.scaledStandard,
                    end = VittifySpacing.scaledStandard,
                    top = VittifySpacing.scaledStandard + paddingValues.calculateTopPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(VittifySpacing.scaledStandard)
        ) {
            // Section 1: Personalization & Preferences
            FoldableSection(
                title = stringResource(R.string.settings_section_personalization),
                expanded = isPersonalizationExpanded,
                onToggle = { isPersonalizationExpanded = !isPersonalizationExpanded }
            ) {
                val profileImageUri = userPreferences?.profileImageUri?.toUri()
                val profileBackgroundColor = Color(userPreferences?.profileBackgroundColor ?: Color.Transparent.toArgb())

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    // Profile
                    ListItem(
                        modifier = Modifier.settingItemHighlight("profile", bringIntoViewRequesters["profile"], highlightedSettingId, ListItemPosition.Top.toShape()),
                        headline = {
                            Text(
                                text = userPreferences?.userName ?: stringResource(R.string.default_user_name),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.profile_setup_prompt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            ProfileAvatarImage(
                                profileImageUri = userPreferences?.profileImageUri?.toUri(),
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToProfile() },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Appearance
                    ListItem(
                        modifier = Modifier.settingItemHighlight("appearance", bringIntoViewRequesters["appearance"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.appearance),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.appearance_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = purple_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.SCALLOP_12.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = purple_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToAppearance() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Language
                    ListItem(
                        modifier = Modifier.settingItemHighlight("language", bringIntoViewRequesters["language"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.language),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = currentLanguageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = blue_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.COOKIE_8.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Translate,
                                    contentDescription = null,
                                    tint = blue_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showLanguageBottomSheet = true },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Currency
                    ListItem(
                        modifier = Modifier.settingItemHighlight("currency", bringIntoViewRequesters["currency"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.currency),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = userPreferences?.baseCurrency ?: "INR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = green_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.PENTAGON.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.DollarCircle,
                                    contentDescription = null,
                                    tint = green_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToCurrency() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Notifications
                    ListItem(
                        modifier = Modifier.settingItemHighlight("notifications", bringIntoViewRequesters["notifications"], highlightedSettingId, ListItemPosition.Bottom.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.notifications),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.notifications_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = yellow_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.TILTED_PILL.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.NotificationBing,
                                    contentDescription = null,
                                    tint = yellow_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToNotifications() },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }
            }

            // Section 2: Finances & Automation
            FoldableSection(
                title = stringResource(R.string.settings_section_finances),
                expanded = isFinancesExpanded,
                onToggle = { isFinancesExpanded = !isFinancesExpanded }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    // Accounts
                    ListItem(
                        modifier = Modifier.settingItemHighlight("accounts", bringIntoViewRequesters["accounts"], highlightedSettingId, ListItemPosition.Top.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.accounts_and_cards),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.accounts_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = blue_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.STAR_8.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = blue_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToManageAccounts() },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Categories
                    ListItem(
                        modifier = Modifier.settingItemHighlight("categories", bringIntoViewRequesters["categories"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.categories),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.categories_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = purple_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.CLOVER_4.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.Box2,
                                    contentDescription = null,
                                    tint = purple_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToCategories() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Budgets
                    ListItem(
                        modifier = Modifier.settingItemHighlight("budgets", bringIntoViewRequesters["budgets"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.budgets),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.budgets_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = green_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.TILTED_OVAL.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.Status,
                                    contentDescription = null,
                                    tint = green_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToBudgets() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Smart Rules
                    ListItem(
                        modifier = Modifier.settingItemHighlight("rules", bringIntoViewRequesters["rules"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.rules),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.rules_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = yellow_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.SCALLOP_12.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.Fireworks7,
                                    contentDescription = null,
                                    tint = yellow_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToRules() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Transaction Settings
                    ListItem(
                        modifier = Modifier.settingItemHighlight("transaction_settings", bringIntoViewRequesters["transaction_settings"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.transaction_settings),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.transaction_settings_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = cyan_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.COOKIE_8.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = cyan_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToTransactionSettings() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // SMS
                    ListItem(
                        modifier = Modifier.settingItemHighlight("sms", bringIntoViewRequesters["sms"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.sms_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.sms_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = cyan_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.PENTAGON.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.Clock,
                                    contentDescription = null,
                                    tint = cyan_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToSms() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Couple Tracker
                    ListItem(
                        modifier = Modifier.settingItemHighlight("couple_tracker", bringIntoViewRequesters["couple_tracker"], highlightedSettingId, if (isWebhookModeEnabled) ListItemPosition.Middle.toShape() else ListItemPosition.Bottom.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.couple_tracker),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.couple_tracker_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = red_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.TILTED_PILL.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = red_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToCoupleTracker() },
                        shape = if (isWebhookModeEnabled) ListItemPosition.Middle.toShape() else ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Webhooks (if enabled)
                    if (isWebhookModeEnabled) {
                        ListItem(
                            modifier = Modifier.settingItemHighlight("webhooks", bringIntoViewRequesters["webhooks"], highlightedSettingId, ListItemPosition.Bottom.toShape()),
                            headline = {
                                Text(
                                    text = stringResource(R.string.webhooks),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supporting = {
                                Text(
                                    text = stringResource(R.string.webhooks_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = purple_light,
                                            shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.STAR_8.composeShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Webhook,
                                        contentDescription = null,
                                        tint = purple_dark
                                    )
                                }
                            },
                            trailing = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { onNavigateToWebhooks() },
                            shape = ListItemPosition.Bottom.toShape(),
                            padding = PaddingValues(0.dp)
                        )
                    }
                }
            }

            // Section 3: Data & Sync
            FoldableSection(
                title = stringResource(R.string.settings_section_data_sync),
                expanded = isDataSyncExpanded,
                onToggle = { isDataSyncExpanded = !isDataSyncExpanded }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    // Statements & Reports
                    ListItem(
                        modifier = Modifier.settingItemHighlight("pdf_report", bringIntoViewRequesters["pdf_report"], highlightedSettingId, ListItemPosition.Top.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.statements_reports_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.statements_reports_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = blue_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.CLOVER_4.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.ImportArrow01,
                                    contentDescription = null,
                                    tint = blue_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToPdfReport() },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Backup & Cloud Sync
                    ListItem(
                        modifier = Modifier.settingItemHighlight("cloud_backup", bringIntoViewRequesters["cloud_backup"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.backup_and_sync),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = googleDriveEmail?.let { stringResource(R.string.syncing_with, it) }
                                    ?: stringResource(R.string.backup_sync_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = green_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.TILTED_OVAL.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.CloudSync,
                                    contentDescription = null,
                                    tint = green_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToCloudBackup() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Archived Transactions
                    ListItem(
                        modifier = Modifier.settingItemHighlight("archived_transactions", bringIntoViewRequesters["archived_transactions"], highlightedSettingId, ListItemPosition.Bottom.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.manage_archived_transactions),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.archived_transactions_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = orange_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.SCALLOP_12.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.BagTimer,
                                    contentDescription = null,
                                    tint = orange_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToManageArchivedTransactions() },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }
            }

            // Section 4: System & Security
            FoldableSection(
                title = stringResource(R.string.settings_section_system_security),
                expanded = isSystemSecurityExpanded,
                onToggle = { isSystemSecurityExpanded = !isSystemSecurityExpanded }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    // Data Privacy & Security
                    ListItem(
                        modifier = Modifier.settingItemHighlight("data_privacy", bringIntoViewRequesters["data_privacy"], highlightedSettingId, ListItemPosition.Top.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.data_privacy),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.data_privacy_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = red_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.COOKIE_8.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.SecuritySafe,
                                    contentDescription = null,
                                    tint = red_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToDataPrivacy() },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // AI Integration
                    ListItem(
                        modifier = Modifier.settingItemHighlight("ai", bringIntoViewRequesters["ai"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.ai_integration),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.ai_settings_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = purple_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.PENTAGON.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = purple_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToAi() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Customization
                    ListItem(
                        modifier = Modifier.settingItemHighlight("customization", bringIntoViewRequesters["customization"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.customization),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.customization_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = green_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.TILTED_PILL.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = green_dark
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToCustomization() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // About Vittify
                    ListItem(
                        modifier = Modifier.settingItemHighlight("about", bringIntoViewRequesters["about"], highlightedSettingId, ListItemPosition.Middle.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.about),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.about_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = orange_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.STAR_8.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = stringResource(R.string.about),
                                    tint = orange_dark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToAbout() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Developer Options
                    ListItem(
                        modifier = Modifier.settingItemHighlight("developer", bringIntoViewRequesters["developer"], highlightedSettingId, ListItemPosition.Bottom.toShape()),
                        headline = {
                            Text(
                                text = stringResource(R.string.developer_options),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.developer_options_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = grey_light,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.PENTAGON.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Iconax.CodeCircle,
                                    contentDescription = stringResource(R.string.developer_options),
                                    tint = grey_dark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToDeveloper() },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
            Spacer(modifier = Modifier.height(110.dp + bottomNavPadding + 32.dp))
        }

        if (showLanguageBottomSheet) {
            LanguageSelectionBottomSheet(
                selectedLanguageCode = currentLanguageCode,
                onLanguageSelected = { code ->
                    val localeList = androidx.core.os.LocaleListCompat.forLanguageTags(code)
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
                },
                onDismiss = { showLanguageBottomSheet = false }
            )
        }
    }
}
