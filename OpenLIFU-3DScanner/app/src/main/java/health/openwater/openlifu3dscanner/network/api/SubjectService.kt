package health.openwater.openlifu3dscanner.network.api

import health.openwater.openlifu3dscanner.network.dto.SubjectWithSessions
import retrofit2.http.GET

interface SubjectService {
    @GET("subjects/sessions")
    suspend fun getSubjectsWithSessions(): List<SubjectWithSessions>
}
