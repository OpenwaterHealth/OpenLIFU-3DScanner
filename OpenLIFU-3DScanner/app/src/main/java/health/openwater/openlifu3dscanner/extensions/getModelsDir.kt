package health.openwater.openlifu3dscanner.extensions

import android.os.Environment
import java.io.File

fun getModelsDir(): File {
    return File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner")
}