package com.example.facedetectionar.api

import com.example.facedetectionar.api.dto.Photoscan
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface PhotoscanService {

    @GET("photoscan/{id}")
    suspend fun getPhotoscan( @Path("id") id: Long): Response<Photoscan>

    @GET("photoscan/{id}/mesh")
    @Streaming
    suspend fun getMesh( @Path("id") id: Long): Response<ResponseBody>
}