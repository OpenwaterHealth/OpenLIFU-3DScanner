package health.openwater.openlifu3dscanner.extensions

import android.os.Build
import android.os.Environment

fun hasAllFilesAccess(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}
