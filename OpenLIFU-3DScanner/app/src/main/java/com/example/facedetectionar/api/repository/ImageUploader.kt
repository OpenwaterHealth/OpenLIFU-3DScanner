package com.example.facedetectionar.api.repository

import android.util.Log
import com.example.facedetectionar.api.PhotocollectionService
import com.example.facedetectionar.api.model.ImageUploadProgress
import com.example.facedetectionar.resizeJpegAsByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ImageUploader(
    private val photocollectionId: Long,
    private val imagesDir: File,
    private val imageUploadProgressFlow: MutableStateFlow<ImageUploadProgress?>,
    private val photocollectionService: PhotocollectionService,
    private val scope: CoroutineScope
) {
    private val uploadedImages = mutableSetOf<String>()
    private var job: Job? = null
    private val imageCapturedChannel = Channel<Unit>(Channel.RENDEZVOUS)

    fun start(waitForCaptureEvents: Boolean) {
        stop()
        Log.d(TAG, "started, waiting mode: $waitForCaptureEvents")
        Log.d(TAG, "Directory: $imagesDir")
        Log.d(TAG, "Files: ${getFiles()}")
        job = scope.launch {
            while (isActive) {
                uploadNextImage(this)
                if (waitForCaptureEvents) imageCapturedChannel.receive()
            }
        }
    }

    fun stop() {
        Log.d(TAG, "stopped")
        job?.cancel()
        job = null
    }

    fun onImageCaptured() {
        imageCapturedChannel.trySend(Unit)
    }

    fun isUploadComplete(): Boolean {
        return uploadedImages.isNotEmpty() && uploadedImages == getFiles().toSet()
    }

    private suspend fun uploadNextImage(scope: CoroutineScope): Boolean {
        val files = getFiles()
        val filename = files.filter { !uploadedImages.contains(it) }.minOrNull() ?: return false

        Log.d(TAG, "Uploading: $filename")

        var retries = 3
        while (scope.isActive && retries > 0) {
            try {
                val file = File(imagesDir, filename)
                val bytes = file.resizeJpegAsByteArray(IMAGE_WIDTH, JPEG_QUALITY)

                photocollectionService.uploadPhoto(
                    photocollectionId,
                    filename,
                    bytes.toRequestBody("application/octet-stream".toMediaType())
                )
                break
            } catch (e: Exception) {
                e.printStackTrace()
                delay(5000)
                if (--retries == 0) {
                    Log.d(TAG, "Upload failed: $filename")
                    sendProgress(files.size, true)
                    return false
                }
            }
        }

        Log.d(TAG, "Upload finished: $filename")
        uploadedImages.add(filename)
        sendProgress(files.size, false)
        return true
    }

    private suspend fun sendProgress(totalImages: Int, failed: Boolean) {
        val progress = (uploadedImages.size / totalImages.toFloat() * 100).toInt()
        imageUploadProgressFlow.emit(
            ImageUploadProgress(progress, uploadedImages.size, totalImages, failed)
        )
    }

    private fun getFiles(): List<String> {
        return imagesDir.list()
            ?.filter {
                it.lowercase().endsWith(".jpeg") || it.lowercase().endsWith(".jpg")
            } ?: listOf()
    }

    companion object {
        private val TAG = ImageUploader::class.simpleName
        const val IMAGE_WIDTH = 1024
        const val JPEG_QUALITY = 85
    }
}