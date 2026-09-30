package com.reddy.vittify.presentation.ui.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.reddy.vittify.presentation.ui.components.springPress
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.VittifySpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

enum class HomeShortcutItem(
    val key: String,
    val title: String,
    val icon: ImageVector
) {
    PROFILE("PROFILE", "Profile", Icons.Rounded.Person),
    BACKUP_SYNC("BACKUP_SYNC", "Backup", Icons.Rounded.CloudUpload),
    ACCOUNTS("ACCOUNTS", "Accounts", Icons.Rounded.AccountBalance),
    CATEGORIES("CATEGORIES", "Categories", Icons.Rounded.Category),
    BUDGETS("BUDGETS", "Budgets", Icons.Rounded.AccountBalanceWallet),
    RULES("RULES", "Rules", Icons.Rounded.Rule),
    ANALYTICS("ANALYTICS", "Analytics", Icons.Rounded.BarChart),
    SMS("SMS", "SMS Rules", Icons.Rounded.Sms),
    REPORTS("REPORTS", "Reports", Icons.Rounded.Description),
    COUPLE("COUPLE", "Couple", Icons.Rounded.Favorite);

    companion object {
        fun fromKey(key: String): HomeShortcutItem? = entries.firstOrNull { it.key == key }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeShortcutsWidget(
    activeShortcuts: Set<String>,
    onShortcutClick: (HomeShortcutItem) -> Unit,
    onUpdateShortcuts: (Set<String>) -> Unit,
    blurEffects: Boolean = false,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    var showCustomizeDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val enabledItems = remember(activeShortcuts) {
        HomeShortcutItem.entries.filter { activeShortcuts.contains(it.key) }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VittifySpacing.scaledStandard),
        shape = VittifyShapes.platter,
        color = VittifySurface.platterContainerColor(),
        border = VittifySurface.platterBorder(),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VittifySpacing.scaledStandard, vertical = VittifySpacing.scaled(14.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Shortcuts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showCustomizeDialog = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Customize Shortcuts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (enabledItems.isEmpty()) {
                Text(
                    text = "No shortcuts selected. Tap options above to add.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val minItemWidth = 68.dp
                    val minSpacing = 12.dp
                    val columns = ((maxWidth + minSpacing) / (minItemWidth + minSpacing)).toInt().coerceAtLeast(4)
                    val itemWidth = ((maxWidth - (minSpacing * (columns - 1))) / columns).coerceAtMost(80.dp)
                    val spacing = if (columns > 1) {
                        ((maxWidth - (itemWidth * columns)) / (columns - 1)).coerceAtLeast(minSpacing)
                    } else minSpacing

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        maxItemsInEachRow = columns
                    ) {
                        enabledItems.forEachIndexed { index, item ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(itemWidth)
                                    .springPress(0.92f)
                                    .clip(VittifyShapes.scaled(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onShortcutClick(item)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                            com.reddy.vittify.presentation.ui.components.SettingsShapeIconBadge(
                                icon = item.icon,
                                shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.forIndex(index),
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                size = 46.dp,
                                iconSize = 22.dp,
                                contentDescription = item.title
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

    if (showCustomizeDialog) {
        CustomizeShortcutsDialog(
            currentShortcuts = activeShortcuts,
            onDismiss = { showCustomizeDialog = false },
            onSave = {
                onUpdateShortcuts(it)
                showCustomizeDialog = false
            }
        )
    }
}

@Composable
private fun CustomizeShortcutsDialog(
    currentShortcuts: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selectedKeys by remember { mutableStateOf(currentShortcuts.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = VittifyShapes.dialog,
        title = {
            Text(
                text = "Customize Shortcuts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                items(HomeShortcutItem.entries) { item ->
                    val isChecked = selectedKeys.contains(item.key)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedKeys = if (isChecked) {
                                    selectedKeys - item.key
                                } else {
                                    selectedKeys + item.key
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedKeys = if (checked) {
                                    selectedKeys + item.key
                                } else {
                                    selectedKeys - item.key
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedKeys) }) {
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

