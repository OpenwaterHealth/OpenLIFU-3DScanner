package health.openwater.openlifu3dscanner.repository

import android.util.Log
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.extensions.SCAN_SUBDIR
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.api.PhotoscanService
import health.openwater.openlifu3dscanner.network.dto.Photocollection
import health.openwater.openlifu3dscanner.network.dto.Photoscan
import health.openwater.openlifu3dscanner.network.model.DownloadResult
import health.openwater.openlifu3dscanner.network.model.DownloadingItem
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.network.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.network.model.Type
import health.openwater.openlifu3dscanner.network.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRepository @Inject constructor(
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService,
    private val reconstructionRepository: ReconstructionRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Photo download progress: Pair(downloaded, total)
    private val _photoDownloadProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val photoDownloadProgress: StateFlow<Pair<Int, Int>?> = _photoDownloadProgress.asStateFlow()

    private val downloadResultsFlow = MutableStateFlow<DownloadResult?>(null)
    private val downloadQueue = LinkedBlockingQueue<DownloadingItem>()
    private var downloaderJob: Job? = null

    // Delegate upload state to UploadRepository
    val uploadState: StateFlow<UploadState> = reconstructionRepository.uploadState
    val photocollectionReady: StateFlow<Boolean> = reconstructionRepository.photocollectionReady
    val currentPhotoscanId: Long? get() = reconstructionRepository.currentPhotoscanId

    fun getImageUploadProgress(): StateFlow<ImageUploadProgress?> =
        reconstructionRepository.imageUploadProgress

    fun getReconstructionProgress(): StateFlow<ReconstructionProgress?> =
        reconstructionRepository.reconstructionProgress

    fun getDownloadResultsFlow() = downloadResultsFlow.asStateFlow()

    // Delegate upload operations to UploadRepository
    fun isLoggedInAndOnline() = reconstructionRepository.isLoggedInAndOnline()
    fun getCurrentPhotocollection() = reconstructionRepository.getCurrentPhotocollection()
    fun createPhotocollection(name: String, autoUpload: Boolean) =
        reconstructionRepository.createPhotocollection(name, autoUpload)

    fun onImageCaptured() = reconstructionRepository.onImageCaptured()
    fun onScanComplete() = reconstructionRepository.onScanComplete()
    fun uploadRemainingPhotos() = reconstructionRepository.uploadRemainingPhotos()
    fun setStartingReconstruction() = reconstructionRepository.setStartingReconstruction()
    suspend fun startReconstructionFlow() = reconstructionRepository.startReconstructionFlow()
    fun reset(removeLocalCollection: Boolean) =
        reconstructionRepository.reset(removeLocalCollection)

    fun resetCurrentPhotocollection() = reconstructionRepository.resetCurrentPhotocollection()
    fun getImagesDir(referenceNumber: String) =
        reconstructionRepository.getImagesDir(referenceNumber)

    suspend fun deletePhotocollection(id: Long) = reconstructionRepository.deletePhotocollection(id)

    // Download operations
    fun download(item: DownloadingItem) {
        if (!downloadQueue.contains(item)) {
            downloadQueue.add(item)
            startDownloaderJob()
        }
    }

    suspend fun deletePhotoscan(id: Long): Boolean {
        Log.d(TAG, "Deleting photoscan: $id")
        val result = safeCall { photoscanService.deletePhotoscan(id) }
        return when (result) {
            is Result.Success -> {
                Log.d(TAG, "Photoscan deleted: $id")
                true
            }

            else -> {
                Log.e(TAG, "Failed to delete photoscan: $result")
                false
            }
        }
    }

    fun deleteLocalScanDirectory(collectionName: String): Boolean {
        val dir = getImagesDir(collectionName)
        return if (dir.exists()) {
            Log.d(TAG, "Deleting local directory: $dir")
            dir.deleteRecursively()
        } else {
            Log.d(TAG, "Local directory does not exist: $dir")
            true
        }
    }

    suspend fun getPhotocollection(
        id: Long,
        joinPhotos: Boolean = false,
        joinCoordinates: Boolean = false
    ): Photocollection? {
        val result = safeCall {
            photocollectionService.getPhotocollection(id, joinPhotos, joinCoordinates)
        }
        return when (result) {
            is Result.Success -> result.body
            else -> {
                Log.e(TAG, "Failed to get photocollection: $result")
                null
            }
        }
    }

    suspend fun getPhotoscan(id: Long): Photoscan? {
        val result = safeCall { photoscanService.getPhotoscan(id) }
        return when (result) {
            is Result.Success -> result.body
            else -> {
                Log.e(TAG, "Failed to get photoscan: $result")
                null
            }
        }
    }

    suspend fun downloadPhotoscan(id: Long, outputDir: File): Boolean {
        try {
            val response = photoscanService.getMesh(id)

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val dir = File(outputDir, SCAN_SUBDIR)
                    dir.mkdirs()
                    extractZip(body, dir)
                    Log.d(TAG, "Photoscan saved to ${dir.absolutePath}")
                    return true
                }
            } else {
                Log.e(TAG, "Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun extractZip(body: ResponseBody, dir: File) {
        val zis = ZipInputStream(BufferedInputStream(body.byteStream()))
        val buffer = ByteArray(8192)

        while (true) {
            val ze = zis.nextEntry ?: break
            val filename = ze.name
            val file = File(dir, filename)

            if (ze.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { out ->
                    var count: Int
                    while (zis.read(buffer).also { count = it } != -1) {
                        out.write(buffer, 0, count)
                    }
                }
            }
            zis.closeEntry()
        }
        zis.close()
    }

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

    private fun startDownloaderJob() {
        if (downloaderJob == null || downloaderJob?.isCompleted == true) {
            downloaderJob = scope.launch {
                while (isActive && downloadQueue.isNotEmpty()) {
                    val item = downloadQueue.poll() ?: return@launch
                    val success = when (item.type) {
                        Type.PHOTOCOLLECTION -> downloadPhotocollection(item.id)
                        Type.PHOTOSCAN -> downloadPhotoscan(item.id)
                    }
                    downloadResultsFlow.emit(DownloadResult(item, success))
                }
            }
        }
    }

    private suspend fun downloadPhotocollection(id: Long): Boolean {
        val photocollection = getPhotocollection(id, joinPhotos = true) ?: return false

        val photos = photocollection.photos ?: listOf()
        val outputDir = getImagesDir(photocollection.name ?: return false)
        if (!outputDir.exists()) outputDir.mkdirs()

        val totalPhotos = photos.size
        _photoDownloadProgress.value = Pair(0, totalPhotos)

        for ((index, photo) in photos.withIndex()) {
            Log.d(TAG, "Loading photo ${photo.fileName}")
            try {
                val photoResponse =
                    photocollectionService.downloadPhoto(photocollection.id, photo.fileName)
                if (!photoResponse.isSuccessful) {
                    _photoDownloadProgress.value = null
                    return false
                }

                photoResponse.body()?.let {
                    val file = File(outputDir, photo.fileName)
                    saveResponseBodyToDisk(it, file)
                    Log.d(TAG, "Photo saved to ${file.absolutePath}")
                    _photoDownloadProgress.value = Pair(index + 1, totalPhotos)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ${photo.fileName}", e)
                _photoDownloadProgress.value = null
                return false
            }
        }
        _photoDownloadProgress.value = null
        return true
    }

    private suspend fun downloadPhotoscan(id: Long): Boolean {
        val photoscan = getPhotoscan(id) ?: return false
        val photocollection = getPhotocollection(photoscan.photocollectionId) ?: return false
        val outputDir = getImagesDir(photocollection.name ?: return false)
        if (!outputDir.exists()) outputDir.mkdirs()
        return downloadPhotoscan(photoscan.id, outputDir)
    }

    companion object {
        private val TAG = CloudRepository::class.simpleName
    }
}
