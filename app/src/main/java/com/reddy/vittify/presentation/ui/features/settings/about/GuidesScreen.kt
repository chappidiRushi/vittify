package com.reddy.vittify.presentation.ui.features.settings.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.CheckCircle
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

data class GuideStep(
    val title: String,
    val description: String
)

data class GuideItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val readTimeMinutes: Int,
    val steps: List<GuideStep>,
    val proTip: String? = null
)

private val GUIDES_LIST = listOf(
    GuideItem(
        id = "getting_started",
        title = "Getting Started: Setup & Initial SMS Sync",
        subtitle = "Grant permissions, choose currency, and scan your past bank messages.",
        category = "Basics",
        readTimeMinutes = 2,
        steps = listOf(
            GuideStep("Grant SMS Permissions", "Vittify requires READ_SMS and RECEIVE_SMS permissions to parse transaction alerts locally on your device. These are used purely on-device."),
            GuideStep("Set Base Currency", "Pick your default display currency (e.g., INR ₹, USD $, EUR €) in Settings > Currency."),
            GuideStep("Perform Initial Scan", "Open Settings > SMS Settings > Sync SMS. Select '3 months' or 'All time' and tap 'Start Sync' to populate your past banking history."),
            GuideStep("Review Accounts", "Head to Settings > Manage Accounts to check detected accounts and set nicknames or custom card colors.")
        ),
        proTip = "Vittify uses cryptographic hashes to ensure messages are never imported twice, so you can safely resync anytime."
    ),
    GuideItem(
        id = "sms_parsing_mastery",
        title = "Mastering Bank SMS Auto-Tracking",
        subtitle = "Learn how bank messages turn into expenses and how to fix missed alerts.",
        category = "Tracking",
        readTimeMinutes = 3,
        steps = listOf(
            GuideStep("Automatic Real-Time Detection", "When your bank sends an SMS starting with keywords like 'debited', 'spent', or 'credited', Vittify parses it within seconds."),
            GuideStep("Account Linking", "Transactions are automatically linked to the account matching the last 4 digits mentioned in the SMS."),
            GuideStep("Reviewing Unrecognized SMS", "If a message isn't parsed, find it under Settings > SMS > Unrecognized SMS. You can turn it into a transaction with one click."),
            GuideStep("Editing Parsed Entries", "Tap any transaction in your timeline to correct the category, edit notes, or adjust the merchant name.")
        ),
        proTip = "Make sure your bank notifications are enabled and not silenced or routed to spam by third-party SMS apps."
    ),
    GuideItem(
        id = "quick_add_guide",
        title = "Conversational Quick Add",
        subtitle = "Log cash and split bills in plain English.",
        category = "Tracking",
        readTimeMinutes = 2,
        steps = listOf(
            GuideStep("Tap or Focus Quick Add", "If enabled on your home screen, tap the Quick Add widget, or tap the '+' button."),
            GuideStep("Type in Natural Language", "Simply type phrases like 'Dinner 850 with friends' or 'Uber to airport 420'."),
            GuideStep("Instant Extraction", "Vittify extracts the amount, detects the merchant, and selects the matching category automatically."),
            GuideStep("Save with One Tap", "Confirm with the checkmark button or press enter on your keyboard to instantly record the transaction.")
        ),
        proTip = "You can turn the Quick Add card on or off anytime via Home Screen > Edit Widgets."
    ),
    GuideItem(
        id = "budgets_guide",
        title = "Setting Up Monthly Category Budgets",
        subtitle = "Take control of discretionary spending and avoid overspending.",
        category = "Planning",
        readTimeMinutes = 3,
        steps = listOf(
            GuideStep("Navigate to Budgets", "Tap Budgets in the navigation bar or search for 'Budgets' in global search."),
            GuideStep("Create New Budget", "Tap the '+' action button to set up an overall monthly spending ceiling or create budgets for specific categories like Dining, Shopping, or Travel."),
            GuideStep("Track Burn Rate", "The visual progress ring shows your percentage spent versus the days elapsed in the month."),
            GuideStep("Threshold Alerts", "Receive gentle reminders when your category expenses reach 80% and 100% of your limit.")
        ),
        proTip = "Budgets automatically roll over and reset on the 1st of every calendar month."
    ),
    GuideItem(
        id = "smart_rules_guide",
        title = "Automating Categorization with Smart Rules",
        subtitle = "Create custom keyword triggers so you never have to recategorize twice.",
        category = "Automation",
        readTimeMinutes = 2,
        steps = listOf(
            GuideStep("Open Smart Rules", "Navigate to Settings > Finances > Smart Rules."),
            GuideStep("Add Rule", "Tap '+' and define your matching condition (e.g., Merchant contains 'Swiggy', 'Starbucks', 'Shell')."),
            GuideStep("Select Target Category", "Assign the category (e.g., Food & Dining, Fuel, Groceries) that should be automatically applied."),
            GuideStep("Apply to Existing", "Choose whether the rule should also run retrospectively across past transactions or only on future ones.")
        ),
        proTip = "Smart Rules take precedence over default auto-categorization."
    ),
    GuideItem(
        id = "couple_tracker_guide",
        title = "Couple Tracker: Shared Partner Finances",
        subtitle = "Link two phones via QR code to see combined household spending.",
        category = "Sync",
        readTimeMinutes = 3,
        steps = listOf(
            GuideStep("Open Couple Tracker", "Go to Settings > Finances > Couple Tracker on both devices."),
            GuideStep("Display QR Code", "One partner taps 'Show Pairing Code / QR', displaying a secure local QR code."),
            GuideStep("Scan to Pair", "The second partner scans the QR code. The devices establish an encrypted peer link."),
            GuideStep("Switch Views", "On the Home screen top bar, toggle between 'Both' (combined view), 'Me' (personal view), and 'Partner'.")
        ),
        proTip = "Couple pairing preserves privacy: each partner's database is stored locally on their own phone."
    ),
    GuideItem(
        id = "cloud_backup_guide",
        title = "Zero-Knowledge Cloud Backup",
        subtitle = "Safeguard your data to Google Drive or WebDAV with encryption.",
        category = "Security",
        readTimeMinutes = 3,
        steps = listOf(
            GuideStep("Open Backup & Sync", "Head to Settings > Data & Sync > Backup & Sync."),
            GuideStep("Choose Provider", "Select your Google Drive account or enter your personal WebDAV server credentials."),
            GuideStep("Set Backup Passphrase", "Choose a strong secret passphrase. Your data is encrypted on-device before upload."),
            GuideStep("Restore on New Phone", "When switching phones, sign into Drive / WebDAV during onboarding and enter your passphrase to restore instantly.")
        ),
        proTip = "Remember your passphrase! Because encryption is client-side, nobody else can recover a lost passphrase."
    ),
    GuideItem(
        id = "pdf_report_guide",
        title = "Generating PDF Statements",
        subtitle = "Create clean, professional financial statements ready to share or print.",
        category = "Reports",
        readTimeMinutes = 2,
        steps = listOf(
            GuideStep("Open PDF Report", "Go to Settings > Data & Sync > Transaction Report PDF."),
            GuideStep("Select Date Range", "Choose Last Month, Current Financial Year, or set custom start and end dates."),
            GuideStep("Filter by Account or Category", "Optionally filter to specific bank accounts or spending tags."),
            GuideStep("Export & Share", "Tap 'Generate PDF' to preview and share directly via email, cloud, or print.")
        ),
        proTip = "PDF statements are rendered completely on-device using Apache PDFBox without internet access."
    ),
    GuideItem(
        id = "webhooks_guide",
        title = "External Webhooks & Automation",
        subtitle = "Forward live transactions to Home Assistant, n8n, or custom servers.",
        category = "Automation",
        readTimeMinutes = 3,
        steps = listOf(
            GuideStep("Enable Webhooks", "Go to Settings > System & Security > Webhooks and toggle Webhook Mode ON."),
            GuideStep("Configure Endpoint", "Enter your target POST URL (e.g. your local Home Assistant or automation webhook)."),
            GuideStep("Set Custom Headers", "Optionally add Authorization bearer tokens or custom JSON headers."),
            GuideStep("Test Payload", "Use the 'Send Test Ping' button to confirm your endpoint receives and acknowledges the payload.")
        ),
        proTip = "Failed webhooks are queued with exponential backoff so no events are lost during temporary network outages."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidesScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedGuideId by remember { mutableStateOf<String?>(null) }

    val categories = remember {
        listOf("All") + GUIDES_LIST.map { it.category }.distinct()
    }

    val filteredList = remember(searchQuery, selectedCategory) {
        GUIDES_LIST.filter { guide ->
            val matchesCategory = selectedCategory == "All" || guide.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    guide.title.contains(searchQuery, ignoreCase = true) ||
                    guide.subtitle.contains(searchQuery, ignoreCase = true) ||
                    guide.steps.any { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.guides),
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
            // Header info
            item(key = "guides_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm, bottom = Spacing.xs)
                ) {
                    Text(
                        text = "Step-by-Step Walkthroughs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Practical guides to unlock the full potential of Vittify's privacy-first features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search box
            item(key = "search_box") {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search guides, tips, workflows...",
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

            // Category filter chips
            item(key = "category_chips") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
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
                                    text = category,
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
                            text = "No guides found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Try searching with a different term.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { guide ->
                    val isExpanded = expandedGuideId == guide.id
                    val rotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "guideRotation"
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
                                expandedGuideId = if (isExpanded) null else guide.id
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Surface(
                                            shape = VittifyShapes.pill,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = guide.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "•  ${guide.readTimeMinutes} min read",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = guide.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = guide.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
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
                                        .padding(top = Spacing.md),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                                ) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    )

                                    guide.steps.forEachIndexed { index, step ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = step.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = step.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 20.sp
                                                )
                                            }
                                        }
                                    }

                                    if (guide.proTip != null) {
                                        Surface(
                                            shape = VittifyShapes.hero,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(Spacing.md),
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "💡",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Column {
                                                    Text(
                                                        text = "Pro Tip",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = guide.proTip,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

