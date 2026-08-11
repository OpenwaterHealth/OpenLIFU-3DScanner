package health.openwater.openlifu3dscanner.network.dto

import java.util.Date

data class AuthLoginRequest(
    val email: String,
    val password: String
)

data class AuthRefreshRequest(
    val refreshToken: String
)

data class AuthTokenData(
    val accessToken: String,
    val refreshToken: String,
    val uid: String,
    val expirationDate: Date?
)

data class AuthTokenResponse(
    val status: String,
    val data: AuthTokenData
)
