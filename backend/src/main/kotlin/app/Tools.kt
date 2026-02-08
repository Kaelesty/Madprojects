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

        val accessToken = githubTokensRepo.getAccessToken(userId)
        if (accessToken is GithubTokensRepo.Token.Alive) {
            return accessToken.token
        }

        githubTokensRepo.getRefreshToken(userId)?.let { refresh ->
            if (refresh is GithubTokensRepo.Token.Expired) return null
            val refreshToken = refresh as? GithubTokensRepo.Token.Alive ?: return null
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

            val payload = parseGithubOauthTokens(response.bodyAsText())
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
            return access
        }
        return null
    }
}
