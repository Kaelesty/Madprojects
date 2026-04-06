package ru.kaelesty.madprojects.features.profile.domain

import domain.profile.CommonProfileResponse
import domain.projectgroups.ProjectGroup
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.profile.CuratorProfileResponse
import ru.kaelesty.madprojects.api.profile.UpdateProfileRequest
import ru.kaelesty.madprojects.api.projectgroups.CreateProjectGroupRequest
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.data.CommonProfileApi
import ru.kaelesty.madprojects.utils.KLogger

class GetCommonProfileUseCase(
    private val api: CommonProfileApi,
    private val authContext: AuthContext,
) {
    suspend fun load(): Result {
        KLogger.d(TAG) { "load start" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load failed: access token missing" }
            return Result.Fail
        }
        val response = api.getCommonProfile(accessToken) ?: run {
            KLogger.w(TAG) { "load failed: response is null" }
            return Result.Fail
        }
        KLogger.d(TAG) { "load response: status=${response.status} hasProfile=${response.profile != null}" }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val profile = response.profile ?: run {
                    KLogger.w(TAG) { "load failed: profile missing" }
                    return Result.Fail
                }
                KLogger.i(TAG) { "load success: projects=${profile.projects.size} githubMeta=${profile.githubMeta != null}" }
                Result.Success(profile)
            }
            else -> {
                KLogger.w(TAG) { "load failed: status=${response.status}" }
                Result.Fail
            }
        }
    }

    sealed interface Result {
        data class Success(val profile: CommonProfileResponse) : Result
        data object Fail : Result
    }

    private companion object {
        private const val TAG = "GetCommonProfileUseCase"
    }
}

class GetCuratorProfileUseCase(
    private val api: CommonProfileApi,
    private val authContext: AuthContext,
) {
    suspend fun load(): Result {
        KLogger.d(TAG) { "load start" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load failed: access token missing" }
            return Result.Fail(null)
        }
        val response = api.getCuratorProfile(accessToken) ?: run {
            KLogger.w(TAG) { "load failed: response is null" }
            return Result.Fail(null)
        }
        KLogger.d(TAG) { "load response: status=${response.status} hasProfile=${response.profile != null}" }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val profile = response.profile ?: run {
                    KLogger.w(TAG) { "load failed: profile missing" }
                    return Result.Fail(response.errorMessage)
                }
                KLogger.i(TAG) { "load success: groups=${profile.projectGroups.size} githubMeta=${profile.githubMeta != null}" }
                Result.Success(profile)
            }
            else -> {
                KLogger.w(TAG) { "load failed: status=${response.status} message=${response.errorMessage}" }
                Result.Fail(response.errorMessage)
            }
        }
    }

    sealed interface Result {
        data class Success(val profile: CuratorProfileResponse) : Result
        data class Fail(val message: String?) : Result
    }

    private companion object {
        private const val TAG = "GetCuratorProfileUseCase"
    }
}

class UpdateCommonProfileUseCase(
    private val api: CommonProfileApi,
    private val authContext: AuthContext,
) {
    suspend fun update(request: UpdateProfileRequest): Result {
        KLogger.d(TAG) { "update start" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "update failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.updateCommonProfile(accessToken, request) ?: run {
            KLogger.w(TAG) { "update failed: response is null" }
            return Result.Fail(null, null)
        }
        return if (response.status == HttpStatusCode.OK) {
            KLogger.i(TAG) { "update success" }
            Result.Success
        } else {
            KLogger.w(TAG) { "update failed: status=${response.status} message=${response.errorMessage}" }
            Result.Fail(response.status, response.errorMessage)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "UpdateCommonProfileUseCase"
    }
}

class UpdateCuratorProfileUseCase(
    private val api: CommonProfileApi,
    private val authContext: AuthContext,
) {
    suspend fun update(request: UpdateProfileRequest): Result {
        KLogger.d(TAG) { "update start" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "update failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.updateCuratorProfile(accessToken, request) ?: run {
            KLogger.w(TAG) { "update failed: response is null" }
            return Result.Fail(null, null)
        }
        return if (response.status == HttpStatusCode.OK) {
            KLogger.i(TAG) { "update success" }
            Result.Success
        } else {
            KLogger.w(TAG) { "update failed: status=${response.status} message=${response.errorMessage}" }
            Result.Fail(response.status, response.errorMessage)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "UpdateCuratorProfileUseCase"
    }
}

class CreateProjectGroupUseCase(
    private val api: CommonProfileApi,
    private val authContext: AuthContext,
) {
    suspend fun create(title: String): Result {
        KLogger.d(TAG) { "create start" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "create failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.createProjectGroup(accessToken, CreateProjectGroupRequest(title = title)) ?: run {
            KLogger.w(TAG) { "create failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status != HttpStatusCode.OK) {
            KLogger.w(TAG) { "create failed: status=${response.status} message=${response.errorMessage}" }
            return Result.Fail(response.status, response.errorMessage)
        }
        val projectGroup = response.projectGroup ?: run {
            KLogger.w(TAG) { "create failed: projectGroup is missing in success response" }
            return Result.Fail(response.status, response.errorMessage)
        }
        KLogger.i(TAG) { "create success: groupId=${projectGroup.id}" }
        return Result.Success(projectGroup)
    }

    sealed interface Result {
        data class Success(val group: ProjectGroup) : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "CreateProjectGroupUseCase"
    }
}

class DeleteProjectGroupUseCase(
    private val api: CommonProfileApi,
    private val authContext: AuthContext,
) {
    suspend fun delete(projectGroupId: String): Result {
        KLogger.d(TAG) { "delete start: groupId=$projectGroupId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "delete failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.deleteProjectGroup(accessToken, projectGroupId) ?: run {
            KLogger.w(TAG) { "delete failed: response is null" }
            return Result.Fail(null, null)
        }
        return if (response.status == HttpStatusCode.OK) {
            KLogger.i(TAG) { "delete success: groupId=$projectGroupId" }
            Result.Success
        } else {
            KLogger.w(TAG) { "delete failed: status=${response.status} message=${response.errorMessage}" }
            Result.Fail(response.status, response.errorMessage)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "DeleteProjectGroupUseCase"
    }
}
