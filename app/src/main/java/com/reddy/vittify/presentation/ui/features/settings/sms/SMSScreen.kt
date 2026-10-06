package com.reddy.vittify.presentation.ui.features.settings.sms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.R
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.FoldableSection
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.features.settings.SettingsViewModel
import com.reddy.vittify.presentation.ui.icons.Clock
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
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
fun SMSScreen(
    onNavigateBack: () -> Unit,
    targetOptionId: String? = null,
    onNavigateToUnrecognizedSms: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val smsScanMonths by settingsViewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 3)
    val smsScanAllTime by settingsViewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = true)
    val isScanningSms by settingsViewModel.isScanningSms.collectAsStateWithLifecycle()
    val unreportedCount by settingsViewModel.unreportedSmsCount.collectAsStateWithLifecycle()

    var showSmsScanDialog by remember { mutableStateOf(false) }
    var syncSectionExpanded by remember { mutableStateOf(true) }
    var diagnosticsSectionExpanded by remember { mutableStateOf(true) }

    val bringIntoViewRequesters = remember {
        mapOf(
            "rescan-sms" to BringIntoViewRequester(),
            "scan-range" to BringIntoViewRequester(),
            "scan-all-time" to BringIntoViewRequester(),
            "unrecognized" to BringIntoViewRequester()
        )
    }
    var highlightedOptionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetOptionId) {
        targetOptionId?.let { rawId ->
            val id = SettingsDeepLink.normalizeOptionSlug("sms", rawId)
            when (id) {
                "scan-range", "scan-all-time" -> {
                    showSmsScanDialog = true
                    delay(300)
                    bringIntoViewRequesters["scan-range"]?.bringIntoView()
                    highlightedOptionId = "scan-range"
                    delay(2500)
                    highlightedOptionId = null
                }
                "unrecognized" -> {
                    delay(300)
                    bringIntoViewRequesters["unrecognized"]?.bringIntoView()
                    highlightedOptionId = "unrecognized"
                    delay(2500)
                    highlightedOptionId = null
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

    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.sms_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
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
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content +
                                paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Section 1: Scanning & Synchronization
                FoldableSection(
                    title = stringResource(R.string.sms_section_sync),
                    expanded = syncSectionExpanded,
                    onToggle = { syncSectionExpanded = !syncSectionExpanded }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        // Rescan SMS Messages
                        ListItem(
                            modifier = Modifier.settingOptionHighlight(
                                id = "rescan-sms",
                                requester = bringIntoViewRequesters["rescan-sms"],
                                highlightedId = highlightedOptionId,
                                shape = ListItemPosition.Top.toShape()
                            ),
                            headline = {
                                Text(
                                    text = stringResource(R.string.rescan_sms_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supporting = {
                                Text(
                                    text = if (isScanningSms) stringResource(R.string.rescan_in_progress)
                                    else stringResource(R.string.rescan_sms_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.SCALLOP_12.composeShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            },
                            trailing = {
                                if (isScanningSms) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = MaterialTheme.shapes.large
                                            )
                                            .padding(
                                                horizontal = Spacing.md,
                                                vertical = Spacing.sm
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.rescan_sms_button),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (!isScanningSms) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    settingsViewModel.rescanSms()
                                }
                            },
                            shape = ListItemPosition.Top.toShape(),
                            padding = PaddingValues(0.dp)
                        )

                        // SMS Scan Period
                        ListItem(
                            modifier = Modifier.settingOptionHighlight(
                                id = "scan-range",
                                requester = bringIntoViewRequesters["scan-range"],
                                highlightedId = highlightedOptionId,
                                shape = ListItemPosition.Bottom.toShape()
                            ),
                            headline = {
                                Text(
                                    text = stringResource(R.string.sms_scan_period),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supporting = {
                                Text(
                                    text = if (smsScanAllTime) stringResource(R.string.scan_all_sms)
                                    else stringResource(R.string.scan_months_format, smsScanMonths),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.COOKIE_8.composeShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Iconax.Clock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            },
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.large
                                        )
                                        .clickable(
                                            onClick = { showSmsScanDialog = true }
                                        )
                                        .padding(
                                            horizontal = Spacing.md,
                                            vertical = Spacing.sm
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (smsScanAllTime) stringResource(R.string.range_all_time)
                                        else stringResource(R.string.months_count_format, smsScanMonths),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            },
                            onClick = { showSmsScanDialog = true },
                            shape = ListItemPosition.Bottom.toShape(),
                            padding = PaddingValues(0.dp)
                        )
                    }
                }

                // Section 2: Diagnostics & Reports
                FoldableSection(
                    title = stringResource(R.string.sms_section_diagnostics),
                    expanded = diagnosticsSectionExpanded,
                    onToggle = { diagnosticsSectionExpanded = !diagnosticsSectionExpanded }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        // Unrecognized Bank Messages
                        ListItem(
                            modifier = Modifier.settingOptionHighlight(
                                id = "unrecognized",
                                requester = bringIntoViewRequesters["unrecognized"],
                                highlightedId = highlightedOptionId,
                                shape = ListItemPosition.Single.toShape()
                            ),
                            headline = {
                                Text(
                                    text = stringResource(R.string.unrecognized_bank_messages),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supporting = {
                                Text(
                                    text = if (unreportedCount > 0)
                                        pluralStringResource(R.plurals.unrecognized_messages_count, unreportedCount, unreportedCount)
                                    else stringResource(R.string.no_unrecognized_messages),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.PENTAGON.composeShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.BugReport,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (unreportedCount > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(unreportedCount.toString())
                                        }
                                    }

                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = { onNavigateToUnrecognizedSms() },
                            shape = ListItemPosition.Single.toShape(),
                            padding = PaddingValues(0.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }

        // SMS Scan Period Dialog
        if (showSmsScanDialog) {
            val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            AlertDialog(
                onDismissRequest = { showSmsScanDialog = false },
                title = { Text(stringResource(R.string.sms_scan_period)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                        Text(
                            text = stringResource(R.string.sms_scan_period_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))

                        val options = listOf(-1) + listOf(1, 2, 3, 6, 12, 24)
                        options.forEachIndexed { index, months ->
                            val isSelected = if (months == -1) smsScanAllTime
                            else smsScanMonths == months && !smsScanAllTime

                            ListItem(
                                headline = {
                                    Text(
                                        text = when (months) {
                                            -1 -> stringResource(R.string.range_all_time)
                                            1 -> stringResource(R.string.one_month)
                                            24 -> stringResource(R.string.two_years)
                                            else -> stringResource(R.string.months_count_format, months)
                                        }
                                    )
                                },
                                trailing = {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (months == -1) {
                                        settingsViewModel.updateSmsScanAllTime(true)
                                        showSmsScanDialog = false
                                    } else {
                                        settingsViewModel.updateSmsScanMonths(months)
                                        settingsViewModel.updateSmsScanAllTime(false)
                                        showSmsScanDialog = false
                                    }
                                },
                                shape = ListItemPosition.from(index, options.size).toShape(),
                                listColor = MaterialTheme.colorScheme.surfaceContainer,
                                padding = PaddingValues(0.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSmsScanDialog = false },
                        colors = ButtonDefaults.filledTonalButtonColors(),
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
    }
}
