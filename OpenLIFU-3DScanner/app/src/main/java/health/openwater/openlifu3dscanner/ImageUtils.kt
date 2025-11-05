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
    val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    FileInputStream(this).use { BitmapFactory.decodeStream(it, null, optsBounds) }
    var srcW = optsBounds.outWidth
    var srcH = optsBounds.outHeight
    if (srcW <= 0 || srcH <= 0) error("Not a valid image")

    // Account for rotation when choosing sample size
    val rotate = exifRotationDegrees(absolutePath)
    val logicalW = if (rotate == 90 || rotate == 270) srcH else srcW
    val logicalH = if (rotate == 90 || rotate == 270) srcW else srcH

    // 2) Compute sample size for memory-friendly decode
    // We aim to decode near the target width to save RAM
    val desiredW = minOf(targetWidth, logicalW)
    var inSampleSize = 1
    while ((logicalW / (inSampleSize * 2)) >= desiredW) {
        inSampleSize *= 2
    }

    // 3) Decode with sampling
    val opts = BitmapFactory.Options().apply { inSampleSize = inSampleSize }
    val sampled = FileInputStream(this).use {
        BitmapFactory.decodeStream(it, null, opts) ?: error("Decode failed")
    }

    // 4) Rotate for EXIF orientation
    val rotated = sampled.rotate(rotate).also { if (it !== sampled) sampled.recycle() }

    // 5) Final scale to exact target width (no upscaling)
    val finalW = minOf(targetWidth, rotated.width)
    val scale = finalW.toFloat() / rotated.width
    val finalH = (rotated.height * scale).roundToInt()
    val scaled =
        if (scale < 1f) rotated.scale(finalW, finalH).also {
            if (it !== rotated) rotated.recycle()
        } else rotated

    // 6) Compress to JPEG -> ByteArray -> RequestBody
    val baos = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
    scaled.recycle()

    return baos.toByteArray()
}