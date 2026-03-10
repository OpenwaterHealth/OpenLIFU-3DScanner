package health.openwater.openlifu3dscanner.network.dto

import java.util.Date

data class Session(
    val id: Long,
    val localId: String,
    val name: String,
    val creationDate: Date
)

data class SubjectWithSessions(
    val id: Long,
    val localId: String,
    val name: String,
    val creationDate: Date,
    val sessions: List<Session>
)
