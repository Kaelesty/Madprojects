package ru.kaelesty.madprojects.features.projectcreate.domain

import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.projectcreate.data.VerifyRepoLinkApi
import ru.kaelesty.madprojects.utils.KLogger

class VerifyRepoLinkUseCase(
    private val api: VerifyRepoLinkApi,
    private val authContext: AuthContext,
) {
    suspend fun verify(link: String): Result {
        KLogger.d(TAG) { "verify start: linkLength=${link.length}" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "verify failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.verify(accessToken, link) ?: run {
            KLogger.w(TAG) { "verify failed: response is null" }
            return Result.Fail(null, null)
        }
        return if (response.status == HttpStatusCode.OK) {
            KLogger.i(TAG) { "verify success" }
            Result.Success
        } else {
            KLogger.w(TAG) { "verify failed: status=${response.status} message=${response.errorMessage}" }
            Result.Fail(response.status, response.errorMessage)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "VerifyRepoLinkUseCase"
    }
}
