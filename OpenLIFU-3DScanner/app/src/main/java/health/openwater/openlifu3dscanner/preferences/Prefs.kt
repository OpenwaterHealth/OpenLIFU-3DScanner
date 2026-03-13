package health.openwater.openlifu3dscanner.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.camera.core.ImageCapture
import androidx.core.content.edit
import health.openwater.openlifu3dscanner.R

object Prefs {
    // Keys
    const val IMAGE_SIZE_KEY = "pref_image_size"
    const val PHOTO_COUNT_KEY = "pref_photo_count"
    const val CAPTURE_MODE_ONLINE_KEY = "pref_capture_mode_online"
    const val CAPTURE_MODE_OFFLINE_KEY = "pref_capture_mode_offline"
    const val OVAL_SIZE_ONLINE_KEY = "pref_oval_size_online"
    const val OVAL_SIZE_OFFLINE_KEY = "pref_oval_size_offline"
    const val AUTO_UPLOAD_KEY = "pref_auto_upload"
    const val NOTICE_ACKNOWLEDGED_UID_KEY = "pref_notice_acknowledged_uid"
    const val API_ENV_KEY = "pref_api_env"
    const val ACCESS_TOKEN_KEY = "pref_access_token"
    const val REFRESH_TOKEN_KEY = "pref_refresh_token"
    const val TOKEN_EXPIRATION_MS_KEY = "pref_token_expiration_ms"
    const val USER_UID_KEY = "pref_user_uid"

    // Default values
    const val IMAGE_SIZE_DEFAULT = 1024
    const val PHOTO_COUNT_DEFAULT = 120
    const val CAPTURE_MODE_ONLINE_DEFAULT = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
    const val CAPTURE_MODE_OFFLINE_DEFAULT = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    const val OVAL_SIZE_ONLINE_DEFAULT = 100
    const val OVAL_SIZE_OFFLINE_DEFAULT = 200
    const val AUTO_UPLOAD_DEFAULT = true

    val IMAGE_SIZE_MAP = mapOf(
        1024 to R.string.image_size_1024,
        2048 to R.string.image_size_2048,
    )

    val PHOTO_COUNT_MAP = mapOf(
        120 to R.string.photo_count_120,
        60 to R.string.photo_count_60,
        30 to R.string.photo_count_30
    )

    val OVAL_SIZE_MAP = mapOf(
        100 to R.string.oval_size_1x,
        150 to R.string.oval_size_1_5x,
        200 to R.string.oval_size_2x
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

    fun getCaptureMode(context: Context, isOnline: Boolean): Int {
        val prefs = getInstance(context)
        return if (isOnline) {
            prefs.getInt(CAPTURE_MODE_ONLINE_KEY, CAPTURE_MODE_ONLINE_DEFAULT)
        } else {
            prefs.getInt(CAPTURE_MODE_OFFLINE_KEY, CAPTURE_MODE_OFFLINE_DEFAULT)
        }
    }

    fun getOvalSize(context: Context, isOnline: Boolean): Int {
        val prefs = getInstance(context)
        return if (isOnline) {
            prefs.getInt(OVAL_SIZE_ONLINE_KEY, OVAL_SIZE_ONLINE_DEFAULT)
        } else {
            prefs.getInt(OVAL_SIZE_OFFLINE_KEY, OVAL_SIZE_OFFLINE_DEFAULT)
        }
    }

    fun getAutoUpload(context: Context): Boolean {
        val prefs = getInstance(context)
        return prefs.getBoolean(AUTO_UPLOAD_KEY, AUTO_UPLOAD_DEFAULT)
    }

    fun setAutoUpload(context: Context, enabled: Boolean) {
        getInstance(context).edit { putBoolean(AUTO_UPLOAD_KEY, enabled) }
    }

    fun getNoticeAcknowledgedUid(context: Context): String {
        return getInstance(context).getString(NOTICE_ACKNOWLEDGED_UID_KEY, "") ?: ""
    }

    fun setNoticeAcknowledgedUid(context: Context, uid: String) {
        getInstance(context).edit { putString(NOTICE_ACKNOWLEDGED_UID_KEY, uid) }
    }

    fun getApiEnv(context: Context): ApiEnvironment {
        val key = getInstance(context).getString(API_ENV_KEY, null)
        return ApiEnvironment.fromKey(key ?: ApiEnvironment.DEFAULT.key)
    }

    fun setApiEnv(context: Context, env: ApiEnvironment) {
        getInstance(context).edit(commit = true) { putString(API_ENV_KEY, env.key) }
    }

    fun getApiBaseUrl(context: Context): String = getApiEnv(context).baseUrl

    fun getAccessToken(context: Context): String? =
        getInstance(context).getString(ACCESS_TOKEN_KEY, null)

    fun getRefreshToken(context: Context): String? =
        getInstance(context).getString(REFRESH_TOKEN_KEY, null)

    fun getTokenExpirationMs(context: Context): Long =
        getInstance(context).getLong(TOKEN_EXPIRATION_MS_KEY, 0L)

    fun getUserUid(context: Context): String? =
        getInstance(context).getString(USER_UID_KEY, null)

    fun saveAuthTokens(
        context: Context,
        accessToken: String,
        refreshToken: String,
        expirationMs: Long,
        uid: String
    ) {
        getInstance(context).edit(commit = true) {
            putString(ACCESS_TOKEN_KEY, accessToken)
            putString(REFRESH_TOKEN_KEY, refreshToken)
            putLong(TOKEN_EXPIRATION_MS_KEY, expirationMs)
            putString(USER_UID_KEY, uid)
        }
    }

    fun clearAuthTokens(context: Context) {
        getInstance(context).edit(commit = true) {
            remove(ACCESS_TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
            remove(TOKEN_EXPIRATION_MS_KEY)
            remove(USER_UID_KEY)
        }
    }
}