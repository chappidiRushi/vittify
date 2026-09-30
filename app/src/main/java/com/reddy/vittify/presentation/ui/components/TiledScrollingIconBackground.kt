package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.common.icons.IconResource

/**
 * A background component that tiles an icon and scrolls it vertically.
 * Provides a premium look with reduced opacity and animation.
 */
@Composable
fun TiledScrollingIconBackground(
    iconResource: IconResource,
    modifier: Modifier = Modifier,
    opacity: Float = 0.05f,
    iconSize: Dp = 60.dp,
    rotation: Float = -20f,
    animationDuration: Int = 15000
) {
    TiledScrollingIconBackground(
        iconResources = listOf(iconResource),
        modifier = modifier,
        opacity = opacity,
        iconSize = iconSize,
        rotation = rotation,
        animationDuration = animationDuration
    )
}

/**
 * A background component that tiles multiple icons and scrolls them vertically.
 * Icons alternate in both columns and rows.
 */
@Composable
fun TiledScrollingIconBackground(
    iconResources: List<IconResource>,
    modifier: Modifier = Modifier,
    opacity: Float = 0.05f,
    iconSize: Dp = 60.dp,
    rotation: Float = -20f,
    animationDuration: Int = 15000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TiledBackground")
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScrollAnimation"
    )

    val paintersWithTint = iconResources.map { iconResource ->
        val painter = when (iconResource) {
            is IconResource.DrawableResource -> painterResource(id = iconResource.resId)
            is IconResource.TintedResIcon -> painterResource(id = iconResource.resId)
            is IconResource.VectorIcon -> rememberVectorPainter(image = iconResource.icon)
        }

        val tint = when (iconResource) {
            is IconResource.TintedResIcon -> iconResource.tint
            is IconResource.VectorIcon -> iconResource.tint
            else -> Color.Unspecified
        }
        painter to tint
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val sizePx = iconSize.toPx()
        val spacing = sizePx * 0.4f
        val step = sizePx + spacing
        
        // Offset ranges from 0 to step
        val offsetY = scrollOffset * step

        // Calculate how many items we need to cover the area
        // We add extra to handle rotation and overflow
        val columns = (size.width / step).toInt() + 4
        val rows = (size.height / step).toInt() + 4

        rotate(rotation) {
            for (col in -2..columns) {
                for (row in -2..rows) {
                    val x = col * step
                    // Add scrolling offset to Y position
                    val y = (row * step) + offsetY
                    
                    // Select icon based on position to create alternating pattern
                    val index = (col + row).let { if (it < 0) -it else it } % iconResources.size
                    val (painter, tint) = paintersWithTint[index]

                    translate(left = x, top = y) {
                        val isBrandIcon = iconResources[index] is IconResource.DrawableResource
                        val drawSize = if (isBrandIcon) sizePx else sizePx * 0.7f
                        val offset = if (isBrandIcon) 0f else (sizePx - drawSize) / 2f

                        translate(left = offset, top = offset) {
                            with(painter) {
                                draw(
                                    size = Size(drawSize, drawSize),
                                    alpha = opacity,
                                    colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
