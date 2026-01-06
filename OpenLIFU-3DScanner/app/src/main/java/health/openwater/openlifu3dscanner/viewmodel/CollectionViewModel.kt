package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.api.model.DownloadingItem
import health.openwater.openlifu3dscanner.api.model.Type
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import javax.inject.Inject


@HiltViewModel
class CollectionViewModel @Inject constructor(
    application: Application,
    private val cloudRepository: CloudRepository
) : AndroidViewModel(application) {

    suspend fun getPhotoscans() = cloudRepository.getPhotoscans()

    suspend fun getPhotocollections() = cloudRepository.getPhotocollections()

    fun downloadMesh(scanId: Long) {
        cloudRepository.download(DownloadingItem(scanId, Type.PHOTOSCAN))
    }

    fun getDownloadResultsFlow() = cloudRepository.getDownloadResultsFlow()
}