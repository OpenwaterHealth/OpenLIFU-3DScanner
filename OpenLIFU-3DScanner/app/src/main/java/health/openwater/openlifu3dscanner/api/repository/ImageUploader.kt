package health.openwater.openlifu3dscanner.api.repository

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import health.openwater.openlifu3dscanner.api.PhotocollectionService
import health.openwater.openlifu3dscanner.api.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.resizeJpegAsByteArray
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
import java.io.FileInputStream
import java.io.FileOutputStream

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
                val originalExif = ExifInterface(file)
                val bytes = file.resizeJpegAsByteArray(IMAGE_WIDTH, JPEG_QUALITY)

                val resizedFile = File(imagesDir, "resized_$filename")
                FileOutputStream(resizedFile).use {
                    it.write(bytes)
                }

                val resizedExif = ExifInterface(resizedFile)
                copyExifAttributes(originalExif, resizedExif)
                resizedExif.saveAttributes()

                val resizedBytes = FileInputStream(resizedFile).use {
                    it.readBytes()
                }
                resizedFile.delete()

                photocollectionService.uploadPhoto(
                    photocollectionId,
                    filename,
                    resizedBytes.toRequestBody("application/octet-stream".toMediaType())
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

    private fun copyExifAttributes(source: ExifInterface, target: ExifInterface) {
        val tags = listOf(
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY
        )
        for (tag in tags) {
            source.getAttribute(tag)?.let {
                target.setAttribute(tag, it)
            }
        }
        target.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )
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