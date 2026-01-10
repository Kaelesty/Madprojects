package ru.kaelesty.madprojects.features.projectcreate.domain

import domain.project.CreateProjectRequest
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.projectcreate.data.CreateProjectApi
import ru.kaelesty.madprojects.utils.KLogger

class CreateProjectUseCase(
    private val api: CreateProjectApi,
    private val authContext: AuthContext,
) {
    suspend fun create(request: CreateProjectRequest): Result {
        KLogger.d(TAG) { "create start: titleLength=${request.title.length} repos=${request.repoLinks.size}" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "create failed: access token missing" }
            return Result.Fail(null)
        }
        val response = api.createProject(accessToken, request) ?: run {
            KLogger.w(TAG) { "create failed: response is null" }
            return Result.Fail(null)
        }
        if (response.status == HttpStatusCode.OK) {
            val projectId = response.projectId
            if (projectId != null) {
                KLogger.i(TAG) { "create success: projectId=$projectId" }
                return Result.Success(projectId)
            }
            KLogger.w(TAG) { "create failed: projectId missing" }
        } else {
            KLogger.w(TAG) { "create failed: status=${response.status} message=${response.errorMessage}" }
        }
        return Result.Fail(response.errorMessage)
    }

    sealed interface Result {
        data class Success(val projectId: String) : Result
        data class Fail(val message: String?) : Result
    }

    private companion object {
        private const val TAG = "CreateProjectUseCase"
    }
}
