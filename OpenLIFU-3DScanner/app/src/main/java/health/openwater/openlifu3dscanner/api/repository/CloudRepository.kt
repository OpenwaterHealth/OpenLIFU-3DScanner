package health.openwater.openlifu3dscanner.api.repository

import android.os.Environment
import android.util.Log
import health.openwater.openlifu3dscanner.api.AuthService
import health.openwater.openlifu3dscanner.api.PhotocollectionService
import health.openwater.openlifu3dscanner.api.PhotoscanService
import health.openwater.openlifu3dscanner.api.WebsocketService
import health.openwater.openlifu3dscanner.api.dto.CreatePhotocollectionRequest
import health.openwater.openlifu3dscanner.api.dto.Photocollection
import health.openwater.openlifu3dscanner.api.dto.Photoscan
import health.openwater.openlifu3dscanner.api.dto.StartPhotoscanRequest
import health.openwater.openlifu3dscanner.api.model.DownloadResult
import health.openwater.openlifu3dscanner.api.model.DownloadingItem
import health.openwater.openlifu3dscanner.api.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.api.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.api.model.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue

class CloudRepository(
    private val authService: AuthService,
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService,
    private val websocketService: WebsocketService,
    private val userRepository: UserRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var currentPhotocollection: Photocollection? = null
    private val imageUploadProgressFlow = MutableStateFlow<ImageUploadProgress?>(null)
    private val reconstructionProgressFlow = MutableStateFlow<ReconstructionProgress?>(null)
    private val downloadResultsFlow = MutableStateFlow<DownloadResult?>(null)

    private var imageUploader: ImageUploader? = null
    private var autoUpload: Boolean = false

    private val downloadQueue = LinkedBlockingQueue<DownloadingItem>()
    private var downloaderJob: Job? = null

    var currentReferenceNumber: String? = null
        private set

    var totalImageCount: String? = null

    fun isLoggedInAndOnline(): Boolean {
        return runBlocking {
            authService.isSignedIn() && userRepository.isCloudAvailable()
        }
    }

    fun resetCurrentPhotocollection() {
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

    fun download(item: DownloadingItem) {
        if (!downloadQueue.contains(item)) {
            downloadQueue.add(item)
            startDownloaderJob()
        }
    }

    fun getDownloadingItems(): List<DownloadingItem> = downloadQueue.toList()
    fun getDownloadResultsFlow(): Flow<DownloadResult?> = downloadResultsFlow

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
            scope.launch {
                deletePhotocollection(it.id)
            }
        }
    }

    suspend fun deletePhotocollection(id: Long) {
        Log.d(TAG, "Deleting photocollection: $id")
        try {
            val response = photocollectionService.deletePhotocollection(id)
            if (response.isSuccessful) {
                Log.d(TAG, "Photocollection deleted: $id")
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    suspend fun getPhotocollection(id: Long, joinPhotos: Boolean = false): Photocollection? {
        try {
            val response = photocollectionService.getPhotocollection(id, joinPhotos)
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
        try {
            val response = photoscanService.getMesh(id)

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val fileName = "scan_$name.zip"
                    val file = File(outputDir, fileName)
                    saveResponseBodyToDisk(body, file)
                    Log.d(TAG, "Photoscan saved to ${file.absolutePath}")
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

    fun getImagesDir(referenceNumber: String) = File(
        Environment.getExternalStorageDirectory(),
        "$OPENLIFU_DIR/$referenceNumber"
    )

    suspend fun getReferenceNumbers(localOnly: Boolean): Set<String> {
        val result = mutableSetOf<String>()
        val localDir = File(Environment.getExternalStorageDirectory(), OPENLIFU_DIR)
        if (localDir.exists()) {
            localDir.list()?.let {
                result.addAll(it)
            }
        }
        val uid = authService.getCurrentUser()?.uid
        if (!localOnly && uid != null) {
            try {
                val names = photocollectionService.getPhotocollections(uid, joinPhotos = false)
                    .body()?.mapNotNull { it.name } ?: listOf()
                result.addAll(names)
            } catch (e: Exception) {
                Log.w(TAG, "Can't load photocollections: $e")
            }
        }
        return result
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
        try {
            val response = photocollectionService.getPhotocollection(id, joinPhotos = true)
            if (response.isSuccessful) {
                Log.d(TAG, "Loading photocollection $id")

                response.body()?.let { photocollection ->
                    val photos = photocollection.photos ?: listOf()
                    val outputDir = getImagesDir(photocollection.name ?: return false)
                    if (!outputDir.exists()) outputDir.mkdirs()

                    for (photo in photos) {
                        Log.d(TAG, "Loading photo ${photo.fileName}")
                        val photoResponse = photocollectionService.downloadPhoto(photocollection.id, photo.fileName)
                        if (!photoResponse.isSuccessful) return false

                        photoResponse.body()?.let {
                            val file = File(outputDir, photo.fileName)
                            saveResponseBodyToDisk(it, file)
                            Log.d(TAG, "Photo saved to ${file.absolutePath}")
                        }
                    }
                    return true
                }
            } else {
                Log.e(TAG, "Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
        }
        return false
    }

    private suspend fun downloadPhotoscan(id: Long): Boolean {
        val photoscan = getPhotoscan(id) ?: return false
        val photocollection = getPhotocollection(photoscan.photocollectionId) ?: return false
        val outputDir = getImagesDir(photocollection.name ?: return false)
        if (!outputDir.exists()) outputDir.mkdirs()
        return downloadPhotoscanZip(photoscan.id, photocollection.name, outputDir)
    }

    companion object {
        private val TAG = CloudRepository::class.simpleName
        private val OPENLIFU_DIR = "OpenLIFU-3DScanner"
    }

}