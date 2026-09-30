package com.reddy.vittify.presentation.ui.features.settings.archived

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDeleteSettingsBottomSheet(
    sheetState: SheetState,
    initialEnabled: Boolean,
    initialDays: Int,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, days: Int) -> Unit
) {
    var enabled by remember { mutableStateOf(initialEnabled) }
    var selectedDays by remember { mutableIntStateOf(initialDays) }
    val haptic = rememberAppHapticFeedback()

    val options = listOf(
        7 to stringResource(R.string.days_7),
        14 to stringResource(R.string.days_14),
        30 to stringResource(R.string.days_30),
        60 to stringResource(R.string.days_60),
        90 to stringResource(R.string.days_90)
    )

    com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimensions.Padding.content)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.archived_auto_delete_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.archived_auto_delete_settings_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Switch Item
            PreferenceSwitch(
                title = stringResource(R.string.enable_auto_delete),
                subtitle = if (enabled) {
                    stringResource(R.string.auto_delete_banner_desc, selectedDays)
                } else {
                    stringResource(R.string.auto_delete_banner_off_desc)
                },
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                },
                isSingle = true,
                padding = PaddingValues(0.dp)
            )

            // Retention Period Section
            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SectionHeader(
                        title = stringResource(R.string.retention_period),
                        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs)
                    )

                    options.forEachIndexed { index, (days, label) ->
                        val position = when {
                            options.size == 1 -> ListItemPosition.Single
                            index == 0 -> ListItemPosition.Top
                            index == options.lastIndex -> ListItemPosition.Bottom
                            else -> ListItemPosition.Middle
                        }

                        ListItem(
                            headline = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selectedDays == days) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            trailing = {
                                RadioButton(
                                    selected = selectedDays == days,
                                    onClick = {
                                        haptic.click()
                                        selectedDays = days
                                    }
                                )
                            },
                            onClick = {
                                haptic.click()
                                selectedDays = days
                            },
                            shape = position.toShape(),
                            padding = PaddingValues(0.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Button(
                onClick = {
                    haptic.click()
                    onSave(enabled, selectedDays)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(Spacing.sm))
                Text(
                    text = stringResource(R.string.done),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

