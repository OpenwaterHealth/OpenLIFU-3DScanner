package health.openwater.openlifu3dscanner.network.dto

data class Institution(
    val id: Int,
    val name: String,
    val creationDate: String?,
    val modificationDate: String?
)

data class InstitutionResponseData(
    val institution: Institution
)

data class InstitutionResponse(
    val status: String,
    val data: InstitutionResponseData?
)
