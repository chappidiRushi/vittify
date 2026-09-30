package com.reddy.vittify.presentation.ui.features.settings.appearance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R
import com.reddy.vittify.data.preferences.AccentColor
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.theme.*

/**
 * Bottom Sheet for picking from all 24 curated themes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratedPalettesSheet(
    sheetState: SheetState,
    selectedAccent: AccentColor,
    isDark: Boolean,
    onAccentSelected: (AccentColor) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = rememberAppHapticFeedback()

    com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = VittifyShapes.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 32.dp,
                height = 4.dp,
                shape = CircleShape
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs)
            ) {
                Text(
                    text = "Curated Palettes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose from 24 harmonious Material 3 themes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                AccentColor.entries.forEachIndexed { index, accent ->
                    val isSelected = selectedAccent == accent
                    val position = ListItemPosition.from(index, AccentColor.entries.size)
                    val shape = position.toShape()
                    val name = getAccentThemeName(accent)
                    val primaryColor = getAccentColorForDisplay(accent, isDark)
                    val secondaryColor = getSecondaryColorForDisplay(accent, isDark)
                    val tertiaryColor = getTertiaryColorForDisplay(accent, isDark)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.click()
                                onAccentSelected(accent)
                            },
                        shape = shape,
                        color = if (isSelected) primaryColor.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .height(28.dp)
                                        .clip(VittifyShapes.scaled(6.dp)),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1.5f).fillMaxHeight().background(primaryColor))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(secondaryColor))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(tertiaryColor))
                                }
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet for configuring animated background style & intensity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundEffectsSheet(
    sheetState: SheetState,
    currentStyle: String,
    opacity: Float,
    onStyleSelected: (String) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = rememberAppHapticFeedback()

    val styles = listOf(
        Triple("WAVY", R.string.bg_style_wavy, Icons.Rounded.Waves),
        Triple("RINGS", R.string.bg_style_rings, Icons.Rounded.BlurCircular),
        Triple("PARTICLES", R.string.bg_style_particles, Icons.Rounded.Grain),
        Triple("SHOOTING_STARS", R.string.bg_style_shooting_stars, Icons.Rounded.AutoAwesome),
        Triple("SPACE", R.string.bg_style_space, Icons.Default.DarkMode),
        Triple("CONSTELLATION", R.string.bg_style_constellation, Icons.Rounded.Hub),
        Triple("RIPPLES", R.string.bg_style_ripples, Icons.Rounded.WaterDrop),
        Triple("AURORA", R.string.bg_style_aurora, Icons.Rounded.Gradient)
    )

    com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = VittifyShapes.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 32.dp,
                height = 4.dp,
                shape = CircleShape
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs)
            ) {
                Text(
                    text = "Background Effects",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.background_style_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Grouped style selector list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                styles.forEachIndexed { index, (key, labelRes, icon) ->
                    val isSelected = currentStyle == key
                    val position = ListItemPosition.from(index, styles.size)
                    val shape = position.toShape()

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.click()
                                onStyleSelected(key)
                            },
                        shape = shape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Opacity slider card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(VittifyShapes.input)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        VittifyShapes.input
                    )
                    .padding(Spacing.md)
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
                            text = stringResource(R.string.background_opacity),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = opacity,
                        onValueChange = onOpacityChanged,
                        valueRange = 0.10f..1.00f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

/**
 * Bottom Sheet for deep Surface & Geometry token tuning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurfaceGeometrySheet(
    sheetState: SheetState,
    surfaceOpacity: Float,
    borderThickness: Float,
    borderOpacity: Float,
    cornerRadiusScale: Float,
    paddingScale: Float,
    onSurfaceOpacityChange: (Float) -> Unit,
    onBorderThicknessChange: (Float) -> Unit,
    onBorderOpacityChange: (Float) -> Unit,
    onCornerRadiusScaleChange: (Float) -> Unit,
    onPaddingScaleChange: (Float) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = rememberAppHapticFeedback()

    com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = VittifyShapes.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 32.dp,
                height = 4.dp,
                shape = CircleShape
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs)
            ) {
                Text(
                    text = "Geometry & Surface Tokens",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fine-tune container opacity, squircle radii, and borders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Live preview card inside sheet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(VittifyShapes.hero)
                    .background(VittifySurface.platterCardColor())
                    .then(
                        VittifySurface.platterBorder()?.let { Modifier.border(it, VittifyShapes.hero) } ?: Modifier
                    )
                    .padding(Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Platter Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Opacity: ${(surfaceOpacity * 100).toInt()}% • Border: ${String.format(java.util.Locale.US, "%.1f", borderThickness)}dp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(VittifyShapes.button)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    ) {
                        Text(
                            text = "CTA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5 Token Controls Platter
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                // 1. Surface Opacity
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ListItemPosition.Top.toShape(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Surface Opacity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(surfaceOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = surfaceOpacity,
                            onValueChange = onSurfaceOpacityChange,
                            valueRange = 0.40f..1.00f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 2. Border Thickness
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ListItemPosition.Middle.toShape(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Border Thickness",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (borderThickness <= 0f) "None" else "${String.format(java.util.Locale.US, "%.1f", borderThickness)} dp",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val thicknessPresets = listOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            thicknessPresets.forEach { thickness ->
                                val isSelected = kotlin.math.abs(borderThickness - thickness) < 0.1f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.click()
                                        onBorderThicknessChange(thickness)
                                    },
                                    label = {
                                        Text(
                                            text = if (thickness == 0f) "None" else "${thickness}dp",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Border Opacity
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ListItemPosition.Middle.toShape(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Border Opacity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(borderOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = borderOpacity,
                            onValueChange = onBorderOpacityChange,
                            valueRange = 0.00f..0.50f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. Corner Radius
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ListItemPosition.Middle.toShape(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Corner Radius Scale",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (cornerRadiusScale <= 0.02f) "0 (Sharp)" else "${String.format(java.util.Locale.US, "%.2f", cornerRadiusScale)}x",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val cornerPresets = listOf(
                            "Sharp" to 0.00f,
                            "Compact" to 0.75f,
                            "Standard" to 1.00f,
                            "Expressive" to 1.25f,
                            "Playful" to 1.50f
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            cornerPresets.forEach { (label, scale) ->
                                val isSelected = kotlin.math.abs(cornerRadiusScale - scale) < 0.08f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.click()
                                        onCornerRadiusScaleChange(scale)
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                        Slider(
                            value = cornerRadiusScale,
                            onValueChange = onCornerRadiusScaleChange,
                            valueRange = 0.00f..1.50f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 5. Padding Scale
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ListItemPosition.Bottom.toShape(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Padding Scale",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", paddingScale)}x",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val paddingPresets = listOf(
                            "Compact" to 0.75f,
                            "Standard" to 1.00f,
                            "Relaxed" to 1.25f,
                            "Spacious" to 1.50f
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            paddingPresets.forEach { (label, scale) ->
                                val isSelected = kotlin.math.abs(paddingScale - scale) < 0.08f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.click()
                                        onPaddingScaleChange(scale)
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                        Slider(
                            value = paddingScale,
                            onValueChange = onPaddingScaleChange,
                            valueRange = 0.00f..1.75f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Reset Button
            Button(
                onClick = {
                    haptic.click()
                    onResetDefaults()
                }, 
                modifier = Modifier
                    .fillMaxWidth()
                    .springPress(0.96f),
                shape = VittifyShapes.button,
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(text = "Reset Geometry to Default")
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

