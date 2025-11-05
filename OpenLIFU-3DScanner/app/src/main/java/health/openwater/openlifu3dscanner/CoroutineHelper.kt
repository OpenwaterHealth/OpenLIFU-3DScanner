package health.openwater.openlifu3dscanner

import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import kotlinx.coroutines.runBlocking

object CoroutineHelper {
    @JvmStatic
    fun startReconstruction(cloudRepository: CloudRepository, photocollectionId: Long): Long? =
        runBlocking { cloudRepository.startReconstruction(photocollectionId) }
}
