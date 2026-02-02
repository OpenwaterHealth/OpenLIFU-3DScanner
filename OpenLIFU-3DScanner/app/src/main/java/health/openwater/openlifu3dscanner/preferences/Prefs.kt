package health.openwater.openlifu3dscanner.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import health.openwater.openlifu3dscanner.R

object Prefs {
    // Keys
    const val IMAGE_SIZE_KEY = "pref_image_size"
    const val PHOTO_COUNT_KEY = "pref_photo_count"
    const val CAPTURE_MODE_KEY = "pref_capture_mode"

    // Default values
    const val IMAGE_SIZE_DEFAULT = 1024
    const val PHOTO_COUNT_DEFAULT = 120
    const val CAPTURE_MODE_DEFAULT = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY

    val IMAGE_SIZE_MAP = mapOf(
        1024 to R.string.image_size_1024,
        2048 to R.string.image_size_2048,
    )

    val PHOTO_COUNT_MAP = mapOf(
        120 to R.string.photo_count_120,
        60 to R.string.photo_count_60,
        30 to R.string.photo_count_30
    )

    val CAPTURE_MODE_MAP = mapOf(
        ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY to R.string.capture_mode_max_quality,
        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY to R.string.capture_mode_min_latency,
        ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG to R.string.capture_mode_zero_shutter_lag
    )

    fun getInstance(context: Context): SharedPreferences {

        return context.getSharedPreferences(
            "prefs",
            Context.MODE_PRIVATE
        )
    }

    fun getPhotoCount(context: Context): Int {
        val prefs = getInstance(context)
        return prefs.getInt(PHOTO_COUNT_KEY, PHOTO_COUNT_DEFAULT)
    }

    fun getImageSize(context: Context): Int {
        val prefs = getInstance(context)
        return prefs.getInt(IMAGE_SIZE_KEY, IMAGE_SIZE_DEFAULT)
    }

    fun getCaptureMode(context: Context): Int {
        val prefs = getInstance(context)
        return prefs.getInt(CAPTURE_MODE_KEY, CAPTURE_MODE_DEFAULT)
    }
}