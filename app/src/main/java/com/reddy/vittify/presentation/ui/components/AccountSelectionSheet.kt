package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.utils.CurrencyFormatter

@Composable
fun AccountSelectionSheet(
    accounts: List<AccountBalanceEntity>,
    selectedAccount: AccountBalanceEntity?,
    title: String = "Select Account",
    onAccountSelected: (AccountBalanceEntity?) -> Unit,
    isTransitioning: Boolean = false,
    showNoneOption: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth()
        )

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No accounts found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = !isTransitioning
            ) {
                if (showNoneOption) {
                    item(span = { GridItemSpan(2) }) {
                        val isSelected = selectedAccount == null
                        Surface(
                            onClick = { onAccountSelected(null) },
                            shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.input,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else com.reddy.vittify.presentation.ui.theme.VittifySurface.surfaceContainerLowColor(),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else com.reddy.vittify.presentation.ui.theme.VittifySurface.platterBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "None (Manual Entry)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                items(accounts, key = { it.id }) { account ->
                    val isSelected = selectedAccount?.id == account.id
                    Surface(
                        onClick = { onAccountSelected(account) },
                        shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.input,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else com.reddy.vittify.presentation.ui.theme.VittifySurface.surfaceContainerLowColor(),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else com.reddy.vittify.presentation.ui.theme.VittifySurface.platterBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = account.bankName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (account.isWallet || account.accountLast4.equals("wallet", ignoreCase = true)) "Wallet" else "**** ${account.accountLast4}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.formatCurrency(account.balance, account.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
