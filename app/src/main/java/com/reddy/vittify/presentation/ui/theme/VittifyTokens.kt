package com.reddy.vittify.presentation.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dynamic Material 3 Expressive centralized surface and geometry tokens for Vittify.
 * Adjustments in Settings > Appearance immediately update all components consuming these tokens.
 */
data class VittifySurfaceTokens(
    val surfaceOpacity: Float = 0.85f,
    val borderThickness: Dp = 0.dp,
    val borderOpacity: Float = 0.15f,
    val cornerRadiusScale: Float = 1.0f,
    val paddingScale: Float = 1.0f
)

val LocalVittifyTokens = compositionLocalOf { VittifySurfaceTokens() }

/**
 * Material 3 Expressive centralized spacing tokens for Vittify
 * strictly following GEMINI.md standards.
 */
object VittifySpacing {
    val micro = 4.dp
    val tight = 8.dp
    val compact = 12.dp
    val standard = 16.dp
    val comfortable = 20.dp
    val section = 24.dp
    val expressive = 28.dp
    val hero = 32.dp
    val huge = 48.dp

    // Dynamic scaled spacing tokens responding to paddingScale
    val scaledMicro: Dp
        @Composable get() = (4f * LocalVittifyTokens.current.paddingScale).dp

    val scaledTight: Dp
        @Composable get() = (8f * LocalVittifyTokens.current.paddingScale).dp

    val scaledCompact: Dp
        @Composable get() = (12f * LocalVittifyTokens.current.paddingScale).dp

    val scaledStandard: Dp
        @Composable get() = (16f * LocalVittifyTokens.current.paddingScale).dp

    val scaledComfortable: Dp
        @Composable get() = (20f * LocalVittifyTokens.current.paddingScale).dp

    val scaledSection: Dp
        @Composable get() = (24f * LocalVittifyTokens.current.paddingScale).dp

    val scaledExpressive: Dp
        @Composable get() = (28f * LocalVittifyTokens.current.paddingScale).dp

    val scaledHero: Dp
        @Composable get() = (32f * LocalVittifyTokens.current.paddingScale).dp

    val scaledHuge: Dp
        @Composable get() = (48f * LocalVittifyTokens.current.paddingScale).dp

    @Composable
    fun scaled(base: Dp): Dp = (base.value * LocalVittifyTokens.current.paddingScale).dp
}

/**
 * Dynamic squircle geometry respecting the global corner radius scale.
 */
object VittifyShapes {
    private val heroBase = 28.dp
    private val platterBase = 28.dp
    private val inputBase = 16.dp
    private val buttonBase = 18.dp
    private val bottomSheetBase = 32.dp
    private val dialogBase = 28.dp

    // 28dp squircle for hero balances and modular widgets
    val hero: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            return RoundedCornerShape((heroBase.value * scale).dp)
        }

    // 28dp squircle for grouped platters and content islands
    val platter: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            return RoundedCornerShape((platterBase.value * scale).dp)
        }

    // 16dp squircle for inputs, search bars, and nested pickers
    val input: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            return RoundedCornerShape((inputBase.value * scale).dp)
        }

    // 18dp squircle for buttons and primary CTAs
    val button: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            return RoundedCornerShape((buttonBase.value * scale).dp)
        }

    // 50dp / capsule for pills, chips, and tags
    val pill = CircleShape

    // 32dp top corners for bottom sheets
    val bottomSheet: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            val r = (bottomSheetBase.value * scale).dp
            return RoundedCornerShape(topStart = r, topEnd = r)
        }

    // 28dp squircle for dialogs
    val dialog: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            return RoundedCornerShape((dialogBase.value * scale).dp)
        }

    val small: RoundedCornerShape
        @Composable get() = scaled(8.dp)

    val medium: RoundedCornerShape
        @Composable get() = scaled(16.dp)

    val large: RoundedCornerShape
        @Composable get() = scaled(24.dp)

    val extraLarge: RoundedCornerShape
        @Composable get() = scaled(32.dp)

    val card: RoundedCornerShape
        @Composable get() = platter

    // Dynamic list item shapes respecting cornerRadiusScale
    val listTop: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            val large = (24f * scale).dp
            val small = (4f * scale).dp
            return RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
        }

    val listMiddle: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            val small = (4f * scale).dp
            return RoundedCornerShape(small)
        }

    val listBottom: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            val large = (24f * scale).dp
            val small = (4f * scale).dp
            return RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
        }

    val listSingle: RoundedCornerShape
        @Composable get() {
            val scale = LocalVittifyTokens.current.cornerRadiusScale
            val large = (24f * scale).dp
            return RoundedCornerShape(large)
        }

    @Composable
    fun scaled(radius: Dp): RoundedCornerShape {
        val scale = LocalVittifyTokens.current.cornerRadiusScale
        return RoundedCornerShape((radius.value * scale).dp)
    }

    @Composable
    fun scaled(
        topStart: Dp = 0.dp,
        topEnd: Dp = 0.dp,
        bottomStart: Dp = 0.dp,
        bottomEnd: Dp = 0.dp
    ): RoundedCornerShape {
        val scale = LocalVittifyTokens.current.cornerRadiusScale
        return RoundedCornerShape(
            topStart = (topStart.value * scale).dp,
            topEnd = (topEnd.value * scale).dp,
            bottomStart = (bottomStart.value * scale).dp,
            bottomEnd = (bottomEnd.value * scale).dp
        )
    }
}

/**
 * Dynamic surface and border styling driven by LocalVittifyTokens.
 */
object VittifySurface {
    val platterAlpha: Float
        @Composable get() = LocalVittifyTokens.current.surfaceOpacity

    val platterCardAlpha: Float
        @Composable get() = LocalVittifyTokens.current.surfaceOpacity

    val borderThickness: Dp
        @Composable get() = LocalVittifyTokens.current.borderThickness

    val borderOpacity: Float
        @Composable get() = LocalVittifyTokens.current.borderOpacity

    val cornerRadiusScale: Float
        @Composable get() = LocalVittifyTokens.current.cornerRadiusScale

    val paddingScale: Float
        @Composable get() = LocalVittifyTokens.current.paddingScale

    val dividerAlpha = 0.20f

    @Composable
    fun surfaceContainerLowestColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun surfaceContainerLowColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun platterContainerColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.surfaceContainer.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun platterCardColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun surfaceContainerColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.surfaceContainer.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun surfaceContainerHighColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun surfaceContainerHighestColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun surfaceColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.surface.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun primaryContainerColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.primaryContainer.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun secondaryContainerColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.secondaryContainer.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun tertiaryContainerColor(): Color {
        val tokens = LocalVittifyTokens.current
        return MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = tokens.surfaceOpacity)
    }

    @Composable
    fun platterBorder(strokeColor: Color? = null): BorderStroke? {
        val tokens = LocalVittifyTokens.current
        if (tokens.borderThickness <= 0.dp || tokens.borderOpacity <= 0f) return null
        val baseColor = strokeColor ?: MaterialTheme.colorScheme.outlineVariant
        return BorderStroke(
            width = tokens.borderThickness,
            color = baseColor.copy(alpha = tokens.borderOpacity)
        )
    }

    @Composable
    fun cardBorder(strokeColor: Color? = null): BorderStroke? = platterBorder(strokeColor)

    @Composable
    fun inputBorder(strokeColor: Color? = null): BorderStroke? = platterBorder(strokeColor)

    @Composable
    fun cardColors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor: Color = MaterialTheme.colorScheme.onSurface
    ): CardColors = CardDefaults.cardColors(
        containerColor = containerColor.copy(alpha = LocalVittifyTokens.current.surfaceOpacity),
        contentColor = contentColor
    )

    @Composable
    fun dividerColor(): Color {
        return MaterialTheme.colorScheme.outlineVariant.copy(alpha = dividerAlpha)
    }
}
