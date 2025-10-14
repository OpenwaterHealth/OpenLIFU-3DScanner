package com.example.facedetectionar.api

import com.example.facedetectionar.api.dto.UserCreditsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface UserService {

    @GET("users/{uid}/credits")
    suspend fun getCredits(@Path("uid") uid: String): Response<UserCreditsResponse>
}