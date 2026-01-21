package health.openwater.openlifu3dscanner.api

sealed interface DomainError {
    data object Auth : DomainError
    data class Network(val code: Int, val message: String?) : DomainError
    data class Unknown(val throwable: Throwable) : DomainError
}

sealed interface DomainResult<out T> {
    data object Loading : DomainResult<Nothing>
    data class Success<T>(val data: T) : DomainResult<T>
    data class Error(val error: DomainError) : DomainResult<Nothing>
}