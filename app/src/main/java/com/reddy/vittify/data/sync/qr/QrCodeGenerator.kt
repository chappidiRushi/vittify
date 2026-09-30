package com.reddy.vittify.data.sync.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrCodeGenerator {

    /**
     * Generates a modern Material 3 Expressive QR Code with rounded modules,
     * squircle finder eyes, and an optional center logo/badge watermark.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 600,
        context: Context? = null,
        centerLogoResId: Int? = null,
        primaryColor: Int = Color.parseColor("#1B3B2B"),
        dataColor: Int = Color.parseColor("#263238")
    ): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 0)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val matrixSize = bitMatrix.width

            val quietZoneModules = 2
            val totalModules = matrixSize + (quietZoneModules * 2)
            val moduleSize = size.toFloat() / totalModules
            val offset = quietZoneModules * moduleSize

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawColor(Color.WHITE)

            // Paints
            val dataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = dataColor
                style = Paint.Style.FILL
            }
            val eyeOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                style = Paint.Style.FILL
            }
            val eyeInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                style = Paint.Style.FILL
            }
            val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                alpha = 60
                style = Paint.Style.STROKE
                strokeWidth = moduleSize * 0.35f
            }

            // Reserve center for brand logo/watermark
            val centerModules = 7
            val centerStart = (matrixSize - centerModules) / 2
            val centerEnd = centerStart + centerModules

            fun isInCenter(x: Int, y: Int): Boolean {
                return x in centerStart until centerEnd && y in centerStart until centerEnd
            }

            fun isInFinderPattern(x: Int, y: Int): Boolean {
                val inTopLeft = x in 0..6 && y in 0..6
                val inTopRight = x in (matrixSize - 7) until matrixSize && y in 0..6
                val inBottomLeft = x in 0..6 && y in (matrixSize - 7) until matrixSize
                return inTopLeft || inTopRight || inBottomLeft
            }

            // Draw Data Modules with rounded squircles
            val dataCornerRadius = moduleSize * 0.35f
            for (x in 0 until matrixSize) {
                for (y in 0 until matrixSize) {
                    if (bitMatrix[x, y] && !isInFinderPattern(x, y) && !isInCenter(x, y)) {
                        val left = offset + x * moduleSize + (moduleSize * 0.08f)
                        val top = offset + y * moduleSize + (moduleSize * 0.08f)
                        val right = offset + (x + 1) * moduleSize - (moduleSize * 0.08f)
                        val bottom = offset + (y + 1) * moduleSize - (moduleSize * 0.08f)
                        canvas.drawRoundRect(
                            RectF(left, top, right, bottom),
                            dataCornerRadius,
                            dataCornerRadius,
                            dataPaint
                        )
                    }
                }
            }

            // Draw Finder Patterns (Eyes)
            val eyePositions = listOf(
                Pair(0, 0),
                Pair(matrixSize - 7, 0),
                Pair(0, matrixSize - 7)
            )

            for ((ex, ey) in eyePositions) {
                val eyeLeft = offset + ex * moduleSize
                val eyeTop = offset + ey * moduleSize
                val eyeRight = eyeLeft + 7 * moduleSize
                val eyeBottom = eyeTop + 7 * moduleSize

                // 1. Outer squircle (7x7)
                canvas.drawRoundRect(
                    RectF(eyeLeft, eyeTop, eyeRight, eyeBottom),
                    moduleSize * 1.8f,
                    moduleSize * 1.8f,
                    eyeOuterPaint
                )

                // 2. Inner white mask (5x5)
                val innerLeft = eyeLeft + moduleSize
                val innerTop = eyeTop + moduleSize
                val innerRight = eyeRight - moduleSize
                val innerBottom = eyeBottom - moduleSize
                canvas.drawRoundRect(
                    RectF(innerLeft, innerTop, innerRight, innerBottom),
                    moduleSize * 1.2f,
                    moduleSize * 1.2f,
                    whitePaint
                )

                // 3. Center pupil squircle (3x3)
                val pupilLeft = eyeLeft + 2 * moduleSize
                val pupilTop = eyeTop + 2 * moduleSize
                val pupilRight = eyeRight - 2 * moduleSize
                val pupilBottom = eyeBottom - 2 * moduleSize
                canvas.drawRoundRect(
                    RectF(pupilLeft, pupilTop, pupilRight, pupilBottom),
                    moduleSize * 0.85f,
                    moduleSize * 0.85f,
                    eyeInnerPaint
                )
            }

            // Draw Center Badge & Watermark
            val badgeLeft = offset + centerStart * moduleSize - (moduleSize * 0.15f)
            val badgeTop = offset + centerStart * moduleSize - (moduleSize * 0.15f)
            val badgeRight = offset + centerEnd * moduleSize + (moduleSize * 0.15f)
            val badgeBottom = offset + centerEnd * moduleSize + (moduleSize * 0.15f)
            val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)
            val badgeRadius = (badgeRight - badgeLeft) * 0.32f

            // White badge with subtle border
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, whitePaint)
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, borderPaint)

            // Draw center icon (Vector Heart for pairing sync or brand icon)
            val badgeCenterX = (badgeLeft + badgeRight) / 2f
            val badgeCenterY = (badgeTop + badgeBottom) / 2f
            val iconSize = (badgeRight - badgeLeft) * 0.52f

            drawHeartWatermark(canvas, badgeCenterX, badgeCenterY, iconSize, primaryColor)

            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun drawHeartWatermark(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val halfSize = size / 2f
        val path = Path().apply {
            // Smooth bezier heart shape
            moveTo(centerX, centerY + halfSize * 0.65f)
            cubicTo(
                centerX - halfSize * 1.15f, centerY - halfSize * 0.25f,
                centerX - halfSize * 0.95f, centerY - halfSize * 0.95f,
                centerX - halfSize * 0.05f, centerY - halfSize * 0.35f
            )
            cubicTo(
                centerX + halfSize * 0.95f, centerY - halfSize * 0.95f,
                centerX + halfSize * 1.15f, centerY - halfSize * 0.25f,
                centerX, centerY + halfSize * 0.65f
            )
            close()
        }
        canvas.drawPath(path, paint)
    }
}

