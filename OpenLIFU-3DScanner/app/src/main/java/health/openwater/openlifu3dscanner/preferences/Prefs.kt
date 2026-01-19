package health.openwater.openlifu3dscanner.preferences

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    // Keys
    const val IMAGE_SIZE_KEY = "pref_image_size"
    const val PHOTO_COUNT_KEY = "pref_photo_count"

    // Default values
    const val IMAGE_SIZE_DEFAULT = 1024
    const val PHOTO_COUNT_DEFAULT = 120

    val IMAGE_SIZE_MAP = mapOf(
        1024 to "1024x1024",
        2048 to "2048x2048",
    )

    val PHOTO_COUNT_MAP = mapOf(
        120 to "120 photos",
        60 to "60 photos",
        30 to "30 photos"
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
}