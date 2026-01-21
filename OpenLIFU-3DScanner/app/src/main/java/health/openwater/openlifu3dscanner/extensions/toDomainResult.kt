package health.openwater.openlifu3dscanner.extensions

import health.openwater.openlifu3dscanner.api.DomainError
import health.openwater.openlifu3dscanner.api.DomainResult
import retrofit2.Response

inline fun <reified T> Response<T>.toDomainResult(): DomainResult<T> {
    return if (isSuccessful) {
        val body = body()
        if (body != null) {
            DomainResult.Success(body)
        } else {
            DomainResult.Error(
                DomainError.Unknown(
                    IllegalStateException("Response body is null")
                )
            )
        }
    } else {
        DomainResult.Error(
            DomainError.Network(
                code = code(),
                message = errorBody()?.string()
            )
        )
    }
}


suspend inline fun <reified T> callDomain(
    crossinline block: suspend () -> Response<T>
): DomainResult<T> {
    return try {
        val response = block()
        response.toDomainResult()
    } catch (t: Throwable) {
        DomainResult.Error(DomainError.Unknown(t))
    }
}