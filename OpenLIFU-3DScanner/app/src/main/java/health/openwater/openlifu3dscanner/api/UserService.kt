package health.openwater.openlifu3dscanner.api

import health.openwater.openlifu3dscanner.api.dto.UserCreditsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface UserService {

    @GET("users/{uid}/credits")
    suspend fun getCredits(@Path("uid") uid: String): Response<UserCreditsResponse>
}