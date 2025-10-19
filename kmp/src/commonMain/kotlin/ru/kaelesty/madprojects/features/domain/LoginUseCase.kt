package ru.kaelesty.madprojects.features.domain

class LoginUseCase {
    suspend fun login(email: String, password: String): Result {
        return LoginUseCase.Result.Fail
    }

    sealed interface Result {
        data object Success: Result
        data object Fail: Result
        data object Unavailable: Result
    }
}