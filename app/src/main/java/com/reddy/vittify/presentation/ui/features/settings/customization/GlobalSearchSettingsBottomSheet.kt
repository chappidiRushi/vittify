package com.reddy.vittify.presentation.ui.features.settings.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.globalsearch.GlobalSearchViewModel
import com.reddy.vittify.presentation.ui.icons.Box2
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.ReceiptItem
import com.reddy.vittify.presentation.ui.icons.Setting2
import com.reddy.vittify.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSettingsBottomSheet(
    viewModel: GlobalSearchViewModel,
    onDismiss: () -> Unit
) {
    val preferences by viewModel.preferences.collectAsState()
    val haptic = rememberAppHapticFeedback()

    com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimensions.Padding.content)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.global_search_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.global_search_settings_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Toggles Group
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                PreferenceSwitch(
                    title = stringResource(R.string.search_accounts),
                    subtitle = stringResource(R.string.search_accounts_desc),
                    checked = preferences.includeAccounts,
                    onCheckedChange = { viewModel.toggleAccounts(it) },
                    isFirst = true,
                    padding = PaddingValues(horizontal = 0.dp, vertical = 1.5.dp),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(red_light),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = red_dark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                PreferenceSwitch(
                    title = stringResource(R.string.search_settings),
                    subtitle = stringResource(R.string.search_settings_desc),
                    checked = preferences.includeSettings,
                    onCheckedChange = { viewModel.toggleSettings(it) },
                    padding = PaddingValues(horizontal = 0.dp, vertical = 1.5.dp),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(blue_light),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Iconax.Setting2,
                                contentDescription = null,
                                tint = blue_dark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                PreferenceSwitch(
                    title = stringResource(R.string.global_search_transactions),
                    subtitle = stringResource(R.string.search_transactions_desc),
                    checked = preferences.includeTransactions,
                    onCheckedChange = { viewModel.toggleTransactions(it) },
                    padding = PaddingValues(horizontal = 0.dp, vertical = 1.5.dp),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(green_light),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Iconax.ReceiptItem,
                                contentDescription = null,
                                tint = green_dark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                PreferenceSwitch(
                    title = stringResource(R.string.search_pages),
                    subtitle = stringResource(R.string.search_pages_desc),
                    checked = preferences.includePages,
                    onCheckedChange = { viewModel.togglePages(it) },
                    isLast = true,
                    padding = PaddingValues(horizontal = 0.dp, vertical = 1.5.dp),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(purple_light),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Iconax.Box2,
                                contentDescription = null,
                                tint = purple_dark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            // Results per type stepper
            ListItem(
                headline = {
                    Text(
                        text = stringResource(R.string.results_per_type),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                },
                supporting = {
                    Text(
                        text = stringResource(R.string.results_per_type_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leading = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(orange_light),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = orange_dark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (preferences.resultsPerType > 1) {
                                    haptic.click()
                                    viewModel.updateResultsPerType(preferences.resultsPerType - 1)
                                }
                            },
                            enabled = preferences.resultsPerType > 1,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (preferences.resultsPerType > 1)
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Surface(
                            shape = VittifyShapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = preferences.resultsPerType.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (preferences.resultsPerType < 10) {
                                    haptic.click()
                                    viewModel.updateResultsPerType(preferences.resultsPerType + 1)
                                }
                            },
                            enabled = preferences.resultsPerType < 10,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (preferences.resultsPerType < 10)
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = ListItemPosition.Single.toShape(),
                padding = PaddingValues(0.dp)
            )
        }
    }
}
