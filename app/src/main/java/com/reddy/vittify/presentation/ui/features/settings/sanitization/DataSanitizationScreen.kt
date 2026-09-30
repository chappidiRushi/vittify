package com.reddy.vittify.presentation.ui.features.settings.sanitization

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.model.AccountDiscrepancy
import com.reddy.vittify.data.model.DuplicateTransactionGroup
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.*
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import com.reddy.vittify.utils.CurrencyFormatter
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun DataSanitizationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccountAudit: (String, String) -> Unit = { _, _ -> },
    initialTabName: String? = null,
    viewModel: DataSanitizationViewModel = hiltViewModel(),
    blurEffects: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = remember { HazeState() }
    val haptic = rememberAppHapticFeedback()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Handle initial tab selection if routed with argument (e.g. "archived" or "accounts")
    LaunchedEffect(initialTabName) {
        when (initialTabName?.lowercase()) {
            "archived" -> viewModel.selectTab(SanitizationTab.ARCHIVED)
            "accounts" -> viewModel.selectTab(SanitizationTab.ACCOUNTS)
            "ownership" -> viewModel.selectTab(SanitizationTab.OWNERSHIP)
            "transactions" -> viewModel.selectTab(SanitizationTab.TRANSACTIONS)
            "system" -> viewModel.selectTab(SanitizationTab.SYSTEM)
        }
    }

    // Snackbar notifications
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Dialog & Sheet States
    var confirmDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDialogTitle by remember { mutableStateOf("") }
    var confirmDialogDesc by remember { mutableStateOf("") }
    var confirmDialogDanger by remember { mutableStateOf(false) }

    var accountToEdit by remember { mutableStateOf<AccountBalanceEntity?>(null) }
    var showAutoDeleteSheet by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.data_sanitization_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actionContent = {
                    IconButton(
                        onClick = {
                            haptic.click()
                            viewModel.runFullAudit()
                        }
                    ) {
                        val rotationAnim by animateFloatAsState(
                            targetValue = if (uiState.isScanning) 360f else 0f,
                            animationSpec = if (uiState.isScanning) infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ) else tween(300),
                            label = "spin"
                        )
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            modifier = Modifier.graphicsLayer { rotationZ = rotationAnim }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { paddingValues ->
        if (uiState.isLoading && !uiState.isScanning && uiState.healthAudit.totalRecordsScanned == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                LoadingCircle()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .overScrollVertical(),
                state = listState,
                flingBehavior = rememberOverscrollFlingBehavior { listState },
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + Spacing.sm,
                    bottom = 120.dp,
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 1. Health Score Hero Platter
                item(key = "hero_score") {
                    HealthScoreHeroPlatter(
                        audit = uiState.healthAudit,
                        isScanning = uiState.isScanning,
                        onQuickSanitize = {
                            haptic.click()
                            viewModel.quickSanitizeAll()
                        }
                    )
                }

                // 2. Segmented Filter Tabs
                item(key = "tabs_row") {
                    SanitizationTabBar(
                        activeTab = uiState.activeTab,
                        audit = uiState.healthAudit,
                        onTabSelected = { tab ->
                            haptic.selection()
                            viewModel.selectTab(tab)
                        }
                    )
                }

                // 3. Tab Contents with Animated Transitions
                when (uiState.activeTab) {
                    SanitizationTab.OVERVIEW -> {
                        overviewTabContent(
                            audit = uiState.healthAudit,
                            onSelectTab = { tab ->
                                haptic.selection()
                                viewModel.selectTab(tab)
                            },
                            onQuickSanitize = {
                                haptic.click()
                                viewModel.quickSanitizeAll()
                            }
                        )
                    }

                    SanitizationTab.OWNERSHIP -> {
                        ownershipTabContent(
                            issue = uiState.healthAudit.ownership,
                            myDeviceId = uiState.myDeviceId,
                            partnerName = uiState.partnerName,
                            isCoupleTracking = uiState.isCoupleTrackingEnabled,
                            expandedSectionId = uiState.expandedSectionId,
                            onToggleSection = { viewModel.toggleSection(it) },
                            onClaimAll = {
                                haptic.click()
                                viewModel.claimAllUnassigned()
                            },
                            onClaimAccount = { accId ->
                                haptic.click()
                                viewModel.claimAccount(accId)
                            },
                            onClaimCard = { cardId ->
                                haptic.click()
                                viewModel.claimCard(cardId)
                            },
                            onAlignOwners = {
                                haptic.click()
                                viewModel.alignOwnersWithAccounts()
                            },
                            onReassignForeign = { fromId, toId ->
                                haptic.click()
                                viewModel.reassignForeignRecords(fromId, toId)
                            }
                        )
                    }

                    SanitizationTab.ACCOUNTS -> {
                        accountsTabContent(
                            issue = uiState.healthAudit.accounts,
                            expandedSectionId = uiState.expandedSectionId,
                            onToggleSection = { viewModel.toggleSection(it) },
                            onUnhideAll = {
                                haptic.click()
                                viewModel.unhideAllAccounts()
                            },
                            onUnhideSingle = { bank, last4 ->
                                haptic.click()
                                viewModel.unhideAccount(bank, last4)
                            },
                            onFixAllBalances = {
                                haptic.click()
                                viewModel.fixAllAccountDiscrepancies()
                            },
                            onFixBalance = { accId, expected ->
                                haptic.click()
                                viewModel.fixAccountDiscrepancy(accId, expected)
                            },
                            onAuditAccount = { bank, last4 ->
                                onNavigateToAccountAudit(bank, last4)
                            },
                            onEditAccount = { accountToEdit = it },
                            onDeleteAccount = { acc ->
                                confirmDialogTitle = "Delete Account"
                                confirmDialogDesc = "Are you sure you want to delete ${acc.bankName} (••• ${acc.accountLast4})? This cannot be undone."
                                confirmDialogDanger = true
                                confirmDialogAction = { viewModel.deleteAccount(acc) }
                            },
                            onDeleteCard = { cardId ->
                                confirmDialogTitle = "Delete Card"
                                confirmDialogDesc = "Are you sure you want to delete this orphaned card? This cannot be undone."
                                confirmDialogDanger = true
                                confirmDialogAction = { viewModel.deleteCard(cardId) }
                            }
                        )
                    }

                    SanitizationTab.TRANSACTIONS -> {
                        transactionsTabContent(
                            issue = uiState.healthAudit.transactions,
                            expandedSectionId = uiState.expandedSectionId,
                            onToggleSection = { viewModel.toggleSection(it) },
                            onDeduplicateAll = {
                                haptic.click()
                                viewModel.autoDeduplicateTransactions()
                            },
                            onDeleteSingleTransaction = { id ->
                                haptic.click()
                                viewModel.deleteTransaction(id)
                            },
                            onMarkDuplicateDismissed = { id ->
                                haptic.click()
                                viewModel.markDuplicateDismissed(id)
                            },
                            onMarkDuplicateGroupDismissed = { group ->
                                haptic.click()
                                viewModel.markDuplicateGroupDismissed(group)
                            },
                            onDeleteCorrupted = {
                                haptic.click()
                                viewModel.deleteCorruptedTransactions()
                            },
                            onUnlinkAllOrphaned = {
                                haptic.click()
                                viewModel.unlinkOrphanedTransactions()
                            },
                            onUnlinkSingleTransaction = { id ->
                                haptic.click()
                                viewModel.unlinkSingleTransaction(id)
                            },
                            onDeleteOrphanedTransactions = {
                                confirmDialogTitle = "Delete All Orphaned Transactions"
                                confirmDialogDesc = "Are you sure you want to permanently delete all ${uiState.healthAudit.transactions.orphanedTransactions.size} orphaned transactions? This cannot be undone."
                                confirmDialogDanger = true
                                confirmDialogAction = { viewModel.deleteOrphanedTransactions() }
                            },
                            onAutoResolveUnknownMerchants = {
                                haptic.click()
                                viewModel.autoResolveUnknownMerchants()
                            },
                            onUpdateMerchantName = { id, name ->
                                haptic.click()
                                viewModel.updateMerchantName(id, name)
                            }
                        )
                    }

                    SanitizationTab.ARCHIVED -> {
                        archivedTabContent(
                            uiState = uiState,
                            onSearchQueryChanged = { viewModel.onArchivedSearchQueryChanged(it) },
                            onToggleSelectionMode = {
                                haptic.click()
                                viewModel.toggleArchivedSelectionMode()
                            },
                            onToggleSelect = { id ->
                                haptic.selection()
                                viewModel.toggleSelectArchivedTransaction(id)
                            },
                            onSelectAll = {
                                haptic.click()
                                viewModel.selectAllArchived()
                            },
                            onClearSelection = {
                                haptic.click()
                                viewModel.clearArchivedSelection()
                            },
                            onRestoreSelected = {
                                haptic.click()
                                viewModel.restoreSelectedArchivedTransactions()
                            },
                            onRestoreSingle = {
                                haptic.click()
                                viewModel.restoreSingleArchivedTransaction(it)
                            },
                            onDeleteSelected = {
                                confirmDialogTitle = "Permanently Delete Selected"
                                confirmDialogDesc = "Are you sure you want to permanently delete ${uiState.archivedSelectedIds.size} transactions? This cannot be undone."
                                confirmDialogDanger = true
                                confirmDialogAction = { viewModel.permanentlyDeleteSelectedArchived() }
                            },
                            onDeleteSingle = { tx ->
                                confirmDialogTitle = "Permanently Delete"
                                confirmDialogDesc = "Permanently delete ${tx.merchantName} (${CurrencyFormatter.formatCurrency(tx.amount, tx.currency)})?"
                                confirmDialogDanger = true
                                confirmDialogAction = { viewModel.permanentlyDeleteSingleArchived(tx) }
                            },
                            onEmptyArchive = {
                                confirmDialogTitle = "Empty Archive Trash"
                                confirmDialogDesc = "Are you sure you want to permanently delete all ${uiState.allArchivedTransactions.size} archived transactions? This action is irreversible."
                                confirmDialogDanger = true
                                confirmDialogAction = { viewModel.emptyArchivedTrash() }
                            },
                            onConfigureAutoDelete = { showAutoDeleteSheet = true }
                        )
                    }

                    SanitizationTab.SYSTEM -> {
                        systemTabContent(
                            issue = uiState.healthAudit.system,
                            onPurgeSample = {
                                confirmDialogTitle = "Remove All Sample Data"
                                confirmDialogDesc = "This will permanently remove all generated demo transactions, sample accounts, cards, and budgets."
                                confirmDialogDanger = true
                                confirmDialogAction = { viewModel.purgeSampleData() }
                            },
                            onPurgeSms = {
                                confirmDialogTitle = "Purge Unrecognized SMS"
                                confirmDialogDesc = "Clear all ${uiState.healthAudit.system.unrecognizedSmsCount} unparsed SMS records?"
                                confirmDialogDanger = false
                                confirmDialogAction = { viewModel.purgeUnrecognizedSms() }
                            },
                            onPurgeTrash = {
                                confirmDialogTitle = "Empty Trash"
                                confirmDialogDesc = "Permanently delete all soft-deleted archived transactions?"
                                confirmDialogDanger = true
                                confirmDialogAction = { viewModel.purgeTrashTransactions() }
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (confirmDialogAction != null) {
        AlertDialog(
            onDismissRequest = { confirmDialogAction = null },
            icon = {
                Icon(
                    imageVector = if (confirmDialogDanger) Icons.Rounded.DeleteForever else Icons.Rounded.CleaningServices,
                    contentDescription = null,
                    tint = if (confirmDialogDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = confirmDialogTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = confirmDialogDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = confirmDialogAction
                        confirmDialogAction = null
                        action?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (confirmDialogDanger) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    ),
                    shape = VittifyShapes.button
                ) {
                    Text(if (confirmDialogDanger) stringResource(R.string.delete) else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDialogAction = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = VittifyShapes.dialog,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }

    // Edit Account Sheet
    accountToEdit?.let { account ->
        EditAccountQuickSheet(
            account = account,
            onDismiss = { accountToEdit = null },
            onSave = { updated ->
                accountToEdit = null
                viewModel.runFullAudit()
            }
        )
    }

    // Auto-Delete Sheet
    if (showAutoDeleteSheet) {
        AutoDeleteConfigSheet(
            enabled = uiState.autoDeleteArchivedEnabled,
            currentDays = uiState.autoDeleteArchivedDays,
            onDismiss = { showAutoDeleteSheet = false },
            onSave = { enabled, days ->
                viewModel.updateAutoDeleteSettings(enabled, days)
                showAutoDeleteSheet = false
            }
        )
    }
}

// ---------------------------------------------------------------------------
// 1. HERO PLATTER (Health Score & 1-Tap Sanitize)
// ---------------------------------------------------------------------------

@Composable
fun HealthScoreHeroPlatter(
    audit: com.reddy.vittify.data.model.DataHealthAudit,
    isScanning: Boolean,
    onQuickSanitize: () -> Unit
) {
    val animatedScore by animateIntAsState(
        targetValue = audit.healthScore,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "score"
    )

    val (scoreColor, statusText, statusIcon) = when {
        audit.healthScore >= 90 -> Triple(
            Color(0xFF2E7D32),
            stringResource(R.string.health_status_clean),
            Icons.Rounded.CheckCircle
        )
        audit.healthScore >= 70 -> Triple(
            Color(0xFFF57C00),
            stringResource(R.string.health_status_good),
            Icons.Rounded.WarningAmber
        )
        else -> Triple(
            MaterialTheme.colorScheme.error,
            stringResource(R.string.health_status_warning),
            Icons.Rounded.ErrorOutline
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = VittifyShapes.hero,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Top Row: Score & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Circular Gauge Badge
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$animatedScore%",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor
                            )
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = scoreColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = scoreColor
                                )
                            )
                        }
                        Text(
                            text = if (isScanning) "Scanning your financial records..."
                            else "${audit.totalIssuesFound} issue(s) detected across ${audit.totalRecordsScanned} records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stats Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatPill(
                    label = "Ownership",
                    count = audit.ownership.totalIssuesCount,
                    isHealthy = audit.ownership.totalIssuesCount == 0,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Accounts",
                    count = audit.accounts.totalIssuesCount,
                    isHealthy = audit.accounts.totalIssuesCount == 0,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Duplicates",
                    count = audit.transactions.totalDuplicateTxnCount,
                    isHealthy = audit.transactions.totalDuplicateTxnCount == 0,
                    modifier = Modifier.weight(1f)
                )
            }

            // 1-Tap Sanitize CTA (shown when issues exist)
            if (audit.totalIssuesFound > 0) {
                Button(
                    onClick = onQuickSanitize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .scale(scaleAnim),
                    interactionSource = interactionSource,
                    shape = VittifyShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.quick_sanitize_action),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    count: Int,
    isHealthy: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = VittifyShapes.pill,
        color = if (isHealthy) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isHealthy) "✓" else count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. TAB SEGMENT ROW
// ---------------------------------------------------------------------------

@Composable
fun SanitizationTabBar(
    activeTab: SanitizationTab,
    audit: com.reddy.vittify.data.model.DataHealthAudit,
    onTabSelected: (SanitizationTab) -> Unit
) {
    val tabs = SanitizationTab.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = Spacing.xs)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = activeTab == tab
            val badgeCount = when (tab) {
                SanitizationTab.OVERVIEW -> audit.totalIssuesFound
                SanitizationTab.OWNERSHIP -> audit.ownership.totalIssuesCount
                SanitizationTab.ACCOUNTS -> audit.accounts.totalIssuesCount
                SanitizationTab.TRANSACTIONS -> audit.transactions.totalIssuesCount
                SanitizationTab.ARCHIVED -> audit.system.archivedTransactionsCount
                SanitizationTab.SYSTEM -> audit.system.totalIssuesCount
            }

            SegmentedButton(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tab.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (badgeCount > 0 && tab != SanitizationTab.OVERVIEW) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.errorContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. OVERVIEW TAB
// ---------------------------------------------------------------------------

fun androidx.compose.foundation.lazy.LazyListScope.overviewTabContent(
    audit: com.reddy.vittify.data.model.DataHealthAudit,
    onSelectTab: (SanitizationTab) -> Unit,
    onQuickSanitize: () -> Unit
) {
    item {
        Text(
            text = "Health Audit Summary",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = Spacing.xs, top = Spacing.sm)
        )
    }

    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Ownership Platter
                DomainSummaryRow(
                    title = "Ownership & Sync Hygiene",
                    desc = if (audit.ownership.totalIssuesCount == 0) "All data assigned to your verified device"
                    else "${audit.ownership.totalUnassignedCount} unassigned & ${audit.ownership.totalForeignCount} foreign records",
                    icon = Icons.Rounded.PersonPin,
                    issueCount = audit.ownership.totalIssuesCount,
                    onClick = { onSelectTab(SanitizationTab.OWNERSHIP) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // 2. Accounts Platter
                DomainSummaryRow(
                    title = "Account Health & Diagnostics",
                    desc = if (audit.accounts.totalIssuesCount == 0) "No duplicate or malformed accounts detected"
                    else "${audit.accounts.duplicateAccountGroups.size} duplicate groups • ${audit.accounts.discrepancies.size} balance discrepancies",
                    icon = Icons.Rounded.AccountBalance,
                    issueCount = audit.accounts.totalIssuesCount,
                    onClick = { onSelectTab(SanitizationTab.ACCOUNTS) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // 3. Transactions Platter
                DomainSummaryRow(
                    title = "Transaction Hygiene & Anomalies",
                    desc = if (audit.transactions.totalIssuesCount == 0) "No duplicate or corrupted transactions found"
                    else "${audit.transactions.totalDuplicateTxnCount} duplicates • ${audit.transactions.corruptedTransactions.size} corrupted",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    issueCount = audit.transactions.totalIssuesCount,
                    onClick = { onSelectTab(SanitizationTab.TRANSACTIONS) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // 4. Archived Platter
                DomainSummaryRow(
                    title = "Archived Transactions (Trash)",
                    desc = "${audit.system.archivedTransactionsCount} transaction(s) waiting in recycle bin",
                    icon = Icons.Rounded.DeleteSweep,
                    issueCount = audit.system.archivedTransactionsCount,
                    onClick = { onSelectTab(SanitizationTab.ARCHIVED) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // 5. System / Junk Platter
                DomainSummaryRow(
                    title = "Demo Data & SMS Junk",
                    desc = "${audit.system.totalSampleCount} sample items • ${audit.system.unrecognizedSmsCount} unparsed SMS",
                    icon = Icons.Rounded.CleaningServices,
                    issueCount = audit.system.totalSampleCount + audit.system.unrecognizedSmsCount,
                    onClick = { onSelectTab(SanitizationTab.SYSTEM) }
                )
            }
        }
    }
}

@Composable
fun DomainSummaryRow(
    title: String,
    desc: String,
    icon: ImageVector,
    issueCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (issueCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (issueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. OWNERSHIP TAB
// ---------------------------------------------------------------------------

fun androidx.compose.foundation.lazy.LazyListScope.ownershipTabContent(
    issue: com.reddy.vittify.data.model.OwnershipIssue,
    myDeviceId: String,
    partnerName: String?,
    isCoupleTracking: Boolean,
    expandedSectionId: String?,
    onToggleSection: (String) -> Unit,
    onClaimAll: () -> Unit,
    onClaimAccount: (String) -> Unit,
    onClaimCard: (Long) -> Unit,
    onAlignOwners: () -> Unit,
    onReassignForeign: (String, String) -> Unit
) {
    if (issue.totalIssuesCount == 0) {
        item {
            HealthyStateCard(
                title = "Ownership Verified",
                desc = "All transactions, accounts, cards, and budgets belong to your active device (${if (myDeviceId.length > 8) myDeviceId.take(8) + "..." else myDeviceId})."
            )
        }
    } else {
        // 1. Unassigned records
        if (issue.totalUnassignedCount > 0) {
            item {
                SanitizationGroupPlatter(
                    title = "Unassigned Records (${issue.totalUnassignedCount})",
                    desc = "Records missing an owner device ID. Claim them so they sync and track under your active device.",
                    icon = Icons.Rounded.PersonOff,
                    primaryActionLabel = stringResource(R.string.claim_all_mine),
                    onPrimaryAction = onClaimAll,
                    isExpanded = expandedSectionId == "unassigned" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("unassigned") }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // Unassigned Accounts
                        if (issue.unassignedAccounts.isNotEmpty()) {
                            Text(
                                text = "Accounts (${issue.unassignedAccounts.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            issue.unassignedAccounts.forEach { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(VittifyShapes.card)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (acc.isWallet || acc.accountLast4.equals("wallet", ignoreCase = true)) "Cash (Wallet)" else "${acc.bankName} (••• ${acc.accountLast4})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Balance: ${CurrencyFormatter.formatCurrency(acc.balance, acc.currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { onClaimAccount(acc.id) },
                                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                                    ) {
                                        Text("Claim", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Unassigned Cards
                        if (issue.unassignedCards.isNotEmpty()) {
                            Text(
                                text = "Cards (${issue.unassignedCards.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            issue.unassignedCards.forEach { card ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(VittifyShapes.card)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${card.bankName} (•••• ${card.cardLast4})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${card.cardType.name} Card",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { onClaimCard(card.id) },
                                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                                    ) {
                                        Text("Claim", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Unassigned Transactions
                        if (issue.unassignedTransactions.isNotEmpty()) {
                            Text(
                                text = "Transactions (${issue.unassignedTransactions.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            issue.unassignedTransactions.take(5).forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(VittifyShapes.card)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.merchantName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = tx.dateTime.toLocalDate().toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(tx.amount, tx.currency),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            if (issue.unassignedTransactions.size > 5) {
                                Text(
                                    text = "+ ${issue.unassignedTransactions.size - 5} more transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = Spacing.xs)
                                )
                            }
                        }

                        // Unassigned Subscriptions
                        if (issue.unassignedSubscriptions.isNotEmpty()) {
                            Text(
                                text = "Subscriptions (${issue.unassignedSubscriptions.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            issue.unassignedSubscriptions.forEach { sub ->
                                Text("• ${sub.merchantName} (${CurrencyFormatter.formatCurrency(sub.amount, sub.currency)})", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Unassigned Budgets
                        if (issue.unassignedBudgets.isNotEmpty()) {
                            Text(
                                text = "Budgets (${issue.unassignedBudgets.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            issue.unassignedBudgets.forEach { b ->
                                Text("• ${b.name} (${CurrencyFormatter.formatCurrency(b.amount, b.currency)})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // 2. Foreign records
        if (issue.totalForeignCount > 0) {
            val foreignOwnerId = issue.foreignTransactions.firstOrNull()?.ownerId
                ?: issue.foreignAccounts.firstOrNull()?.ownerId ?: ""
            item {
                SanitizationGroupPlatter(
                    title = "Foreign Device Records (${issue.totalForeignCount})",
                    desc = "Records tagged with unknown owner ID (${if (foreignOwnerId.length > 12) foreignOwnerId.take(12) + "..." else foreignOwnerId}) not recognized on this device.",
                    icon = Icons.Rounded.DevicesOther,
                    primaryActionLabel = stringResource(R.string.reassign_to_current),
                    onPrimaryAction = {
                        if (foreignOwnerId.isNotBlank()) onReassignForeign(foreignOwnerId, myDeviceId)
                    },
                    isExpanded = expandedSectionId == "foreign" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("foreign") }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (issue.foreignAccounts.isNotEmpty()) {
                            Text(
                                text = "Accounts (${issue.foreignAccounts.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            issue.foreignAccounts.forEach { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(VittifyShapes.card)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${acc.bankName} (••• ${acc.accountLast4})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Owner: ${acc.ownerId.take(12)}... • Bal: ${CurrencyFormatter.formatCurrency(acc.balance, acc.currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (issue.foreignTransactions.isNotEmpty()) {
                            Text(
                                text = "Transactions (${issue.foreignTransactions.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            issue.foreignTransactions.take(5).forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(VittifyShapes.card)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.merchantName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Owner: ${tx.ownerId.take(12)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(CurrencyFormatter.formatCurrency(tx.amount, tx.currency), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            if (issue.foreignTransactions.size > 5) {
                                Text(
                                    text = "+ ${issue.foreignTransactions.size - 5} more transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Mismatched Transaction-Account owners
        if (issue.ownerMismatchedTransactions.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Owner Mismatches (${issue.ownerMismatchedTransactions.size})",
                    desc = "Transactions whose owner ID differs from the bank account they are linked to.",
                    icon = Icons.Rounded.SyncProblem,
                    primaryActionLabel = stringResource(R.string.align_owners_action),
                    onPrimaryAction = onAlignOwners,
                    isExpanded = expandedSectionId == "mismatched" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("mismatched") }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        issue.ownerMismatchedTransactions.take(5).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.card)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.merchantName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Tx Owner: ${tx.ownerId.take(8)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(CurrencyFormatter.formatCurrency(tx.amount, tx.currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. ACCOUNTS TAB (Consolidating Account Diagnostics)
// ---------------------------------------------------------------------------

fun androidx.compose.foundation.lazy.LazyListScope.accountsTabContent(
    issue: com.reddy.vittify.data.model.AccountHealthIssue,
    expandedSectionId: String?,
    onToggleSection: (String) -> Unit,
    onUnhideAll: () -> Unit,
    onUnhideSingle: (String, String) -> Unit,
    onFixAllBalances: () -> Unit,
    onFixBalance: (String, BigDecimal) -> Unit,
    onAuditAccount: (String, String) -> Unit,
    onEditAccount: (AccountBalanceEntity) -> Unit,
    onDeleteAccount: (AccountBalanceEntity) -> Unit,
    onDeleteCard: (Long) -> Unit
) {
    if (issue.totalIssuesCount == 0) {
        item {
            HealthyStateCard(
                title = "All Accounts Healthy",
                desc = "No duplicate, hidden, or malformed accounts found. All running balances match verified ledgers."
            )
        }
    } else {
        // 1. Balance Discrepancies
        if (issue.discrepancies.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Balance Discrepancies (${issue.discrepancies.size})",
                    desc = "Accounts whose recorded balance differs from the verified running ledger sum.",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    primaryActionLabel = stringResource(R.string.fix_all_balances),
                    onPrimaryAction = onFixAllBalances,
                    isExpanded = expandedSectionId == "discrepancies" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("discrepancies") }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        issue.discrepancies.forEach { disc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.card)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${disc.account.bankName} (••• ${disc.account.accountLast4})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Recorded: ${CurrencyFormatter.formatCurrency(disc.currentBalance, disc.account.currency)} → Verified: ${CurrencyFormatter.formatCurrency(disc.expectedBalance, disc.account.currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { onAuditAccount(disc.account.bankName, disc.account.accountLast4) }) {
                                        Text("Audit")
                                    }
                                    FilledTonalButton(onClick = { onFixBalance(disc.account.id, disc.expectedBalance) }) {
                                        Text(stringResource(R.string.fix_balance))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Duplicate Account Groups
        if (issue.duplicateAccountGroups.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Duplicate Accounts (${issue.duplicateAccountGroups.size} group(s))",
                    desc = "Multiple accounts detected sharing the same last-4 digits.",
                    icon = Icons.Rounded.ContentCopy,
                    primaryActionLabel = null,
                    onPrimaryAction = null,
                    isExpanded = expandedSectionId == "dup_accounts" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("dup_accounts") }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        issue.duplicateAccountGroups.forEach { (last4, accounts) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.card)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Text("Ending in ••• $last4", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                accounts.forEach { acc ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${acc.bankName} • ${CurrencyFormatter.formatCurrency(acc.balance, acc.currency)}", style = MaterialTheme.typography.bodySmall)
                                        IconButton(onClick = { onDeleteAccount(acc) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Bad / Malformed Accounts
        if (issue.badAccounts.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Bad / Malformed Accounts (${issue.badAccounts.size})",
                    desc = "Accounts with invalid account numbers or missing bank names.",
                    icon = Icons.Rounded.ReportProblem,
                    primaryActionLabel = null,
                    onPrimaryAction = null,
                    isExpanded = expandedSectionId == "bad_accounts" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("bad_accounts") }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        issue.badAccounts.forEach { acc ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc.bankName.ifBlank { "Missing Bank Name" }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Number: '${acc.accountLast4}'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                Row {
                                    IconButton(onClick = { onEditAccount(acc) }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { onDeleteAccount(acc) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Hidden Accounts
        if (issue.hiddenAccounts.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Hidden Accounts (${issue.hiddenAccounts.size})",
                    desc = "Accounts currently hidden from your home screen and account lists.",
                    icon = Icons.Rounded.VisibilityOff,
                    primaryActionLabel = "Unhide All",
                    onPrimaryAction = onUnhideAll,
                    isExpanded = expandedSectionId == "hidden_accounts" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("hidden_accounts") }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        issue.hiddenAccounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.card)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${acc.bankName} (••• ${acc.accountLast4})", style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { onUnhideSingle(acc.bankName, acc.accountLast4) }) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = "Unhide")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Orphaned Cards
        if (issue.orphanedCards.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Orphaned Cards (${issue.orphanedCards.size})",
                    desc = "Debit cards linked to non-existent bank accounts.",
                    icon = Icons.Rounded.CreditCard,
                    primaryActionLabel = null,
                    onPrimaryAction = null,
                    isExpanded = expandedSectionId == "orphaned_cards" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("orphaned_cards") }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        issue.orphanedCards.forEach { card ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.card)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${card.bankName} (•••• ${card.cardLast4})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Missing linked account: ••• ${card.accountLast4 ?: "None"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(onClick = { onDeleteCard(card.id) }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete Card",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Ghost Accounts
        if (issue.ghostAccounts.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Inactive / Ghost Accounts (${issue.ghostAccounts.size})",
                    desc = "Accounts with zero balance and no transaction history.",
                    icon = Icons.Rounded.AccountBalance,
                    primaryActionLabel = null,
                    onPrimaryAction = null,
                    isExpanded = expandedSectionId == "ghost_accounts" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("ghost_accounts") }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        issue.ghostAccounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.card)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${acc.bankName} (••• ${acc.accountLast4})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Zero balance • No transactions recorded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteAccount(acc) }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete Account",
                                        tint = MaterialTheme.colorScheme.error
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

// ---------------------------------------------------------------------------
// 6. TRANSACTIONS TAB
// ---------------------------------------------------------------------------

fun androidx.compose.foundation.lazy.LazyListScope.transactionsTabContent(
    issue: com.reddy.vittify.data.model.TransactionAnomalyIssue,
    expandedSectionId: String?,
    onToggleSection: (String) -> Unit,
    onDeduplicateAll: () -> Unit,
    onDeleteSingleTransaction: (Long) -> Unit,
    onMarkDuplicateDismissed: (Long) -> Unit,
    onMarkDuplicateGroupDismissed: (DuplicateTransactionGroup) -> Unit,
    onDeleteCorrupted: () -> Unit,
    onUnlinkAllOrphaned: () -> Unit,
    onUnlinkSingleTransaction: (Long) -> Unit,
    onDeleteOrphanedTransactions: () -> Unit,
    onAutoResolveUnknownMerchants: () -> Unit,
    onUpdateMerchantName: (Long, String) -> Unit
) {
    if (issue.totalIssuesCount == 0) {
        item {
            HealthyStateCard(
                title = "Transactions Clean & Organized",
                desc = "No duplicate transactions, zero-amount entries, or orphaned account references found."
            )
        }
    } else {
        // 1. Orphaned Transactions (Linked to deleted accounts)
        if (issue.orphanedTransactions.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Orphaned Transactions (${issue.orphanedTransactions.size})",
                    desc = "Transactions pointing to deleted bank accounts. Unlinking them restores ledger health while keeping your history.",
                    icon = Icons.Rounded.LinkOff,
                    primaryActionLabel = stringResource(R.string.unlink_all_action),
                    onPrimaryAction = onUnlinkAllOrphaned,
                    isExpanded = expandedSectionId == "orphaned_txns" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("orphaned_txns") }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = VittifyShapes.card,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "These transactions belonged to accounts that were deleted. Unlinking removes the broken reference while preserving your spending timeline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        issue.orphanedTransactions.take(10).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.card)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.merchantName.ifBlank { "Unknown" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = buildString {
                                            append(CurrencyFormatter.formatCurrency(tx.amount, tx.currency))
                                            if (!tx.fromAccount.isNullOrBlank()) {
                                                append(" • A/C ••• ${tx.fromAccount}")
                                            }
                                            append(" • ${tx.dateTime.toLocalDate()}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onUnlinkSingleTransaction(tx.id) }) {
                                        Text(stringResource(R.string.unlink_action))
                                    }
                                    IconButton(
                                        onClick = { onDeleteSingleTransaction(tx.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (issue.orphanedTransactions.size > 10) {
                            Text(
                                text = "+ ${issue.orphanedTransactions.size - 10} more orphaned transactions will be unlinked with \"Unlink All\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)
                            )
                        }

                        TextButton(
                            onClick = onDeleteOrphanedTransactions,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete All Orphaned", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 2. Unknown / Blank Merchant Names
        if (issue.blankMerchantTransactions.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Unknown / Blank Merchants (${issue.blankMerchantTransactions.size})",
                    desc = "Transactions recorded with 'Unknown' or missing merchant names.",
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    primaryActionLabel = stringResource(R.string.auto_resolve_merchants_action),
                    onPrimaryAction = onAutoResolveUnknownMerchants,
                    isExpanded = expandedSectionId == "blank_merchants" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("blank_merchants") }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        issue.blankMerchantTransactions.take(10).forEach { tx ->
                            val suggestion = when {
                                !tx.subcategory.isNullOrBlank() -> tx.subcategory
                                !tx.category.isBlank() && !tx.category.equals("Miscellaneous", ignoreCase = true) -> "${tx.category} Expense"
                                else -> "General Expense"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.card)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Unknown → Suggested: $suggestion",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatCurrency(tx.amount, tx.currency)} • ${tx.category}${tx.subcategory?.let { " / $it" } ?: ""} • ${tx.dateTime.toLocalDate()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { onUpdateMerchantName(tx.id, suggestion) },
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 4.dp)
                                ) {
                                    Text("Apply", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Suspected Duplicate Transactions
        if (issue.duplicateGroups.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Duplicate Transactions (${issue.totalDuplicateTxnCount} extra)",
                    desc = "Transactions with same amount, merchant, and timestamp within 5 minutes.",
                    icon = Icons.Rounded.CopyAll,
                    primaryActionLabel = stringResource(R.string.deduplicate_all_action),
                    onPrimaryAction = onDeduplicateAll,
                    isExpanded = expandedSectionId == "dup_txns" || expandedSectionId == null,
                    onToggleExpand = { onToggleSection("dup_txns") }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        issue.duplicateGroups.take(10).forEach { group ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = VittifyShapes.card,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = group.merchantName.ifBlank { "Unknown Merchant" },
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${group.transactions.size} identical entries detected",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        val currency = group.transactions.firstOrNull()?.currency ?: "INR"
                                        Text(
                                            text = CurrencyFormatter.formatCurrency(group.amount, currency),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    )

                                    group.transactions.forEachIndexed { index, tx ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.dateTime.toLocalDate().toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${tx.dateTime.toLocalTime().toString().take(5)} • ${when (tx.transactionType) {
                                                        TransactionType.INCOME -> "Income"
                                                        TransactionType.EXPENSE -> "Expense"
                                                        TransactionType.TRANSFER -> "Transfer"
                                                        else -> tx.transactionType.name
                                                    }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { onMarkDuplicateDismissed(tx.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Mark as not duplicate",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onDeleteSingleTransaction(tx.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Delete,
                                                        contentDescription = "Remove duplicate",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (index < group.transactions.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { onMarkDuplicateGroupDismissed(group) },
                                            contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text(
                                                text = "Keep All (Not Duplicate)",
                                                style = MaterialTheme.typography.labelMedium
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

        // 4. Corrupted Transactions (<= 0)
        if (issue.corruptedTransactions.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Corrupted Zero-Amount (${issue.corruptedTransactions.size})",
                    desc = "Transactions recorded with zero or negative amounts.",
                    icon = Icons.Rounded.MoneyOff,
                    primaryActionLabel = stringResource(R.string.purge_corrupted_action),
                    onPrimaryAction = onDeleteCorrupted,
                    isExpanded = expandedSectionId == "corrupted",
                    onToggleExpand = { onToggleSection("corrupted") }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        issue.corruptedTransactions.take(5).forEach { tx ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tx.merchantName.ifBlank { "Unknown" }, style = MaterialTheme.typography.bodySmall)
                                IconButton(onClick = { onDeleteSingleTransaction(tx.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Extreme Date Transactions
        if (issue.extremeDateTransactions.isNotEmpty()) {
            item {
                SanitizationGroupPlatter(
                    title = "Extreme Dates (${issue.extremeDateTransactions.size})",
                    desc = "Transactions dated in the far future or before year 2000.",
                    icon = Icons.Rounded.EventBusy,
                    primaryActionLabel = null,
                    onPrimaryAction = null,
                    isExpanded = expandedSectionId == "extreme_dates",
                    onToggleExpand = { onToggleSection("extreme_dates") }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        issue.extremeDateTransactions.take(5).forEach { tx ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(tx.merchantName.ifBlank { "Unknown" }, style = MaterialTheme.typography.titleSmall)
                                    Text("Date: ${tx.dateTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { onDeleteSingleTransaction(tx.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 7. ARCHIVED TRANSACTIONS TAB (Moved directly to this page!)
// ---------------------------------------------------------------------------

fun androidx.compose.foundation.lazy.LazyListScope.archivedTabContent(
    uiState: DataSanitizationUiState,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRestoreSelected: () -> Unit,
    onRestoreSingle: (TransactionEntity) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteSingle: (TransactionEntity) -> Unit,
    onEmptyArchive: () -> Unit,
    onConfigureAutoDelete: () -> Unit
) {
    // 1. Auto-Delete Policy Banner
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.platter,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Auto-Delete Retention",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (uiState.autoDeleteArchivedEnabled) "Deletes trash after ${uiState.autoDeleteArchivedDays} days"
                            else "Auto-delete disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = onConfigureAutoDelete) {
                    Text("Policy")
                }
            }
        }
    }

    // 2. Search & Selection Toolbar
    if (uiState.allArchivedTransactions.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = uiState.archivedSearchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search trash...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    shape = VittifyShapes.pill,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                IconButton(onClick = onToggleSelectionMode) {
                    Icon(
                        imageVector = if (uiState.isArchivedSelectionMode) Icons.Rounded.Close else Icons.Rounded.Checklist,
                        contentDescription = "Selection Mode"
                    )
                }

                IconButton(onClick = onEmptyArchive) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = "Empty Archive",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Selection Action Bar
        if (uiState.isArchivedSelectionMode) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${uiState.archivedSelectedIds.size} selected",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        TextButton(onClick = onSelectAll) { Text("Select All") }
                        IconButton(onClick = onRestoreSelected, enabled = uiState.archivedSelectedIds.isNotEmpty()) {
                            Icon(Icons.Rounded.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDeleteSelected, enabled = uiState.archivedSelectedIds.isNotEmpty()) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Transaction List
        items(uiState.archivedTransactions, key = { "archived_${it.id}" }) { tx ->
            val isSelected = uiState.archivedSelectedIds.contains(tx.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (uiState.isArchivedSelectionMode) onToggleSelect(tx.id)
                    },
                shape = VittifyShapes.platter,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (uiState.isArchivedSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect(tx.id) }
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.merchantName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${tx.category} • ${tx.dateTime.toLocalDate()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = CurrencyFormatter.formatCurrency(tx.amount, tx.currency),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = Spacing.sm)
                    )

                    if (!uiState.isArchivedSelectionMode) {
                        Row {
                            IconButton(onClick = { onRestoreSingle(tx) }) {
                                Icon(Icons.Rounded.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteSingle(tx) }) {
                                Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    } else {
        item {
            HealthyStateCard(
                title = "Recycle Bin is Empty",
                desc = "There are no deleted or archived transactions taking up space."
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 8. SYSTEM / JUNK TAB
// ---------------------------------------------------------------------------

fun androidx.compose.foundation.lazy.LazyListScope.systemTabContent(
    issue: com.reddy.vittify.data.model.SystemJunkIssue,
    onPurgeSample: () -> Unit,
    onPurgeSms: () -> Unit,
    onPurgeTrash: () -> Unit
) {
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Sample Data Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(Icons.Rounded.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Demo / Sample Data", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("${issue.totalSampleCount} test item(s) in database", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (issue.totalSampleCount > 0) {
                        Button(
                            onClick = onPurgeSample,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = VittifyShapes.button
                        ) {
                            Text("Remove")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // 2. Unrecognized SMS Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(Icons.Rounded.SmsFailed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Unrecognized SMS Backlog", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("${issue.unrecognizedSmsCount} unparseable message(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (issue.unrecognizedSmsCount > 0) {
                        FilledTonalButton(onClick = onPurgeSms, shape = VittifyShapes.button) {
                            Text("Purge")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 9. REUSABLE COMPONENTS & DIALOGS
// ---------------------------------------------------------------------------

@Composable
fun SanitizationGroupPlatter(
    title: String,
    desc: String,
    icon: ImageVector,
    primaryActionLabel: String?,
    onPrimaryAction: (() -> Unit)?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = VittifyShapes.platter,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(Dimensions.Padding.card),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }

                    Column {
                        Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    content()
                    if (primaryActionLabel != null && onPrimaryAction != null) {
                        Button(
                            onClick = onPrimaryAction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md)
                                .height(48.dp),
                            shape = VittifyShapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(primaryActionLabel, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthyStateCard(title: String, desc: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = VittifyShapes.platter,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountQuickSheet(
    account: AccountBalanceEntity,
    onDismiss: () -> Unit,
    onSave: (AccountBalanceEntity) -> Unit
) {
    var bankName by remember { mutableStateOf(account.bankName) }
    var last4 by remember { mutableStateOf(account.accountLast4) }

    VittifyModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text("Edit Account Format", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank / Source Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = VittifyShapes.input
            )
            OutlinedTextField(
                value = last4,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) last4 = it },
                label = { Text("Account Last 4 Digits") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = VittifyShapes.input
            )
            Button(
                onClick = { onSave(account.copy(bankName = bankName.trim(), accountLast4 = last4.trim())) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = VittifyShapes.button
            ) {
                Text("Save Changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDeleteConfigSheet(
    enabled: Boolean,
    currentDays: Int,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int) -> Unit
) {
    var isEnabled by remember { mutableStateOf(enabled) }
    var days by remember { mutableStateOf(currentDays) }

    VittifyModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text("Auto-Delete Retention Policy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Automatically remove transactions that have been in the trash longer than the selected duration.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Auto-Delete", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
            }

            if (isEnabled) {
                Text("Retention Duration", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    listOf(30, 60, 90).forEach { d ->
                        FilterChip(
                            selected = days == d,
                            onClick = { days = d },
                            label = { Text("$d Days") },
                            shape = VittifyShapes.pill
                        )
                    }
                }
            }

            Button(
                onClick = { onSave(isEnabled, days) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = VittifyShapes.button
            ) {
                Text("Save Policy")
            }
        }
    }
}
