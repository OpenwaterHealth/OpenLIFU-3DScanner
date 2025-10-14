package com.example.facedetectionar.api

import retrofit2.Response
import retrofit2.http.GET

interface PhotocollectionService {

    @GET("health")
    suspend fun healthCheck(): Response<Void>
}