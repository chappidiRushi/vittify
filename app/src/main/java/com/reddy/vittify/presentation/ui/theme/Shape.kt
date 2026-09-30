package com.reddy.vittify.presentation.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

object ExpressiveShapes {
    val SquircleCard: RoundedCornerShape @Composable get() = VittifyShapes.platter
    val HeroCard: RoundedCornerShape @Composable get() = VittifyShapes.hero
    val BottomSheetTop: RoundedCornerShape @Composable get() = VittifyShapes.bottomSheet
    val PillTag = CircleShape
    val TouchTargetButton: RoundedCornerShape @Composable get() = VittifyShapes.button
}