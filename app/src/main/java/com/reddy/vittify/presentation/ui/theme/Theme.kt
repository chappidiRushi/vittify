package com.reddy.vittify.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import com.reddy.vittify.data.preferences.AppFont
import com.reddy.vittify.data.preferences.ThemeStyle
import com.reddy.vittify.data.preferences.AccentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val LocalBlurEffects = staticCompositionLocalOf { true }
val LocalBaseBackgroundColor = staticCompositionLocalOf { Color(0xFFe2e2e9) }


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VittifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    isAmoledMode: Boolean = false,
    accentColor: AccentColor = AccentColor.BLUE,
    appFont: AppFont = AppFont.SYSTEM,
    customFontPath: String? = null,
    dynamicSeedColor: Int = -1,
    blurEffects: Boolean = true,
    playfulAnimation: Boolean = false,
    surfaceOpacity: Float = 0.85f,
    borderThickness: Float = 0.0f,
    borderOpacity: Float = 0.15f,
    cornerRadiusScale: Float = 1.0f,
    paddingScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var colorScheme =
        when {
            themeStyle == ThemeStyle.DYNAMIC && dynamicSeedColor != -1 -> {
                generateDynamicColorSchemeFromSeed(Color(dynamicSeedColor), darkTheme)
            }
            themeStyle == ThemeStyle.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            }
            themeStyle == ThemeStyle.DEFAULT -> {
                if (darkTheme) getCustomDarkColorScheme(accentColor)
                else getCustomLightColorScheme(accentColor)
            }
            // Fallback
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            }
            darkTheme -> darkColorScheme()
            else -> lightColorScheme()
        }

    // Apply Amoled Black if enabled in Dark Mode
    if (darkTheme && isAmoledMode) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
        )
    }

    val baseBackgroundColor = colorScheme.background
    val effectiveColorScheme = run {
        val base = if (playfulAnimation) {
            colorScheme.copy(background = Color.Transparent)
        } else {
            colorScheme
        }
        base.copy(
            surface = base.surface.copy(alpha = surfaceOpacity),
            surfaceDim = base.surfaceDim.copy(alpha = surfaceOpacity),
            surfaceBright = base.surfaceBright.copy(alpha = surfaceOpacity),
            surfaceContainerLowest = base.surfaceContainerLowest.copy(alpha = surfaceOpacity),
            surfaceContainerLow = base.surfaceContainerLow.copy(alpha = surfaceOpacity),
            surfaceContainer = base.surfaceContainer.copy(alpha = surfaceOpacity),
            surfaceContainerHigh = base.surfaceContainerHigh.copy(alpha = surfaceOpacity),
            surfaceContainerHighest = base.surfaceContainerHighest.copy(alpha = surfaceOpacity),
            surfaceVariant = base.surfaceVariant.copy(alpha = surfaceOpacity),
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        SideEffect {
            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Enforce transparent system bars for edge-to-edge on O+

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            // Control whether status bar icons should be dark or light
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val fontFamily = when (appFont) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.SANS_SERIF -> FontFamily.SansSerif
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONOSPACE -> FontFamily.Monospace
        AppFont.CURSIVE -> FontFamily.Cursive
        AppFont.SN_PRO -> SNProFontFamily
        AppFont.CUSTOM -> {
            if (!customFontPath.isNullOrBlank()) {
                val file = java.io.File(customFontPath)
                if (file.exists()) {
                    try {
                        FontFamily(android.graphics.Typeface.createFromFile(file))
                    } catch (e: Exception) {
                        FontFamily.Default
                    }
                } else FontFamily.Default
            } else FontFamily.Default
        }
    }

    val surfaceTokens = remember(surfaceOpacity, borderThickness, borderOpacity, cornerRadiusScale, paddingScale) {
        VittifySurfaceTokens(
            surfaceOpacity = surfaceOpacity,
            borderThickness = borderThickness.dp,
            borderOpacity = borderOpacity,
            cornerRadiusScale = cornerRadiusScale,
            paddingScale = paddingScale
        )
    }

    val scaledShapes = remember(cornerRadiusScale) {
        Shapes(
            extraSmall = RoundedCornerShape((8f * cornerRadiusScale).dp),
            small = RoundedCornerShape((14f * cornerRadiusScale).dp),
            medium = RoundedCornerShape((20f * cornerRadiusScale).dp),
            large = RoundedCornerShape((28f * cornerRadiusScale).dp),
            extraLarge = RoundedCornerShape((36f * cornerRadiusScale).dp)
        )
    }

    CompositionLocalProvider(
        LocalBlurEffects provides blurEffects,
        LocalBaseBackgroundColor provides baseBackgroundColor,
        LocalVittifyTokens provides surfaceTokens
    ) {
        MaterialExpressiveTheme(
            colorScheme = effectiveColorScheme,
            typography = getTypography(fontFamily = fontFamily),
            shapes = scaledShapes,
            content = content
        )
    }
}


fun getCustomLightColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = when (accent) {
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
    val secondaryColor = when (accent) {
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

    val tertiaryColor = when (accent) {
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

    val onPrimaryColor = when (accent) {
        AccentColor.YELLOW -> Color(0xFF1a1b20)

        AccentColor.PINE_IRIS, AccentColor.PINE_PINE,
        AccentColor.PINE_LOVE, AccentColor.PINE_FOAM, AccentColor.PINE_MUTED, AccentColor.PINE_SUBTLE,
        AccentColor.PINE_TEXT,  AccentColor.PINE_SURFACE,
        AccentColor.PINE_OVERLAY -> Dawn_Surface_Base

        AccentColor.PINE_ROSE, AccentColor.PINE_GOLD,
        AccentColor.PINE_HIGHLIGHT-> Dawn_OnBackground
        else -> Color(0xFFffffff)
    }

    val onSecondaryColor = when (accent) {
        AccentColor.YELLOW -> Color(0xFF1a1b20)
        AccentColor.PINE_ROSE,  AccentColor.PINE_GOLD,
        AccentColor.PINE_LOVE -> Dawn_OnBackground

        AccentColor.PINE_IRIS, AccentColor.PINE_PINE, AccentColor.PINE_MUTED,
        AccentColor.PINE_SUBTLE, AccentColor.PINE_TEXT, AccentColor.PINE_FOAM,
        AccentColor.PINE_HIGHLIGHT, AccentColor.PINE_SURFACE,
        AccentColor.PINE_OVERLAY -> Dawn_Surface_Base
        else -> Color(0xFFffffff)
    }

    val onTertiaryColor = when (accent) {
        AccentColor.YELLOW, AccentColor.ROSEWATER, AccentColor.FLAMINGO -> Color(0xFF1a1b20)

        AccentColor.PINE_ROSE, AccentColor.PINE_HIGHLIGHT, -> Dawn_OnBackground

        AccentColor.PINE_IRIS, AccentColor.PINE_PINE,   AccentColor.PINE_GOLD,
        AccentColor.PINE_MUTED, AccentColor.PINE_LOVE,
        AccentColor.PINE_SUBTLE, AccentColor.PINE_FOAM,
        AccentColor.PINE_SURFACE, AccentColor.PINE_TEXT,
        AccentColor.PINE_OVERLAY -> Dawn_Surface_Base
        else -> Color(0xFFffffff)
    }

    val isPine = accent.name.startsWith("PINE_")

    return lightColorScheme(
        primary = primaryColor,
        onPrimary = onPrimaryColor,
        primaryContainer = primaryColor,
        onPrimaryContainer = onPrimaryColor,
        inversePrimary = Color(0xFF000000),
        secondary = secondaryColor,
        onSecondary = onSecondaryColor,
        secondaryContainer = secondaryColor,
        onSecondaryContainer = onSecondaryColor,
        tertiary = tertiaryColor,
        onTertiary = onTertiaryColor,
        tertiaryContainer = tertiaryColor,
        onTertiaryContainer = onTertiaryColor,
        background = if (isPine) Dawn_Background else Color(0xFFe2e2e9),
        onBackground = if (isPine) Dawn_OnBackground else Color(0xFF1a1b20),
        surface = if (isPine) Dawn_Background else Color(0xFFE5E5EA),
        onSurface = if (isPine) Dawn_OnSurface else Color(0xFF1a1b20),
        surfaceVariant = if (isPine) Dawn_SurfaceVariant else Color(0xFFc4c6d0),
        onSurfaceVariant = if (isPine) Dawn_OnSurfaceVariant else Color(0xFF44474f),
        inverseSurface = if (isPine) Color(0xFF26233A) else Color(0xFF2f3036),
        inverseOnSurface = if (isPine) Color(0xFFE0DEF4) else Color(0xFFf0f0f7),
        error = if (isPine) Dawn_Love else Latte_Red,
        onError = Color(0xFFffffff),
        errorContainer = if (isPine) Dawn_Love else Latte_Red,
        onErrorContainer = Color(0xFFffffff),
        surfaceBright = if (isPine) Dawn_Surface_Base else Color(0xFFE8E9EC),
        surfaceDim = if (isPine) Dawn_SurfaceVariant else Color(0xFFd9d9e0),
        surfaceContainer = if (isPine) Dawn_Background else Color(0xFFf9f9ff),
        surfaceContainerHigh = if (isPine) Dawn_SurfaceVariant else Color(0xFFe8e7ee),
        surfaceContainerHighest = if (isPine) Color(0xFFE6DDD5) else Color(0xFFe2e2e9),
        surfaceContainerLow = if (isPine) Dawn_Surface_Base else Color(0xFFffffff),
        surfaceContainerLowest = Color(0xFFf9f9ff)
    )
}

fun getCustomDarkColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = when (accent) {
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
    }

    val secondaryColor = when (accent) {
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
    }

    val tertiaryColor = when (accent) {
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
    }

    val onPrimaryColor = when (accent) {
        AccentColor.BLUE -> Color.White
        AccentColor.PINE_PINE, AccentColor.PINE_MUTED, AccentColor.PINE_SURFACE, AccentColor.PINE_OVERLAY -> Color.White
        else -> Color(0xFF1a1b20) // Dark text for bright pastel accents
    }

    val onSecondaryColor = when (accent) {
        AccentColor.BLUE -> Color.White
        AccentColor.PINE_PINE, AccentColor.PINE_MUTED, AccentColor.PINE_TEXT, AccentColor.PINE_SURFACE, AccentColor.PINE_OVERLAY -> Color(0xFF1a1b20) // Pine secondary is Foam (bright)
        else -> Color(0xFF1a1b20) 
    }

    val onTertiaryColor = when (accent) {
        AccentColor.BLUE -> Color.White
        AccentColor.PINE_PINE, AccentColor.PINE_MUTED, AccentColor.PINE_SURFACE, AccentColor.PINE_OVERLAY -> Color.White
        else -> Color(0xFF1a1b20) 
    }

    val isPine = accent.name.startsWith("PINE_")

    return darkColorScheme(
        primary = primaryColor,
        onPrimary = onPrimaryColor,
        primaryContainer = primaryColor,
        onPrimaryContainer = onPrimaryColor,
        inversePrimary = Color(0xFFFFFFFF),
        secondary = secondaryColor,
        onSecondary = onSecondaryColor,
        secondaryContainer = secondaryColor,
        onSecondaryContainer = onSecondaryColor,
        tertiary = tertiaryColor,
        onTertiary = onTertiaryColor,
        tertiaryContainer = tertiaryColor,
        onTertiaryContainer = onTertiaryColor,
        background = if (isPine) RosePine_Background else Color(0xFF111318),
        onBackground = if (isPine) RosePine_OnBackground else Color(0xFFe2e2e9),
        surface = if (isPine) RosePine_Background else Color(0xFF111318),
        onSurface = if (isPine) RosePine_OnSurface else Color(0xFFe2e2e9),
        surfaceVariant = if (isPine) RosePine_SurfaceVariant else Color(0xFF1e1f25),
        onSurfaceVariant = if (isPine) RosePine_OnSurfaceVariant else Color(0xFFc4c6d0),
        inverseSurface = if (isPine) Color(0xFFE0DEF4) else Color(0xFFe2e2e9),
        inverseOnSurface = if (isPine) Color(0xFF26233A) else Color(0xFF2f3036),
        error = if (isPine) RosePine_Love else Macchiato_Red_dim,
        onError = Color(0xFFffffff),
        errorContainer = if (isPine) RosePine_Love else Macchiato_Red_dim,
        onErrorContainer = Color(0xFFffffff),
        surfaceBright = if (isPine) RosePine_SurfaceVariant else Color(0xFF37393e),
        surfaceDim = if (isPine) RosePine_Background else Color(0xFF0c0e13),
        surfaceContainer = if (isPine) RosePine_Surface_Base else Color(0xFF1e1f25),
        surfaceContainerHigh = if (isPine) RosePine_SurfaceVariant else Color(0xFF282a2f),
        surfaceContainerHighest = if (isPine) Color(0xFF403D52) else Color(0xFF33353a),
        surfaceContainerLow = if (isPine) RosePine_Surface_Base else Color(0xFF1e1f25),
        surfaceContainerLowest = if (isPine) RosePine_Background else Color(0xFF1a1b20)
    )
}

fun generateDynamicColorSchemeFromSeed(seed: Color, darkTheme: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            (seed.alpha * 255).toInt(),
            (seed.red * 255).toInt(),
            (seed.green * 255).toInt(),
            (seed.blue * 255).toInt()
        ),
        hsv
    )
    val hue = hsv[0]
    val sat = hsv[1]

    fun hsvColor(h: Float, s: Float, v: Float): Color {
        val rgb = android.graphics.Color.HSVToColor(
            floatArrayOf((h % 360f + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
        )
        return Color(rgb)
    }

    return if (darkTheme) {
        val primary = hsvColor(hue, (sat * 0.75f).coerceIn(0.3f, 0.7f), 0.88f)
        val onPrimary = hsvColor(hue, sat, 0.2f)
        val primaryContainer = hsvColor(hue, (sat * 0.8f).coerceIn(0.4f, 0.8f), 0.35f)
        val onPrimaryContainer = hsvColor(hue, 0.25f, 0.95f)
        val secondary = hsvColor(hue + 15f, (sat * 0.4f).coerceIn(0.2f, 0.5f), 0.8f)
        val onSecondary = hsvColor(hue + 15f, sat, 0.2f)
        val secondaryContainer = hsvColor(hue + 15f, (sat * 0.5f).coerceIn(0.3f, 0.6f), 0.3f)
        val onSecondaryContainer = hsvColor(hue + 15f, 0.2f, 0.95f)
        val tertiary = hsvColor(hue + 60f, (sat * 0.5f).coerceIn(0.25f, 0.6f), 0.82f)
        val onTertiary = hsvColor(hue + 60f, sat, 0.2f)
        val tertiaryContainer = hsvColor(hue + 60f, (sat * 0.6f).coerceIn(0.3f, 0.65f), 0.32f)
        val onTertiaryContainer = hsvColor(hue + 60f, 0.25f, 0.95f)

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = Color(0xFF111318),
            onBackground = Color(0xFFE2E2E9),
            surface = Color(0xFF111318),
            onSurface = Color(0xFFE2E2E9),
            surfaceVariant = Color(0xFF1E2025),
            onSurfaceVariant = Color(0xFFC4C6D0),
            surfaceContainerLow = Color(0xFF181A1F),
            surfaceContainer = Color(0xFF1E2025),
            surfaceContainerHigh = Color(0xFF282A30),
            surfaceContainerHighest = Color(0xFF33353C),
            outline = Color(0xFF8E9099),
            outlineVariant = Color(0xFF44474E)
        )
    } else {
        val primary = seed
        val onPrimary = if (primary.luminance() > 0.5f) Color(0xFF1A1C1E) else Color.White
        val primaryContainer = hsvColor(hue, (sat * 0.25f).coerceIn(0.12f, 0.35f), 0.94f)
        val onPrimaryContainer = hsvColor(hue, (sat * 0.9f).coerceIn(0.6f, 1f), 0.28f)
        val secondary = hsvColor(hue + 15f, (sat * 0.5f).coerceIn(0.2f, 0.55f), 0.55f)
        val onSecondary = Color.White
        val secondaryContainer = hsvColor(hue + 15f, (sat * 0.2f).coerceIn(0.1f, 0.3f), 0.94f)
        val onSecondaryContainer = hsvColor(hue + 15f, 0.8f, 0.25f)
        val tertiary = hsvColor(hue + 60f, (sat * 0.5f).coerceIn(0.25f, 0.6f), 0.55f)
        val onTertiary = Color.White
        val tertiaryContainer = hsvColor(hue + 60f, (sat * 0.22f).coerceIn(0.12f, 0.32f), 0.94f)
        val onTertiaryContainer = hsvColor(hue + 60f, 0.8f, 0.25f)

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = Color(0xFFF9F9FC),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFF9F9FC),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFE1E2EC),
            onSurfaceVariant = Color(0xFF44474E),
            surfaceContainerLow = Color(0xFFF3F3F7),
            surfaceContainer = Color(0xFFECEEF3),
            surfaceContainerHigh = Color(0xFFE6E8EE),
            surfaceContainerHighest = Color(0xFFE0E2E8),
            outline = Color(0xFF74777F),
            outlineVariant = Color(0xFFC4C6D0)
        )
    }
}
