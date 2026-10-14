package com.reddy.vittify.presentation.ui.features.settings.importwizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R
import com.reddy.vittify.data.backup.importwizard.RangeSelectionTracker
import com.reddy.vittify.presentation.ui.theme.VittifySurface

@Composable
fun SelectionActionBar(
    totalCount: Int,
    selectedCount: Int,
    rangeTracker: RangeSelectionTracker,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onApplyRange: () -> Unit,
    modifier: Modifier = Modifier,
    itemNameSingular: String = "item",
    itemNamePlural: String = "items"
) {
    val allSelected = selectedCount == totalCount && totalCount > 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = VittifySurface.surfaceContainerHighColor(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.items_selected_format, selectedCount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (allSelected) onDeselectAll() else onSelectAll()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                            contentDescription = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (rangeTracker.isRangeReady) {
                        FilterChip(
                            selected = true,
                            onClick = onApplyRange,
                            label = {
                                Text(
                                    "Select Range (${rangeTracker.rangeCount})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            if (!rangeTracker.isRangeReady) {
                Text(
                    text = if (rangeTracker.firstIndex != null)
                        "1 item picked for range. Tap another item to select range."
                    else
                        "Tip: Select any 2 items to quickly select a range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
