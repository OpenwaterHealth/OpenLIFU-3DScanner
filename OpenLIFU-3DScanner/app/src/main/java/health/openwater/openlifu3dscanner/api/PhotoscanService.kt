package health.openwater.openlifu3dscanner.api

import health.openwater.openlifu3dscanner.api.dto.Photoscan
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface PhotoscanService {

    @GET("photoscan/{id}")
    suspend fun getPhotoscan(@Path("id") id: Long): Response<Photoscan>

    @GET("photoscan/account/{uid}")
    suspend fun getPhotoscans(
        @Path("uid") uid: String,
        @Query("page") page: Int = 0,
        @Query("limit") perPage: Int = 5,
        @Query("join_progress_history") joinProgressHistory: Boolean = false
    ): Response<List<Photoscan>>

    @GET("photoscan/{id}/mesh")
    @Streaming
    suspend fun getMesh(@Path("id") id: Long): Response<ResponseBody>
}