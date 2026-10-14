package com.reddy.vittify.presentation.ui.features.settings.importwizard.steps

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.backup.importwizard.ImportAccountItem
import com.reddy.vittify.data.backup.importwizard.ImportTransactionItem
import com.reddy.vittify.data.backup.importwizard.RangeSelectionTracker
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.presentation.ui.components.BrandIcon
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.components.VittifyCheckbox
import com.reddy.vittify.presentation.ui.features.settings.importwizard.components.SelectionActionBar
import com.reddy.vittify.presentation.ui.theme.green_dark
import com.reddy.vittify.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter

@Composable
fun TransactionsPreviewStep(
    transactions: List<ImportTransactionItem>,
    accounts: List<ImportAccountItem>,
    rangeTracker: RangeSelectionTracker,
    hasUnmappedTransactions: Boolean,
    unmappedTransactionsCount: Int,
    onToggleTransaction: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onApplyRange: () -> Unit,
    onMapAccount: (index: Int, accountId: String) -> Unit,
    onBulkMapUnmapped: (accountId: String) -> Unit,
    onMapToAccount: (index: Int, toAccountId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var bulkDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Preview Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Review transactions and tap the account pill to map them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Selection Action Bar
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SelectionActionBar(
                totalCount = transactions.size,
                selectedCount = transactions.count { it.isSelected },
                rangeTracker = rangeTracker,
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                onApplyRange = onApplyRange,
                itemNameSingular = "transaction",
                itemNamePlural = "transactions"
            )
        }

        // Responsive Bulk mapping banner for unmapped transactions
        if (hasUnmappedTransactions && accounts.isNotEmpty()) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$unmappedTransactionsCount transactions need an account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { bulkDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assign All to Account...", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = bulkDropdownExpanded,
                                onDismissRequest = { bulkDropdownExpanded = false }
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.displayTitle) },
                                        leadingIcon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) },
                                        onClick = {
                                            onBulkMapUnmapped(acc.id)
                                            bulkDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transactions List styled like TransactionsScreen (responsive and robust)
        val accountMap = remember(accounts) { accounts.associateBy { it.id } }
        val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.5.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(transactions) { index, txn ->
                val mappedAccount = txn.mappedAccountId?.let { accountMap[it] }
                val position = ListItemPosition.from(index, transactions.size)

                TransactionPreviewListItem(
                    transaction = txn,
                    mappedAccount = mappedAccount,
                    accounts = accounts,
                    dateFormatter = dateFormatter,
                    position = position,
                    onToggle = { onToggleTransaction(index) },
                    onMapAccount = { accountId -> onMapAccount(index, accountId) },
                    onMapToAccount = { toAccountId -> onMapToAccount(index, toAccountId) }
                )
            }
        }
    }
}

@Composable
private fun TransactionPreviewListItem(
    transaction: ImportTransactionItem,
    mappedAccount: ImportAccountItem?,
    accounts: List<ImportAccountItem>,
    dateFormatter: DateTimeFormatter,
    position: ListItemPosition,
    onToggle: () -> Unit,
    onMapAccount: (String) -> Unit,
    onMapToAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var toAccountDropdownExpanded by remember { mutableStateOf(false) }

    val isTransfer = transaction.transactionType == TransactionType.TRANSFER
    val isIncome = transaction.transactionType == TransactionType.INCOME
    val amountColor = when {
        isTransfer -> Color(0xFF9C27B0)
        isIncome -> green_dark
        else -> MaterialTheme.colorScheme.onSurface
    }
    val prefix = when {
        isTransfer -> ""
        isIncome -> "+ "
        else -> "- "
    }

    val mappedToAccount = transaction.mappedToAccountId?.let { id -> accounts.firstOrNull { it.id == id } }

    ListItem(
        shape = position.toShape(),
        padding = PaddingValues(horizontal = 16.dp, vertical = 1.dp),
        onClick = onToggle,
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VittifyCheckbox(
                    checked = transaction.isSelected,
                    onCheckedChange = { onToggle() }
                )

                if (isTransfer) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = Color(0xFF9C27B0).copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    BrandIcon(
                        merchantName = transaction.merchantName,
                        size = 36.dp,
                        showBackground = true,
                        category = transaction.categoryName,
                        subcategory = transaction.subcategoryName
                    )
                }
            }
        },
        headline = {
            Text(
                text = transaction.merchantName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supporting = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transaction.dateTime.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTransfer) Color(0xFF9C27B0) else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        },
        trailing = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val amountDisplay = if (isTransfer && transaction.targetAmount != null &&
                    transaction.targetCurrency != null && !transaction.targetCurrency.equals(transaction.currency, true)
                ) {
                    "${CurrencyFormatter.formatCurrency(transaction.amount, transaction.currency)} → ${CurrencyFormatter.formatCurrency(transaction.targetAmount, transaction.targetCurrency)}"
                } else {
                    prefix + CurrencyFormatter.formatCurrency(transaction.amount, transaction.currency)
                }

                Text(
                    text = amountDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                if (isTransfer) {
                    // Transfer account pill: From -> To
                    Box {
                        val fromTitle = mappedAccount?.displayTitle ?: (transaction.originalBankName ?: "From")
                        val toTitle = mappedToAccount?.displayTitle ?: (transaction.toBankName ?: "To")

                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = Color(0xFF9C27B0).copy(alpha = 0.12f),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { accountDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 140.dp)
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "$fromTitle → $toTitle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9C27B0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF9C27B0)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Transfer Source Account (From):", fontWeight = FontWeight.Bold) },
                                onClick = {}
                            )
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("From: ${acc.displayTitle}") },
                                    leadingIcon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) },
                                    onClick = {
                                        onMapAccount(acc.id)
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Transfer Target Account (To):", fontWeight = FontWeight.Bold) },
                                onClick = {}
                            )
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("To: ${acc.displayTitle}") },
                                    leadingIcon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) },
                                    onClick = {
                                        onMapToAccount(acc.id)
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Non-transfer single account pill
                    Box {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (mappedAccount != null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { accountDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 110.dp)
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = mappedAccount?.accountLast4?.takeIf { it.isNotBlank() }?.let { "•••• $it" }
                                        ?: (mappedAccount?.displayTitle ?: "⚠️ Account"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (mappedAccount != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (mappedAccount != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.displayTitle) },
                                    leadingIcon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) },
                                    onClick = {
                                        onMapAccount(acc.id)
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

