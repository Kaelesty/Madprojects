package ru.kaelesty.madprojects.features.profile.domain

import domain.profile.CommonProfileResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.data.CommonProfileApi

class GetCommonProfileUseCase(
    private val api: CommonProfileApi,
    private val authContext: AuthContext,
) {
    suspend fun load(): Result {
        val accessToken = authContext.tokens.value?.accessToken ?: return Result.Fail
        val response = api.getCommonProfile(accessToken) ?: return Result.Fail
        delay(10000)
        return when (response.status) {
            HttpStatusCode.OK -> {
                val profile = response.profile ?: return Result.Fail
                Result.Success(profile)
            }
            else -> Result.Fail
        }
    }

    sealed interface Result {
        data class Success(val profile: CommonProfileResponse) : Result
        data object Fail : Result
    }
}
