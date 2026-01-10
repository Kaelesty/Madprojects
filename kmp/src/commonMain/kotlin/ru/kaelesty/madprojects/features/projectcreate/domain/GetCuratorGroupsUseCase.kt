package ru.kaelesty.madprojects.features.projectcreate.domain

import domain.projectgroups.ProjectGroup
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.projectcreate.data.ProjectGroupsApi
import ru.kaelesty.madprojects.utils.KLogger

class GetCuratorGroupsUseCase(
    private val api: ProjectGroupsApi,
    private val authContext: AuthContext,
) {
    suspend fun load(curatorId: String): Result {
        KLogger.d(TAG) { "load groups start: curatorId=$curatorId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load groups failed: access token missing" }
            return Result.Fail(null)
        }
        val response = api.getCuratorGroups(accessToken, curatorId) ?: run {
            KLogger.w(TAG) { "load groups failed: response is null" }
            return Result.Fail(null)
        }
        if (response.status != HttpStatusCode.OK) {
            KLogger.w(TAG) { "load groups failed: status=${response.status}" }
            return Result.Fail(response.errorMessage)
        }
        val groups = response.groups ?: run {
            KLogger.w(TAG) { "load groups failed: empty body" }
            return Result.Fail(response.errorMessage)
        }
        KLogger.i(TAG) { "load groups success: count=${groups.size}" }
        return Result.Success(groups)
    }

    sealed interface Result {
        data class Success(val groups: List<ProjectGroup>) : Result
        data class Fail(val message: String?) : Result
    }

    private companion object {
        private const val TAG = "GetCuratorGroupsUseCase"
    }
}
