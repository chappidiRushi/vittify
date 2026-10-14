package com.reddy.vittify.presentation.ui.features.settings.importwizard.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.backup.importwizard.ImportSubscriptionItem
import com.reddy.vittify.data.backup.importwizard.RangeSelectionTracker
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.components.BrandIcon
import com.reddy.vittify.presentation.ui.components.VittifyCheckbox
import com.reddy.vittify.presentation.ui.features.settings.importwizard.components.SelectionActionBar
import com.reddy.vittify.utils.CurrencyFormatter

@Composable
fun SubscriptionsPreviewStep(
    subscriptions: List<ImportSubscriptionItem>,
    rangeTracker: RangeSelectionTracker,
    onToggleSubscription: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onApplyRange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Preview Subscriptions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Select recurring subscriptions to import.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SelectionActionBar(
            totalCount = subscriptions.size,
            selectedCount = subscriptions.count { it.isSelected },
            rangeTracker = rangeTracker,
            onSelectAll = onSelectAll,
            onDeselectAll = onDeselectAll,
            onApplyRange = onApplyRange,
            itemNameSingular = "subscription",
            itemNamePlural = "subscriptions"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.5.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(subscriptions) { index, subscription ->
                val position = com.reddy.vittify.presentation.ui.components.ListItemPosition.from(index, subscriptions.size)

                com.reddy.vittify.presentation.ui.components.ListItem(
                    shape = position.toShape(),
                    padding = PaddingValues(0.dp),
                    onClick = { onToggleSubscription(index) },
                    leading = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VittifyCheckbox(
                                checked = subscription.isSelected,
                                onCheckedChange = { onToggleSubscription(index) }
                            )

                            com.reddy.vittify.presentation.ui.components.BrandIcon(
                                merchantName = subscription.merchantName,
                                size = 38.dp,
                                showBackground = true,
                                category = subscription.categoryName
                            )
                        }
                    },
                    headline = {
                        Text(
                            text = subscription.merchantName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supporting = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = subscription.billingCycle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            if (!subscription.categoryName.isNullOrBlank()) {
                                Text(
                                    text = subscription.categoryName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    trailing = {
                        Text(
                            text = CurrencyFormatter.formatCurrency(subscription.amount, subscription.currency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }
    }
}
