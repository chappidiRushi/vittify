package com.reddy.vittify.presentation.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.sync.ViewMode
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun getNextMode(mode: ViewMode): ViewMode = when (mode) {
    ViewMode.PERSONAL -> ViewMode.COMBINED
    ViewMode.COMBINED -> ViewMode.PARTNER
    ViewMode.PARTNER -> ViewMode.PERSONAL
}

private fun getPrevMode(mode: ViewMode): ViewMode = when (mode) {
    ViewMode.PERSONAL -> ViewMode.PARTNER
    ViewMode.PARTNER -> ViewMode.COMBINED
    ViewMode.COMBINED -> ViewMode.PERSONAL
}

/**
 * Floating profile switcher button that switches modes via click or vertical swipe gestures.
 * Swipe up advances to the next profile, while swipe down returns to the previous profile.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun FloatingProfileSwitcher(
    activeMode: ViewMode,
    partnerName: String?,
    userName: String?,
    onModeSelected: (ViewMode) -> Unit,
    blurEffects: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val effectiveUserName = userName?.ifBlank { null }?.takeIf { it != "User" } ?: "Me"
    val effectivePartnerName = partnerName?.ifBlank { null } ?: "Partner"

    val (currentLabel, currentIcon) = when (activeMode) {
        ViewMode.PERSONAL -> effectiveUserName to Icons.Rounded.Person
        ViewMode.COMBINED -> "Both" to Icons.Rounded.Favorite
        ViewMode.PARTNER -> effectivePartnerName to Icons.Rounded.Favorite
    }

    val containerColor = MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer

    // Track direction for the slide animation: +1 for upward / forward, -1 for downward / backward
    var slideDirection by remember { mutableIntStateOf(1) }

    // Swipe and tactile spring drag state
    val swipeThresholdPx = with(density) { 20.dp.toPx() }
    val maxOffsetPx = with(density) { 14.dp.toPx() }
    val flingThresholdPx = with(density) { 250.dp.toPx() }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val dragOffsetY = remember { Animatable(0f) }

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            slideDirection = 1
            onModeSelected(getNextMode(activeMode))
        },
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .height(48.dp)
            .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragAccumulator += delta
                    coroutineScope.launch {
                        dragOffsetY.snapTo((dragAccumulator * 0.35f).coerceIn(-maxOffsetPx, maxOffsetPx))
                    }
                },
                onDragStarted = {
                    dragAccumulator = 0f
                },
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        dragOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }

                    val isSwipeDown = dragAccumulator > swipeThresholdPx || velocity > flingThresholdPx
                    val isSwipeUp = dragAccumulator < -swipeThresholdPx || velocity < -flingThresholdPx

                    if (isSwipeDown) {
                        slideDirection = -1
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onModeSelected(getPrevMode(activeMode))
                    } else if (isSwipeUp) {
                        slideDirection = 1
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onModeSelected(getNextMode(activeMode))
                    }
                }
            )
            .clip(CircleShape)
    ) {
        AnimatedContent(
            targetState = currentLabel to currentIcon,
            transitionSpec = {
                if (slideDirection >= 0) {
                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                } else {
                    (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                }
            },
            contentAlignment = Alignment.Center,
            label = "FloatingProfileSwitcherAnimation"
        ) { (label, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }
    }
}

