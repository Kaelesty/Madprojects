package ru.kaelesty.madprojects.features.auth.domain

import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.data.LoginApi
import ru.kaelesty.madprojects.features.auth.data.LoginApi.LoginRequest

class LoginUseCase(
    private val api: LoginApi,
) {
    suspend fun login(email: String, password: String): Result {
        val status = api.login(LoginRequest(email = email, password = password))
        return when (status) {
            HttpStatusCode.OK -> Result.Success
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
