package health.openwater.openlifu3dscanner.network.dto

data class UserCredit(
    val uid: String,
    val credit: Int
)

data class UserData(
    val user: UserCredit
)

data class UserCreditsResponse(
    val status: String,
    val data: UserData?
)