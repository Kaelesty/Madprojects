package ru.kaelesty.madprojects.features.auth.domain

import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.data.RegisterApi
import ru.kaelesty.madprojects.features.auth.data.RegisterApi.RegisterRequest

class RegisterUseCase(
    private val api: RegisterApi,
) {
    suspend fun register(
        username: String,
        lastName: String,
        firstName: String,
        secondName: String,
        data: String,
        email: String,
        password: String,
        userType: UserType,
    ): Result {
        val status = api.register(
            RegisterRequest(
                username = username,
                lastName = lastName,
                firstName = firstName,
                secondName = secondName,
                data = data,
                email = email,
                password = password,
                userType = userType,
            )
        )
        return when (status) {
            HttpStatusCode.OK -> Result.Success
            HttpStatusCode.Conflict -> Result.EmailTaken
            HttpStatusCode.NotAcceptable -> Result.UsernameTaken
            HttpStatusCode.Forbidden -> Result.WeakPassword
            else -> Result.Unavailable
        }
    }

    sealed interface Result {
        data object Success : Result
        data object EmailTaken : Result
        data object UsernameTaken : Result
        data object WeakPassword : Result
        data object Unavailable : Result
    }
}
