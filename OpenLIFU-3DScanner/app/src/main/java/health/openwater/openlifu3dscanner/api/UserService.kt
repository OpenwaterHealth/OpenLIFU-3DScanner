package health.openwater.openlifu3dscanner.api

import health.openwater.openlifu3dscanner.api.dto.ResetPasswordRequest
import health.openwater.openlifu3dscanner.api.dto.StatusResponse
import health.openwater.openlifu3dscanner.api.dto.UserCreditsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserService {

    @GET("users/{uid}/credits")
    suspend fun getCredits(@Path("uid") uid: String): Response<UserCreditsResponse>

    @POST("users/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<StatusResponse>
}