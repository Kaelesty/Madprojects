package ru.kaelesty.madprojects.features.project.domain

import domain.sprints.ProfileSprint
import domain.sprints.SprintView
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.activity.ActivityResponse
import ru.kaelesty.madprojects.api.sprints.CreateSprintRequest
import ru.kaelesty.madprojects.api.sprints.UpdateSprintRequest
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.data.ProjectSprintsApi
import ru.kaelesty.madprojects.utils.KLogger
import shared_domain.entities.KanbanState

class GetProjectSprintsUseCase(
    private val api: ProjectSprintsApi,
    private val authContext: AuthContext,
) {
    suspend fun load(projectId: String): Result {
        KLogger.d(TAG) { "load project sprints start: projectId=$projectId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load project sprints failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.getProjectSprints(accessToken, projectId) ?: run {
            KLogger.w(TAG) { "load project sprints failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            val sprints = response.sprints ?: run {
                KLogger.w(TAG) { "load project sprints failed: body missing" }
                return Result.Fail(response.status, response.errorMessage)
            }
            KLogger.i(TAG) { "load project sprints success: count=${sprints.size}" }
            return Result.Success(sprints)
        }
        KLogger.w(TAG) { "load project sprints failed: status=${response.status} message=${response.errorMessage}" }
        return Result.Fail(response.status, response.errorMessage)
    }

    sealed interface Result {
        data class Success(val sprints: List<ProfileSprint>) : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "GetProjectSprintsUseCase"
    }
}

class GetProjectActivitiesUseCase(
    private val api: ProjectSprintsApi,
    private val authContext: AuthContext,
) {
    suspend fun load(projectId: String, count: Int? = 7): Result {
        KLogger.d(TAG) { "load project activities start: projectId=$projectId count=$count" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load project activities failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.getProjectActivities(accessToken, projectId, count) ?: run {
            KLogger.w(TAG) { "load project activities failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            val body = response.body ?: run {
                KLogger.w(TAG) { "load project activities failed: body missing" }
                return Result.Fail(response.status, response.errorMessage)
            }
            KLogger.i(TAG) { "load project activities success: count=${body.activities.size}" }
            return Result.Success(body)
        }
        KLogger.w(TAG) { "load project activities failed: status=${response.status} message=${response.errorMessage}" }
        return Result.Fail(response.status, response.errorMessage)
    }

    sealed interface Result {
        data class Success(val response: ActivityResponse) : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "GetProjectActivitiesUseCase"
    }
}

class GetSprintUseCase(
    private val api: ProjectSprintsApi,
    private val authContext: AuthContext,
) {
    suspend fun load(sprintId: String): Result {
        KLogger.d(TAG) { "load sprint start: sprintId=$sprintId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load sprint failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.getSprint(accessToken, sprintId) ?: run {
            KLogger.w(TAG) { "load sprint failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            val sprint = response.sprint ?: run {
                KLogger.w(TAG) { "load sprint failed: body missing" }
                return Result.Fail(response.status, response.errorMessage)
            }
            KLogger.i(TAG) { "load sprint success: title=${sprint.meta.title}" }
            return Result.Success(sprint)
        }
        KLogger.w(TAG) { "load sprint failed: status=${response.status} message=${response.errorMessage}" }
        return Result.Fail(response.status, response.errorMessage)
    }

    sealed interface Result {
        data class Success(val sprint: SprintView) : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "GetSprintUseCase"
    }
}

class CreateSprintUseCase(
    private val api: ProjectSprintsApi,
    private val authContext: AuthContext,
) {
    suspend fun create(request: CreateSprintRequest): Result {
        KLogger.d(TAG) { "create sprint start: projectId=${request.projectId}" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "create sprint failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.createSprint(accessToken, request) ?: run {
            KLogger.w(TAG) { "create sprint failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            val sprintId = response.sprintId ?: run {
                KLogger.w(TAG) { "create sprint failed: sprintId missing" }
                return Result.Fail(response.status, response.errorMessage)
            }
            KLogger.i(TAG) { "create sprint success: sprintId=$sprintId" }
            return Result.Success(sprintId)
        }
        KLogger.w(TAG) { "create sprint failed: status=${response.status} message=${response.errorMessage}" }
        return Result.Fail(response.status, response.errorMessage)
    }

    sealed interface Result {
        data class Success(val sprintId: String) : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "CreateSprintUseCase"
    }
}

class UpdateSprintUseCase(
    private val api: ProjectSprintsApi,
    private val authContext: AuthContext,
) {
    suspend fun update(request: UpdateSprintRequest): Result {
        KLogger.d(TAG) { "update sprint start: sprintId=${request.sprintId}" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "update sprint failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.updateSprint(accessToken, request) ?: run {
            KLogger.w(TAG) { "update sprint failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            KLogger.i(TAG) { "update sprint success" }
            return Result.Success
        }
        KLogger.w(TAG) { "update sprint failed: status=${response.status} message=${response.errorMessage}" }
        return Result.Fail(response.status, response.errorMessage)
    }

    sealed interface Result {
        data object Success : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "UpdateSprintUseCase"
    }
}

class FinishSprintUseCase(
    private val api: ProjectSprintsApi,
    private val authContext: AuthContext,
) {
    suspend fun finish(sprintId: String): Result {
        KLogger.d(TAG) { "finish sprint start: sprintId=$sprintId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "finish sprint failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.finishSprint(accessToken, sprintId) ?: run {
            KLogger.w(TAG) { "finish sprint failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            KLogger.i(TAG) { "finish sprint success" }
            return Result.Success
        }
        KLogger.w(TAG) { "finish sprint failed: status=${response.status} message=${response.errorMessage}" }
        return Result.Fail(response.status, response.errorMessage)
    }

    sealed interface Result {
        data object Success : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "FinishSprintUseCase"
    }
}

class GetProjectKardsUseCase(
    private val api: ProjectSprintsApi,
    private val authContext: AuthContext,
) {
    suspend fun load(projectId: String): Result {
        KLogger.d(TAG) { "load project kards start: projectId=$projectId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load project kards failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.getProjectKards(accessToken, projectId) ?: run {
            KLogger.w(TAG) { "load project kards failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            val kards = response.kards ?: run {
                KLogger.w(TAG) { "load project kards failed: body missing" }
                return Result.Fail(response.status, response.errorMessage)
            }
            KLogger.i(TAG) { "load project kards success: count=${kards.size}" }
            return Result.Success(kards)
        }
        KLogger.w(TAG) { "load project kards failed: status=${response.status} message=${response.errorMessage}" }
        return Result.Fail(response.status, response.errorMessage)
    }

    sealed interface Result {
        data class Success(val kards: List<KanbanState.Kard>) : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "GetProjectKardsUseCase"
    }
}
