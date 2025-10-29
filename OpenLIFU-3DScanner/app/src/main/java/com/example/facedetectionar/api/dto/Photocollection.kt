package com.example.facedetectionar.api.dto

import java.sql.Date

data class Photocollection(
    val id: Long,
    val accountId: String,
    val name: String?,
    val creationDate: Date,
    val modificationDate: Date?,
    val photos: List<Photo>?
)