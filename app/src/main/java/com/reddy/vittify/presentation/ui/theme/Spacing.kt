package com.reddy.vittify.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

object Spacing {
    val xs: Dp @Composable get() = VittifySpacing.scaledMicro
    val sm: Dp @Composable get() = VittifySpacing.scaledTight
    val md: Dp @Composable get() = VittifySpacing.scaledStandard
    val lg: Dp @Composable get() = VittifySpacing.scaledSection
    val xl: Dp @Composable get() = VittifySpacing.scaledHero
    val xxl: Dp @Composable get() = VittifySpacing.scaledHuge

    val scaledXs: Dp @Composable get() = VittifySpacing.scaledMicro
    val scaledSm: Dp @Composable get() = VittifySpacing.scaledTight
    val scaledMd: Dp @Composable get() = VittifySpacing.scaledStandard
    val scaledLg: Dp @Composable get() = VittifySpacing.scaledSection
    val scaledXl: Dp @Composable get() = VittifySpacing.scaledHero
    val scaledXxl: Dp @Composable get() = VittifySpacing.scaledHuge
}