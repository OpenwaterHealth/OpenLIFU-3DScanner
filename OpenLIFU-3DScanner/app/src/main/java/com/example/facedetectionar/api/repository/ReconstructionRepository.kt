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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

class ReconstructionRepository(
    private val authService: AuthService,
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService,
    private val websocketService: WebsocketService
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var currentPhotocollection: Photocollection? = null
    private val imageUploadProgressFlow = MutableStateFlow<ImageUploadProgress?>(null)
    private var reconstructionProgressFlow = MutableStateFlow<ReconstructionProgress?>(null)

    private var imageUploader: ImageUploader? = null
    private var autoUpload: Boolean = false

    var currentReferenceNumber: String? = null
        private set

    var totalImageCount: String? = null

    fun reset() {
        if (imageUploader?.isUploadComplete() == false) {
            deletePhotocollection()
        }
        imageUploader?.stop()
        imageUploader = null
        currentReferenceNumber = null
        autoUpload = false
        currentPhotocollection = null
        totalImageCount = null
        imageUploadProgressFlow.value = null
        reconstructionProgressFlow.value = null
        Log.d(TAG, "reset")
    }

    fun getImageUploadProgress(): StateFlow<ImageUploadProgress?> = imageUploadProgressFlow
    fun getReconstructionProgress(): StateFlow<ReconstructionProgress?> = reconstructionProgressFlow

    fun createPhotocollection(name: String, autoUpload: Boolean) {
        this.currentReferenceNumber = name
        this.autoUpload = autoUpload
        val uid = authService.getCurrentUser()?.uid ?: return

        scope.launch {
            try {
                val response = photocollectionService.createPhotocollection(
                    CreatePhotocollectionRequest(
                        accountId = uid,
                        name = name
                    )
                )
                if (response.isSuccessful) {
                    currentPhotocollection = response.body()
                    currentPhotocollection?.id?.let { id ->
                        Log.d(TAG, "Photocollection created: $id")
                        imageUploader = ImageUploader(
                            id,
                            getImagesDir(name),
                            imageUploadProgressFlow,
                            photocollectionService,
                            scope
                        )
                        if (autoUpload) {
                            imageUploader?.start(waitForCaptureEvents = true)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message ?: "Can't create photo collection")
            }
        }
    }

    fun deletePhotocollection() {
        currentPhotocollection?.let {
            Log.d(TAG, "Deleting photocollection: $it")
            scope.launch {
                try {
                    val response = photocollectionService.deletePhotocollection(it.id)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Photocollection deleted: ${it.id}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onImageCaptured() {
        imageUploader?.onImageCaptured()
    }

    fun uploadRemainingPhotos() {
        imageUploader?.start(waitForCaptureEvents = false)
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