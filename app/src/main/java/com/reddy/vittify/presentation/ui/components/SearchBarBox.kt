package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.reddy.vittify.presentation.ui.theme.VittifyShapes

// Search Box Composable used in Category icon selection, Currency, etc.
@Composable
fun SearchBarBox(
    modifier: Modifier = Modifier,
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    leadingIcon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {},
    label: @Composable () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val themeColors = MaterialTheme.colorScheme
    val shape = VittifyShapes.input
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.015f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "search_bar_focus_scale"
    )

    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { label() },
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                color = if (isFocused) themeColors.surfaceContainerHighest else themeColors.surfaceContainerHigh,
                shape = shape
            )
            .border(
                BorderStroke(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = if (isFocused) themeColors.primary.copy(alpha = 0.6f) else themeColors.outlineVariant.copy(alpha = 0.25f)
                ),
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused },
        singleLine = true,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = themeColors.onSurface),
        colors = TextFieldDefaults.colors(
            focusedTextColor = themeColors.onSurface,
            unfocusedTextColor = themeColors.onSurface,
            focusedPlaceholderColor = themeColors.onSurfaceVariant.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = themeColors.onSurfaceVariant.copy(alpha = 0.7f),
            focusedLeadingIconColor = themeColors.primary,
            unfocusedLeadingIconColor = themeColors.onSurfaceVariant,
            focusedTrailingIconColor = themeColors.primary,
            unfocusedTrailingIconColor = themeColors.onSurfaceVariant,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}
