package com.reddy.vittify.presentation.ui.features.settings.rules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.domain.model.rule.TransactionRule
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.VittifyCard
import com.reddy.vittify.presentation.ui.components.VittifySegmentedCard
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.LoadingCircle
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.components.RulesBatchApplyDialog
import com.reddy.vittify.presentation.ui.components.RulesDeleteDialog
import com.reddy.vittify.presentation.ui.components.RulesResetDialog
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.icons.Bag
import com.reddy.vittify.presentation.ui.icons.Edit2
import com.reddy.vittify.presentation.ui.icons.History
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RulesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateRule: () -> Unit,
    onEditRule: (TransactionRule) -> Unit,
    rulesViewModel: RulesViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val rules by rulesViewModel.rules.collectAsStateWithLifecycle()
    val uiState by rulesViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState.isLoading
    val batchApplyProgress = uiState.batchApplyProgress
    val batchApplyResult = uiState.batchApplyResult

    var showBatchApplyDialog by remember { mutableStateOf(false) }
    var selectedRuleForBatch by remember { mutableStateOf<TransactionRule?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.smart_rules),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = { NavigationContent(onNavigateBack) },
                actionContent = {
                    // Add reset button for advanced users
                    var showResetDialog by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showResetDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shapes =  IconButtonDefaults.shapes(),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.reset_to_defaults)
                        )
                    }

                    if (showResetDialog) {
                        RulesResetDialog(
                            onDismiss = { showResetDialog = false },
                            onConfirm = {
                                rulesViewModel.resetToDefaults()
                                showResetDialog = false
                            },
                            blurEffects = blurEffects,
                            hazeState = hazeState
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateRule,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = (LocalBottomNavPadding.current - 16.dp).coerceAtLeast(0.dp))
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.create_rule))
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding() + LocalBottomNavPadding.current),
                contentAlignment = Alignment.Center
            ) {
                LoadingCircle()
            }
        } else {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical(),
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = 100.dp + LocalBottomNavPadding.current
                ),
                state = lazyListState,
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },

                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Info Card
                item{
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.automatic_categorization),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.automatic_categorization_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                            )
                        }
                    }
                }
                }

                item {// Group rules by category for better organization
                    val groupedRules = rules.groupBy { rule ->
                        when {
                            rule.name.contains("Food", ignoreCase = true) ||
                                    rule.name.contains(
                                        "Fuel",
                                        ignoreCase = true
                                    ) -> "Daily Expenses"

                            rule.name.contains("Salary", ignoreCase = true) ||
                                    rule.name.contains(
                                        "Cashback",
                                        ignoreCase = true
                                    ) -> "Income & Cashback"

                            rule.name.contains("Rent", ignoreCase = true) ||
                                    rule.name.contains("EMI", ignoreCase = true) ||
                                    rule.name.contains(
                                        "Subscription",
                                        ignoreCase = true
                                    ) -> "Recurring Payments"

                            rule.name.contains("Investment", ignoreCase = true) ||
                                    rule.name.contains(
                                        "Transfer",
                                        ignoreCase = true
                                    ) -> "Banking & Investments"

                            rule.name.contains("Healthcare", ignoreCase = true) -> "Healthcare"

                            else -> "Other"
                        }
                    }

                    groupedRules.forEach { (category, categoryRules) ->
                        if (categoryRules.isNotEmpty()) {
                            val localizedCategory = when (category) {
                                "Daily Expenses" -> stringResource(R.string.rules_group_daily)
                                "Income & Cashback" -> stringResource(R.string.rules_group_income)
                                "Recurring Payments" -> stringResource(R.string.rules_group_recurring)
                                "Banking & Investments" -> stringResource(R.string.rules_group_banking)
                                "Healthcare" -> stringResource(R.string.rules_group_healthcare)
                                else -> stringResource(R.string.rules_group_other)
                            }
                            SectionHeader(
                                title = localizedCategory,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.5.dp)
                            ) {
                                categoryRules.forEachIndexed { index, rule ->
                                    RuleCard(
                                        rule = rule,
                                        position = ListItemPosition.from(index, categoryRules.size),
                                        onToggle = { isActive ->
                                            rulesViewModel.toggleRule(rule.id, isActive)
                                        },
                                        onDelete = {
                                            rulesViewModel.deleteRule(rule.id)
                                        },
                                        onApplyToPast = {
                                            selectedRuleForBatch = rule
                                            showBatchApplyDialog = true
                                        },
                                        onEdit = { onEditRule(rule) },
                                        blurEffects = blurEffects,
                                        hazeState = hazeState
                                    )
                                }
                            }
                        }
                    }
                }

                // Help text at the bottom
                item {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        text = stringResource(R.string.rules_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.md)
                    )
                }
            }
        }
    }

    // Batch Apply Dialog
    val ruleForBatch = selectedRuleForBatch
    if (showBatchApplyDialog && ruleForBatch != null) {
        RulesBatchApplyDialog(
            rule = ruleForBatch,
            progress = batchApplyProgress,
            result = batchApplyResult,
            onDismiss = {
                showBatchApplyDialog = false
                selectedRuleForBatch = null
                rulesViewModel.clearBatchApplyResult()
            },
            onApplyToAll = {
                rulesViewModel.applyRuleToPastTransactions(ruleForBatch, applyToUncategorizedOnly = false)
            },
            onApplyToUncategorized = {
                rulesViewModel.applyRuleToPastTransactions(ruleForBatch, applyToUncategorizedOnly = true)
            },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }
}

@Composable
private fun RuleCard(
    rule: TransactionRule,
    position: ListItemPosition = ListItemPosition.Single,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onApplyToPast: () -> Unit,
    onEdit: () -> Unit,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    VittifySegmentedCard(
        position = position,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )

                rule.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Show simple condition summary
                val conditionSummary = when {
                    rule.name.contains("Small Payments", ignoreCase = true) -> stringResource(R.string.rule_summary_small_payments)
                    rule.name.contains("UPI Cashback", ignoreCase = true) -> stringResource(R.string.rule_summary_upi_cashback)
                    rule.name.contains("Salary", ignoreCase = true) -> stringResource(R.string.rule_summary_salary)
                    rule.name.contains("Rent", ignoreCase = true) -> stringResource(R.string.rule_summary_rent)
                    rule.name.contains("EMI", ignoreCase = true) -> stringResource(R.string.rule_summary_emi)
                    rule.name.contains("Investment", ignoreCase = true) -> stringResource(R.string.rule_summary_investment)
                    rule.name.contains("Subscription", ignoreCase = true) -> stringResource(R.string.rule_summary_subscription)
                    rule.name.contains("Fuel", ignoreCase = true) -> stringResource(R.string.rule_summary_fuel)
                    rule.name.contains("Healthcare", ignoreCase = true) -> stringResource(R.string.rule_summary_healthcare)
                    rule.name.contains("Transfer", ignoreCase = true) -> stringResource(R.string.rule_summary_transfer)
                    else -> null
                }

                conditionSummary?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Priority badge (only show for non-default priority)
                if (rule.priority != 100) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.priority_format, rule.priority),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // More actions menu - only show when rule is active
                if (rule.isActive) {
                    Box {
                        IconButton(
                            onClick = { showActionsMenu = true }
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.more_options_desc)
                            )
                        }

                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                            shape = MaterialTheme.shapes.large
                        ) {
                             // Edit rule
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_rule)) },
                                leadingIcon = {
                                    Icon(
                                        Iconax.Edit2,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    showActionsMenu = false
                                    onEdit()
                                },
                            )
                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = MaterialTheme.colorScheme.surface
                            )

                            // Apply to past transactions
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.apply_to_past_transactions)) },
                                leadingIcon = {
                                    Icon(
                                        Iconax.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp))
                                },
                                onClick = {
                                    showActionsMenu = false
                                    onApplyToPast()
                                },
                            )


                            // Only show delete for custom rules
                            if (!rule.isSystemTemplate) {
                                HorizontalDivider(
                                    thickness = 1.5.dp,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.delete),
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Iconax.Bag,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showActionsMenu = false
                                        showDeleteDialog = true
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error,
                                    )
                                )
                            }
                        }
                    }
                }

                Switch(
                    checked = rule.isActive,
                    onCheckedChange = onToggle
                )
            }
        }
    }

    if (showDeleteDialog) {
        RulesDeleteDialog(
            rule = rule,
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                onDelete()
                showDeleteDialog = false
            },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }
}