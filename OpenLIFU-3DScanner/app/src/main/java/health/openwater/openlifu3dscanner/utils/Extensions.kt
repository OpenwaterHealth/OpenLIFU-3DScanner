package health.openwater.openlifu3dscanner.utils

import com.google.gson.GsonBuilder
import health.openwater.openlifu3dscanner.api.dto.Coordinates
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import java.io.File

fun Coordinates.writeToFile(dir: File) {
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    val jsonString = gson.toJson(this)
    val fileName = CloudRepository.COORDINATES_FILE_NAME
    val file = File(dir, fileName)
    file.writeText(jsonString)
}