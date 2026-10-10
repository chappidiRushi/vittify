package com.reddy.vittify.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/**
 * Utility functions for handling and optimizing receipt images for AI analysis.
 */
object ReceiptImageUtils {

    private const val TAG = "ReceiptImageUtils"

    /**
     * Resolves the [Activity] from a given [Context], traversing through [ContextWrapper] if needed.
     */
    fun findActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    /**
     * Loads a receipt image directly from a local [File], rotates according to EXIF,
     * downscales to [maxDimension], and compresses to JPEG bytes.
     */
    fun prepareReceiptImageForAi(
        context: Context,
        file: File,
        maxDimension: Int = 2048,
        quality: Int = 85
    ): ByteArray? {
        return try {
            if (!file.exists() || file.length() == 0L) {
                Log.e(TAG, "Receipt file does not exist or is empty: ${file.absolutePath}")
                return null
            }
            val rawBytes = file.readBytes()
            prepareReceiptBytesForAi(rawBytes, maxDimension, quality)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading receipt file for AI: ${file.absolutePath}", e)
            null
        }
    }

    /**
     * Loads an image from [imageUri], reads all bytes once safely, rotates based on EXIF,
     * downscales to [maxDimension], and compresses to JPEG bytes.
     */
    fun prepareReceiptImageForAi(
        context: Context,
        imageUri: Uri,
        maxDimension: Int = 2048,
        quality: Int = 85
    ): ByteArray? {
        return try {
            val rawBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            if (rawBytes == null || rawBytes.isEmpty()) {
                Log.e(TAG, "Failed to read bytes from URI: $imageUri")
                return null
            }
            prepareReceiptBytesForAi(rawBytes, maxDimension, quality)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading receipt image URI for AI: $imageUri", e)
            null
        }
    }

    /**
     * Decodes [rawBytes], reads EXIF orientation from stream, applies rotation, scales down
     * if exceeding [maxDimension], and compresses to JPEG byte array.
     */
    fun prepareReceiptBytesForAi(
        rawBytes: ByteArray,
        maxDimension: Int = 2048,
        quality: Int = 85
    ): ByteArray? {
        return try {
            // 1. Decode bounds to compute inSampleSize
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e(TAG, "Invalid image dimensions: $originalWidth x $originalHeight")
                return null
            }

            // Calculate sample size
            var inSampleSize = 1
            var maxSide = max(originalWidth, originalHeight)
            while (maxSide / 2 >= maxDimension) {
                inSampleSize *= 2
                maxSide /= 2
            }

            // 2. Decode full bitmap with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return null

            // 3. Check EXIF rotation and rotate if needed
            val orientation = try {
                val exif = ExifInterface(ByteArrayInputStream(rawBytes))
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read EXIF orientation", e)
                ExifInterface.ORIENTATION_NORMAL
            }

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            // 4. Downscale further if still larger than maxDimension
            val currentMax = max(bitmap.width, bitmap.height)
            if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                matrix.postScale(scale, scale)
            }

            if (!matrix.isIdentity) {
                val transformed = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
                if (transformed != bitmap) {
                    bitmap.recycle()
                    bitmap = transformed
                }
            }

            // 5. Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 100), outputStream)
            bitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing receipt bytes for AI", e)
            null
        }
    }
}
