package health.openwater.openlifu3dscanner.network

import android.content.Context
import android.util.Log
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.preferences.Prefs
import health.openwater.openlifu3dscanner.utils.resizeJpegAsSquareByteArray
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
    val context: Context,
    private val photocollectionId: Long,
    private val imagesDir: File,
    private val imageUploadProgressFlow: MutableStateFlow<ImageUploadProgress?>,
    private val photocollectionService: PhotocollectionService,
    private val scope: CoroutineScope
) {
    private val uploadedImages = mutableSetOf<String>()
    private var job: Job? = null
    private val imageCapturedChannel = Channel<Unit>(Channel.Factory.RENDEZVOUS)

    fun start(autoUpload: Boolean) {
        stop()
        Log.d(TAG, "started, waiting mode: $autoUpload")
        Log.d(TAG, "Directory: $imagesDir")
        Log.d(TAG, "Files: ${getFiles()}")
        job = createUploadingJob(autoUpload)
    }

    private fun createUploadingJob(autoUpload: Boolean) = scope.launch {
        while (isActive) {
            while (uploadNextImage(this)) {
                Log.d(TAG, "Checking is there any more images to upload")
            }
            if (autoUpload) {
                imageCapturedChannel.receive()
            } else {
                break
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun onImageCaptured() {
        imageCapturedChannel.trySend(Unit)
    }

    private suspend fun uploadNextImage(scope: CoroutineScope): Boolean {
        val files = getFiles()
        val filename = files.filter { !uploadedImages.contains(it) }.minOrNull() ?: run {
            Log.d(TAG, "No more images to upload")
            return false
        }
        val imageSize = Prefs.getImageSize(context)

        var retries = 3
        while (scope.isActive && retries > 0) {
            try {
                val file = File(imagesDir, filename)
                val resizedBytes = file.resizeJpegAsSquareByteArray(imageSize, JPEG_QUALITY)

                photocollectionService.uploadPhoto(
                    photocollectionId = photocollectionId,
                    fileName = filename,
                    body = resizedBytes.toRequestBody("application/octet-stream".toMediaType())
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

    private fun getFiles() = imagesDir.list()
        ?.filter { it.lowercase().endsWith(".jpg") } ?: listOf()

    companion object {
        private val TAG = ImageUploader::class.simpleName
        const val JPEG_QUALITY = 90
    }
}