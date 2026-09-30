package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Universal tactile spring press modifier for buttons, bottom bars, and interactive components.
 * Slightly compresses (targetScale = 0.96f) on press and springs back with a subtle,
 * refined tactile snap without excessive wobble (dampingRatio = 0.68f, stiffness = 450f).
 */
fun Modifier.springPress(
    targetScale: Float = 0.96f,
    pressedScale: Float = targetScale,
    scaleDown: Float = pressedScale,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isTouchDown by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isInteractionPressed by interactionSource.collectIsPressedAsState()
    val isPressed = isTouchDown || isInteractionPressed
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = 450f
        ),
        label = "spring_press_scale"
    )
    val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isTouchDown = true
                waitForUpOrCancellation()
                isTouchDown = false
            }
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
 * Adds a tactile spring scale press animation to icons on touch.
 */
fun Modifier.animatedIconClick(
    pressedScale: Float = 0.92f,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = 450f
        ),
        label = "iconClickScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                val up = waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Adds a spring scale-pop and rotation bounce when the icon selection state changes.
 */
fun Modifier.animatedIconSelection(
    isSelected: Boolean,
    maxScale: Float = 1.18f,
    rotationDegree: Float = 12f
): Modifier = composed {
    val scaleAnim = remember { Animatable(1.0f) }
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        // Trigger spring pop when selection toggles
        scaleAnim.snapTo(if (isSelected) maxScale else 0.88f)
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            rotationAnim.snapTo(-rotationDegree)
            rotationAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            rotationAnim.snapTo(0f)
        }
    }

    this.graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
        rotationZ = rotationAnim.value
    }
}

/**
 * Adds a smooth spring scale-in entrance animation when the icon is rendered.
 */
fun Modifier.animatedIconEntrance(
    initialScale: Float = 0.7f,
    stiffness: Float = Spring.StiffnessMedium
): Modifier = composed {
    val scaleAnim = remember { Animatable(initialScale) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = stiffness
            )
        )
    }

    this.graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
    }
}

/**
 * Reusable Animated Icon supporting ImageVector vectors with touch & selection animations.
 */
@Composable
fun AnimatedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    isSelected: Boolean = false,
    animateSelection: Boolean = false,
    animateEntrance: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .then(if (animateEntrance) Modifier.animatedIconEntrance() else Modifier)
            .then(if (animateSelection) Modifier.animatedIconSelection(isSelected) else Modifier)
            .animatedIconClick(onClick = onClick)
    )
}

/**
 * Reusable Animated Icon supporting Painter resources with touch & selection animations.
 */
@Composable
fun AnimatedIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
    isSelected: Boolean = false,
    animateSelection: Boolean = false,
    animateEntrance: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .then(if (animateEntrance) Modifier.animatedIconEntrance() else Modifier)
            .then(if (animateSelection) Modifier.animatedIconSelection(isSelected) else Modifier)
            .animatedIconClick(onClick = onClick)
    )
}
