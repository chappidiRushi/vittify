package com.reddy.vittify.presentation.ui.features.settings.statementsreports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.GenericTypeSwitcher
import com.reddy.vittify.presentation.ui.components.VittifyCard
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.features.settings.dataprivacy.DataPrivacyViewModel
import com.reddy.vittify.presentation.ui.features.settings.dataprivacy.PdfImportSheet
import com.reddy.vittify.presentation.ui.features.settings.dataprivacy.PdfProcessingDialog
import com.reddy.vittify.presentation.ui.features.settings.pdfreport.DateRangeOption
import com.reddy.vittify.presentation.ui.features.settings.pdfreport.PdfReportUiState
import com.reddy.vittify.presentation.ui.features.settings.pdfreport.PdfReportViewModel
import com.reddy.vittify.presentation.ui.features.settings.pdfreport.accountKey
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.cyan_dark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun StatementsReportsScreen(
    onNavigateBack: () -> Unit,
    blurEffects: Boolean = true,
    pdfReportViewModel: PdfReportViewModel = hiltViewModel(),
    dataPrivacyViewModel: DataPrivacyViewModel = hiltViewModel()
) {
    val reportUiState by pdfReportViewModel.uiState.collectAsStateWithLifecycle()
    val privacyUiState by dataPrivacyViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    var selectedTab by remember { mutableIntStateOf(0) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { dataPrivacyViewModel.analyzePdfStatement(it) }
        }
    )

    LaunchedEffect(reportUiState.errorMessage) {
        if (!reportUiState.errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(reportUiState.errorMessage!!)
            pdfReportViewModel.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.statements_reports_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = 120.dp
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Tab Selector
            GenericTypeSwitcher(
                selectedIndex = selectedTab,
                onIndexChange = { index -> selectedTab = index },
                options = listOf(
                    stringResource(R.string.export_report_tab),
                    stringResource(R.string.import_statement_tab)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val isForward = targetState > initialState
                    val springSpec = spring<IntOffset>(dampingRatio = 0.62f, stiffness = 340f)
                    val scaleSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 340f)
                    if (isForward) {
                        (slideInHorizontally(springSpec) { it } + scaleIn(initialScale = 0.95f, animationSpec = scaleSpring) + fadeIn())
                            .togetherWith(slideOutHorizontally(springSpec) { -it / 3 } + scaleOut(targetScale = 0.95f, animationSpec = scaleSpring) + fadeOut())
                    } else {
                        (slideInHorizontally(springSpec) { -it } + scaleIn(initialScale = 0.95f, animationSpec = scaleSpring) + fadeIn())
                            .togetherWith(slideOutHorizontally(springSpec) { it / 3 } + scaleOut(targetScale = 0.95f, animationSpec = scaleSpring) + fadeOut())
                    }
                },
                label = "statements_reports_tab_transition"
            ) { tab ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (tab == 0) {
                        // EXPORT REPORT TAB CONTENT
                        HeroBannerCard()

                // Account selection
                SectionCard(
                    title = stringResource(R.string.title_accounts),
                    icon = { Icon(Icons.Rounded.CheckCircle, null, tint = cyan_dark, modifier = Modifier.size(20.dp)) },
                    action = if (reportUiState.accounts.isNotEmpty()) {
                        {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { pdfReportViewModel.selectAllAccounts() },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.xs, vertical = 0.dp)
                                ) {
                                    Text("All", style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(
                                    onClick = { pdfReportViewModel.deselectAllAccounts() },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.xs, vertical = 0.dp)
                                ) {
                                    Text("None", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    } else null
                ) {
                    if (reportUiState.accounts.isEmpty()) {
                        Text(
                            text = "No accounts found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            reportUiState.accounts.forEach { account ->
                                val key = account.accountKey()
                                val selected = reportUiState.selectedAccountKeys.contains(key)
                                AccountChip(
                                    account = account,
                                    selected = selected,
                                    onClick = { pdfReportViewModel.toggleAccount(key) }
                                )
                            }
                        }
                    }
                }

                // Date range
                SectionCard(
                    title = "Date Range",
                    icon = { Icon(Icons.Rounded.DateRange, null, tint = cyan_dark, modifier = Modifier.size(20.dp)) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        DateRangeOption.entries.forEach { option ->
                            val selected = reportUiState.dateRangeOption == option
                            FilterChip(
                                selected = selected,
                                onClick = { pdfReportViewModel.setDateRange(option) },
                                label = { Text(option.label, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = if (selected) { { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp)) } } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = reportUiState.dateRangeOption == DateRangeOption.CUSTOM,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            DatePickerRow("From", reportUiState.customStartDate) { pdfReportViewModel.setCustomStartDate(it) }
                            DatePickerRow("To", reportUiState.customEndDate) { pdfReportViewModel.setCustomEndDate(it) }
                        }
                    }
                }

                // Preview summary
                if (reportUiState.selectedAccountKeys.isNotEmpty()) {
                    val (start, end) = resolveRange(reportUiState)
                    SectionCard(
                        title = "Report Preview",
                        icon = { Icon(Icons.Rounded.Description, null, tint = cyan_dark, modifier = Modifier.size(20.dp)) }
                    ) {
                        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
                        Text(
                            text = "${reportUiState.selectedAccountKeys.size} account(s)  •  ${start.format(fmt)} → ${end.format(fmt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "PDF will include: Account Summaries, Category Pie Chart, Transaction Ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Generate button
                val isReady = reportUiState.selectedAccountKeys.isNotEmpty() && !reportUiState.isGenerating
                Button(
                    onClick = { pdfReportViewModel.generatePdf() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = isReady,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = VittifyShapes.button
                ) {
                    if (reportUiState.isGenerating) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pdf_generating))
                    } else {
                        Icon(Icons.Rounded.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pdf_generate), fontWeight = FontWeight.SemiBold)
                    }
                }

                // Share button if file available
                AnimatedVisibility(visible = reportUiState.generatedFile != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = VittifyShapes.platter
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "PDF Ready!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        reportUiState.generatedFile?.name ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { pdfReportViewModel.downloadPdf() }) {
                                        Icon(Icons.Rounded.Download, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Download")
                                    }
                                    OutlinedButton(onClick = { pdfReportViewModel.sharePdf() }) {
                                        Icon(Icons.Rounded.Share, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // IMPORT STATEMENT TAB CONTENT
                VittifyCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.import_pdf_statement),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = stringResource(R.string.import_pdf_statement_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        Button(
                            onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = VittifyShapes.button,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Select PDF Statement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
}

    // PDF Processing / Error dialog
    if (privacyUiState.isPdfProcessing || privacyUiState.pdfProcessingError != null) {
        PdfProcessingDialog(
            isVisible = privacyUiState.isPdfProcessing,
            error = privacyUiState.pdfProcessingError,
            onDismissError = { dataPrivacyViewModel.dismissPdfImport() },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // PDF Import Review BottomSheet (Unified review of accounts and transactions)
    privacyUiState.pdfAnalysisResult?.let { result ->
        PdfImportSheet(
            analysisResult = result,
            availableAccounts = privacyUiState.availableAccounts,
            onConfirm = { transactionDecisions, accountDecisions, accountMappings, shouldUpdateBalances ->
                dataPrivacyViewModel.confirmPdfImport(
                    accountDecisions = accountDecisions,
                    accountMappings = accountMappings,
                    transactionDecisions = transactionDecisions,
                    shouldUpdateBalances = shouldUpdateBalances
                )
            },
            onDismiss = { dataPrivacyViewModel.dismissPdfImport() }
        )
    }
}

@Composable
private fun HeroBannerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = VittifyShapes.large,
        border = VittifySurface.platterBorder(),
        colors = CardDefaults.cardColors(
            containerColor = VittifySurface.surfaceContainerLowColor()
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Transaction Report PDF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Beautifully branded PDF with summaries, charts & ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: @Composable () -> Unit,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = VittifyShapes.large,
        border = VittifySurface.platterBorder(),
        colors = CardDefaults.cardColors(containerColor = VittifySurface.surfaceContainerLowColor())
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    icon()
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun AccountChip(account: AccountBalanceEntity, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val chipShape = VittifyShapes.scaled(12.dp)
    Row(
        modifier = Modifier
            .clip(chipShape)
            .background(bgColor)
            .border(1.dp, borderColor, chipShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null,
            modifier = Modifier.size(16.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
        Column {
            Text(account.bankName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text("••• ${account.accountLast4}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DatePickerRow(label: String, date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val dateShape = VittifyShapes.scaled(10.dp)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .clip(dateShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.60f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), dateShape)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(date.format(fmt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun resolveRange(state: PdfReportUiState): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    return when (state.dateRangeOption) {
        DateRangeOption.CURRENT_MONTH -> today.withDayOfMonth(1) to today
        DateRangeOption.PREVIOUS_MONTH -> {
            val prev = today.minusMonths(1)
            prev.withDayOfMonth(1) to prev.withDayOfMonth(prev.lengthOfMonth())
        }
        DateRangeOption.CURRENT_YEAR -> today.withDayOfYear(1) to today
        DateRangeOption.ALL_TIME -> LocalDate.of(2000, 1, 1) to today
        DateRangeOption.CUSTOM -> state.customStartDate to state.customEndDate
    }
}

