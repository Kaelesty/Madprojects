package ru.kaelesty.madprojects.features.auth.domain

import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.auth.LoginRequest
import ru.kaelesty.madprojects.features.auth.data.api.LoginApi

class LoginUseCase(
    private val api: LoginApi,
    private val authController: AuthController,
) {
    suspend fun login(email: String, password: String): Result {
        val response = api.login(LoginRequest(email = email, password = password))
        return when (response?.status) {
            HttpStatusCode.OK -> {
                val tokens = response.tokens ?: return Result.Unavailable
                val userType = response.userType ?: return Result.Unavailable
                authController.onAuthorized(tokens, userType)
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
}
