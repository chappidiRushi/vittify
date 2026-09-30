package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.utils.CurrencyFormatter

@Composable
fun AccountSelectorRow(
    accounts: List<AccountBalanceEntity>,
    selectedAccount: AccountBalanceEntity?,
    onAccountSelected: (AccountBalanceEntity) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!title.isNullOrEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (accounts.isEmpty()) {
            Text(
                text = "No available accounts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accounts, key = { it.id }) { account ->
                    val isSelected = selectedAccount?.id == account.id
                    Surface(
                        onClick = { onAccountSelected(account) },
                        shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.input,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else com.reddy.vittify.presentation.ui.theme.VittifySurface.surfaceContainerLowColor(),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else com.reddy.vittify.presentation.ui.theme.VittifySurface.platterBorder(),
                        modifier = Modifier.width(160.dp)
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
            }
        }
    }
}
