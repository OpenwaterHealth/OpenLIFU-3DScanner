package health.openwater.openlifu3dscanner.extensions

import android.os.Environment
import java.io.File

const val MODEL_FILENAME = "texturedMesh.obj"
const val SCAN_SUBDIR = "scan"

fun getModelsDir(): File {
    return File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner")
}

fun hasLocalModel(collectionName: String): Boolean {
    val scanDir = File(getModelsDir(), "$collectionName/$SCAN_SUBDIR")
    return scanDir.exists() && scanDir.listFiles()?.any { it.name == MODEL_FILENAME } == true
}