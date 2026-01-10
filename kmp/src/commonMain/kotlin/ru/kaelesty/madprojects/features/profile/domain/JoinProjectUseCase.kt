package ru.kaelesty.madprojects.features.profile.domain

import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.data.JoinProjectApi
import ru.kaelesty.madprojects.utils.KLogger

class JoinProjectUseCase(
    private val api: JoinProjectApi,
    private val authContext: AuthContext,
) {
    suspend fun join(invite: String): Result {
        KLogger.d(TAG) { "join start: inviteLength=${invite.length}" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "join failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.join(accessToken, invite) ?: run {
            KLogger.w(TAG) { "join failed: response is null" }
            return Result.Fail(null, null)
        }
        return if (response.status == HttpStatusCode.OK) {
            val projectId = response.projectId
            if (projectId == null) {
                KLogger.w(TAG) { "join failed: projectId missing" }
                Result.Fail(response.status, response.errorMessage)
            } else {
                KLogger.i(TAG) { "join success: projectId=$projectId" }
                Result.Success(projectId)
            }
        } else {
            KLogger.w(TAG) { "join failed: status=${response.status} message=${response.errorMessage}" }
            Result.Fail(response.status, response.errorMessage)
        }
    }

    sealed interface Result {
        data class Success(val projectId: String) : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "JoinProjectUseCase"
    }
}
