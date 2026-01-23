package health.openwater.openlifu3dscanner.network.api

import health.openwater.openlifu3dscanner.network.dto.Photoscan
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface PhotoscanService {

    @GET("photoscan/{id}")
    suspend fun getPhotoscan(@Path("id") id: Long): Photoscan

    @GET("photoscan/account/{uid}")
    suspend fun getPhotoscans(
        @Path("uid") uid: String,
        @Query("page") page: Int = 0,
        @Query("limit") perPage: Int = 5,
        @Query("join_progress_history") joinProgressHistory: Boolean = false
    ): List<Photoscan>

    @GET("photoscan/{id}/mesh")
    @Streaming
    suspend fun getMesh(@Path("id") id: Long): Response<ResponseBody>

    @DELETE("photoscan/{id}")
    suspend fun deletePhotoscan(@Path("id") id: Long)
}