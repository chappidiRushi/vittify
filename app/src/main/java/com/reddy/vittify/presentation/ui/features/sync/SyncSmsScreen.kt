package com.reddy.vittify.presentation.ui.features.sync

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.LoadingCircle
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.LocalVittifyTokens
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.expense_dark
import com.reddy.vittify.presentation.ui.theme.expense_light
import com.reddy.vittify.presentation.ui.theme.income_dark
import com.reddy.vittify.presentation.ui.theme.income_light
import com.reddy.vittify.utils.CurrencyFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun SyncSmsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncSmsViewModel = hiltViewModel(),
    blurEffects: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.sync_sms_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                hasBackButton = true,
                navigationContent = {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            if (uiState.isEditingSelection) {
                                viewModel.toggleEditMode(false)
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                hasActionButton = uiState.isEditingSelection,
                actionContent = {
                    if (uiState.isEditingSelection) {
                        TextButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.toggleEditMode(false)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.done),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                blurEffects = blurEffects
            )
        },
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    !uiState.hasPermission -> {
                        PermissionRequestView(
                            blurEffects = blurEffects,
                            onGrantClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                permissionLauncher.launch(Manifest.permission.READ_SMS)
                            }
                        )
                    }

                    uiState.isScanning -> {
                        ScanningProgressView(
                            scannedCount = uiState.scannedSmsCount,
                            totalCount = uiState.totalSmsCount,
                            blurEffects = blurEffects,
                            onCancelClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.cancelScan()
                            }
                        )
                    }

                    uiState.isImporting -> {
                        ImportingProgressView(
                            progress = uiState.importProgress,
                            total = uiState.selectedCount,
                            blurEffects = blurEffects
                        )
                    }

                    uiState.importCompleted -> {
                        ImportCompletedView(
                            importedCount = uiState.importedCount,
                            blurEffects = blurEffects,
                            onDoneClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onNavigateBack()
                            }
                        )
                    }

                    uiState.scanCompleted -> {
                        AnimatedContent(
                            targetState = uiState.isEditingSelection,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "SyncSmsViewMode"
                        ) { isEditing ->
                            if (isEditing) {
                                MessageSelectionListView(
                                    uiState = uiState,
                                    isDark = isDark,
                                    blurEffects = blurEffects,
                                    onToggleItem = { id ->
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.toggleItemSelection(id)
                                    },
                                    onToggleMonth = { itemIds ->
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.toggleMonthSelection(itemIds)
                                    },
                                    onSelectAll = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.selectAll()
                                    },
                                    onUnselectAll = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.unselectAll()
                                    },
                                    onSelectBetween = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.selectBetweenTwoSelected()
                                    },
                                    onDone = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.toggleEditMode(false)
                                    },
                                    onImport = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        viewModel.importSelectedMessages()
                                    }
                                )
                            } else {
                                ScanSummaryView(
                                    uiState = uiState,
                                    blurEffects = blurEffects,
                                    onEditClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.toggleEditMode(true)
                                    },
                                    onCloseClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        onNavigateBack()
                                    },
                                    onImportClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        viewModel.importSelectedMessages()
                                    },
                                    onScanAgainClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.resetToReady()
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        ReadyToScanView(
                            forceResync = uiState.forceResync,
                            blurEffects = blurEffects,
                            onToggleForceResync = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.toggleForceResync()
                            },
                            onScanClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                if (!uiState.hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                                } else {
                                    viewModel.startScan()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    uiState.unmappedBankPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnmappedAccountPrompt() },
            shape = VittifyShapes.hero,
            containerColor = VittifySurface.surfaceContainerHighestColor(),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "New Bank Account Found",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Transactions were discovered for ${prompt.bankName}${if (prompt.accountLast4.isNotBlank()) " (•••• ${prompt.accountLast4})" else ""}, which isn't in your accounts list yet. Would you like to create this account now?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        viewModel.acceptAddUnmappedAccount()
                    },
                    shape = VittifyShapes.button
                ) {
                    Text("Add Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissUnmappedAccountPrompt()
                    }
                ) {
                    Text("Not Now")
                }
            }
        )
    }
}

/**
 * Custom rounded circular checkbox matching Vittify M3 expressive design.
 * Features a bouncy spring scale animation, color transitions, and check/dash icon.
 */
@Composable
private fun VittifyRoundedCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    partiallyChecked: Boolean = false,
    enabled: Boolean = true
) {
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (checked || partiallyChecked) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkboxScale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            checked -> MaterialTheme.colorScheme.primary
            partiallyChecked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "checkboxBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            checked || partiallyChecked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "checkboxBorder"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
                enabled = enabled,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onCheckedChange()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .size(24.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            } else if (partiallyChecked) {
                Box(
                    modifier = Modifier
                        .size(width = 10.dp, height = 2.5.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            VittifyShapes.scaled(1.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun ReadyToScanView(
    forceResync: Boolean,
    onToggleForceResync: () -> Unit,
    onScanClick: () -> Unit,
    blurEffects: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.Padding.content),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.platterContainerColor(),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = VittifySurface.platterBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.ready_to_scan_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.ready_to_scan_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                FilterChip(
                    selected = forceResync,
                    onClick = onToggleForceResync,
                    label = {
                        Text(
                            text = stringResource(R.string.full_resync_chip),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = if (forceResync) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    shape = VittifyShapes.large,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                if (forceResync) {
                    Text(
                        text = stringResource(R.string.full_resync_chip_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Button(
                    onClick = onScanClick,
                    shape = VittifyShapes.button,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = if (forceResync) {
                            stringResource(R.string.start_full_resync)
                        } else {
                            stringResource(R.string.scan_sms_button)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestView(
    onGrantClick: () -> Unit,
    blurEffects: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.Padding.content),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.platterContainerColor(),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = VittifySurface.platterBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.permission_sms_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.grant_sms_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Button(
                    onClick = onGrantClick,
                    shape = VittifyShapes.button,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.grant_permission),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScanningProgressView(
    scannedCount: Int,
    totalCount: Int,
    onCancelClick: () -> Unit,
    blurEffects: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.Padding.content),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.platterContainerColor(),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = VittifySurface.platterBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                LoadingCircle(modifier = Modifier.size(48.dp))

                Text(
                    text = stringResource(R.string.scanning_sms_messages_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (totalCount > 0) {
                    val progress = (scannedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Text(
                        text = stringResource(R.string.scanning_sms_messages_progress, scannedCount, totalCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    LinearWavyProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Text(
                        text = stringResource(R.string.starting_scan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                OutlinedButton(
                    onClick = onCancelClick,
                    shape = VittifyShapes.button,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.cancel_scan))
                }
            }
        }
    }
}

@Composable
private fun ScanSummaryView(
    uiState: SyncSmsUiState,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
    onImportClick: () -> Unit,
    onScanAgainClick: () -> Unit = {},
    blurEffects: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.Padding.content),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.platterContainerColor(),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = VittifySurface.platterBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Large status circle icon with high contrast
                Surface(
                    shape = CircleShape,
                    color = if (uiState.discoveredMessages.isNotEmpty())
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (uiState.discoveredMessages.isNotEmpty())
                                Icons.Rounded.Check
                            else Icons.Default.Inbox,
                            contentDescription = null,
                            tint = if (uiState.discoveredMessages.isNotEmpty())
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.scan_completed_desc),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.discoveredMessages.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_new_messages_found),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(R.string.no_new_messages_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = onScanAgainClick,
                            shape = VittifyShapes.button,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.scan_again))
                        }

                        Button(
                            onClick = onCloseClick,
                            shape = VittifyShapes.button,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val countText = if (uiState.selectedCount == uiState.totalDiscoveredCount) {
                            if (uiState.totalDiscoveredCount == 1) {
                                stringResource(R.string.new_message_found_format)
                            } else {
                                stringResource(R.string.new_messages_found_format, uiState.totalDiscoveredCount)
                            }
                        } else {
                            stringResource(
                                R.string.selected_messages_format,
                                uiState.selectedCount,
                                uiState.totalDiscoveredCount
                            )
                        }

                        Text(
                            text = countText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.width(Spacing.xs))

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.edit_selection_cd),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.xs),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Action buttons: Close and Import
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = onCloseClick,
                            shape = VittifyShapes.button,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.close))
                        }

                        Button(
                            onClick = onImportClick,
                            enabled = uiState.selectedCount > 0,
                            shape = VittifyShapes.button,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Text(
                                stringResource(
                                    R.string.import_selected_count,
                                    uiState.selectedCount
                                )
                            )
                        }
                    }

                    TextButton(
                        onClick = onScanAgainClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.scan_again),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MessageSelectionListView(
    uiState: SyncSmsUiState,
    isDark: Boolean,
    onToggleItem: (String) -> Unit,
    onToggleMonth: (List<String>) -> Unit,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit,
    onSelectBetween: () -> Unit,
    onDone: () -> Unit,
    onImport: () -> Unit,
    blurEffects: Boolean = false,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("MMM d • h:mm a") }
    val view = LocalView.current

    val sortedMessages = remember(uiState.discoveredMessages) {
        uiState.discoveredMessages.sortedByDescending { it.timestamp }
    }

    val groupedByMonth = remember(sortedMessages) {
        sortedMessages.groupBy { item ->
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.timestamp), ZoneId.systemDefault())
            dt.format(monthFormatter)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Options at top: Select All, Unselect All, Select Between
        Surface(
            color = if (blurEffects) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.selectedCount == uiState.totalDiscoveredCount && uiState.totalDiscoveredCount > 0,
                    onClick = onSelectAll,
                    label = { Text(stringResource(R.string.select_all)) },
                    shape = VittifyShapes.large
                )

                FilterChip(
                    selected = uiState.selectedCount == 0,
                    onClick = onUnselectAll,
                    label = { Text(stringResource(R.string.deselect_all)) },
                    shape = VittifyShapes.large
                )

                if (uiState.canSelectBetween) {
                    FilterChip(
                        selected = false,
                        onClick = onSelectBetween,
                        label = {
                            Text(
                                stringResource(
                                    R.string.select_between_count,
                                    uiState.selectBetweenCount
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            iconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = VittifyShapes.large
                    )
                }
            }
        }

        // List grouped by month subheading
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Spacing.xs,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            groupedByMonth.forEach { (month, items) ->
                val isMonthAllSelected = items.all { it.isSelected }
                val isMonthPartiallySelected = items.any { it.isSelected } && !isMonthAllSelected

                stickyHeader {
                    Surface(
                        color = if (blurEffects) MaterialTheme.colorScheme.surface.copy(alpha = 0.90f) else MaterialTheme.colorScheme.surface,
                        shape = VittifyShapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(VittifyShapes.medium)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    onToggleMonth(items.map { it.id })
                                }
                                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Spacer(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.tertiary,
                                            VittifyShapes.pill
                                        )
                                )

                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = "${items.count { it.isSelected }}/${items.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Month Checkbox on the right
                            VittifyRoundedCheckbox(
                                checked = isMonthAllSelected,
                                partiallyChecked = isMonthPartiallySelected,
                                onCheckedChange = { onToggleMonth(items.map { it.id }) }
                            )
                        }
                    }
                }

                items(items, key = { it.id }) { item ->
                    MessageSelectionRow(
                        item = item,
                        timeFormatter = timeFormatter,
                        isDark = isDark,
                        blurEffects = blurEffects,
                        onToggle = { onToggleItem(item.id) }
                    )
                }
            }
        }

        // Bottom Sticky Action Bar: Done button and Import button
        Surface(
            color = if (blurEffects) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f) else MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)
                    .padding(WindowInsets.navigationBars.asPaddingValues()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDone,
                    shape = VittifyShapes.button,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.done))
                }

                Button(
                    onClick = onImport,
                    enabled = uiState.selectedCount > 0,
                    shape = VittifyShapes.button,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                ) {
                    Text(
                        stringResource(
                            R.string.import_selected_count,
                            uiState.selectedCount
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageSelectionRow(
    item: SyncMessageItem,
    timeFormatter: DateTimeFormatter,
    isDark: Boolean,
    onToggle: () -> Unit,
    blurEffects: Boolean = false,
    modifier: Modifier = Modifier
) {
    val parsed = item.parsedTransaction
    val formattedTime = remember(item.timestamp) {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.timestamp), ZoneId.systemDefault())
        dt.format(timeFormatter)
    }

    val isIncome = parsed.type == com.reddy.parser.core.TransactionType.INCOME
    val amountColor = if (isIncome) {
        if (isDark) income_dark else income_light
    } else {
        if (isDark) expense_dark else expense_light
    }

    val tokens = LocalVittifyTokens.current
    val itemShape = VittifyShapes.medium

    val cardBg = if (item.isSelected) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = (if (blurEffects) 0.85f else 1f) * tokens.surfaceOpacity)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = (if (blurEffects) 0.70f else 0.90f) * tokens.surfaceOpacity)
    }

    val borderColor = if (item.isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f * tokens.borderOpacity)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f * tokens.borderOpacity)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(itemShape)
            .clickable(onClick = onToggle),
        shape = itemShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(tokens.borderThickness, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon on the left
            Surface(
                shape = CircleShape,
                color = if (isIncome) {
                    if (isDark) income_dark.copy(alpha = 0.16f) else income_light.copy(alpha = 0.16f)
                } else {
                    if (isDark) expense_dark.copy(alpha = 0.16f) else expense_light.copy(alpha = 0.16f)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Details in center
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = parsed.merchant?.takeIf { it.isNotBlank() }
                        ?: parsed.bankName.takeIf { it.isNotBlank() }
                        ?: item.sender,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!parsed.accountLast4.isNullOrBlank()) {
                        Text(
                            text = "• A/C ${parsed.accountLast4}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.smsBody.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Right side: Amount and Rounded Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                val sign = if (isIncome) "+" else "-"
                Text(
                    text = "$sign${CurrencyFormatter.formatCurrency(parsed.amount, parsed.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                VittifyRoundedCheckbox(
                    checked = item.isSelected,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportingProgressView(
    progress: Int,
    total: Int,
    blurEffects: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.Padding.content),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.platterContainerColor(),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = VittifySurface.platterBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                LoadingCircle(modifier = Modifier.size(48.dp))

                Text(
                    text = stringResource(R.string.importing_transactions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (total > 0) {
                    val progressRatio = (progress.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    LinearWavyProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Text(
                        text = stringResource(R.string.import_progress_format, progress, total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportCompletedView(
    importedCount: Int,
    onDoneClick: () -> Unit,
    blurEffects: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.Padding.content),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.platterContainerColor(),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = VittifySurface.platterBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.import_success_format, importedCount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Button(
                    onClick = onDoneClick,
                    shape = VittifyShapes.button,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}
