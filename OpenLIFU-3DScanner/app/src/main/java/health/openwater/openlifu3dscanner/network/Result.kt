package health.openwater.openlifu3dscanner.network

import retrofit2.HttpException
import java.io.IOException

sealed interface Result<out T> {
    data class Success<T>(val body: T) : Result<T>
    data class NetworkError(val message: String?) : Result<Nothing>
    data object AuthError : Result<Nothing>
    data class ServerError(val code: Int, val message: String?) : Result<Nothing>
    data class UnexpectedError(val message: String?) : Result<Nothing>
}

suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: IOException) {
        // connectivity, timeouts
        Result.NetworkError(e.message)
    } catch (e: HttpException) {
        // HTTP codes like 400/500
        Result.ServerError(code = e.code(), message = e.message())
    } catch (e: Exception) {
        Result.UnexpectedError(message = e.message)
    }
}
