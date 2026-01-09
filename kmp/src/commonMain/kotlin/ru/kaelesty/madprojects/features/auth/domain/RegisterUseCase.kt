package ru.kaelesty.madprojects.features.auth.domain

import domain.auth.RegisterRequest
import domain.auth.UserType
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.data.api.RegisterApi
import ru.kaelesty.madprojects.utils.KLogger

class RegisterUseCase(
    private val api: RegisterApi,
    private val authContext: AuthContext,
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
        KLogger.d(TAG) { "register start: userType=$userType emailLength=${email.length} usernameLength=${username.length}" }
        val response = api.register(
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
        if (response == null) {
            KLogger.w(TAG) { "register failed: response is null" }
            return Result.Unavailable
        }
        KLogger.d(TAG) { "register response: status=${response.status} hasTokens=${response.tokens != null} hasUserType=${response.userType != null}" }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val tokens = response.tokens ?: run {
                    KLogger.w(TAG) { "register failed: tokens missing" }
                    return Result.Unavailable
                }
                val userType = response.userType ?: run {
                    KLogger.w(TAG) { "register failed: userType missing" }
                    return Result.Unavailable
                }
                authContext.onAuthorized(tokens, userType)
                Result.Success
            }
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

    private companion object {
        private const val TAG = "RegisterUseCase"
    }
}
