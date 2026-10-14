package com.reddy.vittify.presentation.ui.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.R
import com.reddy.vittify.data.preferences.AppIcon
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.SecuritySafe
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private data class AmbientSparkle(
    val initialX: Float,
    val initialY: Float,
    val radiusDp: Float,
    val speedY: Float,
    val driftX: Float,
    val phase: Float,
    val maxAlpha: Float,
    val isPrimary: Boolean
)

private val ambientSparkles = listOf(
    AmbientSparkle(0.18f, 0.28f, 2.4f, 0.08f, 22f, 0.2f, 0.38f, true),
    AmbientSparkle(0.82f, 0.34f, 3.2f, 0.06f, -18f, 1.4f, 0.42f, false),
    AmbientSparkle(0.24f, 0.65f, 2.0f, 0.09f, 16f, 2.8f, 0.32f, true),
    AmbientSparkle(0.74f, 0.72f, 3.6f, 0.05f, -26f, 0.9f, 0.45f, false),
    AmbientSparkle(0.14f, 0.48f, 1.8f, 0.11f, 14f, 3.6f, 0.28f, false),
    AmbientSparkle(0.86f, 0.52f, 2.2f, 0.07f, -15f, 4.2f, 0.36f, true),
    AmbientSparkle(0.36f, 0.18f, 3.0f, 0.06f, 20f, 5.1f, 0.38f, true),
    AmbientSparkle(0.64f, 0.22f, 2.4f, 0.08f, -22f, 2.0f, 0.34f, false),
    AmbientSparkle(0.48f, 0.82f, 2.6f, 0.10f, 15f, 1.1f, 0.40f, true),
    AmbientSparkle(0.16f, 0.78f, 3.2f, 0.07f, -16f, 4.8f, 0.32f, false),
    AmbientSparkle(0.84f, 0.16f, 2.0f, 0.09f, 18f, 3.2f, 0.36f, true),
    AmbientSparkle(0.52f, 0.12f, 2.8f, 0.05f, -14f, 0.5f, 0.42f, false),
    AmbientSparkle(0.30f, 0.40f, 1.8f, 0.12f, 15f, 2.4f, 0.30f, true),
    AmbientSparkle(0.70f, 0.44f, 2.5f, 0.07f, -20f, 5.7f, 0.38f, false)
)

/**
 * Expressive Material 3 splash screen featuring a 3D perspective spring entrance,
 * dynamic specular light sheen sweep, ethereal floating micro-sparkles,
 * concentric squircle sonic ripples, and seamless handoff to the Home screen.
 */
@Composable
fun SplashScreen(
    currentAppIcon: AppIcon = AppIcon.ORIGINAL,
    onSplashFinished: () -> Unit
) {
    val density = LocalDensity.current

    // Central Hero Platter Entrance & 3D Physics
    val logoScale = remember { Animatable(0.32f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRotationZ = remember { Animatable(-14f) }
    val logoRotationX = remember { Animatable(16f) }
    val logoRotationY = remember { Animatable(-14f) }
    val shimmerProgress = remember { Animatable(0f) }

    // Typography Staggered Physics
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(32f) }
    val titleScale = remember { Animatable(0.90f) }

    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(22f) }

    // Privacy Badge Physics
    val badgeAlpha = remember { Animatable(0f) }
    val badgeOffsetY = remember { Animatable(28f) }

    // Ambient loop transitions
    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")

    // Ambient breathing aura
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    // Concentric sonic ripple waves
    val ripplePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_pulse"
    )

    // Idle subtle floating levitation & breathing
    val idleLevitationY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_levitation"
    )
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_scale"
    )

    // Ambient micro-sparkle drift
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_progress"
    )

    val logoRes = remember(currentAppIcon) {
        when (currentAppIcon) {
            AppIcon.ORIGINAL -> R.drawable.vittify_original
            AppIcon.ANARCHY -> R.drawable.vittify_anarchy
            AppIcon.ZENITH -> R.drawable.vittify_zenith
            AppIcon.MONOCHROME -> R.drawable.vittify_pastel
            AppIcon.COMIC -> R.drawable.vittify_comic
        }
    }

    var hasFinished by remember { mutableStateOf(false) }
    val finishSplash = rememberUpdatedState {
        if (!hasFinished) {
            hasFinished = true
            onSplashFinished()
        }
    }

    LaunchedEffect(Unit) {
        // 1. Logo entrance with 3D tactile perspective spring overshoot
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            logoRotationZ.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            logoRotationX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            logoRotationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // 2. Specular Shimmer Sheen sweep across the logo platter
        launch {
            delay(360)
            shimmerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing)
            )
        }

        // 3. Staggered Title entrance
        delay(130)
        launch {
            titleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
        launch {
            titleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            titleScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // 4. Staggered Subtitle entrance
        delay(80)
        launch {
            subtitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            subtitleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // 5. Bottom security & privacy trust badge reveal
        delay(70)
        launch {
            badgeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            badgeOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // Hold gracefully to display the living brand presentation, then hand off smoothly
        delay(1250)
        finishSplash.value()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Allow user to tap to dismiss immediately with fluid transition
                finishSplash.value()
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric floating micro-sparkles
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.tertiary
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(logoAlpha.value)
        ) {
            ambientSparkles.forEach { sparkle ->
                val yProgress = ((sparkle.initialY - particleProgress * sparkle.speedY * 5f) % 1f + 1f) % 1f
                val xOffset = sparkle.driftX * sin(particleProgress * 2f * PI.toFloat() + sparkle.phase)
                val x = sparkle.initialX * size.width + xOffset
                val y = yProgress * size.height
                val alphaFactor = sin(yProgress * PI.toFloat()).coerceIn(0f, 1f)
                val color = if (sparkle.isPrimary) primaryColor else secondaryColor
                drawCircle(
                    color = color.copy(alpha = sparkle.maxAlpha * alphaFactor),
                    radius = sparkle.radiusDp * density.density,
                    center = Offset(x, y)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        ) {
            // Ambient Aura Glow, Concentric Sonic Ripples & Hero Logo Platter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Ethereal ambient glow behind the platter
                Box(
                    modifier = Modifier
                        .size(135.dp)
                        .scale(auraScale)
                        .alpha(auraAlpha * logoAlpha.value)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            ),
                            shape = VittifyShapes.hero
                        )
                )

                // Concentric squircle sonic ripple waves
                listOf(ripplePulse, (ripplePulse + 0.5f) % 1f).forEach { progress ->
                    val waveScale = 1.0f + progress * 0.70f
                    val waveAlpha = (1f - progress) * 0.40f * logoAlpha.value
                    Surface(
                        modifier = Modifier
                            .size(108.dp)
                            .scale(waveScale)
                            .alpha(waveAlpha),
                        shape = VittifyShapes.hero,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = waveAlpha)
                        )
                    ) {}
                }

                // Main App Logo in Squircle Platter with 3D tactile physics & specular shimmer
                Surface(
                    modifier = Modifier
                        .size(108.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = VittifyShapes.hero,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                        )
                        .graphicsLayer {
                            scaleX = logoScale.value * idleScale
                            scaleY = logoScale.value * idleScale
                            rotationZ = logoRotationZ.value
                            rotationX = logoRotationX.value
                            rotationY = logoRotationY.value
                            translationY = idleLevitationY * density.density
                            alpha = logoAlpha.value
                            cameraDistance = 16f * density.density
                        },
                    shape = VittifyShapes.hero,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = VittifySurface.platterBorder(
                        strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ) ?: BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                            )
                        )
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(logoRes),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(VittifyShapes.hero)
                                .drawWithContent {
                                    drawContent()
                                    val progress = shimmerProgress.value
                                    if (progress in 0.001f..0.999f) {
                                        val w = size.width
                                        val start = -w + progress * (3f * w)
                                        val beam = w * 0.7f
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.08f),
                                                    Color.White.copy(alpha = 0.50f),
                                                    Color.White.copy(alpha = 0.08f),
                                                    Color.Transparent
                                                ),
                                                start = Offset(start, start),
                                                end = Offset(start + beam, start + beam)
                                            ),
                                            blendMode = BlendMode.SrcAtop
                                        )
                                    }
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // App Name & Tagline with staggered physical entrance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        translationY = titleOffsetY.value
                        scaleX = titleScale.value
                        scaleY = titleScale.value
                        alpha = titleAlpha.value
                    }
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Signature vibrant accent dot
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp, top = 12.dp)
                            .size(7.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = "A tiny, smart financial companion",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.graphicsLayer {
                        translationY = subtitleOffsetY.value
                        alpha = subtitleAlpha.value
                    }
                )
            }
        }

        // Bottom privacy & trust badge with spring reveal
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.xxl)
                .graphicsLayer {
                    translationY = badgeOffsetY.value
                    alpha = badgeAlpha.value
                },
            shape = VittifyShapes.pill,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
            border = VittifySurface.platterBorder(
                strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
            ) ?: BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            ),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs + 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)
            ) {
                Icon(
                    imageVector = Iconax.SecuritySafe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "100% On-Device Parsing, Optional Cloud AI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

