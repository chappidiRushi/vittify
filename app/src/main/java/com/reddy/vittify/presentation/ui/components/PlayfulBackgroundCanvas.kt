package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private class FloatingParticle(
    var xRatio: Float,
    var yRatio: Float,
    val radiusDp: Float,
    val speedY: Float,
    val swayAmplitudePx: Float,
    val swayFrequency: Float,
    val pulseSpeed: Float,
    val phase: Float,
    val colorIndex: Int
)

private class Star(
    val xRatio: Float,
    val yRatio: Float,
    val sizeDp: Float,
    val twinkleSpeed: Float,
    val phase: Float,
    val layer: Int = 1 // 1 = distant, 2 = mid, 3 = prominent cross star
)

private class Meteor(
    var active: Boolean = false,
    var startXRatio: Float = 0f,
    var startYRatio: Float = 0f,
    var progress: Float = 0f,
    var speed: Float = 0.9f,
    var lengthPx: Float = 160f,
    var angleRad: Float = 0.65f,
    var waitTime: Float = 0f
)

private class ConstellationNode(
    val baseXRatio: Float,
    val baseYRatio: Float,
    val ampXRatio: Float,
    val ampYRatio: Float,
    val speedX: Float,
    val speedY: Float,
    val phaseX: Float,
    val phaseY: Float,
    val radiusDp: Float
)

private class RippleCenter(
    val xRatio: Float,
    val yRatio: Float,
    val phaseOffset: Float,
    val maxRadiusDp: Float = 140f
)

@Composable
fun PlayfulBackgroundCanvas(
    modifier: Modifier = Modifier,
    style: String = "WAVY",
    backgroundOpacity: Float = 0.60f,
    blobColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    topHeaderPadding: Dp = 110.dp,
    blobAlphaMultiplier: Float = 1f,
    currencyParticleCount: Int = 18,
    enableFallingCurrencies: Boolean = true,
    enableBlobs: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val topPaddingPx = with(LocalDensity.current) { topHeaderPadding.toPx() }

    val currentBackgroundOpacity by rememberUpdatedState(backgroundOpacity)
    val opacityAnim = remember { Animatable(0f) }
    var isStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Delay a couple of seconds so homepage finishes initial layout and Room queries
        delay(2000L)
        isStarted = true
        opacityAnim.animateTo(
            targetValue = currentBackgroundOpacity,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(backgroundOpacity) {
        if (isStarted) {
            opacityAnim.animateTo(
                targetValue = backgroundOpacity,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    val effectiveOpacity = (opacityAnim.value * blobAlphaMultiplier).coerceIn(0f, 1f)

    val currentStyle by rememberUpdatedState(
        when (style) {
            "MINIMAL_GEOMETRIC" -> "WAVY"
            else -> style
        }
    )

    // Reusable Paths to avoid per-frame allocations
    val wavePath = remember { Path() }
    val auroraPath = remember { Path() }

    // Particles initialization (Floating Dust / Fireflies)
    val particles = remember {
        val random = Random(1337)
        List(42) { index ->
            FloatingParticle(
                xRatio = random.nextFloat(),
                yRatio = random.nextFloat() * 1.1f - 0.05f,
                radiusDp = 1.6f + random.nextFloat() * 2.8f,
                speedY = 0.028f + random.nextFloat() * 0.038f,
                swayAmplitudePx = 8f + random.nextFloat() * 16f,
                swayFrequency = 0.35f + random.nextFloat() * 0.45f,
                pulseSpeed = 0.6f + random.nextFloat() * 1.2f,
                phase = random.nextFloat() * 6.28f,
                colorIndex = index % 3
            )
        }
    }

    // Twinkling stars for Shooting Stars background
    val shootingStarTwinkles = remember {
        val random = Random(2026)
        List(48) {
            Star(
                xRatio = random.nextFloat(),
                yRatio = random.nextFloat(),
                sizeDp = 1.0f + random.nextFloat() * 1.8f,
                twinkleSpeed = 0.8f + random.nextFloat() * 1.6f,
                phase = random.nextFloat() * 6.28f
            )
        }
    }

    // Meteors for Shooting Stars background
    val meteors = remember {
        val random = Random(777)
        List(3) { index ->
            Meteor(
                active = false,
                waitTime = 0.5f + index * 1.2f + random.nextFloat() * 0.8f
            )
        }
    }

    // Space scene stars (multi-depth)
    val spaceStars = remember {
        val random = Random(4242)
        buildList {
            // Layer 1: Distant micro stars
            repeat(40) {
                add(
                    Star(
                        xRatio = random.nextFloat(),
                        yRatio = random.nextFloat(),
                        sizeDp = 0.9f + random.nextFloat() * 0.6f,
                        twinkleSpeed = 0.4f + random.nextFloat() * 0.8f,
                        phase = random.nextFloat() * 6.28f,
                        layer = 1
                    )
                )
            }
            // Layer 2: Mid-ground stars
            repeat(25) {
                add(
                    Star(
                        xRatio = random.nextFloat(),
                        yRatio = random.nextFloat(),
                        sizeDp = 1.8f + random.nextFloat() * 0.9f,
                        twinkleSpeed = 0.9f + random.nextFloat() * 1.3f,
                        phase = random.nextFloat() * 6.28f,
                        layer = 2
                    )
                )
            }
            // Layer 3: Prominent beacon stars with cross diffraction spikes
            repeat(8) {
                add(
                    Star(
                        xRatio = 0.08f + random.nextFloat() * 0.84f,
                        yRatio = 0.06f + random.nextFloat() * 0.88f,
                        sizeDp = 2.8f + random.nextFloat() * 0.8f,
                        twinkleSpeed = 1.4f + random.nextFloat() * 1.2f,
                        phase = random.nextFloat() * 6.28f,
                        layer = 3
                    )
                )
            }
        }
    }

    // Constellation dynamic nodes
    val constellationNodes = remember {
        val random = Random(888)
        List(24) {
            ConstellationNode(
                baseXRatio = 0.08f + random.nextFloat() * 0.84f,
                baseYRatio = 0.10f + random.nextFloat() * 0.82f,
                ampXRatio = 0.03f + random.nextFloat() * 0.05f,
                ampYRatio = 0.03f + random.nextFloat() * 0.05f,
                speedX = 0.18f + random.nextFloat() * 0.22f,
                speedY = 0.15f + random.nextFloat() * 0.20f,
                phaseX = random.nextFloat() * 6.28f,
                phaseY = random.nextFloat() * 6.28f,
                radiusDp = 2.2f + random.nextFloat() * 1.4f
            )
        }
    }

    // Ripple centers
    val rippleCenters = remember {
        listOf(
            RippleCenter(xRatio = 0.32f, yRatio = 0.28f, phaseOffset = 0.0f, maxRadiusDp = 135f),
            RippleCenter(xRatio = 0.74f, yRatio = 0.48f, phaseOffset = 0.33f, maxRadiusDp = 150f),
            RippleCenter(xRatio = 0.22f, yRatio = 0.68f, phaseOffset = 0.66f, maxRadiusDp = 130f),
            RippleCenter(xRatio = 0.68f, yRatio = 0.82f, phaseOffset = 0.18f, maxRadiusDp = 140f)
        )
    }

    var animationTime by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isStarted) {
        if (!isStarted) return@LaunchedEffect
        var lastTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                val dt = ((frameTime - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = frameTime
                animationTime += dt

                when (currentStyle) {
                    "PARTICLES" -> {
                        particles.forEach { p ->
                            p.yRatio -= p.speedY * dt
                            if (p.yRatio < -0.05f) {
                                p.yRatio = 1.05f
                                p.xRatio = Random.nextFloat()
                            }
                        }
                    }
                    "SHOOTING_STARS" -> {
                        meteors.forEach { m ->
                            if (!m.active) {
                                m.waitTime -= dt
                                if (m.waitTime <= 0f) {
                                    m.active = true
                                    m.progress = 0f
                                    m.startXRatio = 0.05f + Random.nextFloat() * 0.70f
                                    m.startYRatio = 0.02f + Random.nextFloat() * 0.40f
                                    m.angleRad = 0.55f + Random.nextFloat() * 0.25f
                                    m.speed = 0.75f + Random.nextFloat() * 0.60f
                                    m.lengthPx = 140f + Random.nextFloat() * 120f
                                }
                            } else {
                                m.progress += m.speed * dt
                                if (m.progress >= 1.25f) {
                                    m.active = false
                                    m.waitTime = 0.6f + Random.nextFloat() * 2.2f
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f || effectiveOpacity <= 0.001f) return@Canvas
        val minDim = min(width, height)

        when (currentStyle) {
            // 1. WAVY BACKGROUND (Geometric Waves) - retained and enhanced
            "WAVY", "MINIMAL_GEOMETRIC" -> {
                val waveCount = 6
                val stepY = (height - topPaddingPx) / (waveCount + 1)
                val baseStrokeAlpha = (if (isDark) 0.52f else 0.38f) * effectiveOpacity

                for (i in 0 until waveCount) {
                    val baseY = topPaddingPx + stepY * (i + 1)
                    val color = if (i % 2 == 0) blobColor else secondaryColor
                    wavePath.reset()
                    wavePath.moveTo(0f, baseY)
                    var x = 0f
                    val waveLength = width / 2.2f
                    val waveHeight = 22.dp.toPx()
                    val speed = 0.55f + i * 0.08f
                    val phase = i * 0.9f
                    while (x <= width + 40f) {
                        val y = baseY +
                                sin((x / waveLength) * 2 * Math.PI.toFloat() + animationTime * speed + phase) * waveHeight +
                                cos((x / (waveLength * 0.6f)) * 2 * Math.PI.toFloat() + animationTime * (speed * 0.7f) + phase * 1.3f) * (waveHeight * 0.35f)
                        wavePath.lineTo(x, y)
                        x += 16f
                    }
                    drawPath(
                        path = wavePath,
                        color = color.copy(alpha = baseStrokeAlpha),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 2. CONCENTRIC RINGS
            "RINGS" -> {
                val ringCount = 7
                val maxRadius = max(width, height) * 0.75f
                val cx = width * 0.5f
                val cy = topPaddingPx + (height - topPaddingPx) * 0.38f
                val baseAlpha = (if (isDark) 0.55f else 0.40f) * effectiveOpacity

                // Primary expanding harmonic rings
                for (i in 0 until ringCount) {
                    val progress = (animationTime * 0.10f + i.toFloat() / ringCount) % 1f
                    val radius = progress * maxRadius
                    val ringAlpha = sin(progress * Math.PI.toFloat()) * baseAlpha
                    val color = when (i % 3) {
                        0 -> blobColor
                        1 -> secondaryColor
                        else -> tertiaryColor
                    }
                    drawCircle(
                        color = color.copy(alpha = ringAlpha.coerceIn(0f, 1f)),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = (2.4f - progress * 0.8f).dp.toPx())
                    )
                }

                // Secondary subtle harmonic epicenter (lower right) for optical interference
                val cx2 = width * 0.80f
                val cy2 = topPaddingPx + (height - topPaddingPx) * 0.75f
                for (i in 0 until 5) {
                    val progress = (animationTime * 0.08f + i.toFloat() / 5f) % 1f
                    val radius = progress * (maxRadius * 0.55f)
                    val ringAlpha = sin(progress * Math.PI.toFloat()) * (baseAlpha * 0.65f)
                    drawCircle(
                        color = tertiaryColor.copy(alpha = ringAlpha.coerceIn(0f, 1f)),
                        radius = radius,
                        center = Offset(cx2, cy2),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }

            // 3. FLOATING PARTICLES (Stardust / Fireflies)
            "PARTICLES" -> {
                val baseAlpha = (if (isDark) 0.75f else 0.55f) * effectiveOpacity

                particles.forEach { p ->
                    val x = width * p.xRatio + p.swayAmplitudePx * sin(animationTime * p.swayFrequency + p.phase)
                    val y = height * p.yRatio
                    val pulse = 0.55f + 0.45f * sin(animationTime * p.pulseSpeed + p.phase)
                    val alpha = (pulse * baseAlpha).coerceIn(0f, 1f)
                    val color = when (p.colorIndex) {
                        0 -> blobColor
                        1 -> secondaryColor
                        else -> tertiaryColor
                    }
                    val r = p.radiusDp.dp.toPx()
                    // Soft outer glow halo
                    drawCircle(
                        color = color.copy(alpha = alpha * 0.30f),
                        radius = r * 2.5f,
                        center = Offset(x, y)
                    )
                    // Bright core
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = r,
                        center = Offset(x, y)
                    )
                }
            }

            // 4. SHOOTING STARS & TWINKLING SKY
            "SHOOTING_STARS" -> {
                // Background twinkling stars
                shootingStarTwinkles.forEach { s ->
                    val x = width * s.xRatio
                    val y = height * s.yRatio
                    val twinkle = 0.25f + 0.75f * abs(sin(animationTime * s.twinkleSpeed + s.phase))
                    val alpha = twinkle * (if (isDark) 0.60f else 0.42f) * effectiveOpacity
                    val r = s.sizeDp.dp.toPx()
                    drawCircle(
                        color = blobColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                        radius = r,
                        center = Offset(x, y)
                    )
                }

                // Active shooting stars (meteors)
                meteors.forEach { m ->
                    if (m.active && m.progress in 0f..1.2f) {
                        val totalDist = minDim * 1.4f
                        val headX = width * m.startXRatio + cos(m.angleRad) * (m.progress * totalDist)
                        val headY = height * m.startYRatio + sin(m.angleRad) * (m.progress * totalDist)
                        val tailX = headX - cos(m.angleRad) * m.lengthPx
                        val tailY = headY - sin(m.angleRad) * m.lengthPx

                        val env = when {
                            m.progress < 0.2f -> (m.progress / 0.2f)
                            m.progress > 0.8f -> ((1.2f - m.progress) / 0.4f).coerceAtLeast(0f)
                            else -> 1f
                        }
                        val headAlpha = (0.92f * env * effectiveOpacity).coerceIn(0f, 1f)

                        // Meteor trail gradient line
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(blobColor.copy(alpha = headAlpha), secondaryColor.copy(alpha = 0f)),
                                start = Offset(headX, headY),
                                end = Offset(tailX, tailY)
                            ),
                            start = Offset(headX, headY),
                            end = Offset(tailX, tailY),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Radiant glowing head
                        drawCircle(
                            color = blobColor.copy(alpha = headAlpha),
                            radius = 2.5.dp.toPx(),
                            center = Offset(headX, headY)
                        )
                        drawCircle(
                            color = secondaryColor.copy(alpha = headAlpha * 0.40f),
                            radius = 5.5.dp.toPx(),
                            center = Offset(headX, headY)
                        )
                    }
                }
            }

            // 5. DEEP SPACE & COSMIC NEBULA
            "SPACE" -> {
                // Soft Cosmic Nebula Clouds
                val nebulaBreathing = sin(animationTime * 0.2f) * 0.08f
                val r1 = minDim * (0.75f + nebulaBreathing)
                val cx1 = width * 0.28f
                val cy1 = topPaddingPx + (height - topPaddingPx) * 0.32f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor.copy(alpha = 0.28f * effectiveOpacity), Color.Transparent),
                        center = Offset(cx1, cy1),
                        radius = r1
                    ),
                    radius = r1,
                    center = Offset(cx1, cy1)
                )

                val r2 = minDim * (0.68f - nebulaBreathing)
                val cx2 = width * 0.78f
                val cy2 = topPaddingPx + (height - topPaddingPx) * 0.62f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiaryColor.copy(alpha = 0.24f * effectiveOpacity), Color.Transparent),
                        center = Offset(cx2, cy2),
                        radius = r2
                    ),
                    radius = r2,
                    center = Offset(cx2, cy2)
                )

                val r3 = minDim * (0.62f + nebulaBreathing * 0.5f)
                val cx3 = width * 0.42f
                val cy3 = topPaddingPx + (height - topPaddingPx) * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryColor.copy(alpha = 0.20f * effectiveOpacity), Color.Transparent),
                        center = Offset(cx3, cy3),
                        radius = r3
                    ),
                    radius = r3,
                    center = Offset(cx3, cy3)
                )

                // Multi-depth Starfield
                spaceStars.forEach { s ->
                    val x = width * s.xRatio
                    val y = height * s.yRatio
                    val twinkle = 0.30f + 0.70f * abs(sin(animationTime * s.twinkleSpeed + s.phase))
                    val baseAlpha = when (s.layer) {
                        1 -> (0.42f * twinkle * effectiveOpacity)
                        2 -> (0.72f * twinkle * effectiveOpacity)
                        else -> (0.92f * twinkle * effectiveOpacity)
                    }
                    val r = s.sizeDp.dp.toPx()
                    drawCircle(
                        color = blobColor.copy(alpha = baseAlpha.coerceIn(0f, 1f)),
                        radius = r,
                        center = Offset(x, y)
                    )

                    // Diffraction spikes for layer 3 beacon stars (4-point cross)
                    if (s.layer == 3) {
                        val spikeLen = 8.dp.toPx()
                        val spikeAlpha = (baseAlpha * 0.50f).coerceIn(0f, 1f)
                        drawLine(
                            color = blobColor.copy(alpha = spikeAlpha),
                            start = Offset(x - spikeLen, y),
                            end = Offset(x + spikeLen, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = blobColor.copy(alpha = spikeAlpha),
                            start = Offset(x, y - spikeLen),
                            end = Offset(x, y + spikeLen),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // 6. CONSTELLATION WEB
            "CONSTELLATION" -> {
                val baseAlpha = (if (isDark) 0.65f else 0.45f) * effectiveOpacity
                val thresholdPx = 115.dp.toPx()
                val thresholdSq = thresholdPx * thresholdPx

                // Calculate positions for nodes
                val nodePositions = constellationNodes.map { node ->
                    val x = width * (node.baseXRatio + node.ampXRatio * sin(animationTime * node.speedX + node.phaseX))
                    val y = height * (node.baseYRatio + node.ampYRatio * cos(animationTime * node.speedY + node.phaseY))
                    Offset(x, y)
                }

                // Draw connection lines between nearby nodes
                for (i in 0 until nodePositions.size) {
                    val p1 = nodePositions[i]
                    for (j in i + 1 until nodePositions.size) {
                        val p2 = nodePositions[j]
                        val dx = p1.x - p2.x
                        val dy = p1.y - p2.y
                        val distSq = dx * dx + dy * dy
                        if (distSq < thresholdSq) {
                            val dist = sqrt(distSq)
                            val lineAlpha = (1f - dist / thresholdPx) * (baseAlpha * 0.55f)
                            drawLine(
                                color = blobColor.copy(alpha = lineAlpha.coerceIn(0f, 1f)),
                                start = p1,
                                end = p2,
                                strokeWidth = 1.2.dp.toPx()
                            )
                        }
                    }
                }

                // Draw node stars
                for (i in 0 until nodePositions.size) {
                    val pos = nodePositions[i]
                    val r = constellationNodes[i].radiusDp.dp.toPx()
                    drawCircle(
                        color = blobColor.copy(alpha = baseAlpha),
                        radius = r,
                        center = pos
                    )
                    drawCircle(
                        color = secondaryColor.copy(alpha = baseAlpha * 0.35f),
                        radius = r * 2.2f,
                        center = pos
                    )
                }
            }

            // 7. WATER RIPPLES
            "RIPPLES" -> {
                val baseAlpha = (if (isDark) 0.55f else 0.40f) * effectiveOpacity
                rippleCenters.forEach { center ->
                    val cx = width * center.xRatio
                    val cy = topPaddingPx + (height - topPaddingPx) * center.yRatio
                    val maxR = center.maxRadiusDp.dp.toPx()

                    for (ring in 0 until 3) {
                        val progress = (animationTime * 0.22f + center.phaseOffset + ring * 0.33f) % 1f
                        val r = progress * maxR
                        val alpha = (1f - progress) * baseAlpha
                        drawCircle(
                            color = blobColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = secondaryColor.copy(alpha = baseAlpha * 0.60f),
                        radius = 2.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            // 8. SOFT AURORA CURTAINS
            "AURORA" -> {
                val curtainCount = 3
                val curtainColors = listOf(blobColor, tertiaryColor, secondaryColor)
                val baseAlpha = (if (isDark) 0.35f else 0.22f) * effectiveOpacity

                for (c in 0 until curtainCount) {
                    val color = curtainColors[c % curtainColors.size]
                    auroraPath.reset()
                    val startX = width * (0.15f + c * 0.35f)
                    val widthSpread = width * 0.45f
                    val speed = 0.25f + c * 0.08f
                    val phase = c * 1.4f

                    auroraPath.moveTo(startX, 0f)
                    var curY = 0f
                    while (curY <= height) {
                        val sway = sin((curY / height) * 3 * Math.PI.toFloat() + animationTime * speed + phase) * (36.dp.toPx())
                        auroraPath.lineTo(startX + sway, curY)
                        curY += 24f
                    }
                    auroraPath.lineTo(startX + widthSpread, height)
                    curY = height
                    while (curY >= 0f) {
                        val sway = sin((curY / height) * 3 * Math.PI.toFloat() + animationTime * speed + phase + 0.5f) * (36.dp.toPx())
                        auroraPath.lineTo(startX + widthSpread + sway, curY)
                        curY -= 24f
                    }
                    auroraPath.close()

                    drawPath(
                        path = auroraPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                color.copy(alpha = baseAlpha),
                                color.copy(alpha = baseAlpha * 0.7f),
                                Color.Transparent
                            ),
                            startY = topPaddingPx,
                            endY = height
                        )
                    )
                }
            }

            else -> {
                // Fallback to Wavy lines
                val waveCount = 6
                val stepY = (height - topPaddingPx) / (waveCount + 1)
                val baseStrokeAlpha = (if (isDark) 0.52f else 0.38f) * effectiveOpacity

                for (i in 0 until waveCount) {
                    val baseY = topPaddingPx + stepY * (i + 1)
                    val color = if (i % 2 == 0) blobColor else secondaryColor
                    wavePath.reset()
                    wavePath.moveTo(0f, baseY)
                    var x = 0f
                    val waveLength = width / 2.2f
                    val waveHeight = 22.dp.toPx()
                    val speed = 0.55f + i * 0.08f
                    val phase = i * 0.9f
                    while (x <= width + 40f) {
                        val y = baseY +
                                sin((x / waveLength) * 2 * Math.PI.toFloat() + animationTime * speed + phase) * waveHeight +
                                cos((x / (waveLength * 0.6f)) * 2 * Math.PI.toFloat() + animationTime * (speed * 0.7f) + phase * 1.3f) * (waveHeight * 0.35f)
                        wavePath.lineTo(x, y)
                        x += 16f
                    }
                    drawPath(
                        path = wavePath,
                        color = color.copy(alpha = baseStrokeAlpha),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
