package health.openwater.openlifu3dscanner.core

import android.content.Context
import android.util.Log
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.preferences.Prefs
import health.openwater.openlifu3dscanner.utils.resizeJpegAsSquareByteArray
import kotlinx.coroutines.CancellationException
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
    private val context: Context,
    private val photocollectionId: Long,
    private val imagesDir: File,
    private val progressFlow: MutableStateFlow<ImageUploadProgress?>,
    private val photocollectionService: PhotocollectionService,
    private val scope: CoroutineScope
) {
    // Track uploaded images to avoid re-uploading
    private val uploadedImages = mutableSetOf<String>()
    private var job: Job? = null
    private val imageCapturedChannel = Channel<Unit>(Channel.CONFLATED)

    val isRunning: Boolean get() = job?.isActive == true

    /**
     * Start uploading images.
     * @param autoUpload If true, waits for new images after uploading all existing ones.
     *                   If false, uploads all existing images and stops.
     * @param resetProgress If true, clears the uploaded images set to re-upload everything.
     */
    fun start(autoUpload: Boolean, resetProgress: Boolean = false) {
        if (isRunning) {
            Log.d(TAG, "Already running, ignoring start request")
            return
        }

        if (resetProgress) {
            uploadedImages.clear()
        }

        Log.d(TAG, "Starting uploader - autoUpload: $autoUpload, resetProgress: $resetProgress")
        Log.d(TAG, "Directory: $imagesDir")
        Log.d(TAG, "Files found: ${getFiles()}")
        Log.d(TAG, "Already uploaded: $uploadedImages")

        job = createUploadingJob(autoUpload)
    }

    private fun createUploadingJob(autoUpload: Boolean) = scope.launch {
        try {
            // Initial progress emission
            emitCurrentProgress()

            while (isActive) {
                // Upload all pending images
                var uploadedAny = false
                while (isActive && uploadNextImage()) {
                    uploadedAny = true
                }

                if (!isActive) break

                if (autoUpload) {
                    // Wait for new images to be captured
                    Log.d(TAG, "Waiting for new images...")
                    imageCapturedChannel.receive()
                    Log.d(TAG, "New image notification received")
                } else {
                    // All images uploaded, we're done
                    Log.d(TAG, "All images uploaded, finishing")
                    break
                }
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Upload job cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Upload job failed", e)
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping uploader")
        job?.cancel()
        job = null
    }

    fun onImageCaptured() {
        imageCapturedChannel.trySend(Unit)
    }

    private suspend fun uploadNextImage(): Boolean {
        val files = getFiles()
        val pendingFiles = files.filter { !uploadedImages.contains(it) }
        val filename = pendingFiles.minOrNull()

        if (filename == null) {
            Log.d(TAG, "No pending images to upload")
            return false
        }

        Log.d(TAG, "Uploading: $filename (${pendingFiles.size} pending of ${files.size} total)")

        val imageSize = Prefs.getImageSize(context)
        var retries = 3

        while (retries > 0) {
            try {
                val file = File(imagesDir, filename)
                if (!file.exists()) {
                    Log.w(TAG, "File no longer exists: $filename")
                    return true // Continue to next file
                }

                val resizedBytes = file.resizeJpegAsSquareByteArray(imageSize, JPEG_QUALITY)

                photocollectionService.uploadPhoto(
                    photocollectionId = photocollectionId,
                    fileName = filename,
                    body = resizedBytes.toRequestBody("application/octet-stream".toMediaType())
                )

                // Success
                uploadedImages.add(filename)
                Log.d(TAG, "Upload completed: $filename")
                emitCurrentProgress()
                return true

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                retries--
                Log.e(TAG, "Upload failed for $filename, retries left: $retries", e)

                if (retries > 0) {
                    delay(3000)
                } else {
                    Log.e(TAG, "Upload permanently failed: $filename")
                    emitProgress(failed = true)
                    return false
                }
            }
        }
        return false
    }

    private suspend fun emitCurrentProgress() {
        emitProgress(failed = false)
    }

    private suspend fun emitProgress(failed: Boolean) {
        val files = getFiles()
        val totalImages = files.size
        val uploadedCount = uploadedImages.size
        val progress = if (totalImages > 0) {
            (uploadedCount.toFloat() / totalImages * 100).toInt()
        } else 0

        progressFlow.emit(
            ImageUploadProgress(
                progress = progress,
                uploadedImages = uploadedCount,
                totalImages = totalImages,
                failed = failed
            )
        )
    }

    private fun getFiles(): List<String> {
        return imagesDir.list()
            ?.filter { it.lowercase().endsWith(".jpg") }
            ?.sorted()
            ?: emptyList()
    }

    companion object {
        private val TAG = ImageUploader::class.simpleName
        const val JPEG_QUALITY = 90
    }
}