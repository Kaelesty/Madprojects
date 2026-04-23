package app

import domain.GithubTokensRepo
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters

class GithubTokenUtil(
    private val githubTokensRepo: GithubTokensRepo,
    private val httpClient: HttpClient,
    private val config: app.config.Config
) {
    suspend fun getGithubAccessToken(userId: String): String? {
        LogManager.emitError("GithubTokenUtil.getGithubAccessToken start: userId=$userId")

        val accessToken = githubTokensRepo.getAccessToken(userId)
        if (accessToken is GithubTokensRepo.Token.Alive) {
            LogManager.emitError(
                "GithubTokenUtil.getGithubAccessToken access token alive: userId=$userId, length=${accessToken.token.length}"
            )
            return accessToken.token
        }
        if (accessToken is GithubTokensRepo.Token.Expired) {
            LogManager.emitError("GithubTokenUtil.getGithubAccessToken access token expired: userId=$userId")
        } else if (accessToken == null) {
            LogManager.emitError("GithubTokenUtil.getGithubAccessToken access token missing: userId=$userId")
        }

        githubTokensRepo.getRefreshToken(userId)?.let { refresh ->
            if (refresh is GithubTokensRepo.Token.Expired) {
                LogManager.emitError("GithubTokenUtil.getGithubAccessToken refresh token expired: userId=$userId")
                return null
            }
            val refreshToken = refresh as? GithubTokensRepo.Token.Alive ?: return null
            LogManager.emitError(
                "GithubTokenUtil.getGithubAccessToken trying refresh: userId=$userId, refreshLength=${refreshToken.token.length}"
            )
            val gConfig = config.github
            val response = httpClient.submitForm(
                url = "https://github.com/login/oauth/access_token",
                formParameters = parameters {
                    append("client_id", gConfig.clientId)
                    append("client_secret", gConfig.clientSecret)
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken.token)
                }
            ) {
                headers.append(HttpHeaders.Accept, "application/json")
            }

            val responseText = response.bodyAsText()
            val payload = parseGithubOauthTokens(responseText)
            LogManager.emitError(
                "GithubTokenUtil.getGithubAccessToken refresh response: userId=$userId, " +
                    "status=${response.status.value}, hasAccess=${!payload?.access_token.isNullOrBlank()}, " +
                    "hasRefresh=${!payload?.refresh_token.isNullOrBlank()}, error=${payload?.error}, " +
                    "description=${payload?.error_description}, rawLength=${responseText.length}"
            )
            if (response.status != HttpStatusCode.OK || payload?.access_token.isNullOrBlank()) {
                LogManager.emitError(
                    "GitHub refresh failed for userId=$userId, status=${response.status.value}, " +
                        "error=${payload?.error}, description=${payload?.error_description}"
                )
                return null
            }
            val access = payload.access_token ?: return null

            githubTokensRepo.updateTokens(
                access = access,
                refresh = payload.refresh_token ?: refreshToken.token,
                userId = userId,
            )
            LogManager.emitError(
                "GithubTokenUtil.getGithubAccessToken refresh success: userId=$userId, newAccessLength=${access.length}, " +
                    "newRefreshProvided=${!payload?.refresh_token.isNullOrBlank()}"
            )
            return access
        }
        LogManager.emitError("GithubTokenUtil.getGithubAccessToken refresh token missing: userId=$userId")
        return null
    }
}
