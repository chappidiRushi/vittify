package com.reddy.vittify.presentation.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconax.VittifyOutline: ImageVector
    get() {
        if (_VittifyOutline != null) {
            return _VittifyOutline!!
        }
        _VittifyOutline = ImageVector.Builder(
            name = "VittifyOutline",
            defaultWidth = 192.dp,
            defaultHeight = 192.dp,
            viewportWidth = 192f,
            viewportHeight = 192f
        ).apply {
            // Outline Layer 1: Back Card Upper Arc
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(42f, 88f)
                lineTo(42f, 60f)
                arcTo(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = true, 66f, 36f)
                lineTo(126f, 36f)
                arcTo(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = true, 150f, 60f)
                lineTo(150f, 88f)
            }

            // Outline Layer 2: Glowing Coin Arc
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(64f, 44f)
                arcTo(34f, 34f, 0f, isMoreThanHalf = false, isPositiveArc = true, 128f, 44f)
            }

            // Outline Layer 3: Front Sleeve Pocket
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(38f, 94f)
                curveTo(38f, 85f, 45f, 78f, 54f, 78f)
                lineTo(72f, 78f)
                curveTo(80f, 78f, 86f, 84f, 96f, 88f)
                curveTo(106f, 84f, 112f, 78f, 120f, 78f)
                lineTo(138f, 78f)
                curveTo(147f, 78f, 154f, 85f, 154f, 94f)
                lineTo(154f, 134f)
                curveTo(154f, 147f, 143f, 158f, 130f, 158f)
                lineTo(62f, 158f)
                curveTo(49f, 158f, 38f, 147f, 38f, 134f)
                close()
            }

            // Outline Layer 4: Indian Rupee ('₹') Symbol
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(80f, 107f)
                lineTo(112f, 107f)
                moveTo(80f, 117f)
                lineTo(112f, 117f)
                moveTo(86f, 107f)
                lineTo(86f, 117f)
                curveTo(104f, 117f, 104f, 129f, 86f, 129f)
                moveTo(86f, 129f)
                lineTo(108f, 147f)
            }
        }.build()

        return _VittifyOutline!!
    }

@Suppress("ObjectPropertyName")
private var _VittifyOutline: ImageVector? = null
