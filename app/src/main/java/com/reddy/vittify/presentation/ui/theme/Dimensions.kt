package com.reddy.vittify.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized dimensions for consistent UI throughout the app,
 * fully consolidated with VittifyTokens and VittifySpacing.
 */
object Dimensions {
    
    // Padding values (consolidated with VittifySpacing)
    object Padding {
        val content: Dp @Composable get() = VittifySpacing.scaledStandard      // Standard content padding (16.dp)
        val card: Dp @Composable get() = VittifySpacing.scaledComfortable     // Card internal padding (20.dp)
        val empty: Dp @Composable get() = VittifySpacing.scaledHero           // Empty state padding (32.dp)
        val fab: Dp @Composable get() = VittifySpacing.scaledStandard         // FAB padding (16.dp)

        val scaledContent: Dp @Composable get() = VittifySpacing.scaledStandard
        val scaledCard: Dp @Composable get() = VittifySpacing.scaledComfortable
        val scaledEmpty: Dp @Composable get() = VittifySpacing.scaledHero
        val scaledFab: Dp @Composable get() = VittifySpacing.scaledStandard
    }

    // Corner radius and spacing radius (consolidated with VittifySpacing & VittifyTokens)
    object Radius {
        val xs: Dp @Composable get() = (VittifySpacing.micro.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val sm: Dp @Composable get() = (VittifySpacing.tight.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val md: Dp @Composable get() = (VittifySpacing.standard.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val lg: Dp @Composable get() = (VittifySpacing.section.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val xl: Dp @Composable get() = (VittifySpacing.hero.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val xxl: Dp @Composable get() = (VittifySpacing.huge.value * LocalVittifyTokens.current.cornerRadiusScale).dp

        val scaledXs: Dp @Composable get() = (VittifySpacing.micro.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val scaledSm: Dp @Composable get() = (VittifySpacing.tight.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val scaledMd: Dp @Composable get() = (VittifySpacing.standard.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val scaledLg: Dp @Composable get() = (VittifySpacing.section.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val scaledXl: Dp @Composable get() = (VittifySpacing.hero.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val scaledXxl: Dp @Composable get() = (VittifySpacing.huge.value * LocalVittifyTokens.current.cornerRadiusScale).dp
    }
    
    // Elevation values
    object Elevation {
        val card = 1.dp
        val fab = 6.dp
        val bottomBar = 3.dp
        val dialog = VittifySpacing.tight        // 8.dp
    }
    
    // Alpha values for transparency (consolidated with VittifySurface)
    object Alpha {
        const val high = 0.87f
        const val medium = 0.6f
        const val disabled = 0.38f
        const val divider = 0.12f
        const val surface = 0.7f
        const val subtitle = 0.8f

        val dynamicSurface: Float @Composable get() = VittifySurface.platterAlpha
        val dynamicDivider: Float @Composable get() = VittifySurface.dividerAlpha
    }
    
    // Icon sizes (consolidated with VittifySpacing)
    object Icon {
        val small = VittifySpacing.standard       // 16.dp
        val medium = VittifySpacing.section       // 24.dp
        val large = VittifySpacing.huge           // 48.dp
        val extraLarge = 120.dp                  // For empty states
    }
    
    // Corner radius (consolidated with VittifySpacing & VittifyTokens)
    object CornerRadius {
        val small: Dp @Composable get() = (VittifySpacing.micro.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val medium: Dp @Composable get() = (VittifySpacing.tight.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val large: Dp @Composable get() = (VittifySpacing.compact.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val full = 50.dp                         // For circular elements

        val scaledSmall: Dp @Composable get() = (VittifySpacing.micro.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val scaledMedium: Dp @Composable get() = (VittifySpacing.tight.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val scaledLarge: Dp @Composable get() = (VittifySpacing.compact.value * LocalVittifyTokens.current.cornerRadiusScale).dp
        val scaledFull: Dp @Composable get() = 50.dp
    }
    
    // Text sizes
    object TextSize {
        val small = 12.sp
        val body = 14.sp
        val medium = 16.sp
        val large = 20.sp
        val title = 24.sp
        val display = 32.sp
    }
    
    // Component specific dimensions (consolidated with VittifySpacing & VittifyTokens)
    object Component {
        val bottomBarHeight = 80.dp
        val buttonHeight = VittifySpacing.huge    // 48.dp
        val minTouchTarget = VittifySpacing.huge  // 48.dp
        val dividerThickness = 1.dp
        val progressIndicatorSize = VittifySpacing.section // 24.dp
        val chipHeight = VittifySpacing.hero      // 32.dp

        val dynamicBorderThickness: Dp @Composable get() = VittifySurface.borderThickness
    }
    
    // Animation durations (in milliseconds)
    object Animation {
        const val short = 150
        const val medium = 300
        const val long = 500
    }
}