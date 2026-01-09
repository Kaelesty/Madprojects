package ru.kaelesty.madprojects.features.auth.domain

import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.auth.LoginRequest
import ru.kaelesty.madprojects.features.auth.data.api.LoginApi
import ru.kaelesty.madprojects.utils.KLogger

class LoginUseCase(
    private val api: LoginApi,
    private val authContext: AuthContext,
) {
    suspend fun login(email: String, password: String): Result {
        KLogger.d(TAG) { "login start: emailLength=${email.length}" }
        val response = api.login(LoginRequest(email = email, password = password))
        if (response == null) {
            KLogger.w(TAG) { "login failed: response is null" }
            return Result.Unavailable
        }
        KLogger.d(TAG) { "login response: status=${response.status} hasTokens=${response.tokens != null} hasUserType=${response.userType != null}" }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val tokens = response.tokens ?: run {
                    KLogger.w(TAG) { "login failed: tokens missing" }
                    return Result.Unavailable
                }
                val userType = response.userType ?: run {
                    KLogger.w(TAG) { "login failed: userType missing" }
                    return Result.Unavailable
                }
                authContext.onAuthorized(tokens, userType)
                Result.Success
            }
            HttpStatusCode.Forbidden -> Result.Fail
            else -> Result.Unavailable
        }
    }

    sealed interface Result {
        data object Success : Result
        data object Fail : Result
        data object Unavailable : Result
    }

    private companion object {
        private const val TAG = "LoginUseCase"
    }
}
