package health.openwater.openlifu3dscanner.extensions

import android.content.Context
import java.io.File

const val MODEL_FILENAME = "texturedMesh.obj"
const val SCAN_SUBDIR = "scan"

fun getModelsDir(context: Context): File {
    return File(context.getExternalFilesDir(null), "OpenLIFU-3DScanner")
}

fun hasLocalModel(context: Context, collectionName: String): Boolean {
    val scanDir = File(getModelsDir(context), "$collectionName/$SCAN_SUBDIR")
    return scanDir.exists() && scanDir.listFiles()?.any { it.name == MODEL_FILENAME } == true
}
