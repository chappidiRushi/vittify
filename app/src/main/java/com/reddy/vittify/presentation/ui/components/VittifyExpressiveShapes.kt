package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val LocalListItemPressed = compositionLocalOf { false }

enum class VittifySvgShape(val pathData: String) {
    SCALLOP_12("M34.4683 19.8398C35.1732 18.7201 36.8268 18.7201 37.5317 19.8398L39.2214 22.5237C39.6848 23.2599 40.6197 23.5592 41.4338 23.232L44.4021 22.0391C45.6404 21.5415 46.9781 22.499 46.8804 23.8131L46.6461 26.963C46.5819 27.8269 47.1597 28.6104 48.0135 28.8172L51.1265 29.571C52.4252 29.8855 52.9362 31.4349 52.0731 32.4414L50.0044 34.854C49.437 35.5158 49.437 36.4842 50.0044 37.146L52.0731 39.5586C52.9362 40.5651 52.4252 42.1145 51.1265 42.429L48.0135 43.1828C47.1597 43.3896 46.5819 44.1731 46.6461 45.037L46.8804 48.1869C46.9781 49.501 45.6404 50.4585 44.4021 49.9609L41.4338 48.768C40.6197 48.4408 39.6848 48.7401 39.2214 49.4763L37.5317 52.1602C36.8268 53.2799 35.1732 53.2799 34.4683 52.1602L32.7786 49.4763C32.3152 48.7401 31.3803 48.4408 30.5662 48.768L27.5979 49.9609C26.3596 50.4585 25.0219 49.501 25.1196 48.1869L25.3539 45.037C25.4181 44.1731 24.8403 43.3896 23.9865 43.1828L20.8735 42.429C19.5748 42.1145 19.0638 40.5651 19.9268 39.5586L21.9956 37.146C22.563 36.4842 22.563 35.5158 21.9956 34.854L19.9268 32.4414C19.0638 31.4349 19.5748 29.8855 20.8735 29.571L23.9865 28.8172C24.8403 28.6104 25.4181 27.8269 25.3539 26.963L25.1196 23.8131C25.0219 22.499 26.3596 21.5415 27.5979 22.0391L30.5662 23.232C31.3803 23.5592 32.3152 23.2599 32.7786 22.5237L34.4683 19.8398Z"),
    COOKIE_8("M32.3091 92.6036C32.4924 92.454 32.584 92.3791 32.6677 92.316C34.6389 90.8279 37.3611 90.8279 39.3323 92.316C39.416 92.3791 39.5076 92.454 39.6909 92.6036C39.7727 92.6704 39.8136 92.7038 39.8541 92.7356C40.7818 93.4644 41.9191 93.8774 43.0993 93.914C43.1508 93.9156 43.2037 93.9163 43.3094 93.9176C43.5462 93.9205 43.6646 93.922 43.7694 93.9273C46.2381 94.0516 48.3234 95.7974 48.8748 98.2015C48.8982 98.3035 48.9202 98.4196 48.9642 98.6518C48.9838 98.7554 48.9937 98.8072 49.0042 98.8575C49.2452 100.011 49.8504 101.057 50.7309 101.842C50.7693 101.876 50.8094 101.91 50.8895 101.979C51.069 102.133 51.1588 102.21 51.2357 102.281C53.0467 103.96 53.5194 106.635 52.393 108.83C52.3451 108.923 52.2872 109.026 52.1714 109.232C52.1197 109.324 52.0938 109.37 52.0694 109.415C51.5111 110.454 51.3009 111.643 51.4697 112.809C51.4771 112.86 51.4856 112.912 51.5027 113.016C51.5409 113.249 51.56 113.366 51.573 113.469C51.879 115.917 50.5179 118.269 48.2407 119.228C48.1441 119.269 48.0333 119.311 47.8118 119.394C47.7129 119.431 47.6635 119.45 47.6156 119.469C46.5192 119.906 45.592 120.683 44.9701 121.684C44.943 121.728 44.916 121.773 44.862 121.864C44.741 122.067 44.6806 122.168 44.6236 122.256C43.2814 124.327 40.7234 125.256 38.3609 124.531C38.2606 124.5 38.1489 124.461 37.9253 124.383C37.8256 124.348 37.7757 124.33 37.7268 124.314C36.6052 123.946 35.3948 123.946 34.2732 124.314C34.2243 124.33 34.1744 124.348 34.0747 124.383C33.8511 124.461 33.7394 124.5 33.6391 124.531C31.2767 125.256 28.7186 124.327 27.3764 122.256C27.3194 122.168 27.259 122.067 27.138 121.864C27.084 121.773 27.057 121.728 27.0299 121.684C26.408 120.683 25.4808 119.906 24.3844 119.469C24.3365 119.45 24.2871 119.431 24.1882 119.394C23.9667 119.311 23.8559 119.269 23.7593 119.228C21.4821 118.269 20.121 115.917 20.427 113.469C20.44 113.366 20.4591 113.249 20.4973 113.016C20.5144 112.912 20.5229 112.86 20.5303 112.809C20.6991 111.643 20.4889 110.454 19.9306 109.415C19.9062 109.37 19.8804 109.324 19.8286 109.232C19.7128 109.026 19.6549 108.923 19.607 108.83C18.4806 106.635 18.9533 103.96 20.7643 102.281C20.8412 102.21 20.931 102.133 21.1105 101.979C21.1906 101.91 21.2307 101.876 21.2691 101.842C22.1496 101.057 22.7548 100.011 22.9958 98.8575C23.0063 98.8072 23.0162 98.7554 23.0358 98.6518C23.0798 98.4196 23.1019 98.3035 23.1253 98.2015C23.6766 95.7974 25.7619 94.0516 28.2306 93.9273C28.3354 93.922 28.4538 93.9205 28.6906 93.9176C28.7963 93.9163 28.8492 93.9156 28.9007 93.914C30.0809 93.8774 31.2182 93.4644 32.1459 92.7356C32.1864 92.7038 32.2273 92.6704 32.3091 92.6036Z"),
    PENTAGON("M30.6481 165.937C32.3067 164.729 33.136 164.125 34.022 163.825C35.3035 163.392 36.6965 163.392 37.978 163.825C38.864 164.125 39.6933 164.729 41.3519 165.937L44.9794 168.58L48.6049 171.056C50.3249 172.231 51.1849 172.819 51.7598 173.557C52.5914 174.626 53.0282 175.939 52.9986 177.282C52.9781 178.211 52.6363 179.182 51.9526 181.123L50.529 185.166L49.215 189.308C48.5983 191.252 48.29 192.224 47.7509 192.98C46.9712 194.074 45.838 194.878 44.5341 195.263C43.6327 195.53 42.5936 195.514 40.5154 195.483L36 195.416L31.4846 195.483C29.4064 195.514 28.3673 195.53 27.4659 195.263C26.162 194.878 25.0288 194.074 24.2491 192.98C23.71 192.224 23.4017 191.252 22.785 189.308L21.471 185.166L20.0474 181.123C19.3637 179.182 19.0219 178.211 19.0014 177.282C18.9718 175.939 19.4086 174.626 20.2402 173.557C20.8151 172.819 21.6751 172.231 23.395 171.056L27.0206 168.58L30.6481 165.937Z"),
    TILTED_PILL("M28.9857 240.777C34.0217 235.741 42.1869 235.741 47.2229 240.777C52.259 245.813 52.259 253.978 47.2229 259.014L43.0143 263.223C37.9783 268.259 29.8131 268.259 24.7771 263.223C19.741 258.187 19.741 250.022 24.7771 244.986L28.9857 240.777Z"),
    STAR_8("M45.1856 311.124C46.2431 311.196 46.7718 311.232 47.1991 311.418C47.8175 311.689 48.3111 312.183 48.5816 312.801C48.7685 313.228 48.8044 313.757 48.8762 314.814L49.0402 317.227C49.0693 317.655 49.0838 317.869 49.1303 318.073C49.1975 318.368 49.3142 318.65 49.4754 318.906C49.5869 319.084 49.7279 319.245 50.0097 319.568L51.6001 321.39C52.297 322.189 52.6455 322.588 52.8155 323.022C53.0615 323.651 53.0615 324.349 52.8155 324.978C52.6455 325.412 52.297 325.811 51.6001 326.61L50.0097 328.432C49.7279 328.755 49.5869 328.916 49.4754 329.094C49.3142 329.35 49.1975 329.632 49.1303 329.927C49.0838 330.131 49.0693 330.345 49.0402 330.773L48.8762 333.186C48.8044 334.243 48.7685 334.772 48.5816 335.199C48.3111 335.817 47.8175 336.311 47.1991 336.582C46.7718 336.768 46.2431 336.804 45.1856 336.876L42.7725 337.04C42.3448 337.069 42.131 337.084 41.9267 337.13C41.6316 337.197 41.3499 337.314 41.0937 337.475C40.9164 337.587 40.7549 337.728 40.4319 338.01L38.6097 339.6C37.8111 340.297 37.4119 340.645 36.9775 340.815C36.3491 341.062 35.6509 341.062 35.0225 340.815C34.5881 340.645 34.1889 340.297 33.3903 339.6L31.5681 338.01C31.2451 337.728 31.0836 337.587 30.9063 337.475C30.6501 337.314 30.3684 337.197 30.0733 337.13C29.869 337.084 29.6552 337.069 29.2275 337.04L26.8144 336.876C25.7569 336.804 25.2282 336.768 24.8009 336.582C24.1825 336.311 23.6889 335.817 23.4184 335.199C23.2315 334.772 23.1956 334.243 23.1238 333.186L22.9598 330.773C22.9307 330.345 22.9162 330.131 22.8697 329.927C22.8025 329.632 22.6858 329.35 22.5246 329.094C22.4131 328.916 22.2721 328.755 21.9903 328.432L20.3999 326.61C19.703 325.811 19.3545 325.412 19.1845 324.978C18.9385 324.349 18.9385 323.651 19.1845 323.022C19.3545 322.588 19.703 322.189 20.3999 321.39L21.9903 319.568C22.2721 319.245 22.4131 319.084 22.5246 318.906C22.6858 318.65 22.8025 318.368 22.8697 318.073C22.9162 317.869 22.9307 317.655 22.9598 317.227L23.1238 314.814C23.1956 313.757 23.2315 313.228 23.4184 312.801C23.6889 312.183 24.1825 311.689 24.8009 311.418C25.2282 311.232 25.7569 311.196 26.8144 311.124L29.2275 310.96C29.6552 310.931 29.869 310.916 30.0733 310.87C30.3684 310.803 30.6501 310.686 30.9063 310.525C31.0836 310.413 31.2451 310.272 31.5681 309.99L33.3903 308.4C34.1889 307.703 34.5881 307.355 35.0225 307.185C35.6509 306.938 36.3491 306.938 36.9775 307.185C37.4119 307.355 37.8111 307.703 38.6097 308.4L40.4319 309.99C40.7549 310.272 40.9164 310.413 41.0937 310.525C41.3499 310.686 41.6316 310.803 41.9267 310.87C42.131 310.916 42.3448 310.931 42.7725 310.96L45.1856 311.124Z"),
    CLOVER_4("M39.8729 382.621C45.8872 380.009 51.9914 386.113 49.3793 392.127L48.9474 393.121C48.1499 394.958 48.1499 397.042 48.9474 398.879L49.3793 399.873C51.9914 405.887 45.8872 411.991 39.8729 409.379L38.8786 408.947C37.0424 408.15 34.9576 408.15 33.1214 408.947L32.1271 409.379C26.1128 411.991 20.0086 405.887 22.6207 399.873L23.0526 398.879C23.8501 397.042 23.8501 394.958 23.0526 393.121L22.6207 392.127C20.0086 386.113 26.1128 380.009 32.1271 382.621L33.1214 383.053C34.9576 383.85 37.0424 383.85 38.8786 383.053L39.8729 382.621Z"),
    TILTED_OVAL("M44.1309 476.131C37.1705 483.091 27.8877 485.094 23.3971 480.603C18.9065 476.112 20.9087 466.829 27.8691 459.869C34.8295 452.909 44.1123 450.906 48.6029 455.397C53.0935 459.888 51.0913 469.171 44.1309 476.131Z");

    val composeShape: Shape by lazy { SvgPathShape(pathData) }

    companion object {
        fun forIndex(index: Int): VittifySvgShape {
            val values = entries
            return values[(index % values.size + values.size) % values.size]
        }
    }
}

class SvgPathShape(private val pathData: String) : Shape {
    private val basePath: Path by lazy {
        try {
            PathParser().parsePathString(pathData).toPath()
        } catch (_: Exception) {
            Path()
        }
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val bounds: Rect = basePath.getBounds()
        if (bounds.isEmpty || bounds.width <= 0f || bounds.height <= 0f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }

        val scaleX = size.width / bounds.width
        val scaleY = size.height / bounds.height
        val uniformScale = minOf(scaleX, scaleY)
        val dx = (size.width - bounds.width * uniformScale) / 2f
        val dy = (size.height - bounds.height * uniformScale) / 2f

        val finalPath = Path().apply {
            addPath(basePath)
            translate(Offset(-bounds.left, -bounds.top))
        }
        val scaleMatrix = Matrix().apply {
            scale(uniformScale, uniformScale)
        }
        finalPath.transform(scaleMatrix)
        finalPath.translate(Offset(dx, dy))
        return Outline.Generic(finalPath)
    }
}

@Composable
fun SettingsShapeIconBadge(
    icon: ImageVector,
    shape: VittifySvgShape,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    contentDescription: String? = null
) {
    val isRowPressed = LocalListItemPressed.current
    val entranceAnim = remember { Animatable(0.76f) }

    LaunchedEffect(Unit) {
        entranceAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = 380f
            )
        )
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isRowPressed) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 450f
        ),
        label = "badge_press_scale"
    )

    val pressRotation by animateFloatAsState(
        targetValue = if (isRowPressed) -8f else 0f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 420f
        ),
        label = "badge_press_rotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val combinedScale = entranceAnim.value * pressScale
                scaleX = combinedScale
                scaleY = combinedScale
                rotationZ = pressRotation
            }
            .clip(shape.composeShape)
            .background(containerColor, shape.composeShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun SettingsShapeIconBadge(
    painter: Painter,
    shape: VittifySvgShape,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    contentDescription: String? = null
) {
    val isRowPressed = LocalListItemPressed.current
    val entranceAnim = remember { Animatable(0.76f) }

    LaunchedEffect(Unit) {
        entranceAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = 380f
            )
        )
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isRowPressed) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 450f
        ),
        label = "badge_painter_press_scale"
    )

    val pressRotation by animateFloatAsState(
        targetValue = if (isRowPressed) -8f else 0f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 420f
        ),
        label = "badge_painter_press_rotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val combinedScale = entranceAnim.value * pressScale
                scaleX = combinedScale
                scaleY = combinedScale
                rotationZ = pressRotation
            }
            .clip(shape.composeShape)
            .background(containerColor, shape.composeShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
