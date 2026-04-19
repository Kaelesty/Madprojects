package ru.kaelesty.madprojects.features.auth.domain

import io.ktor.http.URLBuilder
import ru.kaelesty.madprojects.ktor.KtorConfig
import ru.kaelesty.madprojects.utils.KLogger

class StartGithubOauthUseCase(
    private val authContext: AuthContext,
) {

    suspend fun buildStartUrl(): Result {
        val accessToken = authContext.getAccessToken()
            ?.takeIf { it.isNotBlank() }
            ?: run {
                KLogger.w(TAG) { "buildStartUrl failed: access token missing" }
                return Result.Fail("Access token is missing")
            }
        val channel = resolveGithubOauthChannel()

        val url = runCatching {
            val normalizedBaseUrl = if (KtorConfig.BaseUrl.endsWith("/")) {
                KtorConfig.BaseUrl.dropLast(1)
            } else {
                KtorConfig.BaseUrl
            }
            URLBuilder("$normalizedBaseUrl$StartPath").apply {
                parameters.append(ChannelParam, channel.value)
                parameters.append(JwtParam, accessToken)
            }.buildString()
        }.getOrElse { error ->
            KLogger.e(TAG, error) { "buildStartUrl failed: url build exception" }
            return Result.Fail("Failed to build OAuth URL")
        }

        KLogger.i(TAG) { "buildStartUrl success: channel=${channel.value}" }
        return Result.Success(url)
    }

    sealed interface Result {
        data class Success(val url: String) : Result
        data class Fail(val message: String) : Result
    }

    private companion object {
        private const val TAG = "StartGithubOauthUseCase"
        private const val StartPath = "/oauth/github/start"
        private const val ChannelParam = "channel"
        private const val JwtParam = "jwt"
    }
}
