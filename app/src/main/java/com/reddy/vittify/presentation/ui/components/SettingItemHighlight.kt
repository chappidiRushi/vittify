package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable modifier to highlight a settings option when navigated via Global Search or Deep Link.
 * Plays a smooth border pulse and attaches to a BringIntoViewRequester.
 */
@Composable
fun Modifier.settingOptionHighlight(
    id: String,
    requester: BringIntoViewRequester?,
    highlightedId: String?,
    shape: Shape = MaterialTheme.shapes.medium,
    borderWidth: Dp = 2.dp
): Modifier {
    val isHighlighted = id.equals(highlightedId, ignoreCase = true)
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 400),
        label = "setting_option_highlight_border"
    )

    return this
        .then(if (requester != null) Modifier.bringIntoViewRequester(requester) else Modifier)
        .border(
            width = if (isHighlighted) borderWidth else 0.dp,
            color = borderColor,
            shape = shape
        )
}

