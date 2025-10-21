package com.example.facedetectionar.api.dto

data class StartPhotoscanRequest(
    val pipelineName: String? = null,
    val inputResizeWidth: Int? = null,
    val useMasks: Boolean? = null,
    val matchingMode: String? = null,
    val windowRadius: Int? = null,
    val numNeighbors: Int? = null,
    val locations: List<List<Float>>? = null,
    val returnDurations: Boolean? = null
)
