package ru.kaelesty.madprojects.features.profile.domain

import domain.project.ProjectStatus
import domain.projectgroups.ProjectInGroupView
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.data.CommonProfileApi
import ru.kaelesty.madprojects.utils.KLogger

class GetCuratorGroupProjectsUseCase(
    private val api: CommonProfileApi,
    private val authContext: AuthContext,
) {
    suspend fun load(groupId: String): Result {
        KLogger.d(TAG) { "load start: groupId=$groupId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load failed: access token missing" }
            return Result.Fail(null)
        }
        val response = api.getGroupProjects(accessToken, groupId) ?: run {
            KLogger.w(TAG) { "load failed: response is null" }
            return Result.Fail(null)
        }
        if (response.status != HttpStatusCode.OK) {
            KLogger.w(TAG) { "load failed: status=${response.status} message=${response.errorMessage}" }
            return Result.Fail(response.errorMessage)
        }
        val payload = response.payload ?: run {
            KLogger.w(TAG) { "load failed: payload is null" }
            return Result.Fail(null)
        }
        val pending = payload.projects.filter { it.status == ProjectStatus.Pending }
        val approved = payload.projects.filter { it.status == ProjectStatus.Approved }
        KLogger.i(TAG) { "load success: title=${payload.title}, pending=${pending.size}, approved=${approved.size}, total=${payload.projects.size}" }
        return Result.Success(
            groupTitle = payload.title,
            pendingProjects = pending,
            approvedProjects = approved,
        )
    }

    sealed interface Result {
        data class Success(
            val groupTitle: String,
            val pendingProjects: List<ProjectInGroupView>,
            val approvedProjects: List<ProjectInGroupView>,
        ) : Result
        data class Fail(val message: String?) : Result
    }

    private companion object {
        private const val TAG = "GetCuratorGroupProjectsUseCase"
    }
}

