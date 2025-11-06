package health.openwater.openlifu3dscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import kotlin.math.roundToInt
import androidx.core.graphics.scale

private fun exifRotationDegrees(path: String): Int {
    val exif = ExifInterface(path)
    return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

private fun Bitmap.rotate(degrees: Int): Bitmap =
    if (degrees == 0) this
    else Bitmap.createBitmap(
        this, 0, 0, width, height,
        Matrix().apply { postRotate(degrees.toFloat()) },
        true
    )

fun File.resizeJpegAsByteArray(
    targetWidth: Int,
    jpegQuality: Int
): ByteArray {
    // 1) Read dimensions only
    val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    FileInputStream(this).use { BitmapFactory.decodeStream(it, null, optsBounds) }
    val srcW = optsBounds.outWidth
    val srcH = optsBounds.outHeight
    if (srcW <= 0 || srcH <= 0) error("Not a valid image")

    // 2) Handle EXIF rotation
    val rotate = exifRotationDegrees(absolutePath)
    val logicalW = if (rotate == 90 || rotate == 270) srcH else srcW
    val logicalH = if (rotate == 90 || rotate == 270) srcW else srcH

    // 3) Compute sampling for memory-friendly decode
    // Decode near target size, but don't oversample if image is already small
    var inSampleSize = 1
    if (logicalW > targetWidth) {
        while ((logicalW / (inSampleSize * 2)) >= targetWidth) {
            inSampleSize *= 2
        }
    }

    // 4) Decode with sampling
    val opts = BitmapFactory.Options().apply { inSampleSize = inSampleSize }
    val sampled = FileInputStream(this).use {
        BitmapFactory.decodeStream(it, null, opts) ?: error("Decode failed")
    }

    // 5) Apply EXIF rotation
    val rotated = sampled.rotate(rotate).also { if (it !== sampled) sampled.recycle() }

    // 6) Scale to ensure *at least* targetWidth (upscale if needed)
    val scale = targetWidth.toFloat() / rotated.width
    val finalW = targetWidth
    val finalH = (rotated.height * scale).roundToInt()
    val scaled = if (scale != 1f) {
        rotated.scale(finalW, finalH).also { if (it !== rotated) rotated.recycle() }
    } else rotated

    // 7) Compress to JPEG → ByteArray
    val baos = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
    scaled.recycle()

    return baos.toByteArray()
}