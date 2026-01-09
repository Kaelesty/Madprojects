package ru.kaelesty.madprojects.features.profile.domain

import domain.profile.ProfileProject
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.data.CommonProfileApi
import ru.kaelesty.madprojects.utils.KLogger

class GetUserProjectsUseCase(
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
                KLogger.i(TAG) { "load success: projects=${profile.projects.size}" }
                Result.Success(profile.projects)
            }
            else -> {
                KLogger.w(TAG) { "load failed: status=${response.status}" }
                Result.Fail
            }
        }
    }

    sealed interface Result {
        data class Success(val projects: List<ProfileProject>) : Result
        data object Fail : Result
    }

    private companion object {
        private const val TAG = "GetUserProjectsUseCase"
    }
}
