package com.example.facedetectionar

import com.example.facedetectionar.api.repository.CloudRepository
import kotlinx.coroutines.runBlocking

object CoroutineHelper {
    @JvmStatic
    fun startReconstruction(cloudRepository: CloudRepository, photocollectionId: Long): Long? =
        runBlocking { cloudRepository.startReconstruction(photocollectionId) }
}
