package app.features.github

import app.GithubTokenUtil
import app.LogManager
import app.openapi.annotations.*
import app.parseGithubOauthTokens
import com.auth0.jwt.JWTVerifier
import domain.BranchesRepo
import domain.GithubTokensRepo
import domain.RepositoriesRepo
import domain.profile.ProfileRepo
import domain.profile.SharedProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.parameters
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.json.Json
import ru.kaelesty.madprojects.api.github.VerifyResponse
import shared_domain.entities.GithubUser
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface GithubFeature {

    suspend fun startGithubOAuth(rc: RoutingContext)

    suspend fun proceedGithubOAuthCallback(rc: RoutingContext)

    suspend fun getUserMeta(rc: RoutingContext)

    suspend fun getRepoBranchContent(rc: RoutingContext)

    suspend fun getProjectRepoBranches(rc: RoutingContext)

    suspend fun verifyRepoLink(rc: RoutingContext)

    suspend fun proceedGithubApiCallback(rc: RoutingContext)
}

class GithubFeatureImpl(
    private val githubTokensRepo: GithubTokensRepo,
    private val repositoriesRepo: RepositoriesRepo,
    private val httpClient: HttpClient,
    private val jwt: JWTVerifier,
    private val profileRepo: ProfileRepo,
    private val branchesRepo: BranchesRepo,
    private val tokenUtil: GithubTokenUtil,
    private val config: app.config.Config
): GithubFeature {

    private val githubAuthLink = "https://github.com/login/oauth/access_token"
    private val githubRepoLink = "https://api.github.com/repos"
    private val oauthSessions = ConcurrentHashMap<String, GithubOAuthSession>()

    @ApiOperation(method = "GET", path = "/oauth/github/start", summary = "Start GitHub OAuth", tags = ["github"])
    @ApiQueryParam(name = "channel", type = String::class, required = true, description = "OAuth target channel")
    @ApiQueryParam(name = "jwt", type = String::class, required = false, description = "JWT token when Authorization header is absent")
    @ApiHeaderParam(name = "Authorization", required = false, description = "Bearer JWT token")
    @ApiResponse(code = 302, description = "Redirect to GitHub authorization")
    @ApiResponse(code = 400, description = "Unsupported OAuth channel")
    @ApiResponse(code = 401, description = "JWT is missing or invalid")
    override suspend fun startGithubOAuth(rc: RoutingContext) {
        with(rc) {
            cleanupExpiredSessions()

            val channel = GithubOAuthChannel.fromRaw(call.parameters["channel"]) ?: run {
                call.respond(HttpStatusCode.BadRequest, "Unsupported oauth channel")
                return
            }
            val jwtToken = call.parameters["jwt"]?.takeIf { it.isNotBlank() }
                ?: call.request.headers[HttpHeaders.Authorization]
                    ?.removePrefix("Bearer")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            val userId = jwtToken?.let(::extractUserIdFromJwt)
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to parse jwt")
                return
            }

            val sessionId = UUID.randomUUID().toString()
            val expiresAtMillis = System.currentTimeMillis() + resolveOauthSessionTtlMillis()
            oauthSessions[sessionId] = GithubOAuthSession(
                sessionId = sessionId,
                channel = channel.value,
                userId = userId,
                expiresAtMillis = expiresAtMillis,
            )

            val authorizeUrl = buildGithubAuthorizeUrl(
                state = "$sessionId:${channel.value}",
                backendCallbackUrl = resolveBackendCallbackUrl()
            )
            LogManager.emitError(
                "GithubFeature.startGithubOAuth redirect: sessionId=$sessionId channel=${channel.value} userId=$userId"
            )
            call.respondRedirect(authorizeUrl, permanent = false)
        }
    }

    @ApiOperation(method = "GET", path = "/oauth/github/callback", summary = "Handle GitHub OAuth callback", tags = ["github"])
    @ApiQueryParam(name = "state", type = String::class, required = true)
    @ApiQueryParam(name = "code", type = String::class, required = false)
    @ApiQueryParam(name = "error", type = String::class, required = false)
    @ApiQueryParam(name = "error_description", type = String::class, required = false)
    @ApiResponse(code = 302, description = "Redirect to client application")
    @ApiResponse(code = 401, description = "OAuth state is invalid")
    override suspend fun proceedGithubOAuthCallback(rc: RoutingContext) {
        with(rc) {
            cleanupExpiredSessions()

            val parsedState = parseOAuthState(call.parameters["state"])
            if (parsedState == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid oauth state")
                return
            }
            val (sessionId, channel) = parsedState
            val session = oauthSessions.remove(sessionId)
            if (session == null || session.expiresAtMillis < System.currentTimeMillis() || session.channel != channel.value) {
                LogManager.emitError(
                    "GithubFeature.proceedGithubOAuthCallback session invalid: sessionId=$sessionId channel=${channel.value}"
                )
                call.respondRedirect(
                    buildClientRedirect(channel = channel, isSuccess = false, reason = "session_invalid"),
                    permanent = false
                )
                return
            }

            val oauthError = call.parameters["error"]
            if (!oauthError.isNullOrBlank()) {
                val reason = call.parameters["error_description"]?.takeIf { it.isNotBlank() } ?: oauthError
                LogManager.emitError(
                    "GithubFeature.proceedGithubOAuthCallback oauth denied: sessionId=$sessionId channel=${channel.value} reason=$reason"
                )
                call.respondRedirect(
                    buildClientRedirect(channel = channel, isSuccess = false, reason = reason),
                    permanent = false
                )
                return
            }

            val githubCode = call.parameters["code"]
            if (githubCode.isNullOrBlank()) {
                call.respondRedirect(
                    buildClientRedirect(channel = channel, isSuccess = false, reason = "missing_code"),
                    permanent = false
                )
                return
            }

            when (val result = exchangeCodeAndSaveTokens(githubCode, session.userId)) {
                is OAuthExchangeResult.Success -> {
                    call.respondRedirect(
                        buildClientRedirect(channel = channel, isSuccess = true, reason = null),
                        permanent = false
                    )
                }
                is OAuthExchangeResult.Fail -> {
                    LogManager.emitError(
                        "GithubFeature.proceedGithubOAuthCallback exchange failed: sessionId=$sessionId channel=${channel.value} " +
                            "status=${result.status.value} message=${result.message}"
                    )
                    call.respondRedirect(
                        buildClientRedirect(channel = channel, isSuccess = false, reason = result.message),
                        permanent = false
                    )
                }
            }
        }
    }

    @ApiOperation(method = "GET", path = "/github/githubCallbackUrl", summary = "Handle legacy GitHub callback", tags = ["github"])
    @ApiQueryParam(name = "code", type = String::class, required = true)
    @ApiQueryParam(name = "state", type = String::class, required = true)
    @ApiResponse(code = 200, description = "GitHub tokens recorded")
    @ApiResponse(code = 401, description = "GitHub code or state is invalid")
    @ApiResponse(code = 424, description = "GitHub user metadata could not be loaded")
    override suspend fun proceedGithubApiCallback(rc: RoutingContext) {
        with(rc) {

            val githubCode = call.parameters["code"]
            if (githubCode == null) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to parse github code")
                return
            }
            val userId = call.parameters["state"]
                ?.let {
                    jwt.verify(it).getClaim("userId").asString()
                }
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to get tokens from code")
                return
            }



            when (val result = exchangeCodeAndSaveTokens(githubCode, userId)) {
                is OAuthExchangeResult.Success -> {
                    call.respond(HttpStatusCode.OK, "Tokens are recorded")
                }
                is OAuthExchangeResult.Fail -> {
                    call.respond(result.status, result.message)
                }
            }
        }
    }

    @ApiOperation(method = "GET", path = "/github/verifyRepoLink", summary = "Verify GitHub repository link", tags = ["github"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "repolink", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Repository link is valid")
    @ApiResponse(code = 400, description = "Repository link is invalid")
    @ApiResponse(code = 401, description = "JWT is invalid")
    @ApiResponse(code = 404, description = "Repository not found")
    @ApiResponse(code = 405, description = "Repository is private")
    @ApiResponse(code = 425, description = "GitHub token is not available")
    override suspend fun verifyRepoLink(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val link = call.parameters["repolink"]
            if (link == null) {
                call.respond(HttpStatusCode.BadRequest, "Bad parameters")
                return
            }
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to parse jwt")
                return
            }
            val githubJwt = tokenUtil.getGithubAccessToken(userId)

            if (githubJwt == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }

            val parts = link.trimEnd('/').split("/")
            if (parts.size < 2) {
                call.respond(HttpStatusCode.BadRequest, "Invalid repolink")
                return
            }
            val owner = parts[parts.size - 2]
            val repo = parts[parts.size - 1]
            val response = httpClient.get("$githubRepoLink/$owner/$repo") {
                header("Authorization", "Bearer $githubJwt")
            }
            if (response.status != HttpStatusCode.OK) {
                call.respond(HttpStatusCode.NotFound, "Invalid repolink")
                return
            }
            val body = response.body<VerifyResponse>()
            if (body.isPrivate) {
                call.respond(HttpStatusCode.MethodNotAllowed, "Private repo")
                return
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    @ApiOperation(method = "GET", path = "/github/getProjectRepoBranches", summary = "Get project repository branches", tags = ["github"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Repository branches returned")
    @ApiResponse(code = 400, description = "Project id is missing")
    @ApiResponse(code = 401, description = "JWT is invalid")
    @ApiResponse(code = 404, description = "Project repositories not found")
    @ApiResponse(code = 425, description = "GitHub token is not available")
    override suspend fun getProjectRepoBranches(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            LogManager.emitError(
                "GithubFeature.getProjectRepoBranches request: userId=$userId, projectId=$projectId"
            )
            if (projectId == null) {
                LogManager.emitError("GithubFeature.getProjectRepoBranches bad parameters: userId=$userId")
                call.respond(HttpStatusCode.BadRequest, "Bad parameters")
                return
            }
            if (userId == null) {
                LogManager.emitError("GithubFeature.getProjectRepoBranches failed to parse jwt userId")
                call.respond(HttpStatusCode.Unauthorized, "Failed to parse jwt")
                return
            }
            val githubJwt = tokenUtil.getGithubAccessToken(userId)

            if (githubJwt == null) {
                LogManager.emitError(
                    "GithubFeature.getProjectRepoBranches github token unavailable -> 425: userId=$userId, projectId=$projectId"
                )
                call.respond(HttpStatusCode.TooEarly)
                return
            }
            LogManager.emitError(
                "GithubFeature.getProjectRepoBranches github token resolved: userId=$userId, projectId=$projectId, tokenLength=${githubJwt.length}"
            )

            val repos = branchesRepo.getProjectRepoBranches(projectId, githubJwt)

            if (repos == null) {
                LogManager.emitError(
                    "GithubFeature.getProjectRepoBranches repo list not found -> 404: userId=$userId, projectId=$projectId"
                )
                call.respond(HttpStatusCode.NotFound)
                return
            }
            LogManager.emitError(
                "GithubFeature.getProjectRepoBranches success: userId=$userId, projectId=$projectId, repos=${repos.size}"
            )

            call.respondText(
                Json.encodeToString(
                    repos
                ), ContentType.Application.Json, HttpStatusCode.OK
            )
        }
    }

    @ApiOperation(method = "GET", path = "/github/getUserMeta", summary = "Get GitHub user metadata", tags = ["github"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "userId", type = String::class, required = false)
    @ApiQueryParam(name = "githubUserId", type = String::class, required = false)
    @ApiResponse(code = 200, description = "GitHub user metadata returned")
    @ApiResponse(code = 404, description = "GitHub user metadata not found")
    override suspend fun getUserMeta(rc: RoutingContext) {
        with(rc) {
            val paramUserId = call.parameters["userId"]
            val githubUserId = call.parameters["githubUserId"]
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val meta = githubTokensRepo.getUserMeta(
                if (paramUserId != null) userId else if (githubUserId != null) githubUserId
                else userId
            )

            if (meta == null) {
                call.respond(HttpStatusCode.NotFound, "User meta was not found")
            }

            call.respondText(Json.encodeToString(meta), ContentType.Application.Json, HttpStatusCode.OK)

        }
    }

    @ApiOperation(method = "GET", path = "/github/getRepoBranchContent", summary = "Get GitHub branch content", tags = ["github"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "sha", type = String::class, required = true)
    @ApiQueryParam(name = "repoName", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Branch content returned")
    @ApiResponse(code = 400, description = "Required parameters are missing")
    @ApiResponse(code = 401, description = "JWT is invalid")
    @ApiResponse(code = 404, description = "Branch not found")
    @ApiResponse(code = 425, description = "GitHub token is not available")
    override suspend fun getRepoBranchContent(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val sha = call.parameters["sha"]
            val repo = call.parameters["repoName"]
            if (sha == null || repo == null) {
                call.respond(HttpStatusCode.BadRequest, "Bad parameters")
                return
            }
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to parse jwt")
                return
            }

            val githubJwt = tokenUtil.getGithubAccessToken(userId)

            if (githubJwt == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }

            val branchCommits = branchesRepo.getRepoBranchContent(
                sha = sha,
                repoName = repo,
                githubJwt = githubJwt,
                profileMaker = { profileRepo.getSharedByGithubId(it)?.let {
                    SharedProfile(
                        firstName = it.firstName,
                        secondName = it.secondName,
                        lastName = it.lastName,
                    )
                } }

            )

            if (branchCommits == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            call.respondText(
                Json.encodeToString(
                    branchCommits
                ), ContentType.Application.Json, HttpStatusCode.OK
            )
        }
    }

    private fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        oauthSessions.entries.removeIf { (_, session) -> session.expiresAtMillis < now }
    }

    private fun parseOAuthState(rawState: String?): Pair<String, GithubOAuthChannel>? {
        if (rawState.isNullOrBlank()) return null
        val separatorIndex = rawState.indexOf(':')
        if (separatorIndex <= 0 || separatorIndex >= rawState.lastIndex) return null
        val sessionId = rawState.substring(0, separatorIndex)
        val channelRaw = rawState.substring(separatorIndex + 1)
        val channel = GithubOAuthChannel.fromRaw(channelRaw) ?: return null
        return sessionId to channel
    }

    private fun extractUserIdFromJwt(jwtToken: String): String? {
        return runCatching { jwt.verify(jwtToken).getClaim("userId").asString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun resolveOauthSessionTtlMillis(): Long {
        val ttlSeconds = config.github.oauthSessionTtlSeconds.coerceAtLeast(60L)
        return ttlSeconds * 1000L
    }

    private fun resolveBackendCallbackUrl(): String {
        return "https://${config.ssl.domain}:8080/oauth/github/callback"
    }

    private fun buildGithubAuthorizeUrl(state: String, backendCallbackUrl: String): String {
        return URLBuilder("https://github.com/login/oauth/authorize").apply {
            parameters.append("client_id", config.github.clientId)
            parameters.append("redirect_uri", backendCallbackUrl)
            parameters.append("state", state)
            parameters.append("allow_signup", "true")
        }.buildString()
    }

    private fun buildClientRedirect(channel: GithubOAuthChannel, isSuccess: Boolean, reason: String?): String {
        val baseUrl = when (channel) {
            GithubOAuthChannel.Web -> config.github.webProfileRedirectUrl
                ?.takeIf { it.isNotBlank() }
                ?: "https://${config.ssl.domain}/profile/"
            GithubOAuthChannel.Android -> config.github.androidProfileRedirectUrl
                ?.takeIf { it.isNotBlank() }
                ?: "madprojects://profile"
            GithubOAuthChannel.Ios -> config.github.iosProfileRedirectUrl
                ?.takeIf { it.isNotBlank() }
                ?: "madprojects-ios://profile"
        }
        return URLBuilder(baseUrl).apply {
            parameters.append("provider", "github")
            parameters.append("status", if (isSuccess) "success" else "error")
            if (!reason.isNullOrBlank()) {
                parameters.append("reason", reason.take(128))
            }
        }.buildString()
    }

    private suspend fun exchangeCodeAndSaveTokens(githubCode: String, userId: String): OAuthExchangeResult {
        val response = httpClient.submitForm(
            url = githubAuthLink,
            formParameters = parameters {
                append("client_id", config.github.clientId)
                append("client_secret", config.github.clientSecret)
                append("code", githubCode)
            }
        ) {
            headers.append(HttpHeaders.Accept, "application/json")
        }
        val payload = parseGithubOauthTokens(response.bodyAsText())
        val accessToken = payload?.access_token
        if (response.status != HttpStatusCode.OK || accessToken.isNullOrBlank()) {
            LogManager.emitError(
                "GitHub code exchange failed for userId=$userId, status=${response.status.value}, " +
                    "error=${payload?.error}, description=${payload?.error_description}"
            )
            return OAuthExchangeResult.Fail(
                status = HttpStatusCode.Unauthorized,
                message = "Failed to get tokens from code"
            )
        }
        val refreshToken = payload.refresh_token.orEmpty()

        val metaResponse = httpClient.get("https://api.github.com/user") {
            header("Authorization", "Bearer $accessToken")
        }
        if (metaResponse.status != HttpStatusCode.OK) {
            return OAuthExchangeResult.Fail(
                status = HttpStatusCode.FailedDependency,
                message = "Failed to load user meta via secondary request"
            )
        }

        val githubUser = runCatching { metaResponse.body<GithubUser>() }
            .getOrElse {
                return OAuthExchangeResult.Fail(
                    status = HttpStatusCode.FailedDependency,
                    message = "Failed to parse user meta from secondary request"
                )
            }
        githubTokensRepo.save(
            access = accessToken,
            refresh = refreshToken,
            userId = userId,
            githubId = githubUser.id,
            avatar = githubUser.avatarUrl,
            profileLink = githubUser.profileLink
        )
        return OAuthExchangeResult.Success
    }
}

private data class GithubOAuthSession(
    val sessionId: String,
    val channel: String,
    val userId: String,
    val expiresAtMillis: Long,
)

private sealed interface OAuthExchangeResult {
    data object Success : OAuthExchangeResult
    data class Fail(val status: HttpStatusCode, val message: String) : OAuthExchangeResult
}

private enum class GithubOAuthChannel(val value: String) {
    Web("web"),
    Android("android"),
    Ios("ios");

    companion object {
        fun fromRaw(raw: String?): GithubOAuthChannel? {
            return entries.firstOrNull { it.value.equals(raw, ignoreCase = true) }
        }
    }
}
