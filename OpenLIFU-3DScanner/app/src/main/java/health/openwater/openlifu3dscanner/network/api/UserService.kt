package health.openwater.openlifu3dscanner.network.api

import health.openwater.openlifu3dscanner.network.dto.InstitutionResponse
import health.openwater.openlifu3dscanner.network.dto.ResetPasswordRequest
import health.openwater.openlifu3dscanner.network.dto.StatusResponse
import health.openwater.openlifu3dscanner.network.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserService {

    @GET("users/{uid}")
    suspend fun getUser(@Path("uid") uid: String): UserResponse

    @GET("institutions/{id}")
    suspend fun getInstitution(@Path("id") id: Int): InstitutionResponse

    @POST("users/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): StatusResponse
}