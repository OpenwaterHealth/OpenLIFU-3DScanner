package health.openwater.openlifu3dscanner.network.api

import health.openwater.openlifu3dscanner.network.dto.CreatePhotocollectionRequest
import health.openwater.openlifu3dscanner.network.dto.Photocollection
import health.openwater.openlifu3dscanner.network.dto.StartPhotoscanRequest
import health.openwater.openlifu3dscanner.network.dto.StartPhotoscanResponse
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
    suspend fun createPhotocollection(@Body request: CreatePhotocollectionRequest): Photocollection

    @GET("photocollection/{id}")
    suspend fun getPhotocollection(
        @Path("id") photocollectionId: Long,
        @Query("join_photos") joinPhotos: Boolean = false,
        @Query("join_coordinates") joinCoordinates: Boolean = false
    ): Photocollection

    @GET("photocollection/account/{uid}")
    suspend fun getPhotocollections(
        @Path("uid") uid: String,
        @Query("join_photos") joinPhotos: Boolean = false
    ): List<Photocollection>

    @DELETE("photocollection/{id}")
    suspend fun deletePhotocollection(
        @Path("id") photocollectionId: Long
    )

    @POST("photocollection/{id}/photo/{name}")
    suspend fun uploadPhoto(
        @Path("id") photocollectionId: Long,
        @Path("name") fileName: String,
        @Body body: RequestBody
    )

    @GET("photocollection/{id}/photo/{name}")
    suspend fun downloadPhoto(
        @Path("id") photocollectionId: Long,
        @Path("name") fileName: String,
    ): Response<ResponseBody>

    @POST("photocollection/{id}/start_photoscan")
    suspend fun startPhotoscan(
        @Path("id") photocollectionId: Long,
        @Body request: StartPhotoscanRequest
    ): StartPhotoscanResponse

}
