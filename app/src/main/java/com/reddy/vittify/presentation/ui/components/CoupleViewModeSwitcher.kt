package com.reddy.vittify.presentation.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.reddy.vittify.data.sync.ViewMode
import com.reddy.vittify.presentation.ui.theme.VittifySurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoupleViewModeSwitcher(
    activeMode: ViewMode,
    partnerName: String?,
    onModeSelected: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
    userName: String? = null
) {
    val haptic = LocalView.current
    val effectiveUserName = userName?.ifBlank { null }?.takeIf { it != "User" } ?: "Me"
    val effectivePartnerName = partnerName?.ifBlank { null } ?: "Partner"
    val items = listOf(
        Triple(ViewMode.PERSONAL, effectiveUserName, Icons.Rounded.Person),
        Triple(ViewMode.COMBINED, "Both", Icons.Rounded.Favorite),
        Triple(ViewMode.PARTNER, effectivePartnerName, Icons.Rounded.Favorite)
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        items.forEachIndexed { index, (mode, label, icon) ->
            val selected = activeMode == mode
            SegmentedButton(
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onModeSelected(mode)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selected) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun CompactCoupleViewModeSwitcher(
    activeMode: ViewMode,
    partnerName: String?,
    onModeSelected: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
    userName: String? = null
) {
    val haptic = LocalView.current
    val effectiveUserName = userName?.ifBlank { null }?.takeIf { it != "User" } ?: "Me"
    val effectivePartnerName = partnerName?.ifBlank { null } ?: "Partner"
    val items = listOf(
        Pair(ViewMode.COMBINED, "Both"),
        Pair(ViewMode.PERSONAL, effectiveUserName),
        Pair(ViewMode.PARTNER, effectivePartnerName)
    )

    Surface(
        modifier = modifier.height(34.dp),
        shape = CircleShape,
        color = VittifySurface.platterCardColor(),
        border = VittifySurface.platterBorder()
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEach { (mode, label) ->
                val selected = activeMode == mode
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onModeSelected(mode)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
