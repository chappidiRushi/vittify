package com.reddy.vittify.presentation.ui.features.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.LoadingCircle
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.components.VittifyCard
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAuditScreen(
    bankName: String,
    accountLast4: String,
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    viewModel: AccountAuditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()

    // Trigger audit on first composition
    LaunchedEffect(bankName, accountLast4) {
        viewModel.runAudit(bankName, accountLast4)
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Balance Audit",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) },
                hasActionButton = true,
                actionContent = {
                    IconButton(onClick = { viewModel.recalculateBalances() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Recalculate Balances"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
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
                    .overScrollVertical()
                    .hazeSource(state = hazeState),
                state = lazyListState,
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                contentPadding = PaddingValues(
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Account header info
                item {
                    AccountInfoHeader(
                        bankName = uiState.bankName,
                        accountLast4 = uiState.accountLast4,
                        transactionCount = uiState.transactionCount,
                        isCreditCard = uiState.isCreditCard
                    )
                }

                // Health Status Card
                item {
                    HealthStatusCard(
                        isHealthy = uiState.isHealthy,
                        discrepancy = uiState.discrepancy,
                        currency = uiState.currency,
                        isCreditCard = uiState.isCreditCard
                    )
                }

                // Balance Breakdown
                item {
                    SectionHeader(
                        title = "Balance Breakdown",
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content + Spacing.sm)
                    )
                }

                item {
                    BalanceBreakdownCard(
                        initialBalance = uiState.initialBalance,
                        totalIncome = uiState.totalIncome,
                        totalExpenses = uiState.totalExpenses,
                        totalTransfersIn = uiState.totalTransfersIn,
                        totalTransfersOut = uiState.totalTransfersOut,
                        expectedBalance = uiState.expectedBalance,
                        currentBalance = uiState.currentBalance,
                        discrepancy = uiState.discrepancy,
                        currency = uiState.currency,
                        isCreditCard = uiState.isCreditCard,
                        creditLimit = uiState.creditLimit
                    )
                }

                // Issues Section
                val hasIssues = !uiState.isHealthy ||
                    uiState.duplicateSuspects.isNotEmpty()

                if (hasIssues) {
                    item {
                        SectionHeader(
                            title = "Issues Found",
                            modifier = Modifier.padding(horizontal = Dimensions.Padding.content + Spacing.sm)
                        )
                    }

                    // Duplicate Suspects
                    if (uiState.duplicateSuspects.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Suspected Duplicates (${uiState.duplicateSuspects.size} groups)",
                                modifier = Modifier.padding(horizontal = Dimensions.Padding.content + Spacing.sm)
                            )
                        }

                        items(
                            items = uiState.duplicateSuspects,
                            key = { it.transactions.first().id }
                        ) { group ->
                            DuplicateGroupCard(
                                group = group,
                                currency = uiState.currency,
                                onDeleteTransaction = { transactionId ->
                                    viewModel.deleteDuplicateTransaction(transactionId)
                                },
                                onKeepTransaction = { transactionId ->
                                    viewModel.markDuplicateDismissed(transactionId)
                                },
                                onKeepAll = {
                                    viewModel.markGroupAsNotDuplicate(group)
                                }
                            )
                        }
                    }
                }

                // Audit & Fix Actions
                item {
                    SectionHeader(
                        title = if (!uiState.isHealthy || uiState.duplicateSuspects.isNotEmpty()) "Fix Actions" else "Actions",
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content + Spacing.sm)
                    )
                }

                item {
                    FixActionsCard(
                        hasDiscrepancy = !uiState.isHealthy,
                        discrepancy = uiState.discrepancy,
                        currency = uiState.currency,
                        onAutoFix = { viewModel.fixDiscrepancyWithCorrection() },
                        onRecalculate = { viewModel.recalculateBalances() }
                    )
                }

                // Bank-statement ledger
                if (uiState.ledgerRows.isNotEmpty()) {
                    item {
                        BankStatementLedger(
                            ledgerRows = uiState.ledgerRows,
                            currency = uiState.currency,
                            isCreditCard = uiState.isCreditCard,
                            onTransactionClick = onTransactionClick,
                            onDeleteTransaction = { viewModel.deleteTransaction(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfoHeader(
    bankName: String,
    accountLast4: String,
    transactionCount: Int,
    isCreditCard: Boolean
) {
    VittifyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = bankName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isCreditCard) "Credit Card •• $accountLast4" else "Account •• $accountLast4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$transactionCount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HealthStatusCard(
    isHealthy: Boolean,
    discrepancy: BigDecimal,
    currency: String,
    isCreditCard: Boolean
) {
    val statusColor by animateColorAsState(
        targetValue = if (isHealthy) Color(0xFF4CAF50) else Color(0xFFFF5722),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "statusColor"
    )

    VittifyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isHealthy) Icons.Rounded.Check else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHealthy) "All Clear" else "Discrepancy Found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isHealthy) {
                        if (isCreditCard) "Your transaction history matches the actual debt perfectly."
                        else "Your transaction history matches the current balance perfectly."
                    } else {
                        if (isCreditCard) "There's a mismatch of ${CurrencyFormatter.formatCurrency(discrepancy.abs(), currency)} between expected and actual debt."
                        else "There's a mismatch of ${CurrencyFormatter.formatCurrency(discrepancy.abs(), currency)} between expected and actual balance."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BalanceBreakdownCard(
    initialBalance: BigDecimal,
    totalIncome: BigDecimal,
    totalExpenses: BigDecimal,
    totalTransfersIn: BigDecimal,
    totalTransfersOut: BigDecimal,
    expectedBalance: BigDecimal,
    currentBalance: BigDecimal,
    discrepancy: BigDecimal,
    currency: String,
    isCreditCard: Boolean,
    creditLimit: BigDecimal?
) {
    VittifyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            BreakdownRow(
                label = if (isCreditCard) "Starting Debt" else "Starting Balance",
                amount = initialBalance,
                currency = currency,
                prefix = ""
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            if (!isCreditCard) {
                BreakdownRow(
                    label = "Total Income",
                    amount = totalIncome,
                    currency = currency,
                    prefix = "+",
                    amountColor = Color(0xFF4CAF50)
                )

                BreakdownRow(
                    label = "Total Expenses",
                    amount = totalExpenses,
                    currency = currency,
                    prefix = "−",
                    amountColor = MaterialTheme.colorScheme.error
                )
            } else {
                BreakdownRow(
                    label = "Total Spends",
                    amount = totalExpenses,
                    currency = currency,
                    prefix = "+",
                    amountColor = MaterialTheme.colorScheme.error
                )

                BreakdownRow(
                    label = "Total Payments",
                    amount = totalIncome,
                    currency = currency,
                    prefix = "−",
                    amountColor = Color(0xFF4CAF50)
                )
            }

            if (totalTransfersIn > BigDecimal.ZERO) {
                BreakdownRow(
                    label = "Transfers In",
                    amount = totalTransfersIn,
                    currency = currency,
                    prefix = if (isCreditCard) "−" else "+",
                    amountColor = Color(0xFF2196F3)
                )
            }

            if (totalTransfersOut > BigDecimal.ZERO) {
                BreakdownRow(
                    label = "Transfers Out",
                    amount = totalTransfersOut,
                    currency = currency,
                    prefix = if (isCreditCard) "+" else "−",
                    amountColor = Color(0xFFFF9800)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.5.dp
            )

            BreakdownRow(
                label = if (isCreditCard) "Expected Debt" else "Expected Balance",
                amount = expectedBalance,
                currency = currency,
                prefix = "=",
                isBold = true
            )

            BreakdownRow(
                label = if (isCreditCard) "Actual Debt" else "Current Balance",
                amount = currentBalance,
                currency = currency,
                prefix = "",
                isBold = true,
                amountColor = MaterialTheme.colorScheme.primary
            )

            if (isCreditCard && creditLimit != null && creditLimit > BigDecimal.ZERO) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.5.dp
                )

                BreakdownRow(
                    label = "Card Limit",
                    amount = creditLimit,
                    currency = currency,
                    prefix = ""
                )

                BreakdownRow(
                    label = "Expected Available Limit",
                    amount = creditLimit - expectedBalance,
                    currency = currency,
                    prefix = "=",
                    isBold = true
                )

                BreakdownRow(
                    label = "Actual Available Limit",
                    amount = creditLimit - currentBalance,
                    currency = currency,
                    prefix = "",
                    isBold = true,
                    amountColor = MaterialTheme.colorScheme.primary
                )
            }

            if (discrepancy.compareTo(BigDecimal.ZERO) != 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                    thickness = 1.5.dp
                )

                BreakdownRow(
                    label = "Discrepancy",
                    amount = discrepancy,
                    currency = currency,
                    prefix = if (discrepancy > BigDecimal.ZERO) "+" else "",
                    isBold = true,
                    amountColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    currency: String,
    prefix: String = "",
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$prefix${CurrencyFormatter.formatCurrency(amount, currency)}",
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = amountColor
        )
    }
}

private enum class IssueSeverity {
    INFO, WARNING, ERROR
}

@Composable
private fun IssueCard(
    title: String,
    description: String,
    severity: IssueSeverity
) {
    val severityColor = when (severity) {
        IssueSeverity.INFO -> Color(0xFF2196F3)
        IssueSeverity.WARNING -> Color(0xFFFF9800)
        IssueSeverity.ERROR -> MaterialTheme.colorScheme.error
    }

    VittifyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.08f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(severityColor)
                    .padding(top = 6.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = severityColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    currency: String,
    onDeleteTransaction: (Long) -> Unit,
    onKeepTransaction: (Long) -> Unit,
    onKeepAll: () -> Unit
) {
    VittifyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.merchantName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${group.transactions.size} identical transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = CurrencyFormatter.formatCurrency(group.amount, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            group.transactions.forEachIndexed { index, transaction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.dateTime.toLocalDate().toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = transaction.dateTime.toLocalTime().toString().substring(0, 5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (transaction.transactionType) {
                                TransactionType.INCOME -> "Income"
                                TransactionType.EXPENSE -> "Expense"
                                TransactionType.TRANSFER -> "Transfer"
                                else -> transaction.transactionType.name
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // For duplicates (index > 0): allow keeping as intentional or deleting
                        if (index > 0) {
                            IconButton(
                                onClick = { onKeepTransaction(transaction.id) },
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
                                onClick = { onDeleteTransaction(transaction.id) },
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
                    onClick = onKeepAll,
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
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FixActionsCard(
    hasDiscrepancy: Boolean,
    discrepancy: BigDecimal,
    currency: String,
    onAutoFix: () -> Unit,
    onRecalculate: () -> Unit
) {
    VittifyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = if (hasDiscrepancy) "Choose how to resolve issues" else "Audit & Recalculate Options",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Auto-fix button
            if (hasDiscrepancy) {
                Button(
                    onClick = onAutoFix,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Auto-Fix Balance (${CurrencyFormatter.formatCurrency(discrepancy.abs(), currency)})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Text(
                    text = "Updates the actual balance to match the expected balance based on your transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Recalculate button
            OutlinedButton(
                onClick = onRecalculate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Recalculate All Balances",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = "Recalculates the balance chain from scratch based on transaction history. Use this to re-sync account balances.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}


/**
 * Bank-statement style ledger: #, Transaction, Paid, Income, Balance columns.
 */
@Composable
private fun BankStatementLedger(
    ledgerRows: List<LedgerRow>,
    currency: String,
    isCreditCard: Boolean,
    onTransactionClick: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "ledgerArrowRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
    ) {
        VittifyCard(modifier = Modifier.fillMaxWidth()) {
            // Collapsible header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Transaction Ledger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${ledgerRows.size} transactions · tap to ${if (isExpanded) "collapse" else "expand"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(arrowRotation),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // Column header row
                    LedgerHeaderRow(isCreditCard = isCreditCard)

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    // Data rows (newest-on-top = reverse the chronological list)
                    ledgerRows.asReversed().forEachIndexed { idx, row ->
                        LedgerDataRow(
                            row = row,
                            currency = currency,
                            isCreditCard = isCreditCard,
                            onClick = { onTransactionClick(row.transaction.id) },
                            onDelete = { onDeleteTransaction(row.transaction.id) }
                        )
                        if (idx < ledgerRows.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Spacing.xs),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerHeaderRow(isCreditCard: Boolean) {
    val headerStyle = MaterialTheme.typography.labelSmall
    val headerColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = Spacing.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // #
        Text(
            text = "#",
            style = headerStyle,
            fontWeight = FontWeight.Bold,
            color = headerColor,
            modifier = Modifier.width(26.dp)
        )
        // Transaction
        Text(
            text = "Transaction",
            style = headerStyle,
            fontWeight = FontWeight.Bold,
            color = headerColor,
            modifier = Modifier.weight(1f)
        )
        // Paid / Debit
        Text(
            text = if (isCreditCard) "Spent" else "Paid",
            style = headerStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        // Income / Credit
        Text(
            text = if (isCreditCard) "Repaid" else "Income",
            style = headerStyle,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        // Balance
        Text(
            text = "Balance",
            style = headerStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(78.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        // delete icon placeholder
        Spacer(modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun LedgerDataRow(
    row: LedgerRow,
    currency: String,
    isCreditCard: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val tx = row.transaction
    val typeLabel = when (tx.transactionType) {
        TransactionType.INCOME     -> "Income"
        TransactionType.EXPENSE    -> "Expense"
        TransactionType.TRANSFER   -> "Transfer"
        TransactionType.CREDIT     -> "Credit"
        TransactionType.INVESTMENT -> "Investment"
        else                       -> tx.transactionType.name
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Row number
        Text(
            text = "${row.rowNumber}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.width(26.dp)
        )

        // Merchant + date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.merchantName.ifBlank { "Unknown" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${tx.dateTime.toLocalDate()} · $typeLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Paid column (debit)
        Text(
            text = if (row.paid != null) CurrencyFormatter.formatCurrency(row.paid, currency) else "-",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (row.paid != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (row.paid != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1
        )

        // Income column (credit)
        Text(
            text = if (row.income != null) CurrencyFormatter.formatCurrency(row.income, currency) else "-",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (row.income != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (row.income != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1
        )

        // Balance column
        Text(
            text = CurrencyFormatter.formatCurrency(row.balance, currency),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(78.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1
        )

        // Delete
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete transaction",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
