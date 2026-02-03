package health.openwater.openlifu3dscanner.repository

import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.api.AuthService
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.api.PhotoscanService
import health.openwater.openlifu3dscanner.network.dto.Photocollection
import health.openwater.openlifu3dscanner.network.dto.Photoscan
import health.openwater.openlifu3dscanner.network.safeCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val authService: AuthService,
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService
) {
    suspend fun getPhotocollections(): Result<List<Photocollection>> {
        val uid = authService.getCurrentUser()?.uid ?: return Result.AuthError
        return safeCall {
            photocollectionService.getPhotocollections(uid)
        }
    }

    suspend fun getPhotoscans(): Result<List<Photoscan>> {
        val uid = authService.getCurrentUser()?.uid ?: return Result.AuthError

        return safeCall {
            photoscanService.getPhotoscans(uid)
        }
    }
}