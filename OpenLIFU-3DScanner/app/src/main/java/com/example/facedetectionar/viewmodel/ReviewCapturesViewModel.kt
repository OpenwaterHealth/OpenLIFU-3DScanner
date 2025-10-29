package com.example.facedetectionar.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facedetectionar.Modals.ReviewData
import com.example.facedetectionar.Modals.Status
import com.example.facedetectionar.api.PhotocollectionService
import com.example.facedetectionar.api.PhotoscanService
import com.example.facedetectionar.api.model.DownloadingItem
import com.example.facedetectionar.api.model.Type
import com.example.facedetectionar.api.repository.CloudRepository
import com.example.facedetectionar.api.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ReviewCapturesViewModel
@Inject constructor(
    private val cloudRepository: CloudRepository,
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService,
    private val userRepository: UserRepository
): ViewModel() {

    private val dataFlow = MutableStateFlow<List<ReviewData>>(listOf())
    private val selectedItemFlow = MutableStateFlow<ReviewData?>(null)
    private val downloadStatusChangeFlow = MutableStateFlow<ReviewData?>(null)
    private val loadingFlow = MutableStateFlow<Boolean>(false)

    init {
        viewModelScope.launch {
            cloudRepository.getDownloadResultsFlow().collect { result ->
                if (result == null) return@collect

                val item = dataFlow.value.firstOrNull {
                    (it.photocollectionId == result.item.id && result.item.type == Type.PHOTOCOLLECTION) ||
                    (it.photoscanId == result.item.id && result.item.type == Type.PHOTOSCAN)
                } ?: return@collect

                val newStatus = if (result.success) Status.LOCAL else Status.CLOUD

                downloadStatusChangeFlow.emit(
                    when (result.item.type) {
                        Type.PHOTOCOLLECTION -> item.copy(photoStatus = newStatus)
                        Type.PHOTOSCAN -> item.copy(meshStatus = newStatus)
                    }
                )
            }
        }
    }

    fun getDataFlow(): Flow<List<ReviewData>> = dataFlow
    fun getSelectedItemFlow(): StateFlow<ReviewData?> = selectedItemFlow
    fun getDownloadStatusChangeFlow(): StateFlow<ReviewData?> = downloadStatusChangeFlow
    fun getLoadingFlow(): Flow<Boolean> = loadingFlow

    fun setSelectedItem(item: ReviewData?) {
        selectedItemFlow.value = item
    }

    fun loadReviewData(showOnlineOnlyScans: Boolean) {
        loadingFlow.value = true
        val reviewList = mutableListOf<ReviewData>()
        reviewList.addAll(getLocalData())

        val uid = userRepository.authService.getCurrentUser()?.uid
        if (uid != null) {
            viewModelScope.launch {
                val cloudData = getCloudData(uid)

                val downloadingItems = cloudRepository.getDownloadingItems()

                for (cloudItem in cloudData) {
                    val isPhotoDownloading = downloadingItems.any {
                        it.type == Type.PHOTOCOLLECTION && it.id == cloudItem.photocollectionId
                    }
                    val isMeshDownloading = downloadingItems.any {
                        it.type == Type.PHOTOSCAN && it.id == cloudItem.photoscanId
                    }

                    reviewList.firstOrNull {
                        it.photocollectionId == null && it.id == cloudItem.id
                    }?.let { localItem ->
                        reviewList.remove(localItem)
                        reviewList.add(cloudItem.copy(
                            photoStatus = mergeStatus(localItem.photoStatus, cloudItem.photoStatus, isPhotoDownloading),
                            meshStatus = mergeStatus(localItem.meshStatus, cloudItem.meshStatus, isMeshDownloading)
                        ))
                    } ?: run {
                        reviewList.add(cloudItem)
                    }
                }

                if (!showOnlineOnlyScans) {
                    val onlineOnlyStatuses = listOf(Status.CLOUD, Status.UNKNOWN)
                    reviewList.removeIf { it.meshStatus in onlineOnlyStatuses && it.photoStatus in onlineOnlyStatuses}
                }

                dataFlow.value = reviewList
                loadingFlow.value = false
            }
        } else {
            dataFlow.value = reviewList
            loadingFlow.value = false
        }
    }

    fun downloadPhotocollection(item: ReviewData) {
        val id = item.photocollectionId ?: return
        if (item.photoStatus != Status.CLOUD) return
        downloadStatusChangeFlow.value = item.copy(photoStatus = Status.DOWNLOADING)
        cloudRepository.download(DownloadingItem(id, Type.PHOTOCOLLECTION))
    }

    fun downloadPhotoscan(item: ReviewData) {
        val id = item.photoscanId ?: return
        if (item.meshStatus != Status.CLOUD) return
        downloadStatusChangeFlow.value = item.copy(meshStatus = Status.DOWNLOADING)
        cloudRepository.download(DownloadingItem(id, Type.PHOTOSCAN))
    }

    private fun mergeStatus(local: Status, cloud: Status, downloading: Boolean): Status {
        return when {
            downloading -> Status.DOWNLOADING
            local in listOf(Status.LOCAL, Status.DOWNLOADING) -> local
            else -> cloud
        }
    }

    private suspend fun getCloudData(uid: String): List<ReviewData> {
        val result = mutableListOf<ReviewData>()
        try {
            val photocollections = photocollectionService.getPhotocollections(uid, joinPhotos = true).body() ?: listOf()
            val photoscans = photoscanService.getPhotoscans(uid).body() ?: listOf()

            for (photocollection in photocollections) {
                val scan = photoscans.firstOrNull { it.photocollectionId == photocollection.id }
                result.add(
                    ReviewData(
                        date = photocollection.creationDate,
                        id = photocollection.name ?: "",
                        count = photocollection.photos?.size ?: 0,
                        photoStatus = Status.CLOUD,
                        meshStatus = if (scan != null) Status.CLOUD else Status.UNKNOWN,
                        photocollectionId = photocollection.id,
                        photoscanId = scan?.id
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun getLocalData(): List<ReviewData> {
        val reviewList = mutableListOf<ReviewData>()
        val rootFolder = File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner")

        if (rootFolder.exists() && rootFolder.isDirectory) {
            val referenceFolders = rootFolder.listFiles { file -> file.isDirectory }

            referenceFolders?.forEach { folder ->
                val imageCount = folder.listFiles { file ->
                    val name = file.name.lowercase()
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                }?.size ?: 0

                val mesh = folder.listFiles { file ->
                    val name = file.name.lowercase()
                    name.startsWith("scan") && name.endsWith(".zip")
                }?.isNotEmpty() ?: false

                if (imageCount > 0) {
                    reviewList.add(
                        ReviewData(
                            date = Date(folder.lastModified()),
                            id = folder.name,
                            count = imageCount,
                            photoStatus = Status.LOCAL,
                            meshStatus = if (mesh) Status.LOCAL else Status.UNKNOWN
                        )
                    )
                } else {
                    // If folder has 0 images, delete the folder and its contents
                    folder.deleteRecursively()
//                    Log.d("ReviewCleanup", "Deleted empty folder: ${folder.name}")
                }
            }
        }

        return reviewList.sortedByDescending { it.date }
    }

}