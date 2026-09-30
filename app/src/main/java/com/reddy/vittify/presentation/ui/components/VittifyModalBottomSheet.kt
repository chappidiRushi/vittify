package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import kotlinx.coroutines.launch

/**
 * Universal Material 3 Expressive ModalBottomSheet wrapper with crisp & tactile spring
 * entrance physics (`dampingRatio = 0.72f`, `stiffness = 390f`), 32dp top squircle corners,
 * haptic feedback on presentation, and an interactive spring drag handle pill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VittifyModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = VittifyShapes.bottomSheet,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { VittifySpringDragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptic = rememberAppHapticFeedback()
    val slideProgress = remember { Animatable(0f) }
    val scaleProgress = remember { Animatable(0.88f) }

    LaunchedEffect(Unit) {
        haptic.click()
        launch {
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = 260f
                )
            )
        }
        launch {
            scaleProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.50f,
                    stiffness = 240f
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val offsetPx = (1f - slideProgress.value) * 64.dp.toPx()
                    translationY = offsetPx
                    scaleX = scaleProgress.value
                    scaleY = scaleProgress.value
                    alpha = (0.4f + 0.6f * slideProgress.value).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
            content = content
        )
    }
}

/**
 * Interactive 32dp × 4dp bottom sheet drag handle that stretches to 44dp × 5.5dp with a crisp
 * spring when touched.
 */
@Composable
fun VittifySpringDragHandle(
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val handleWidth by animateDpAsState(
        targetValue = if (isPressed) 44.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "drag_handle_width"
    )
    val handleHeight by animateDpAsState(
        targetValue = if (isPressed) 5.5.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "drag_handle_height"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = handleWidth, height = handleHeight)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isPressed) 0.65f else 0.38f
                    )
                )
        )
    }
}
