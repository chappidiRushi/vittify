package com.reddy.vittify.presentation.ui.features.settings.importwizard

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.data.backup.importwizard.ImportSource
import com.reddy.vittify.presentation.ui.features.onboarding.steps.OnboardingProgressIndicator
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.BankAccountsPreviewStep
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.CategoriesPreviewStep
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.ImportProgressStep
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.ImportSuccessStep
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.ImportSummaryStep
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.SourceSelectionStep
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.SubscriptionsPreviewStep
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.TransactionsPreviewStep
import com.reddy.vittify.presentation.ui.features.settings.importwizard.steps.WipeWarningStep
import com.reddy.vittify.presentation.ui.theme.VittifySurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWizardScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateAccounts: () -> Unit,
    onNavigateAudit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportWizardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher for file import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.parseSelectedFile(it) }
        }
    )

    // Launchers for sample templates
    val excelExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let { viewModel.downloadExcelTemplate(it) }
        }
    )

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            uri?.let { viewModel.downloadCsvTemplate(it) }
        }
    )

    // Handle Snackbars
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Handle Hardware Back
    BackHandler {
        if (uiState.currentStep == ImportWizardStep.SELECT_SOURCE || uiState.currentStep == ImportWizardStep.SUCCESS) {
            onNavigateBack()
        } else if (uiState.currentStep != ImportWizardStep.IMPORTING) {
            viewModel.goToPreviousStep()
        }
    }

    // Step index for OnboardingProgressIndicator
    val totalSteps = if (uiState.subscriptions.isNotEmpty()) 6 else 5
    val currentStepNumber = when (uiState.currentStep) {
        ImportWizardStep.SELECT_SOURCE -> 1
        ImportWizardStep.WARNING -> 2
        ImportWizardStep.PARSING -> 2
        ImportWizardStep.PREVIEW_BANKS -> 3
        ImportWizardStep.PREVIEW_CATEGORIES -> 4
        ImportWizardStep.PREVIEW_SUBSCRIPTIONS -> 5
        ImportWizardStep.PREVIEW_TRANSACTIONS -> if (uiState.subscriptions.isNotEmpty()) 6 else 5
        ImportWizardStep.PREVIEW_SUMMARY -> if (uiState.subscriptions.isNotEmpty()) 6 else 5
        ImportWizardStep.IMPORTING -> if (uiState.subscriptions.isNotEmpty()) 6 else 5
        ImportWizardStep.SUCCESS -> if (uiState.subscriptions.isNotEmpty()) 6 else 5
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // Reusable OnboardingProgressIndicator styled exactly like Onboarding
                OnboardingProgressIndicator(
                    currentStep = currentStepNumber,
                    totalSteps = totalSteps,
                    titleOverride = uiState.currentStep.title,
                    onBack = {
                        if (uiState.currentStep != ImportWizardStep.SELECT_SOURCE && uiState.currentStep != ImportWizardStep.IMPORTING) {
                            viewModel.goToPreviousStep()
                        }
                    },
                    trailingContent = {
                        // Close button on the right side as requested
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = VittifySurface.surfaceContainerHighColor(),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            // Show bottom navigation bar on preview steps
            if (isStepInPreviewMode(uiState.currentStep)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.goToPreviousStep() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }

                        Button(
                            onClick = { viewModel.goToNextStep() }
                        ) {
                            Text("Next Step")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                val animationSpec = tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing)
                val slideSpec = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)

                if (targetState.stepNumber > initialState.stepNumber) {
                    (slideInHorizontally(slideSpec) { it } + fadeIn(animationSpec)).togetherWith(
                        slideOutHorizontally(slideSpec) { -it } + fadeOut(animationSpec)
                    )
                } else {
                    (slideInHorizontally(slideSpec) { -it } + fadeIn(animationSpec)).togetherWith(
                        slideOutHorizontally(slideSpec) { it } + fadeOut(animationSpec)
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            label = "wizardStepContent"
        ) { step ->
            when (step) {
                ImportWizardStep.SELECT_SOURCE -> {
                    SourceSelectionStep(
                        onSelectSource = { source -> viewModel.selectSource(source) },
                        onDownloadExcelTemplate = { excelExportLauncher.launch("vittify_template.xlsx") },
                        onDownloadCsvTemplate = { csvExportLauncher.launch("vittify_template.csv") }
                    )
                }

                ImportWizardStep.WARNING -> {
                    WipeWarningStep(
                        source = uiState.selectedSource ?: ImportSource.VITTIFY,
                        onProceed = {
                            val mime = when (uiState.selectedSource) {
                                ImportSource.EXCEL -> "*/*"
                                ImportSource.CASHEW_SQLITE -> "*/*"
                                ImportSource.CSV -> "*/*"
                                else -> "*/*"
                            }
                            filePickerLauncher.launch(mime)
                        },
                        onCancel = { viewModel.goToPreviousStep() }
                    )
                }

                ImportWizardStep.PARSING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Reading and analyzing file...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                ImportWizardStep.PREVIEW_BANKS -> {
                    BankAccountsPreviewStep(
                        accounts = uiState.accounts,
                        rangeTracker = uiState.rangeTrackerBanks,
                        onToggleAccount = { viewModel.toggleAccount(it) },
                        onSelectAll = { viewModel.selectAllAccounts() },
                        onDeselectAll = { viewModel.deselectAllAccounts() },
                        onApplyRange = { viewModel.applyRangeSelectionAccounts() },
                        onUpdateAccount = { idx, updated -> viewModel.updateAccount(idx, updated) }
                    )
                }

                ImportWizardStep.PREVIEW_CATEGORIES -> {
                    CategoriesPreviewStep(
                        categories = uiState.categories,
                        existingAppCategories = uiState.existingCategories,
                        existingAppSubcategories = uiState.existingSubcategories,
                        subcategoriesMap = uiState.subcategoriesMap,
                        rangeTracker = uiState.rangeTrackerCategories,
                        onToggleCategory = { viewModel.toggleCategory(it) },
                        onSelectAll = { viewModel.selectAllCategories() },
                        onDeselectAll = { viewModel.deselectAllCategories() },
                        onApplyRange = { viewModel.applyRangeSelectionCategories() },
                        onMapCategorySelection = { idx, cat, sub ->
                            viewModel.mapCategorySelection(idx, cat, sub)
                        },
                        onToggleCreateAsNew = { idx, isNew -> viewModel.toggleCreateCategoryAsNew(idx, isNew) }
                    )
                }

                ImportWizardStep.PREVIEW_SUBSCRIPTIONS -> {
                    SubscriptionsPreviewStep(
                        subscriptions = uiState.subscriptions,
                        rangeTracker = uiState.rangeTrackerSubscriptions,
                        onToggleSubscription = { viewModel.toggleSubscription(it) },
                        onSelectAll = { viewModel.selectAllSubscriptions() },
                        onDeselectAll = { viewModel.deselectAllSubscriptions() },
                        onApplyRange = { viewModel.applyRangeSelectionSubscriptions() }
                    )
                }

                ImportWizardStep.PREVIEW_TRANSACTIONS -> {
                    TransactionsPreviewStep(
                        transactions = uiState.transactions,
                        accounts = uiState.accounts.filter { it.isSelected },
                        rangeTracker = uiState.rangeTrackerTransactions,
                        hasUnmappedTransactions = uiState.hasUnmappedTransactions,
                        unmappedTransactionsCount = uiState.unmappedTransactionsCount,
                        onToggleTransaction = { viewModel.toggleTransaction(it) },
                        onSelectAll = { viewModel.selectAllTransactions() },
                        onDeselectAll = { viewModel.deselectAllTransactions() },
                        onApplyRange = { viewModel.applyRangeSelectionTransactions() },
                        onMapAccount = { idx, accountId -> viewModel.mapTransactionAccount(idx, accountId) },
                        onMapToAccount = { idx, accountId -> viewModel.mapTransactionToAccount(idx, accountId) },
                        onBulkMapUnmapped = { accountId -> viewModel.bulkMapUnmappedTransactions(accountId) }
                    )
                }

                ImportWizardStep.PREVIEW_SUMMARY -> {
                    ImportSummaryStep(
                        source = uiState.selectedSource,
                        accountsCount = uiState.selectedAccountsCount,
                        categoriesCount = uiState.selectedCategoriesCount,
                        newCategoriesCount = uiState.categories.count { it.isSelected && it.createAsNew },
                        subscriptionsCount = uiState.selectedSubscriptionsCount,
                        transactionsCount = uiState.selectedTransactionsCount,
                        onStartImport = { viewModel.executeImport() }
                    )
                }

                ImportWizardStep.IMPORTING -> {
                    ImportProgressStep(
                        progressFraction = uiState.importProgressFraction,
                        statusMessage = uiState.importProgressMessage
                    )
                }

                ImportWizardStep.SUCCESS -> {
                    ImportSuccessStep(
                        accountsCount = uiState.importedAccountsCount,
                        categoriesCount = uiState.importedCategoriesCount,
                        subscriptionsCount = uiState.importedSubscriptionsCount,
                        transactionsCount = uiState.importedTransactionsCount,
                        onNavigateHome = onNavigateHome,
                        onNavigateAccounts = onNavigateAccounts,
                        onNavigateAudit = onNavigateAudit
                    )
                }
            }
        }
    }
}

private fun isStepInPreviewMode(step: ImportWizardStep): Boolean {
    return step in listOf(
        ImportWizardStep.PREVIEW_BANKS,
        ImportWizardStep.PREVIEW_CATEGORIES,
        ImportWizardStep.PREVIEW_SUBSCRIPTIONS,
        ImportWizardStep.PREVIEW_TRANSACTIONS
    )
}
