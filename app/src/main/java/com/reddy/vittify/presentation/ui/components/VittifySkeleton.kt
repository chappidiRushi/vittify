package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySpacing
import com.reddy.vittify.presentation.ui.theme.VittifySurface

/**
 * Shimmer brush modifier for loading skeletons per Material 3 Expressive.
 */
fun Modifier.shimmerEffect(enabled: Boolean = true, durationMillis: Int = 1200): Modifier = composed {
    if (!enabled) return@composed this

    val transition = rememberInfiniteTransition(label = "vittify_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vittify_shimmer_translate"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)

    background(
        brush = Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(translateAnim - 300f, translateAnim - 300f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}

/**
 * Reusable single skeleton placeholder box with rounded/pill geometry.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = VittifyShapes.pill
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmerEffect()
    )
}

/**
 * Structural skeleton for a single transaction row matching TransactionItem layout.
 */
@Composable
fun TransactionRowSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Leading BrandIcon squircle skeleton
        SkeletonBox(
            modifier = Modifier.size(40.dp),
            shape = VittifyShapes.input
        )

        // Middle merchant and date/subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(11.dp)
            )
        }

        // Trailing amount skeleton
        SkeletonBox(
            modifier = Modifier
                .width(64.dp)
                .height(16.dp)
        )
    }
}

/**
 * Structural skeleton for a platter of recent transactions per GEMINI.md Rule 29.
 */
@Composable
fun TransactionPlatterSkeleton(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(VittifyShapes.platter)
    ) {
        repeat(count) { index ->
            TransactionRowSkeleton()
            if (index < count - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
            }
        }
    }
}

/**
 * Structural skeleton for an account card matching AccountCarousel layout.
 */
@Composable
fun AccountCardSkeleton(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(260.dp)
            .height(136.dp),
        shape = VittifyShapes.hero,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = VittifySurface.platterBorder()
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier.size(36.dp),
                    shape = VittifyShapes.input
                )
                SkeletonBox(
                    modifier = Modifier
                        .width(70.dp)
                        .height(14.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SkeletonBox(
                    modifier = Modifier
                        .width(90.dp)
                        .height(12.dp)
                )
                SkeletonBox(
                    modifier = Modifier
                        .width(130.dp)
                        .height(22.dp)
                )
            }
        }
    }
}

/**
 * Structural skeleton for the account carousel on the Home screen.
 */
@Composable
fun AccountCarouselSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VittifySpacing.scaledStandard),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        AccountCardSkeleton(modifier = Modifier.weight(1f))
        AccountCardSkeleton(modifier = Modifier.weight(1f))
    }
}

/**
 * Structural skeleton for TransactionTotalsCard on TransactionsScreen.
 */
@Composable
fun TransactionTotalsSkeleton(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = VittifyShapes.hero,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = VittifySurface.platterBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    SkeletonBox(modifier = Modifier.size(24.dp), shape = VittifyShapes.pill)
                    SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp))
                    SkeletonBox(modifier = Modifier.width(75.dp).height(18.dp))
                }
            }
        }
    }
}
