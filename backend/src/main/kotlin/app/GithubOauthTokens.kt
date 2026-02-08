package app

import io.ktor.http.parseQueryString
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class GithubOauthTokens(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val token_type: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val error_description: String? = null,
    val error_uri: String? = null,
)

private val githubOauthJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun parseGithubOauthTokens(raw: String): GithubOauthTokens? {
    if (raw.isBlank()) return null

    return runCatching {
        githubOauthJson.decodeFromString<GithubOauthTokens>(raw)
    }.getOrElse {
        val params = parseQueryString(raw)
        if (params.names().isEmpty()) return null

        GithubOauthTokens(
            access_token = params["access_token"],
            refresh_token = params["refresh_token"],
            token_type = params["token_type"],
            scope = params["scope"],
            error = params["error"],
            error_description = params["error_description"],
            error_uri = params["error_uri"],
        )
    }
}
