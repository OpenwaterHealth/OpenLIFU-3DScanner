package com.example.facedetectionar.api.model

data class ReconstructionProgress(
    val progress: Int,
    val message: String?,
    val status: String?
)