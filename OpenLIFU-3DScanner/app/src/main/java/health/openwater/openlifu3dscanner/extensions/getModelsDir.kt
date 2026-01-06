package health.openwater.openlifu3dscanner.extensions

import android.content.Context
import android.os.Environment
import java.io.File

fun Context.getModelsDir(): File {
    return File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner")
}