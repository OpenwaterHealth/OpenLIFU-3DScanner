package health.openwater.openlifu3dscanner.network.api

import health.openwater.openlifu3dscanner.network.dto.AuthLoginRequest
import health.openwater.openlifu3dscanner.network.dto.AuthRefreshRequest
import health.openwater.openlifu3dscanner.network.dto.AuthTokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("users/auth/login")
    suspend fun login(@Body request: AuthLoginRequest): AuthTokenResponse

    @POST("users/auth/refresh_token")
    suspend fun refreshToken(@Body request: AuthRefreshRequest): AuthTokenResponse
}
