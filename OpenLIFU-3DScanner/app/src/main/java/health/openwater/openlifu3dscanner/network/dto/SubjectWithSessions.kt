package health.openwater.openlifu3dscanner.network.dto

data class Session(val id: Long, val localId: String, val name: String)
data class SubjectWithSessions(val id: Long, val localId: String, val name: String, val sessions: List<Session>)
