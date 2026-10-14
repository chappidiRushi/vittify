package com.reddy.vittify.presentation.ui.features.settings.importwizard.steps

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.backup.importwizard.ImportAccountItem
import com.reddy.vittify.data.backup.importwizard.RangeSelectionTracker
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.components.VittifyCheckbox
import com.reddy.vittify.presentation.ui.features.settings.importwizard.components.SelectionActionBar
import com.reddy.vittify.utils.CurrencyFormatter

@Composable
fun BankAccountsPreviewStep(
    accounts: List<ImportAccountItem>,
    rangeTracker: RangeSelectionTracker,
    onToggleAccount: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onApplyRange: () -> Unit,
    onUpdateAccount: (index: Int, updated: ImportAccountItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingAccountIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Preview Bank Accounts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Grouped bank accounts & wallets. Tap edit to fill in any missing account numbers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Selection Action Bar
        SelectionActionBar(
            totalCount = accounts.size,
            selectedCount = accounts.count { it.isSelected },
            rangeTracker = rangeTracker,
            onSelectAll = onSelectAll,
            onDeselectAll = onDeselectAll,
            onApplyRange = onApplyRange,
            itemNameSingular = "account",
            itemNamePlural = "accounts"
        )

        // Grouped List of Accounts just like Settings
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.5.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(accounts) { index, account ->
                val position = ListItemPosition.from(index, accounts.size)

                val icon = when {
                    account.isCreditCard -> Icons.Rounded.CreditCard
                    account.isWallet -> Icons.Rounded.Wallet
                    else -> Icons.Rounded.AccountBalance
                }

                ListItem(
                    shape = position.toShape(),
                    padding = PaddingValues(0.dp),
                    onClick = { onToggleAccount(index) },
                    leading = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VittifyCheckbox(
                                checked = account.isSelected,
                                onCheckedChange = { onToggleAccount(index) }
                            )

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    headline = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = account.displayTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (account.accountLast4.isBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "Missing Last 4",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    supporting = {
                        Text(
                            text = if (account.accountLast4.isNotBlank()) account.displaySubtitle else "Tap edit to add last 4 digits",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (account.accountLast4.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    },
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = CurrencyFormatter.formatCurrency(account.balance, account.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = { editingAccountIndex = index },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit Account",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    // Edit Account Dialog to fill missing last 4 or adjust details
    editingAccountIndex?.let { index ->
        val account = accounts.getOrNull(index)
        if (account != null) {
            EditAccountDialog(
                account = account,
                onDismiss = { editingAccountIndex = null },
                onSave = { updated ->
                    onUpdateAccount(index, updated)
                    editingAccountIndex = null
                }
            )
        }
    }
}

@Composable
private fun EditAccountDialog(
    account: ImportAccountItem,
    onDismiss: () -> Unit,
    onSave: (ImportAccountItem) -> Unit
) {
    var bankName by remember { mutableStateOf(account.bankName) }
    var accountName by remember { mutableStateOf(account.accountName ?: "") }
    var last4 by remember { mutableStateOf(account.accountLast4) }
    var isCreditCard by remember { mutableStateOf(account.isCreditCard) }
    var isWallet by remember { mutableStateOf(account.isWallet) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank / Institution Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Nickname (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4) last4 = it },
                    label = { Text("Account Last 4 Digits") },
                    placeholder = { Text("e.g. 4321") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCreditCard && !isWallet,
                        onClick = { isCreditCard = false; isWallet = false },
                        label = { Text("Savings") }
                    )
                    FilterChip(
                        selected = isCreditCard,
                        onClick = { isCreditCard = true; isWallet = false },
                        label = { Text("Credit Card") }
                    )
                    FilterChip(
                        selected = isWallet,
                        onClick = { isCreditCard = false; isWallet = true },
                        label = { Text("Wallet") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        account.copy(
                            bankName = bankName.ifBlank { "Account" },
                            accountName = accountName.ifBlank { bankName },
                            accountLast4 = last4.trim(),
                            isCreditCard = isCreditCard,
                            isWallet = isWallet
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
