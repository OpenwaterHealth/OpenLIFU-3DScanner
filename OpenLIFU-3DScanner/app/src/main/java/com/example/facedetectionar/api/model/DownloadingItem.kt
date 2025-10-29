package com.example.facedetectionar.api.model

enum class Type {
    PHOTOCOLLECTION,
    PHOTOSCAN
}

data class DownloadingItem(
    val id: Long,
    val type: Type
)

data class DownloadResult(
    val item: DownloadingItem,
    val success: Boolean
)