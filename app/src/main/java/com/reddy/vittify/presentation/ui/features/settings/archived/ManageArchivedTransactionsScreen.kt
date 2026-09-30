package com.reddy.vittify.presentation.ui.features.settings.archived

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import com.reddy.vittify.presentation.ui.components.VittifyCheckbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.presentation.effects.BlurredAnimatedVisibility
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.EmptyArchiveConfirmationDialog
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.LoadingCircle
import com.reddy.vittify.presentation.ui.components.PermanentDeleteSelectedArchivedDialog
import com.reddy.vittify.presentation.ui.components.PermanentDeleteSingleArchivedDialog
import com.reddy.vittify.presentation.ui.components.SearchBarBox
import com.reddy.vittify.presentation.ui.components.VittifyCard
import com.reddy.vittify.presentation.ui.components.animatedIconClick
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.icons.Bag
import com.reddy.vittify.presentation.ui.icons.BagTimer
import com.reddy.vittify.presentation.ui.icons.CloseCircle
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.Search
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.credit_dark
import com.reddy.vittify.presentation.ui.theme.credit_light
import com.reddy.vittify.presentation.ui.theme.expense_dark
import com.reddy.vittify.presentation.ui.theme.expense_light
import com.reddy.vittify.presentation.ui.theme.income_dark
import com.reddy.vittify.presentation.ui.theme.income_light
import com.reddy.vittify.presentation.ui.theme.investment_dark
import com.reddy.vittify.presentation.ui.theme.investment_light
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import com.reddy.vittify.presentation.ui.theme.transfer_dark
import com.reddy.vittify.presentation.ui.theme.transfer_light
import com.reddy.vittify.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ManageArchivedTransactionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageArchivedTransactionsViewModel = hiltViewModel(),
    blurEffects: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastRestored by viewModel.lastRestoredTransactions.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = rememberAppHapticFeedback()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    var showAutoDeleteSheet by remember { mutableStateOf(false) }
    val autoDeleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var transactionToDeleteSingle by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showEmptyArchiveDialog by remember { mutableStateOf(false) }

    var searchTextFieldValue by remember {
        mutableStateOf(TextFieldValue(uiState.searchQuery))
    }

    // Handle back button when in selection mode
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.toggleSelectionMode()
    }

    // Handle Restore Undo Snackbar
    LaunchedEffect(lastRestored) {
        lastRestored?.let { restoredList ->
            viewModel.clearLastRestored()
            scope.launch {
                val message = if (restoredList.size == 1) {
                    snackbarHostState.showSnackbar(
                        message = "Transaction restored",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    snackbarHostState.showSnackbar(
                        message = "${restoredList.size} transactions restored",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                }
                if (message == SnackbarResult.ActionPerformed) {
                    viewModel.undoRestore(restoredList)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = if (uiState.isSelectionMode) {
                    stringResource(R.string.items_selected_format, uiState.selectedIds.size)
                } else {
                    stringResource(R.string.archived_transactions)
                },
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = {
                    if (uiState.isSelectionMode) {
                        NavigationContent(onNavigateBack = { viewModel.toggleSelectionMode() })
                    } else {
                        NavigationContent(onNavigateBack = onNavigateBack)
                    }
                },
                actionContent = {
                    if (uiState.isSelectionMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            val allSelected = uiState.selectedIds.size == uiState.filteredTransactions.size &&
                                    uiState.filteredTransactions.isNotEmpty()
                            IconButton(
                                onClick = {
                                    haptic.click()
                                    if (allSelected) viewModel.clearSelection() else viewModel.selectAll()
                                }
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                                    contentDescription = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Restore selected
                            IconButton(
                                onClick = {
                                    haptic.click()
                                    viewModel.restoreSelectedTransactions()
                                },
                                enabled = uiState.selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Restore,
                                    contentDescription = stringResource(R.string.restore_selected),
                                    tint = if (uiState.selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Delete selected permanently
                            IconButton(
                                onClick = {
                                    haptic.click()
                                    showDeleteSelectedDialog = true
                                },
                                enabled = uiState.selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Iconax.Bag,
                                    contentDescription = stringResource(R.string.delete_selected_permanently),
                                    tint = if (uiState.selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            if (uiState.filteredTransactions.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptic.click()
                                        viewModel.toggleSelectionMode()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Checklist,
                                        contentDescription = stringResource(R.string.select_all),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    haptic.click()
                                    showAutoDeleteSheet = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.archived_auto_delete_settings_title),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (uiState.transactions.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptic.click()
                                        showEmptyArchiveDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Iconax.Bag,
                                        contentDescription = stringResource(R.string.empty_archive),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content
                )
        ) {
            // Auto-delete status banner
            VittifyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
                shape = MaterialTheme.shapes.large,
                contentPadding = Spacing.md
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Iconax.BagTimer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (uiState.autoDeleteEnabled) {
                                stringResource(R.string.auto_delete_banner_title)
                            } else {
                                stringResource(R.string.archived_auto_delete_settings_title)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.autoDeleteEnabled) {
                                stringResource(R.string.auto_delete_banner_desc, uiState.autoDeleteDays)
                            } else {
                                stringResource(R.string.auto_delete_banner_off_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            haptic.click()
                            showAutoDeleteSheet = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.configure),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Search Bar & Summary
            if (uiState.transactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    SearchBarBox(
                        modifier = Modifier.weight(1f),
                        searchQuery = searchTextFieldValue,
                        onSearchQueryChange = {
                            searchTextFieldValue = it
                            viewModel.updateSearchQuery(it.text)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Iconax.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            if (searchTextFieldValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchTextFieldValue = TextFieldValue("")
                                        viewModel.updateSearchQuery("")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Iconax.CloseCircle,
                                        contentDescription = stringResource(R.string.clear_search),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.search_archived_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    )
                }

                // Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.archived_summary_format,
                            uiState.filteredTransactions.size,
                            CurrencyFormatter.formatCurrency(uiState.totalAmount, uiState.baseCurrency)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content List or Empty State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingCircle()
                }
            } else if (uiState.filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.padding(Spacing.xl)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Iconax.Bag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) {
                                stringResource(R.string.no_matching_categories_found)
                            } else {
                                stringResource(R.string.no_archived_transactions)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) {
                                stringResource(R.string.global_search_no_results_sub)
                            } else {
                                stringResource(R.string.no_archived_transactions_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp),
                    contentPadding = PaddingValues(bottom = Spacing.xxl)
                ) {
                    itemsIndexed(
                        items = uiState.filteredTransactions,
                        key = { _, item -> item.id }
                    ) { index, transaction ->
                        val position = ListItemPosition.from(index, uiState.filteredTransactions.size)

                        ArchivedTransactionRow(
                            transaction = transaction,
                            position = position,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = uiState.selectedIds.contains(transaction.id),
                            autoDeleteEnabled = uiState.autoDeleteEnabled,
                            autoDeleteDays = uiState.autoDeleteDays,
                            baseCurrency = uiState.baseCurrency,
                            onToggleSelect = {
                                haptic.click()
                                viewModel.toggleTransactionSelection(transaction.id)
                            },
                            onLongClick = {
                                haptic.longClick()
                                if (!uiState.isSelectionMode) {
                                    viewModel.toggleSelectionMode()
                                }
                                viewModel.toggleTransactionSelection(transaction.id)
                            },
                            onRestore = {
                                haptic.click()
                                viewModel.restoreTransaction(transaction)
                            },
                            onDeletePermanently = {
                                haptic.click()
                                transactionToDeleteSingle = transaction
                            }
                        )
                    }
                }
            }
        }
    }

    // Auto-Delete Settings Sheet
    if (showAutoDeleteSheet) {
        AutoDeleteSettingsBottomSheet(
            sheetState = autoDeleteSheetState,
            initialEnabled = uiState.autoDeleteEnabled,
            initialDays = uiState.autoDeleteDays,
            onDismiss = { showAutoDeleteSheet = false },
            onSave = { enabled, days ->
                viewModel.updateAutoDeleteSettings(enabled, days)
                showAutoDeleteSheet = false
            }
        )
    }

    // Delete Single Confirmation Dialog
    transactionToDeleteSingle?.let { tx ->
        PermanentDeleteSingleArchivedDialog(
            onConfirm = {
                viewModel.deleteTransactionPermanently(tx)
                transactionToDeleteSingle = null
            },
            onDismiss = { transactionToDeleteSingle = null },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // Delete Selected Confirmation Dialog
    if (showDeleteSelectedDialog) {
        PermanentDeleteSelectedArchivedDialog(
            selectedCount = uiState.selectedIds.size,
            onConfirm = {
                viewModel.deleteSelectedPermanently()
                showDeleteSelectedDialog = false
            },
            onDismiss = { showDeleteSelectedDialog = false },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // Empty Archive Confirmation Dialog
    if (showEmptyArchiveDialog) {
        EmptyArchiveConfirmationDialog(
            totalCount = uiState.transactions.size,
            onConfirm = {
                viewModel.deleteAllPermanently()
                showEmptyArchiveDialog = false
            },
            onDismiss = { showEmptyArchiveDialog = false },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ArchivedTransactionRow(
    transaction: TransactionEntity,
    position: ListItemPosition,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    autoDeleteEnabled: Boolean,
    autoDeleteDays: Int,
    baseCurrency: String,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> if (!isDark) income_light else income_dark
        TransactionType.EXPENSE -> if (!isDark) expense_light else expense_dark
        TransactionType.CREDIT -> if (!isDark) credit_light else credit_dark
        TransactionType.TRANSFER -> if (!isDark) transfer_light else transfer_dark
        TransactionType.INVESTMENT -> if (!isDark) investment_light else investment_dark
        TransactionType.BALANCE_UPDATE -> if (!isDark) transfer_light else transfer_dark
    }

    // Calculate days until permanent deletion
    val daysSinceUpdated = remember(transaction.updatedAt) {
        ChronoUnit.DAYS.between(transaction.updatedAt, LocalDateTime.now()).toInt()
    }
    val daysRemaining = remember(daysSinceUpdated, autoDeleteDays) {
        (autoDeleteDays - daysSinceUpdated).coerceAtLeast(0)
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val dateText = remember(transaction.dateTime) {
        transaction.dateTime.format(dateFormatter)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(position.toShape())
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onRestore()
                },
                onLongClick = onLongClick
            ),
        shape = position.toShape(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Checkbox in selection mode
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                VittifyCheckbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(38.dp)
                )
            }

            // Transaction Type Icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = amountColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.transactionType) {
                        TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                        TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                        TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                        TransactionType.CREDIT -> Icons.AutoMirrored.Filled.TrendingDown
                        TransactionType.INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
                        TransactionType.BALANCE_UPDATE -> Icons.Rounded.SwapHoriz
                    },
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Details Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.merchantName.ifEmpty { transaction.category },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Deletion countdown pill
                if (autoDeleteEnabled) {
                    Surface(
                        shape = VittifyShapes.scaled(4.dp),
                        color = if (daysRemaining <= 2) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (daysRemaining == 0) {
                                stringResource(R.string.deletes_today)
                            } else {
                                stringResource(R.string.deletes_in_days, daysRemaining)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (daysRemaining <= 2) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Amount Column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = CurrencyFormatter.formatCurrency(transaction.amount, transaction.currency),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                // Quick Action Buttons (Restore / More)
                if (!isSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Quick Restore icon button
                        IconButton(
                            onClick = onRestore,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = stringResource(R.string.restore_transaction),
                                modifier = Modifier.size(16.dp).animatedIconClick()
                            )
                        }

                        Spacer(modifier = Modifier.width(Spacing.xs))

                        // 3-Dots Menu
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = stringResource(R.string.more_options),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp).animatedIconClick()
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = MaterialTheme.shapes.large
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.restore_transaction)) },
                                    onClick = {
                                        showMenu = false
                                        onRestore()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Restore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_permanently)) },
                                    onClick = {
                                        showMenu = false
                                        onDeletePermanently()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Iconax.Bag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

