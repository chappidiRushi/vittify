package com.reddy.vittify.presentation.ui.features.settings.dataprivacy

import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import com.reddy.vittify.presentation.ui.icons.SecuritySafe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.reddy.vittify.presentation.ui.features.settings.SettingsViewModel
import com.reddy.vittify.presentation.ui.components.VittifyCheckbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.data.backup.BackupConfiguration
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.features.settings.applock.AppLockViewModel
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.Padlock
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.green_dark
import com.reddy.vittify.presentation.ui.theme.green_light
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

import androidx.compose.foundation.relocation.BringIntoViewRequester
import com.reddy.vittify.presentation.navigation.SettingsDeepLink
import com.reddy.vittify.presentation.ui.components.settingOptionHighlight
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeApi::class
)
@Composable
fun DataPrivacyScreen(
    onNavigateBack: () -> Unit,
    targetOptionId: String? = null,
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToDataSanitization: (String?) -> Unit = {},
    onNavigateToArchivedTransactions: () -> Unit = {},
    appLockViewModel: AppLockViewModel = hiltViewModel(),
    viewModel: DataPrivacyViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteAllDataDialog by remember { mutableStateOf(false) }

    val bringIntoViewRequesters = remember {
        mapOf(
            "app-lock" to BringIntoViewRequester(),
            "lock-timeout" to BringIntoViewRequester(),
            "data-sanitization" to BringIntoViewRequester(),
            "delete-all-data" to BringIntoViewRequester()
        )
    }
    var highlightedOptionId by remember { mutableStateOf<String?>(null) }

    // Launcher for selecting a backup file to import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.importBackup(it) }
        }
    )

    // Launcher for selecting a PDF statement to import
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.analyzePdfStatement(it) }
        }
    )

    // Launcher for saving the exported backup file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.saveBackupToFile(uri)
            } else {
                viewModel.clearExportedFile()
            }
        }
    )

    // Handle import/export messages
    LaunchedEffect(uiState.importExportMessage) {
        uiState.importExportMessage?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = if (uiState.hasNewAccountsCreated) "View Accounts" else null
            )
            if (result == SnackbarResult.ActionPerformed) {
                onNavigateToAccounts()
            }
            viewModel.clearImportExportMessage()
        }
    }

    // Handle export success (trigger file saver)
    LaunchedEffect(uiState.exportedBackupFile) {
        uiState.exportedBackupFile?.let {
            exportLauncher.launch("vittify_backup_${System.currentTimeMillis()}.zip")
        }
    }

    LaunchedEffect(targetOptionId) {
        targetOptionId?.let { rawId ->
            val id = SettingsDeepLink.normalizeOptionSlug("dataprivacy", rawId)
            when (id) {
                "export-backup" -> {
                    showExportDialog = true
                }
                "import-backup" -> {
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }
                "import-pdf" -> {
                    pdfLauncher.launch(arrayOf("application/pdf"))
                }
                else -> {
                    delay(300)
                    bringIntoViewRequesters[id]?.bringIntoView()
                    highlightedOptionId = id
                    delay(2500)
                    highlightedOptionId = null
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.data_privacy_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            ) }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = Dimensions.Padding.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Security Section
                SectionHeader(
                    title = stringResource(R.string.security_section),
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.md))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PreferenceSwitch(
                        modifier = Modifier.settingOptionHighlight(
                            id = "app-lock",
                            requester = bringIntoViewRequesters["app-lock"],
                            highlightedId = highlightedOptionId,
                            shape = if (!appLockUiState.isLockEnabled) ListItemPosition.Single.toShape() else ListItemPosition.Top.toShape()
                        ),
                        title = stringResource(R.string.app_lock),
                        subtitle =
                        if (appLockUiState.canUseBiometric) {
                            stringResource(R.string.app_lock_biometric_sub)
                        } else {
                            appLockUiState.biometricCapability.getErrorMessage()
                        },
                        checked = appLockUiState.isLockEnabled,
                        onCheckedChange = { enabled ->
                            appLockViewModel.setAppLockEnabled(enabled)
                            if (!enabled && appLockUiState.isLockEnabled) {
                                val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
                                    .filterIsInstance<FragmentActivity>()
                                    .firstOrNull()
                                if (appLockUiState.canUseBiometric && activity != null) {
                                    appLockViewModel.authenticateToDisable(activity)
                                } else {
                                    appLockViewModel.setAppLockEnabled(false)
                                }
                            } else {
                                appLockViewModel.setAppLockEnabled(enabled)
                            }
                        },
                        leadingIcon = {
                            com.reddy.vittify.presentation.ui.components.SettingsShapeIconBadge(
                                icon = Icons.Rounded.Lock,
                                shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.COOKIE_8,
                                containerColor = green_light,
                                contentColor = green_dark,
                                contentDescription = null
                            )
                        },
                        padding = PaddingValues(0.dp),
                        isSingle = !appLockUiState.isLockEnabled,
                        isFirst = true,
                    )

                    // Lock Timeout Setting
                    AnimatedVisibility(visible = appLockUiState.isLockEnabled) {
                        ListItem(
                            modifier = Modifier.settingOptionHighlight(
                                id = "lock-timeout",
                                requester = bringIntoViewRequesters["lock-timeout"],
                                highlightedId = highlightedOptionId,
                                shape = ListItemPosition.Bottom.toShape()
                            ),
                            headline = { Text(stringResource(R.string.lock_timeout)) },
                            supporting = {
                                Text(
                                    when (appLockUiState.timeoutMinutes) {
                                        0 -> stringResource(R.string.lock_timeout_immediate)
                                        1 -> stringResource(R.string.lock_timeout_1min)
                                        else -> stringResource(R.string.lock_timeout_minutes, appLockUiState.timeoutMinutes)
                                    }
                                )
                            },
                            trailing = {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { showTimeoutDialog = true },
                            shape = ListItemPosition.Bottom.toShape(),
                            padding = PaddingValues(0.dp),
                        )
                    }
                }

                // Data Hygiene & Sanitization Section
                SectionHeader(
                    title = "Data Hygiene & Health",
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.md)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ListItem(
                        modifier = Modifier.settingOptionHighlight(
                            id = "data-sanitization",
                            requester = bringIntoViewRequesters["data-sanitization"],
                            highlightedId = highlightedOptionId,
                            shape = ListItemPosition.Top.toShape()
                        ),
                        headline = {
                            Text(
                                text = stringResource(R.string.data_sanitization_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.data_sanitization_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            com.reddy.vittify.presentation.ui.components.SettingsShapeIconBadge(
                                icon = Icons.Rounded.AutoFixHigh,
                                shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.PENTAGON,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToDataSanitization(null) },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    ListItem(
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
                            com.reddy.vittify.presentation.ui.components.SettingsShapeIconBadge(
                                icon = Icons.Rounded.Restore,
                                shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.TILTED_PILL,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.secondary,
                                contentDescription = null
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToDataSanitization("archived") },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }

                // Danger Zone Section
                SectionHeader(
                    title = stringResource(R.string.delete_all_data),
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.md)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ListItem(
                        modifier = Modifier.settingOptionHighlight(
                            id = "delete-all-data",
                            requester = bringIntoViewRequesters["delete-all-data"],
                            highlightedId = highlightedOptionId,
                            shape = ListItemPosition.Single.toShape()
                        ),
                        headline = {
                            Text(
                                text = stringResource(R.string.delete_all_data),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supporting = {
                            Text(
                                text = stringResource(R.string.delete_all_data_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            com.reddy.vittify.presentation.ui.components.SettingsShapeIconBadge(
                                icon = Icons.Rounded.DeleteForever,
                                shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.SCALLOP_12,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                iconSize = 24.dp,
                                contentDescription = null
                            )
                        },
                        onClick = { showDeleteAllDataDialog = true },
                        shape = ListItemPosition.Single.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }

                // Add bottom spacing
                Spacer(modifier = Modifier.size(Spacing.xl))
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }

    if (showDeleteAllDataDialog) {
        val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        AlertDialog(
            onDismissRequest = { showDeleteAllDataDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.delete_all_data_confirm_title)) },
            text = {
                Text(
                    text = stringResource(R.string.delete_all_data_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.deleteAllData()
                        showDeleteAllDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDataDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Timeout Dialog
    if (showTimeoutDialog) {
        val options = listOf(0, 1, 5, 15, 30)
        val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text(stringResource(R.string.lock_timeout)) },
            text = {
                Column {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .selectable(
                                    selected = appLockUiState.timeoutMinutes == minutes,
                                    onClick = {
                                        appLockViewModel.setTimeoutMinutes(minutes)
                                        showTimeoutDialog = false
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLockUiState.timeoutMinutes == minutes,
                                onClick = null
                            )
                            Text(
                                text = when (minutes) {
                                    0 -> stringResource(R.string.immediately)
                                    1 -> stringResource(R.string.one_minute)
                                    else -> stringResource(R.string.minutes_format, minutes)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTimeoutDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .padding(horizontal = Spacing.xl)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            containerColor = if (blurEffects)
                MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .clip(VittifyShapes.dialog)
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(containerColor),
                                blurRadius = 20.dp,
                                noiseFactor = -1f,
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        }
                    ) else Modifier
                ),
            shape = VittifyShapes.dialog,
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { config ->
                viewModel.exportBackup(config)
                showExportDialog = false
            },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // PDF Processing / Error dialog
    if (uiState.isPdfProcessing || uiState.pdfProcessingError != null) {
        PdfProcessingDialog(
            isVisible = uiState.isPdfProcessing,
            error = uiState.pdfProcessingError,
            onDismissError = { viewModel.dismissPdfImport() },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // PDF Import Review BottomSheet (Unified review of accounts and transactions)
    uiState.pdfAnalysisResult?.let { result ->
        PdfImportSheet(
            analysisResult = result,
            availableAccounts = uiState.availableAccounts,
            onConfirm = { transactionDecisions, accountDecisions, accountMappings, shouldUpdateBalances ->
                viewModel.confirmPdfImport(
                    accountDecisions = accountDecisions,
                    accountMappings = accountMappings,
                    transactionDecisions = transactionDecisions,
                    shouldUpdateBalances = shouldUpdateBalances
                )
            },
            onDismiss = { viewModel.dismissPdfImport() }
        )
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (BackupConfiguration) -> Unit,
    blurEffects: Boolean ,
    hazeState: HazeState = remember { HazeState() }
) {
    var includeTransactional by remember { mutableStateOf(true) }
    var includeProfile by remember { mutableStateOf(true) }
    var includeBudgets by remember { mutableStateOf(true) }
    var includePreferences by remember { mutableStateOf(true) }
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_data)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.select_data_to_backup),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExportCheckbox(stringResource(R.string.transactional_data), includeTransactional) { includeTransactional = it }
                ExportCheckbox(stringResource(R.string.profile_data), includeProfile) { includeProfile = it }
                ExportCheckbox(stringResource(R.string.budgets), includeBudgets) { includeBudgets = it }
                ExportCheckbox(stringResource(R.string.app_preferences), includePreferences) { includePreferences = it }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = VittifyShapes.scaled(
                            topStart = 28.dp,
                            topEnd = 4.dp,
                            bottomStart = 28.dp,
                            bottomEnd = 4.dp
                        ),
                        modifier = Modifier
                            .padding(start = Spacing.xl)
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Button(
                        onClick = {
                            onConfirm(
                            BackupConfiguration(
                                includeTransactionalData = includeTransactional,
                                includeProfileData = includeProfile,
                                includeBudgets = includeBudgets,
                                includeAppPreferences = includePreferences
                            ))},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = VittifyShapes.scaled(
                            topStart = 4.dp,
                            topEnd = 28.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 28.dp
                        ),
                        modifier = Modifier
                            .padding(end = Spacing.xl)
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                            Text(
                                text = stringResource(R.string.export),
                                style = MaterialTheme.typography.titleMedium
                            )

                    }

                }
            }
        },
        containerColor = if (blurEffects)
            MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .clip(VittifyShapes.dialog)
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(containerColor),
                            blurRadius = 20.dp,
                            noiseFactor = -1f,
                        )
                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                    }
                ) else Modifier
            ),
        shape = VittifyShapes.dialog,
        dismissButton = {},

    )
}

@Composable
fun ExportCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VittifyCheckbox(checked = checked, onCheckedChange = null)
        Text(
            text = text,
            modifier = Modifier.padding(start = Spacing.sm),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
