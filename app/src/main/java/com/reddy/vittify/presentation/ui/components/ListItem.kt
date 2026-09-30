package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.ui.theme.LocalVittifyTokens
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.VittifySpacing

enum class ListItemPosition {
    Top,
    Middle,
    Bottom,
    Single;

    companion object {
        fun from(index: Int, size: Int) =
            if (size == 1) Single
            else
                when (index) {
                    0 -> Top
                    size - 1 -> Bottom
                    else -> Middle
                }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val listTopItemShape: CornerBasedShape
    @Composable
    get() = VittifyShapes.listTop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val listMiddleItemShape: CornerBasedShape
    @Composable
    get() = VittifyShapes.listMiddle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val listBottomItemShape: CornerBasedShape
    @Composable
    get() = VittifyShapes.listBottom

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val listSingleItemShape: CornerBasedShape
    @Composable
    get() = VittifyShapes.listSingle

@Composable
fun ListItemPosition.toShape(): CornerBasedShape =
    when (this) {
        ListItemPosition.Top -> listTopItemShape
        ListItemPosition.Middle -> listMiddleItemShape
        ListItemPosition.Bottom -> listBottomItemShape
        ListItemPosition.Single -> listSingleItemShape
    }

val listItemPadding = PaddingValues(horizontal = 16.dp, vertical = 1.5.dp)

val scaledListItemPadding: PaddingValues
    @Composable get() = PaddingValues(horizontal = VittifySpacing.scaledStandard, vertical = 1.5.dp)

@Composable
fun ListItem(
        headline: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        supporting: (@Composable () -> Unit)? = null,
        leading: (@Composable () -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
        selected: Boolean = false,
        shape: CornerBasedShape? = null,
        padding: PaddingValues? = null,
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
        border: BorderStroke? = null,
        useDefaultBorder: Boolean = true,
        listColor: Color? = null,
        selectedListColor: Color? = null
) {
    val tokens = LocalVittifyTokens.current
    val effectivePadding = padding ?: scaledListItemPadding
    val effectiveShape = shape ?: if (useDefaultBorder) listSingleItemShape else null
    val effectiveListColor = listColor ?: MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = tokens.surfaceOpacity)
    val effectiveSelectedListColor = selectedListColor ?: MaterialTheme.colorScheme.primaryContainer.copy(
        alpha = (tokens.surfaceOpacity * 0.95f).coerceIn(0.4f, 1f)
    )
    val defaultBorder = if (useDefaultBorder) VittifySurface.platterBorder() else null
    val baseBorder = if (border != null) border else defaultBorder
    val currentBorder = if (baseBorder != null && selected) {
        BorderStroke(baseBorder.width.coerceAtLeast(1.dp), MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    } else {
        baseBorder
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(effectivePadding)
            .then(
                if (effectiveShape != null) {
                    Modifier
                        .clip(effectiveShape)
                        .background(
                            if (selected) effectiveSelectedListColor
                            else effectiveListColor
                        )
                        .then(
                            if (currentBorder != null) Modifier.border(currentBorder, effectiveShape)
                            else Modifier
                        )
                } else Modifier
            )
            .then(
                if (onClick != null || onLongClick != null) {
                    val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()
                    Modifier.combinedClickable(
                        role = Role.Button,
                        onClick = {
                            haptic.click()
                            onClick?.invoke()
                        },
                        onLongClick = if (onLongClick != null) {
                            {
                                haptic.longClick()
                                onLongClick()
                            }
                        } else null
                    )
                } else Modifier
            )
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = VittifySpacing.scaledStandard, vertical = VittifySpacing.scaled(14.dp))
    ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VittifySpacing.scaledCompact)
            ) {
                if (leading != null) {
                    CompositionLocalProvider(
                            LocalContentColor provides
                                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.secondary
                    ) { Box(contentAlignment = Alignment.Center) { leading() } }
                }

                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompositionLocalProvider(
                            LocalContentColor provides
                                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                    ) { ProvideTextStyle(value = MaterialTheme.typography.bodyLarge) { headline() } }

                    if (supporting != null) {
                        CompositionLocalProvider(
                                LocalContentColor provides
                                        if (selected)
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                        alpha = 0.7f
                                                )
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                                supporting()
                            }
                        }
                    }
                }

                if (trailing != null) {
                    CompositionLocalProvider(
                            LocalContentColor provides
                                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                    ) { Box(contentAlignment = Alignment.Center) { trailing() } }
                }
            }
        }
}

/**
 * Organic expressive badge geometry inspired by Material 3 Expressive.
 */
enum class ExpressiveBadgeShapeType {
    SQUIRCLE,
    CLOVER,    // 4 petals
    FLOWER,    // 8 petals
    SCALLOP    // 12 ripples
}

fun getExpressiveBadgeShape(type: ExpressiveBadgeShapeType, scale: Float = 1f): Shape = when (type) {
    ExpressiveBadgeShapeType.SQUIRCLE -> RoundedCornerShape((14f * scale).dp)
    ExpressiveBadgeShapeType.CLOVER -> GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = size.minDimension * 0.42f
        val amp = size.minDimension * 0.08f
        val steps = 72
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
            val r = baseR + amp * kotlin.math.cos(4f * angle)
            val x = cx + r * kotlin.math.cos(angle)
            val y = cy + r * kotlin.math.sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    ExpressiveBadgeShapeType.FLOWER -> GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = size.minDimension * 0.44f
        val amp = size.minDimension * 0.08f
        val steps = 96
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
            val r = baseR + amp * kotlin.math.cos(8f * angle)
            val x = cx + r * kotlin.math.cos(angle)
            val y = cy + r * kotlin.math.sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    ExpressiveBadgeShapeType.SCALLOP -> GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = size.minDimension * 0.45f
        val amp = size.minDimension * 0.04f
        val steps = 120
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
            val r = baseR + amp * kotlin.math.cos(12f * angle)
            val x = cx + r * kotlin.math.cos(angle)
            val y = cy + r * kotlin.math.sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/**
 * Organic 44dp expressive badge with vibrant tonal background and crisp icon.
 */
@Composable
fun ExpressiveIconBadge(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    shapeType: ExpressiveBadgeShapeType = ExpressiveBadgeShapeType.CLOVER,
    size: Dp = 44.dp
) {
    val scale = LocalVittifyTokens.current.cornerRadiusScale
    val shape = remember(shapeType, scale) { getExpressiveBadgeShape(shapeType, scale) }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
    }
}

/**
 * Platter-ready list item for settings and transaction lists.
 * Strictly follows GEMINI.md Rule 16 (Grouped Squircle Platter Rule) without inner card borders.
 */
@Composable
fun ExpressiveListItem(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    leadingBadge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = {
                            haptic.click()
                            onClick()
                        }
                    )
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VittifySpacing.scaledStandard, vertical = VittifySpacing.scaled(13.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VittifySpacing.scaledCompact)
        ) {
            if (leadingBadge != null) {
                leadingBadge()
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    title()
                }

                if (subtitle != null) {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        subtitle()
                    }
                }
            }

            if (trailing != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    trailing()
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (leadingBadge != null) 72.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = VittifySurface.dividerColor()
            )
        }
    }
}

/**
 * Material 3 Expressive Bottom Sheet List Item.
 * Featuring generous squircle shape, soft tonal surface, symbol badge,
 * and highlighted active selection with a primary checkmark badge.
 */
@Composable
fun VittifyBottomSheetListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingSymbol: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    position: ListItemPosition? = null,
    onClick: () -> Unit
) {
    val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()
    val tokens = LocalVittifyTokens.current
    val shape = position?.toShape() ?: VittifyShapes.input
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = (tokens.surfaceOpacity * 0.75f).coerceIn(0.4f, 0.95f))
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = (tokens.surfaceOpacity * 0.55f).coerceIn(0.3f, 0.95f))
    }
    val borderThickness = if (tokens.borderThickness > 0.dp) {
        if (selected) (tokens.borderThickness * 1.5f).coerceAtLeast(1.dp) else tokens.borderThickness
    } else {
        if (selected) 1.5.dp else 0.dp
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = (tokens.borderOpacity * 2f).coerceIn(0.4f, 1f))
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = tokens.borderOpacity)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderThickness > 0.dp) {
                    Modifier.border(width = borderThickness, color = borderColor, shape = shape)
                } else Modifier
            )
            .clickable(role = Role.RadioButton) {
                haptic.click()
                onClick()
            }
            .padding(horizontal = VittifySpacing.scaledStandard, vertical = VittifySpacing.scaled(13.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VittifySpacing.scaledCompact)
        ) {
            if (leadingSymbol != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(VittifyShapes.scaled(12.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = leadingSymbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(VittifyShapes.scaled(12.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    leadingIcon()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
