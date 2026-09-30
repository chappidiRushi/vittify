package com.reddy.vittify.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedContentScope = compositionLocalOf<AnimatedContentScope?> { null }
val LocalBottomNavPadding = compositionLocalOf<Dp> { 0.dp }

// Shared bounds transform configurations
object SharedTransitionTransforms {
    val smooth: BoundsTransform = BoundsTransform { _, _ ->
        spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        )
    }
    
    val snappy: BoundsTransform = BoundsTransform { _, _ ->
        spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        )
    }
    
    val quick: BoundsTransform = BoundsTransform { _, _ ->
        spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        )
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedBounds(
    key: Any,
    boundsTransform: BoundsTransform = SharedTransitionTransforms.smooth
) =
    with(LocalSharedTransitionScope.current) {
        val animatedContentScope = LocalAnimatedContentScope.current
        return@with if (this != null && animatedContentScope != null) {
            sharedBounds(
                rememberSharedContentState(key),
                animatedContentScope,
                boundsTransform = boundsTransform
            ).skipToLookaheadSize()
        } else this@sharedBounds
    }

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedElement(
    key: Any,
    boundsTransform: BoundsTransform = SharedTransitionTransforms.smooth
) =
    with(LocalSharedTransitionScope.current) {
        val animatedContentScope = LocalAnimatedContentScope.current
        return@with if (this != null && animatedContentScope != null) {
            sharedElement(
                rememberSharedContentState(key),
                animatedContentScope,
                boundsTransform = boundsTransform
            )
        } else this@sharedElement
    }
