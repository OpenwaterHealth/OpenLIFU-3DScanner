package health.openwater.openlifu3dscanner.extensions

import android.content.Context
import android.os.Build
import android.os.Environment

fun Context.hasAllFilesAccess(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}
