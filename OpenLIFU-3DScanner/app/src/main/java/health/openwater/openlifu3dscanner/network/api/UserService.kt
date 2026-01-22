package health.openwater.openlifu3dscanner.network.api

import health.openwater.openlifu3dscanner.network.dto.ResetPasswordRequest
import health.openwater.openlifu3dscanner.network.dto.StatusResponse
import health.openwater.openlifu3dscanner.network.dto.UserCreditsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserService {

    @GET("users/{uid}/credits")
    suspend fun getCredits(@Path("uid") uid: String): UserCreditsResponse

    @POST("users/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<StatusResponse>
}