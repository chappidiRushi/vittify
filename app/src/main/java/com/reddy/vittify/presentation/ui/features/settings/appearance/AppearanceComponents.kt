package com.reddy.vittify.presentation.ui.features.settings.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.R
import com.reddy.vittify.data.preferences.AccentColor
import com.reddy.vittify.data.preferences.AppFont
import com.reddy.vittify.data.preferences.AppIcon
import com.reddy.vittify.presentation.ui.theme.*

/**
 * Spring specifications matching homepage bank cards overshoot & bouncy physics.
 */
object AppearanceSprings {
    val BankCardBouncyFloat = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = 340f
    )
    val BankCardBouncyDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.62f,
        stiffness = 340f
    )
    val PressSpringFloat = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = 380f
    )
}

/**
 * Expressive tactile spring press micro-interaction modifier.
 * Mimics bank cards: compresses on press with bouncy spring,
 * springs back on release with juicy overshoot and light haptic feedback.
 */
@Composable
fun Modifier.springPress(
    targetScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = AppearanceSprings.PressSpringFloat,
        label = "spring_press_scale"
    )
    val haptic = rememberAppHapticFeedback()

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.click()
                        onClick()
                    }
                )
            } else Modifier
        )
}

/**
 * Clean grouped platter container following Material 3 Expressive squircle geometry.
 */
@Composable
fun AppearancePlatter(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.sm),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (title != null) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = 2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(VittifyShapes.platter)
                .background(VittifySurface.platterContainerColor())
                .then(
                    VittifySurface.platterBorder()?.let { Modifier.border(it, VittifyShapes.platter) }
                        ?: Modifier.border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                            VittifyShapes.platter
                        )
                )
                .animateContentSize(
                    spring(
                        dampingRatio = 0.68f,
                        stiffness = 400f
                    )
                )
                .padding(contentPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = verticalArrangement
            ) {
                content()
            }
        }
    }
}

/**
 * Top Segmented Tab Bar for switching Appearance categories.
 * Features a real sliding squircle indicator with refined spring bounce and tactile press.
 */
@Composable
fun AppearanceSegmentedTabBar(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberAppHapticFeedback()
    val trackShape = VittifyShapes.input
    val indicatorShape = RoundedCornerShape(12.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                VittifySurface.platterBorder()?.let { Modifier.border(it, trackShape) }
                    ?: Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        trackShape
                    )
            )
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val tabCount = tabs.size
        val tabWidth = totalWidth / tabCount

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab,
            animationSpec = AppearanceSprings.BankCardBouncyDp,
            label = "tab_indicator_offset"
        )

        // Sliding indicator squircle
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(42.dp)
                .clip(indicatorShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        // Interactive tab buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "tab_content_$index"
                )
                val tabTextScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.03f else 1.0f,
                    animationSpec = AppearanceSprings.BankCardBouncyFloat,
                    label = "tab_text_scale_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(indicatorShape)
                        .springPress(targetScale = 0.96f) {
                            if (!isSelected) {
                                haptic.click()
                                onTabSelected(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            scaleX = tabTextScale
                            scaleY = tabTextScale
                        }
                    )
                }
            }
        }
    }
}

/**
 * 3-Way Segmented Button Row (System | Light | Dark).
 * Features a sliding bouncy container and squircle geometry matching settings pages.
 */
@Composable
fun ThemeModeSegmentedControl(
    selectedMode: Boolean?,
    onModeSelected: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberAppHapticFeedback()
    val trackShape = VittifyShapes.input
    val indicatorShape = RoundedCornerShape(12.dp)

    val options = listOf(
        ThemeModeOption(null, stringResource(R.string.theme_system), Icons.Filled.AutoAwesome),
        ThemeModeOption(false, stringResource(R.string.theme_light), Icons.Filled.LightMode),
        ThemeModeOption(true, stringResource(R.string.theme_dark), Icons.Filled.DarkMode)
    )

    val selectedIndex = when (selectedMode) {
        null -> 0
        false -> 1
        true -> 2
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                VittifySurface.platterBorder()?.let { Modifier.border(it, trackShape) }
                    ?: Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        trackShape
                    )
            )
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / options.size

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = AppearanceSprings.BankCardBouncyDp,
            label = "theme_mode_indicator"
        )

        // Sliding indicator squircle
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(44.dp)
                .clip(indicatorShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                    indicatorShape
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index

                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "theme_mode_tint_${option.label}"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1.0f,
                    animationSpec = AppearanceSprings.BankCardBouncyFloat,
                    label = "theme_mode_icon_scale_${option.label}"
                )
                val iconRotation by animateFloatAsState(
                    targetValue = if (isSelected) 0f else -10f,
                    animationSpec = AppearanceSprings.BankCardBouncyFloat,
                    label = "theme_mode_icon_rot_${option.label}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(indicatorShape)
                        .springPress(targetScale = 0.96f) {
                            if (!isSelected) {
                                haptic.click()
                                onModeSelected(option.mode)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                    rotationZ = iconRotation
                                }
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tintColor
                        )
                    }
                }
            }
        }
    }
}

data class ThemeModeOption(
    val mode: Boolean?,
    val label: String,
    val icon: ImageVector
)

/**
 * 2-Way Squircle Switch (e.g. Dynamic vs Curated or Floating vs Standard).
 * Features a sliding indicator squircle with refined spring tactile feel.
 */
@Composable
fun TwoWaySegmentedPill(
    firstOption: String,
    secondOption: String,
    isFirstSelected: Boolean,
    onFirstSelected: () -> Unit,
    onSecondSelected: () -> Unit,
    modifier: Modifier = Modifier,
    firstIcon: ImageVector? = null,
    secondIcon: ImageVector? = null
) {
    val haptic = rememberAppHapticFeedback()
    val trackShape = VittifyShapes.input
    val indicatorShape = RoundedCornerShape(12.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                VittifySurface.platterBorder()?.let { Modifier.border(it, trackShape) }
                    ?: Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        trackShape
                    )
            )
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / 2

        val indicatorOffset by animateDpAsState(
            targetValue = if (isFirstSelected) 0.dp else tabWidth,
            animationSpec = AppearanceSprings.BankCardBouncyDp,
            label = "twoway_indicator"
        )

        // Sliding indicator squircle
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(42.dp)
                .clip(indicatorShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                    indicatorShape
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            // First option
            val firstContent by animateColorAsState(
                targetValue = if (isFirstSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "twoway_first_content"
            )
            val firstScale by animateFloatAsState(
                targetValue = if (isFirstSelected) 1.03f else 1.0f,
                animationSpec = AppearanceSprings.BankCardBouncyFloat,
                label = "twoway_first_scale"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(indicatorShape)
                    .springPress(targetScale = 0.96f) {
                        if (!isFirstSelected) {
                            haptic.click()
                            onFirstSelected()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        scaleX = firstScale
                        scaleY = firstScale
                    }
                ) {
                    if (firstIcon != null) {
                        Icon(firstIcon, null, modifier = Modifier.size(16.dp), tint = firstContent)
                    }
                    Text(
                        text = firstOption,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isFirstSelected) FontWeight.Bold else FontWeight.Medium,
                        color = firstContent
                    )
                }
            }

            // Second option
            val secondContent by animateColorAsState(
                targetValue = if (!isFirstSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "twoway_second_content"
            )
            val secondScale by animateFloatAsState(
                targetValue = if (!isFirstSelected) 1.03f else 1.0f,
                animationSpec = AppearanceSprings.BankCardBouncyFloat,
                label = "twoway_second_scale"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(indicatorShape)
                    .springPress(targetScale = 0.96f) {
                        if (isFirstSelected) {
                            haptic.click()
                            onSecondSelected()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        scaleX = secondScale
                        scaleY = secondScale
                    }
                ) {
                    if (secondIcon != null) {
                        Icon(secondIcon, null, modifier = Modifier.size(16.dp), tint = secondContent)
                    }
                    Text(
                        text = secondOption,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (!isFirstSelected) FontWeight.Bold else FontWeight.Medium,
                        color = secondContent
                    )
                }
            }
        }
    }
}

/**
 * Refined Curated Palette Card for the grid and horizontal preview.
 * Includes bank-card bouncy spring scale on selection and animated check badge.
 */
@Composable
fun CuratedPaletteCard(
    accent: AccentColor,
    isDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = getAccentThemeName(accent)
    val color = getAccentColorForDisplay(accent, isDark)
    val secondary = getSecondaryColorForDisplay(accent, isDark)
    val tertiary = getTertiaryColorForDisplay(accent, isDark)

    val shape = VittifyShapes.input
    val defaultBorder = VittifySurface.platterBorder()
        ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    val border = if (isSelected) BorderStroke(2.dp, color) else defaultBorder
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = AppearanceSprings.BankCardBouncyFloat,
        label = "palette_card_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(shape)
            .background(
                if (isSelected) color.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .then(
                Modifier.border(border, shape)
            )
            .springPress(targetScale = 0.94f) {
                onClick()
            }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(
                        initialScale = 0f,
                        animationSpec = AppearanceSprings.BankCardBouncyFloat
                    ) + fadeIn(),
                    exit = scaleOut(
                        targetScale = 0f,
                        animationSpec = AppearanceSprings.PressSpringFloat
                    ) + fadeOut()
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Dual tonal swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(secondary)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(tertiary)
                )
            }
        }
    }
}

/**
 * App Logo Squircle Option with bank-card spring bounce.
 */
@Composable
fun AppLogoSquircleItem(
    name: String,
    drawableResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoOuterShape = VittifyShapes.hero
    val logoInnerShape = VittifyShapes.input
    val logoScale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
        animationSpec = AppearanceSprings.BankCardBouncyFloat,
        label = "logo_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                }
                .clip(logoOuterShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, logoOuterShape)
                    else {
                        val stroke = VittifySurface.platterBorder()
                            ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        Modifier.border(stroke, logoOuterShape)
                    }
                )
                .springPress(targetScale = 0.90f) {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = drawableResId),
                contentDescription = name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(logoInnerShape)
            )
        }

        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Typography Option Card with bank card bouncy spring.
 */
@Composable
fun FontOptionCard(
    font: AppFont,
    title: String,
    subtitle: String,
    fontFamily: FontFamily,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = VittifyShapes.input
    val defaultBorder = VittifySurface.platterBorder()
        ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else defaultBorder
    val fontScale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1.0f,
        animationSpec = AppearanceSprings.BankCardBouncyFloat,
        label = "font_scale"
    )

    Box(
        modifier = modifier
            .width(108.dp)
            .height(84.dp)
            .graphicsLayer {
                scaleX = fontScale
                scaleY = fontScale
            }
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = VittifySurface.platterAlpha)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .then(
                Modifier.border(border, shape)
            )
            .springPress(targetScale = 0.93f) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(Spacing.xs)
        ) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = fontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class FontOptionItem(
    val font: AppFont,
    val nameRes: Int,
    val subRes: Int,
    val fontFamily: FontFamily
)

// Accent Color Theme Resolvers
@Composable
fun getAccentColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSEWATER -> Macchiato_Rosewater_dim
            AccentColor.FLAMINGO -> Macchiato_Flamingo_dim
            AccentColor.PINK -> Macchiato_Pink_dim
            AccentColor.MAUVE -> Macchiato_Mauve_dim
            AccentColor.RED -> Macchiato_Red_dim
            AccentColor.PEACH -> Macchiato_Peach_dim
            AccentColor.YELLOW -> Macchiato_Yellow_dim
            AccentColor.GREEN -> Macchiato_Green_dim
            AccentColor.TEAL -> Macchiato_Teal_dim
            AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim
            AccentColor.BLUE -> Macchiato_Blue_dim
            AccentColor.LAVENDER -> Macchiato_Lavender_dim
            AccentColor.PINE_ROSE -> RosePine_Rose
            AccentColor.PINE_IRIS -> RosePine_Iris
            AccentColor.PINE_PINE -> RosePine_Pine
            AccentColor.PINE_GOLD -> RosePine_Gold
            AccentColor.PINE_LOVE -> RosePine_Love
            AccentColor.PINE_FOAM -> RosePine_Foam
            AccentColor.PINE_MUTED -> RosePine_Muted
            AccentColor.PINE_SUBTLE -> RosePine_Subtle
            AccentColor.PINE_TEXT -> RosePine_Text
            AccentColor.PINE_HIGHLIGHT -> RosePine_Highlight
            AccentColor.PINE_SURFACE -> RosePine_Surface
            AccentColor.PINE_OVERLAY -> RosePine_Overlay
        }
    } else {
        when (accent) {
            AccentColor.ROSEWATER -> Latte_Rosewater
            AccentColor.FLAMINGO -> Latte_Flamingo
            AccentColor.PINK -> Latte_Pink
            AccentColor.MAUVE -> Latte_Mauve
            AccentColor.RED -> Latte_Red
            AccentColor.PEACH -> Latte_Peach
            AccentColor.YELLOW -> Latte_Yellow
            AccentColor.GREEN -> Latte_Green
            AccentColor.TEAL -> Latte_Teal
            AccentColor.SAPPHIRE -> Latte_Sapphire
            AccentColor.BLUE -> Latte_Blue
            AccentColor.LAVENDER -> Latte_Lavender
            AccentColor.PINE_ROSE -> Dawn_Rose
            AccentColor.PINE_IRIS -> Dawn_Iris
            AccentColor.PINE_PINE -> Dawn_Pine
            AccentColor.PINE_GOLD -> Dawn_Gold
            AccentColor.PINE_LOVE -> Dawn_Love
            AccentColor.PINE_FOAM -> Dawn_Foam
            AccentColor.PINE_MUTED -> Dawn_Muted
            AccentColor.PINE_SUBTLE -> Dawn_Subtle
            AccentColor.PINE_TEXT -> Dawn_Text
            AccentColor.PINE_HIGHLIGHT -> Dawn_Highlight
            AccentColor.PINE_SURFACE -> Dawn_Surface
            AccentColor.PINE_OVERLAY -> Dawn_Overlay
        }
    }
}

@Composable
fun getSecondaryColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSEWATER -> Macchiato_Rosewater_dim_secondary
            AccentColor.FLAMINGO -> Macchiato_Flamingo_dim_secondary
            AccentColor.PINK -> Macchiato_Pink_dim_secondary
            AccentColor.MAUVE -> Macchiato_Mauve_dim_secondary
            AccentColor.RED -> Macchiato_Red_dim_secondary
            AccentColor.PEACH -> Macchiato_Peach_dim_secondary
            AccentColor.YELLOW -> Macchiato_Yellow_dim_secondary
            AccentColor.GREEN -> Macchiato_Green_dim_secondary
            AccentColor.TEAL -> Macchiato_Teal_dim_secondary
            AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim_secondary
            AccentColor.BLUE -> Macchiato_Blue_dim_secondary
            AccentColor.LAVENDER -> Macchiato_Lavender_dim_secondary
            AccentColor.PINE_ROSE -> RosePine_Rose_secondary
            AccentColor.PINE_IRIS -> RosePine_Iris_secondary
            AccentColor.PINE_PINE -> RosePine_Pine_secondary
            AccentColor.PINE_GOLD -> RosePine_Gold_secondary
            AccentColor.PINE_LOVE -> RosePine_Love_secondary
            AccentColor.PINE_FOAM -> RosePine_Foam_secondary
            AccentColor.PINE_MUTED -> RosePine_Muted_secondary
            AccentColor.PINE_SUBTLE -> RosePine_Subtle_secondary
            AccentColor.PINE_TEXT -> RosePine_Text_secondary
            AccentColor.PINE_HIGHLIGHT -> RosePine_Highlight_secondary
            AccentColor.PINE_SURFACE -> RosePine_Surface_secondary
            AccentColor.PINE_OVERLAY -> RosePine_Overlay_secondary
        }
    } else {
        when (accent) {
            AccentColor.ROSEWATER -> Latte_Rosewater_secondary
            AccentColor.FLAMINGO -> Latte_Flamingo_secondary
            AccentColor.PINK -> Latte_Pink_secondary
            AccentColor.MAUVE -> Latte_Mauve_secondary
            AccentColor.RED -> Latte_Red_secondary
            AccentColor.PEACH -> Latte_Peach_secondary
            AccentColor.YELLOW -> Latte_Yellow_secondary
            AccentColor.GREEN -> Latte_Green_secondary
            AccentColor.TEAL -> Latte_Teal_secondary
            AccentColor.SAPPHIRE -> Latte_Sapphire_secondary
            AccentColor.BLUE -> Latte_Blue_secondary
            AccentColor.LAVENDER -> Latte_Lavender_secondary
            AccentColor.PINE_ROSE -> Dawn_Rose_secondary
            AccentColor.PINE_IRIS -> Dawn_Iris_secondary
            AccentColor.PINE_PINE -> Dawn_Pine_secondary
            AccentColor.PINE_GOLD -> Dawn_Gold_secondary
            AccentColor.PINE_LOVE -> Dawn_Love_secondary
            AccentColor.PINE_FOAM -> Dawn_Foam_secondary
            AccentColor.PINE_MUTED -> Dawn_Muted_secondary
            AccentColor.PINE_SUBTLE -> Dawn_Subtle_secondary
            AccentColor.PINE_TEXT -> Dawn_Text_secondary
            AccentColor.PINE_HIGHLIGHT -> Dawn_Highlight_secondary
            AccentColor.PINE_SURFACE -> Dawn_Surface_secondary
            AccentColor.PINE_OVERLAY -> Dawn_Overlay_secondary
        }
    }
}

@Composable
fun getTertiaryColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSEWATER -> Macchiato_Rosewater_dim_tertiary
            AccentColor.FLAMINGO -> Macchiato_Flamingo_dim_tertiary
            AccentColor.PINK -> Macchiato_Pink_dim_tertiary
            AccentColor.MAUVE -> Macchiato_Mauve_dim_tertiary
            AccentColor.RED -> Macchiato_Red_dim_tertiary
            AccentColor.PEACH -> Macchiato_Peach_dim_tertiary
            AccentColor.YELLOW -> Macchiato_Yellow_dim_tertiary
            AccentColor.GREEN -> Macchiato_Green_dim_tertiary
            AccentColor.TEAL -> Macchiato_Teal_dim_tertiary
            AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim_tertiary
            AccentColor.BLUE -> Macchiato_Blue_dim_tertiary
            AccentColor.LAVENDER -> Macchiato_Lavender_dim_tertiary
            AccentColor.PINE_ROSE -> RosePine_Rose_tertiary
            AccentColor.PINE_IRIS -> RosePine_Iris_tertiary
            AccentColor.PINE_PINE -> RosePine_Pine_tertiary
            AccentColor.PINE_GOLD -> RosePine_Gold_tertiary
            AccentColor.PINE_LOVE -> RosePine_Love_tertiary
            AccentColor.PINE_FOAM -> RosePine_Foam_tertiary
            AccentColor.PINE_MUTED -> RosePine_Muted_tertiary
            AccentColor.PINE_SUBTLE -> RosePine_Subtle_tertiary
            AccentColor.PINE_TEXT -> RosePine_Text_tertiary
            AccentColor.PINE_HIGHLIGHT -> RosePine_Highlight_tertiary
            AccentColor.PINE_SURFACE -> RosePine_Surface_tertiary
            AccentColor.PINE_OVERLAY -> RosePine_Overlay_tertiary
        }
    } else {
        when (accent) {
            AccentColor.ROSEWATER -> Latte_Rosewater_tertiary
            AccentColor.FLAMINGO -> Latte_Flamingo_tertiary
            AccentColor.PINK -> Latte_Pink_tertiary
            AccentColor.MAUVE -> Latte_Mauve_tertiary
            AccentColor.RED -> Latte_Red_tertiary
            AccentColor.PEACH -> Latte_Peach_tertiary
            AccentColor.YELLOW -> Latte_Yellow_tertiary
            AccentColor.GREEN -> Latte_Green_tertiary
            AccentColor.TEAL -> Latte_Teal_tertiary
            AccentColor.SAPPHIRE -> Latte_Sapphire_tertiary
            AccentColor.BLUE -> Latte_Blue_tertiary
            AccentColor.LAVENDER -> Latte_Lavender_tertiary
            AccentColor.PINE_ROSE -> Dawn_Rose_tertiary
            AccentColor.PINE_IRIS -> Dawn_Iris_tertiary
            AccentColor.PINE_PINE -> Dawn_Pine_tertiary
            AccentColor.PINE_GOLD -> Dawn_Gold_tertiary
            AccentColor.PINE_LOVE -> Dawn_Love_tertiary
            AccentColor.PINE_FOAM -> Dawn_Foam_tertiary
            AccentColor.PINE_MUTED -> Dawn_Muted_tertiary
            AccentColor.PINE_SUBTLE -> Dawn_Subtle_tertiary
            AccentColor.PINE_TEXT -> Dawn_Text_tertiary
            AccentColor.PINE_HIGHLIGHT -> Dawn_Highlight_tertiary
            AccentColor.PINE_SURFACE -> Dawn_Surface_tertiary
            AccentColor.PINE_OVERLAY -> Dawn_Overlay_tertiary
        }
    }
}

fun getAccentThemeName(accent: AccentColor): String = when (accent) {
    AccentColor.ROSEWATER -> "Rosewater"
    AccentColor.FLAMINGO -> "Flamingo"
    AccentColor.PINK -> "Pink"
    AccentColor.MAUVE -> "Mauve"
    AccentColor.RED -> "Red"
    AccentColor.PEACH -> "Peach"
    AccentColor.YELLOW -> "Yellow"
    AccentColor.GREEN -> "Green"
    AccentColor.TEAL -> "Teal"
    AccentColor.SAPPHIRE -> "Sapphire"
    AccentColor.BLUE -> "Blue"
    AccentColor.LAVENDER -> "Lavender"
    AccentColor.PINE_ROSE -> "Rosé"
    AccentColor.PINE_IRIS -> "Iris"
    AccentColor.PINE_PINE -> "Pine"
    AccentColor.PINE_GOLD -> "Gold"
    AccentColor.PINE_LOVE -> "Love"
    AccentColor.PINE_FOAM -> "Foam"
    AccentColor.PINE_MUTED -> "Muted"
    AccentColor.PINE_SUBTLE -> "Subtle"
    AccentColor.PINE_TEXT -> "Text"
    AccentColor.PINE_HIGHLIGHT -> "Highlight"
    AccentColor.PINE_SURFACE -> "Surface"
    AccentColor.PINE_OVERLAY -> "Overlay"
}
