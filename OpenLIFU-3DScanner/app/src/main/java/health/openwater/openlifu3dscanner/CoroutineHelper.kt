package health.openwater.openlifu3dscanner

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.coroutineScope
import health.openwater.openlifu3dscanner.api.dto.Photocollection
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object CoroutineHelper {
    @JvmStatic
    fun startReconstruction(cloudRepository: CloudRepository, photocollectionId: Long): Long? =
        runBlocking { cloudRepository.startReconstruction(photocollectionId) }

    @JvmStatic
    fun getPhotocollection(
        lifecycle: Lifecycle,
        cloudRepository: CloudRepository,
        photocollectionId: Long,
        joinPhotos: Boolean
    ): LiveData<Photocollection?> {
        val data = MutableLiveData<Photocollection?>()
        lifecycle.coroutineScope.launch {
            data.postValue(cloudRepository.getPhotocollection(photocollectionId, joinPhotos))
        }
        return data
    }
}
