package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.presentation.ui.theme.Dimensions

@Composable
fun GenericTypeSwitcher(
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier,
    badgeCounts: List<Int>? = null,
    selectedColors: List<androidx.compose.ui.graphics.Color>? = null
) {
    val themeColors = MaterialTheme.colorScheme

    val containerShape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(Dimensions.Radius.md)
    val indicatorShape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(12.dp)

    BoxWithConstraints(
        modifier = modifier
            .height(48.dp)
            .clip(containerShape)
            .background(themeColors.surfaceContainerHigh)
            .then(
                com.reddy.vittify.presentation.ui.theme.VittifySurface.platterBorder()?.let { Modifier.border(it, containerShape) }
                    ?: Modifier.border(
                        BorderStroke(1.dp, themeColors.outlineVariant.copy(alpha = 0.20f)),
                        containerShape
                    )
            )
            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val indicatorWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = indicatorWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            ),
            label = "Indicator offset"
        )

        val targetIndicatorColor = selectedColors?.getOrNull(selectedIndex)?.copy(alpha = 0.22f)
            ?: themeColors.primaryContainer
        val indicatorColor by animateColorAsState(
            targetValue = targetIndicatorColor,
            animationSpec = spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            ),
            label = "Indicator color"
        )

        // Animated Indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .fillMaxHeight()
                .clip(indicatorShape)
                .background(indicatorColor)
                .border(
                    BorderStroke(1.dp, themeColors.outlineVariant.copy(alpha = 0.15f)),
                    indicatorShape
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()
            options.forEachIndexed { index, text ->
                TypeButton(
                    text = text,
                    isSelected = selectedIndex == index,
                    count = badgeCounts?.getOrNull(index),
                    selectedColor = selectedColors?.getOrNull(index),
                    compact = options.size > 2,
                    onClick = {
                        haptic.toggle()
                        onIndexChange(index)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TypeButton(
    text: String,
    isSelected: Boolean,
    count: Int? = null,
    selectedColor: androidx.compose.ui.graphics.Color? = null,
    compact: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Text(
                text = text,
                color = if (isSelected) {
                    selectedColor ?: MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = if (count != null && count > 0) {
                    if (compact) 11.sp else 13.sp
                } else {
                    if (compact) 13.5.sp else 14.sp
                },
                maxLines = 1
            )
            if (count != null && count > 0) {
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .clip(com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(6.dp))
                        .background(
                            if (isSelected) {
                                (selectedColor ?: MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            selectedColor ?: MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
