package health.openwater.openlifu3dscanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import androidx.core.graphics.scale
import java.io.FileOutputStream

fun File.resizeJpegAsSquareByteArray(
    targetSize: Int = 1024,
    jpegQuality: Int = 90
): ByteArray {
    // Save original EXIF before processing
    val originalExif = ExifInterface(absolutePath)

    // 1) Read dimensions only
    val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    FileInputStream(this).use { BitmapFactory.decodeStream(it, null, optsBounds) }
    val srcW = optsBounds.outWidth
    val srcH = optsBounds.outHeight
    if (srcW <= 0 || srcH <= 0) error("Not a valid image")

    // 2) Handle EXIF rotation
    val rotate = exifRotationDegrees(absolutePath)
    val logicalW = if (rotate == 90 || rotate == 270) srcH else srcW

    // 3) Aggressive sampling
    var inSampleSize = 1
    while ((logicalW / (inSampleSize * 2)) >= targetSize) {
        inSampleSize *= 2
    }

    // 4) Decode with sampling
    val opts = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val sampled = FileInputStream(this).use {
        BitmapFactory.decodeStream(it, null, opts) ?: error("Decode failed")
    }

    // 5) Rotate if needed
    val rotated = sampled.rotate(rotate).also {
        if (it !== sampled) sampled.recycle()
    }

    // 6) Scale to width=targetSize, maintaining aspect ratio
    val scaledHeight = (rotated.height * targetSize) / rotated.width
    val scaled = if (rotated.width != targetSize) {
        rotated.scale(targetSize, scaledHeight).also {
            rotated.recycle()
        }
    } else rotated

    // 7) Crop top square
    val square = if (scaled.height >= targetSize) {
        Bitmap.createBitmap(scaled, 0, 0, targetSize, targetSize).also {
            if (it !== scaled) scaled.recycle()
        }
    } else scaled

    // 8) Compress to temporary file (needed for ExifInterface)
    val tempFile = File.createTempFile("exif_", ".jpg", parentFile)
    try {
        FileOutputStream(tempFile).use { out ->
            square.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
        }
        square.recycle()

        // 9) Copy EXIF data (reset orientation since we applied rotation)
        val newExif = ExifInterface(tempFile.absolutePath)
        copyExifAttributes(originalExif, newExif)
        newExif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )
        newExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, targetSize.toString())
        newExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, targetSize.toString())
        newExif.saveAttributes()

        // 10) Read final bytes
        return tempFile.readBytes()
    } finally {
        tempFile.delete()
    }
}

private fun copyExifAttributes(source: ExifInterface, dest: ExifInterface) {
    val attributes = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_USER_COMMENT
    )

    attributes.forEach { tag ->
        source.getAttribute(tag)?.let { value ->
            dest.setAttribute(tag, value)
        }
    }
}

private fun exifRotationDegrees(path: String): Int {
    val exif = ExifInterface(path)
    return when (exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )) {
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