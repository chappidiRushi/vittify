package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.success_light
import com.reddy.vittify.presentation.ui.theme.warning_light
import com.reddy.vittify.presentation.ui.util.ToastData
import com.reddy.vittify.presentation.ui.util.ToastManager
import com.reddy.vittify.presentation.ui.util.ToastType
import kotlinx.coroutines.delay

/**
 * Global toast host rendered above all navigation destinations.
 * Displays floating actionable status messages with Material 3 Expressive squircle styling.
 */
@Composable
fun GlobalToastHost(
    modifier: Modifier = Modifier
) {
    val currentToast by ToastManager.currentToast.collectAsStateWithLifecycle()

    LaunchedEffect(currentToast?.id) {
        val toast = currentToast ?: return@LaunchedEffect
        delay(toast.durationMillis)
        if (ToastManager.currentToast.value?.id == toast.id) {
            ToastManager.dismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = Spacing.sm, start = Spacing.md, end = Spacing.md)
            .zIndex(9999f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = currentToast != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut()
        ) {
            currentToast?.let { toast ->
                VittifyToastCard(
                    toast = toast,
                    onDismiss = { ToastManager.dismiss() }
                )
            }
        }
    }
}

/**
 * Expressive 20dp squircle platter toast card.
 */
@Composable
fun VittifyToastCard(
    toast: ToastData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = VittifyShapes.scaled(20.dp)
    val (icon, iconTint, iconBg) = when (toast.type) {
        ToastType.SUCCESS -> Triple(
            Icons.Rounded.CheckCircle,
            success_light,
            success_light.copy(alpha = 0.12f)
        )
        ToastType.ERROR -> Triple(
            Icons.Rounded.Error,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
        ToastType.WARNING -> Triple(
            Icons.Rounded.Warning,
            warning_light,
            warning_light.copy(alpha = 0.15f)
        )
        ToastType.INFO -> Triple(
            Icons.Rounded.Info,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    }

    Surface(
        modifier = modifier
            .widthIn(min = 200.dp, max = 400.dp)
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        shape = shape,
        border = VittifySurface.platterBorder(),
        color = VittifySurface.surfaceContainerHighestColor(),
        shadowElevation = 8.dp,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = toast.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}
