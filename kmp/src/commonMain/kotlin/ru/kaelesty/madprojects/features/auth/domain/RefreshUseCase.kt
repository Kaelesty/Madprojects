package ru.kaelesty.madprojects.features.auth.domain

import domain.auth.UserType
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.auth.Tokens
import ru.kaelesty.madprojects.features.auth.data.api.RefreshApi
import ru.kaelesty.madprojects.utils.KLogger

class RefreshUseCase(
    private val api: RefreshApi,
) {
    suspend fun refresh(refreshToken: String): Result {
        KLogger.d(TAG) { "refresh start: tokenLength=${refreshToken.length}" }
        val response = api.refresh(refreshToken) ?: run {
            KLogger.w(TAG) { "refresh failed: response is null" }
            return Result.Fail
        }
        if (response.status != HttpStatusCode.OK) {
            KLogger.w(TAG) { "refresh failed: status=${response.status}" }
            return Result.Fail
        }
        val tokens = response.tokens ?: run {
            KLogger.w(TAG) { "refresh failed: tokens missing" }
            return Result.Fail
        }
        val userType = response.userType ?: run {
            KLogger.w(TAG) { "refresh failed: userType missing" }
            return Result.Fail
        }
        KLogger.d(TAG) { "refresh success: userType=$userType" }
        return Result.Success(tokens, userType)
    }

    sealed interface Result {
        data class Success(val tokens: Tokens, val userType: UserType) : Result
        data object Fail : Result
    }

    private companion object {
        private const val TAG = "RefreshUseCase"
    }
}
