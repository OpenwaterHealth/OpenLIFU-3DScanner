package com.example.facedetectionar.api.repository

import android.os.Environment
import android.util.Log
import com.example.facedetectionar.api.AuthService
import com.example.facedetectionar.api.PhotocollectionService
import com.example.facedetectionar.api.PhotoscanService
import com.example.facedetectionar.api.WebsocketService
import com.example.facedetectionar.api.dto.CreatePhotocollectionRequest
import com.example.facedetectionar.api.dto.Photocollection
import com.example.facedetectionar.api.dto.Photoscan
import com.example.facedetectionar.api.dto.StartPhotoscanRequest
import com.example.facedetectionar.api.model.ImageUploadProgress
import com.example.facedetectionar.api.model.ReconstructionProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

class ReconstructionRepository(
    private val authService: AuthService,
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService,
    private val websocketService: WebsocketService
) {
    private var currentPhotocollection: Photocollection? = null
    private val imageUploadProgressFlow = MutableStateFlow<ImageUploadProgress?>(null)
    private var reconstructionProgressFlow = MutableStateFlow<ReconstructionProgress?>(null)

    var currentReferenceNumber: String? = null
    var totalImageCount: String? = null
    var autoUpload: Boolean = false

    fun reset() {
        currentReferenceNumber = null
        currentPhotocollection = null
        totalImageCount = null
        imageUploadProgressFlow.value = null
    }

    fun getImageUploadProgress(): StateFlow<ImageUploadProgress?> = imageUploadProgressFlow
    fun getReconstructionProgress(): StateFlow<ReconstructionProgress?> = reconstructionProgressFlow

    suspend fun uploadImages() = withContext(Dispatchers.IO) {
        val referenceNumber = currentReferenceNumber ?: return@withContext
        val imagesDir = getImagesDir(referenceNumber)

        val files = imagesDir.list()
            ?.filter {
                it.lowercase().endsWith(".jpeg") || it.lowercase().endsWith(".jpg")
            } ?: listOf()
        imageUploadProgressFlow.emit(ImageUploadProgress(0, 0, files.size))

        currentPhotocollection = createPhotocollection(referenceNumber) ?: run {
            imageUploadProgressFlow.emit(
                ImageUploadProgress(0, 0, files.size, failed = true)
            )
            return@withContext
        }

        var progress = 0
        files.forEachIndexed { idx, filename ->
            if (!isActive) return@withContext

            var retries = 3
            while (isActive && retries > 0) {
                try {
                    photocollectionService.uploadPhoto(
                        currentPhotocollection?.id ?: 0,
                        filename,
                        File(
                            imagesDir,
                            filename
                        ).asRequestBody("application/octet-stream".toMediaType())
                    )
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(5000)
                    if (--retries == 0) {
                        imageUploadProgressFlow.emit(
                            ImageUploadProgress(progress, idx, files.size, failed = true)
                        )
                        return@withContext
                    }
                }
            }
            progress = ((idx+1) / files.size.toFloat() * 100).toInt()
            imageUploadProgressFlow.emit(
                ImageUploadProgress(progress, idx+1, files.size)
            )
        }
    }

    suspend fun startReconstruction(): Long? {
        val id = currentPhotocollection?.id ?: return null
        return startReconstruction(id)
    }

    suspend fun startReconstruction(photocollectionId: Long): Long? {
        return try {
            val response =
                photocollectionService.startPhotoscan(photocollectionId, StartPhotoscanRequest())
            if (response.isSuccessful)
                response.body()?.photoscanId
            else
                null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun startReconstructionProgressListener(photoscanId: Long): Photoscan? {
        val photoscan = getPhotoscan(photoscanId) ?: return null
        reconstructionProgressFlow.emit(ReconstructionProgress(
            photoscan.progress, photoscan.message, photoscan.status
        ))
        websocketService.connect(photoscanId, reconstructionProgressFlow)
        return photoscan
    }

    fun stopReconstructionProgressListener(photoscanId: Long) {
        websocketService.disconnect(photoscanId)
    }

    suspend fun getPhotocollection(id: Long): Photocollection? {
        try {
            val response = photocollectionService.getPhotocollection(id)
            if (response.isSuccessful) {
                return response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun getPhotoscan(id: Long): Photoscan? {
        try {
            val response = photoscanService.getPhotoscan(id)
            if (response.isSuccessful) {
                return response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun downloadPhotoscanZip(id: Long, name: String, outputDir: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = photoscanService.getMesh(id)

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val fileName = "scan_$name.zip"
                        val file = File(outputDir, fileName)
                        saveResponseBodyToDisk(body, file)
                        Log.d(TAG, "Photoscan saved to ${file.absolutePath}")
                        return@withContext true
                    }
                } else {
                    Log.e(TAG, "Error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext false
        }
    }

    private suspend fun createPhotocollection(name: String): Photocollection? {
        val uid = authService.getCurrentUser()?.uid ?: return null

        try {
            val respsonse = photocollectionService.createPhotocollection(
                CreatePhotocollectionRequest(
                    accountId = uid,
                    name = name
                )
            )
            if (respsonse.isSuccessful) {
                return respsonse.body()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Can't create photo collection")
        }
        return null
    }

    fun getImagesDir(referenceNumber: String) = File(
        Environment.getExternalStorageDirectory(),
        "OpenLIFU-3DScanner/$referenceNumber"
    )

    private fun saveResponseBodyToDisk(body: ResponseBody, file: File) {
        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
        }
    }

    companion object {
        private val TAG = ReconstructionRepository::class.simpleName
    }

}