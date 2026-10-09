package com.reddy.vittify.presentation.ui.features.settings.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.R
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

enum class FaqCategory(val label: String) {
    ALL("All"),
    SMS_PARSING("SMS & Parsing"),
    PRIVACY_SECURITY("Privacy & Security"),
    SYNC_BACKUP("Sync & Backup"),
    ACCOUNTS_TRACKING("Accounts & Transactions"),
    BUDGETS_RULES("Budgets & Rules"),
    FEATURES_AI("AI & Customization")
}

data class FaqItem(
    val id: String,
    val category: FaqCategory,
    val question: String,
    val answer: String,
    val highlight: String? = null
)

private val FAQ_LIST = listOf(
    // SMS & Parsing
    FaqItem(
        id = "how_sms_works",
        category = FaqCategory.SMS_PARSING,
        question = "How does Vittify automatically track my expenses from SMS?",
        answer = "Vittify reads incoming bank and financial SMS alerts directly on your device. It runs an on-device rule engine and parser tailored to Indian financial institutions to extract the amount, transaction type (debit/credit), merchant or recipient, and account reference. Everything happens in real-time right when you receive the SMS alert.",
        highlight = "100% on-device processing for parsing, with optional cloud AI."
    ),
    FaqItem(
        id = "which_banks_supported",
        category = FaqCategory.SMS_PARSING,
        question = "Which Indian banks, credit cards, and wallets are supported?",
        answer = "Vittify supports major Indian banks and payment systems including HDFC, SBI, ICICI, Axis, Kotak Mahindra, PNB, Bank of Baroda, Canara, IndusInd, Federal Bank, IDFC FIRST, Standard Chartered, HSBC, RBL, Yes Bank, Paytm Payments Bank, Airtel Payments Bank, Fi Money, Jupiter, slice, Uni, OneCard, CRED, and standard UPI transactional notifications.",
        highlight = "Over 25+ banking formats supported with continuous updates."
    ),
    FaqItem(
        id = "why_sms_not_parsed",
        category = FaqCategory.SMS_PARSING,
        question = "Why was a particular transaction SMS not detected or categorized?",
        answer = "Bank messages must come from recognized financial sender headers (e.g., AD-HDFCBK, VK-SBIINB) and contain transactional keywords like 'debited', 'credited', 'spent', or 'transferred'. OTPs, promotional SMS, balance inquiry messages, and personal P2P messages are intentionally filtered out. If a genuine transaction SMS was missed, you can view it under Settings > SMS Settings > Unrecognized SMS and submit feedback.",
        highlight = "Tip: You can manually resync past messages anytime in Settings > SMS Settings > Sync SMS."
    ),
    FaqItem(
        id = "unrecognized_sms",
        category = FaqCategory.SMS_PARSING,
        question = "What is the 'Unrecognized SMS' section?",
        answer = "When Vittify detects a message from a financial sender that doesn't match known parser formats, it safely saves it in the Unrecognized SMS list. You can review the raw message, convert it manually into a transaction with one click, or report the template to improve future parser updates.",
        highlight = "Find it in Settings > Finances > SMS > Unrecognized SMS."
    ),
    FaqItem(
        id = "resync_old_sms",
        category = FaqCategory.SMS_PARSING,
        question = "Can I import or rescan my past SMS transaction history?",
        answer = "Yes! Go to Settings > SMS Settings > Sync SMS. You can select your desired scanning window (1 month, 3 months, 6 months, 1 year, or All time). Vittify uses intelligent transaction deduplication (via cryptographic message hashes) so duplicate transactions are never created.",
        highlight = "Safe to rescan anytime without fear of duplicates."
    ),

    // Privacy & Security
    FaqItem(
        id = "data_privacy_manifesto",
        category = FaqCategory.PRIVACY_SECURITY,
        question = "Is my financial data uploaded to external servers?",
        answer = "Never. Vittify operates under an uncompromising privacy-first architecture. There is no central server, no telemetry trackers, no advertising analytics, and no third-party data sharing. Your transactions, account balances, and personal information stay strictly inside your device's sandboxed local Room database.",
        highlight = "Zero remote tracking. Your money, your private business."
    ),
    FaqItem(
        id = "app_lock",
        category = FaqCategory.PRIVACY_SECURITY,
        question = "How do I protect the app with Biometrics or App Lock?",
        answer = "Open Settings > Data Privacy and enable App Lock. You can use your device's fingerprint sensor, face recognition, or system device PIN/pattern. You can also configure the auto-lock timeout duration so the app secures itself automatically whenever you switch apps.",
        highlight = "Uses Android's hardware-backed BiometricPrompt."
    ),
    FaqItem(
        id = "sms_permission_safety",
        category = FaqCategory.PRIVACY_SECURITY,
        question = "Why does Vittify require READ_SMS and RECEIVE_SMS permissions?",
        answer = "Android requires these permissions so apps can read incoming notifications and existing message threads. Vittify only scans messages from verified bank alphanumeric headers. Personal messages from friends, family, contacts, and personal phone numbers are completely ignored and never stored in memory.",
        highlight = "Personal messages are never read, stored, or analyzed."
    ),
    FaqItem(
        id = "delete_data",
        category = FaqCategory.PRIVACY_SECURITY,
        question = "How do I permanently delete all my stored records?",
        answer = "You have full ownership of your data. In Settings > Data Privacy, tap 'Delete All Data'. This wipes all local Room database tables, transactions, accounts, categories, and cached preferences permanently.",
        highlight = "Irreversible complete data wipe available on demand."
    ),

    // Sync & Backup
    FaqItem(
        id = "cloud_backup_drive",
        category = FaqCategory.SYNC_BACKUP,
        question = "How does Cloud Backup work with Google Drive and WebDAV?",
        answer = "Vittify allows zero-knowledge encrypted backups to your personal Google Drive or private self-hosted WebDAV server (such as Nextcloud). Backups are compressed JSON snapshots encrypted with your custom passphrase. Vittify servers never sit in between — the communication is direct between your phone and your chosen storage provider.",
        highlight = "Client-side encrypted backups directly to your personal cloud."
    ),
    FaqItem(
        id = "p2p_device_sync",
        category = FaqCategory.SYNC_BACKUP,
        question = "What is Peer-to-Peer (P2P) Device Sync?",
        answer = "P2P Device Sync allows two devices (such as your phone and tablet, or you and your partner) to synchronize financial transactions directly over your local Wi-Fi or Bluetooth using secure WebRTC connections. No cloud storage is needed.",
        highlight = "Fast, encrypted, serverless local device pairing."
    ),
    FaqItem(
        id = "couple_tracker",
        category = FaqCategory.SYNC_BACKUP,
        question = "How does the Couple Tracker feature work?",
        answer = "Couple Tracker enables partners to link devices seamlessly. Once paired via QR code, you can toggle between 'Both' (combined household spending), 'Me' (personal spending), and 'Partner' from the top bar on the Home screen. Each partner maintains independent control while sharing a unified budget overview.",
        highlight = "Toggle between personal and joint household spending in one tap."
    ),
    FaqItem(
        id = "webhooks_explained",
        category = FaqCategory.SYNC_BACKUP,
        question = "What are Webhooks and how can power users use them?",
        answer = "If you have a self-hosted homelab, n8n instance, Home Assistant, Google Sheets API, or custom backend, you can enable Webhooks under Settings > Finances > Webhooks. Whenever a new transaction is recorded or parsed, Vittify automatically dispatches an HTTP POST payload with full transaction JSON metadata.",
        highlight = "Built for developers and automation enthusiasts."
    ),

    // Accounts & Transactions
    FaqItem(
        id = "manual_transactions",
        category = FaqCategory.ACCOUNTS_TRACKING,
        question = "Can I log cash payments and manual transactions?",
        answer = "Absolutely. Tap the '+' floating action button on any screen to open the Add Transaction screen. You can select Cash, Bank Account, or Credit Card, enter the amount, select an expressive category, and add notes or attachments.",
        highlight = "Seamlessly blend automatic bank SMS and cash records."
    ),
    FaqItem(
        id = "multiple_accounts",
        category = FaqCategory.ACCOUNTS_TRACKING,
        question = "How do I manage multiple bank accounts and credit cards?",
        answer = "Navigate to Settings > Manage Accounts. Here you can add new bank accounts, digital wallets, credit cards, or cash balances. You can specify last 4 digits (to match SMS alerts automatically), set custom card colors, and establish credit limits.",
        highlight = "Automatic balance tracking per account based on parsed SMS."
    ),
    FaqItem(
        id = "archived_transactions",
        category = FaqCategory.ACCOUNTS_TRACKING,
        question = "What happens when I delete a transaction?",
        answer = "Deleted transactions are moved to the Archived Transactions bin so you never lose data accidentally. You can view, restore, or permanently remove archived records anytime from Settings > Data & Sync > Archived Transactions.",
        highlight = "Two-tier safety net prevents accidental loss."
    ),
    FaqItem(
        id = "export_pdf",
        category = FaqCategory.ACCOUNTS_TRACKING,
        question = "Can I export my transactions as a PDF statement or CSV?",
        answer = "Yes! Under Settings > Data & Sync > PDF Report, you can generate beautifully formatted PDF statements for any date range, filter by bank account or category, and share or print them for accounting or tax purposes. You can also export raw CSV / JSON in Settings > Data Privacy.",
        highlight = "Professional PDF export with category breakdowns and summaries."
    ),

    // Budgets & Rules
    FaqItem(
        id = "budgets_how_to",
        category = FaqCategory.BUDGETS_RULES,
        question = "How do Budgets work in Vittify?",
        answer = "You can set an overall monthly spending budget or create granular budgets for specific categories (e.g., Dining, Groceries, Shopping). Vittify tracks your spending pacing, warns you with tactile and visual alerts when approaching 80% or 100% of your threshold, and resets automatically on the 1st of each month.",
        highlight = "Real-time burn rate and warning alerts keep your spending in check."
    ),
    FaqItem(
        id = "smart_rules",
        category = FaqCategory.BUDGETS_RULES,
        question = "What are Smart Rules and how do they automate categorization?",
        answer = "Smart Rules let you define custom rules for automatic tagging. For example, you can create a rule: 'Whenever merchant contains Swiggy or Zomato, set category to Food & Dining'. You can match by merchant keywords, sender names, or amounts. Vittify applies your rules instantly to all incoming transactions.",
        highlight = "Set it once, and let Vittify handle repetitive sorting forever."
    ),
    FaqItem(
        id = "subscriptions_tracking",
        category = FaqCategory.BUDGETS_RULES,
        question = "How do recurring Subscriptions work?",
        answer = "In the Subscriptions section, you can add recurring monthly or yearly services like Netflix, Spotify, gym memberships, or rent. Vittify predicts upcoming billing dates, alerts you before renewal, and counts upcoming bills against your monthly available budget.",
        highlight = "Never get caught off-guard by forgotten auto-renewals."
    ),

    // AI & Customization
    FaqItem(
        id = "nlp_quick_add",
        category = FaqCategory.FEATURES_AI,
        question = "What is Quick Add in plain English?",
        answer = "Quick Add lets you type transactions conversationally. For example, typing 'Paid 450 at Starbucks on coffee' automatically parses the amount (450), merchant (Starbucks), and category (Food & Drink). You can enable or disable the Quick Add widget on the home screen from Edit Widgets.",
        highlight = "Instant conversational expense logging."
    ),
    FaqItem(
        id = "gemini_ai_integration",
        category = FaqCategory.FEATURES_AI,
        question = "How does the optional Gemini AI feature work?",
        answer = "If you provide your own Google Gemini API key in Settings > AI Integration, Vittify can provide intelligent financial spending summaries, personalized savings advice, and categorization assistance. Your API key is stored securely in encrypted local DataStore and only called on demand.",
        highlight = "Bring Your Own Key (BYOK) — optional, private, and customizable."
    ),
    FaqItem(
        id = "customizing_appearance",
        category = FaqCategory.FEATURES_AI,
        question = "How can I customize themes, colors, and layout?",
        answer = "Head to Settings > Appearance to toggle between Pure Black AMOLED mode, Material You Dynamic Monet theming, or curated expressive palettes (Latte, Macchiato, Rosé Pine). You can also customize custom font styles, squircle surface geometry, and choose between standard edge-to-edge navigation or floating footer navigation.",
        highlight = "Built on Material 3 Expressive with full AMOLED & font support."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FaqCategory.ALL) }
    var expandedItemId by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(searchQuery, selectedCategory) {
        FAQ_LIST.filter { item ->
            val matchesCategory = selectedCategory == FaqCategory.ALL || item.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    item.question.contains(searchQuery, ignoreCase = true) ||
                    item.answer.contains(searchQuery, ignoreCase = true) ||
                    (item.highlight?.contains(searchQuery, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.faq),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical()
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = paddingValues.calculateTopPadding(),
                    bottom = 24.dp
                ),
            flingBehavior = rememberOverscrollFlingBehavior { listState },
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Header Info Pill
            item(key = "faq_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm, bottom = Spacing.xs)
                ) {
                    Text(
                        text = "Everything You Need to Know",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Frequently asked questions about on-device tracking, privacy, bank support, and backups.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Box
            item(key = "search_box") {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search questions, banks, privacy...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = VittifyShapes.hero,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }

            // Category Filter Chips
            item(key = "category_chips") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(FaqCategory.entries) { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            onClick = { selectedCategory = category },
                            shape = VittifyShapes.pill,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item(key = "empty_state") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = "No questions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Try searching for a different keyword or choose another category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    val isExpanded = expandedItemId == item.id
                    val rotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "expandRotation"
                    )

                    Surface(
                        shape = VittifyShapes.platter,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VittifyShapes.platter)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                shape = VittifyShapes.platter
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                expandedItemId = if (isExpanded) null else item.id
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = VittifyShapes.pill,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = item.category.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = item.question,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(Spacing.sm))

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.rotate(rotation)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.md)
                                ) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(bottom = Spacing.md)
                                    )

                                    Text(
                                        text = item.answer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 22.sp
                                    )

                                    if (item.highlight != null) {
                                        Spacer(modifier = Modifier.height(Spacing.sm))
                                        Surface(
                                            shape = VittifyShapes.hero,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "💡 ",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = item.highlight,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

