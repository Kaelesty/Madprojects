package ru.kaelesty.madprojects.features.projectcreate.domain

import domain.project.AvailableCurator
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.projectcreate.data.CuratorsApi
import ru.kaelesty.madprojects.utils.KLogger

class GetCuratorsUseCase(
    private val api: CuratorsApi,
    private val authContext: AuthContext,
) {
    suspend fun load(): Result {
        KLogger.d(TAG) { "load curators start" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "load curators failed: access token missing" }
            return Result.Fail(null)
        }
        val response = api.getCurators(accessToken) ?: run {
            KLogger.w(TAG) { "load curators failed: response is null" }
            return Result.Fail(null)
        }
        if (response.status != HttpStatusCode.OK) {
            KLogger.w(TAG) { "load curators failed: status=${response.status}" }
            return Result.Fail(response.errorMessage)
        }
        val curators = response.curators ?: run {
            KLogger.w(TAG) { "load curators failed: empty body" }
            return Result.Fail(response.errorMessage)
        }
        KLogger.i(TAG) { "load curators success: count=${curators.size}" }
        return Result.Success(curators)
    }

    sealed interface Result {
        data class Success(val curators: List<AvailableCurator>) : Result
        data class Fail(val message: String?) : Result
    }

    private companion object {
        private const val TAG = "GetCuratorsUseCase"
    }
}
