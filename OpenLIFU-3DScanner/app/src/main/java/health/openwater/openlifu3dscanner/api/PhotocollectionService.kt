package health.openwater.openlifu3dscanner.api

import health.openwater.openlifu3dscanner.api.dto.CreatePhotocollectionRequest
import health.openwater.openlifu3dscanner.api.dto.Photocollection
import health.openwater.openlifu3dscanner.api.dto.StartPhotoscanRequest
import health.openwater.openlifu3dscanner.api.dto.StartPhotoscanResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PhotocollectionService {

    @GET("health")
    suspend fun healthCheck(): Response<Void>

    @POST("photocollection")
    suspend fun createPhotocollection(@Body request: CreatePhotocollectionRequest): Response<Photocollection>

    @GET("photocollection/{id}")
    suspend fun getPhotocollection(
        @Path("id") photocollectionId: Long,
        @Query("join_photos") joinPhotos: Boolean = false
    ): Response<Photocollection>

    @GET("photocollection/account/{uid}")
    suspend fun getPhotocollections(
        @Path("uid") uid: String,
        @Query("join_photos") joinPhotos: Boolean = false
    ): Response<List<Photocollection>>

    @DELETE("photocollection/{id}")
    suspend fun deletePhotocollection(
        @Path("id") photocollectionId: Long
    ): Response<Void>

    @POST("photocollection/{id}/photo/{name}")
    suspend fun uploadPhoto(
        @Path("id") photocollectionId: Long,
        @Path("name") fileName: String,
        @Body body: RequestBody
    ): Response<Void>

    @GET("photocollection/{id}/photo/{name}")
    suspend fun downloadPhoto(
        @Path("id") photocollectionId: Long,
        @Path("name") fileName: String,
    ): Response<ResponseBody>

    @POST("photocollection/{id}/start_photoscan")
    suspend fun startPhotoscan(
        @Path("id") photocollectionId: Long,
        @Body request: StartPhotoscanRequest
    ): Response<StartPhotoscanResponse>

}