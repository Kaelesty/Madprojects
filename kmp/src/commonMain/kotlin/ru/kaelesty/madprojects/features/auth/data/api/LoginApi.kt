package ru.kaelesty.madprojects.features.auth.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import ru.kaelesty.madprojects.features.auth.data.AuthApiResponse
import ru.kaelesty.madprojects.api.auth.AuthorizedResponse
import ru.kaelesty.madprojects.api.auth.LoginRequest
import ru.kaelesty.madprojects.api.auth.Tokens
import ru.kaelesty.madprojects.utils.KLogger

class LoginApi(
    private val client: HttpClient,
) {
    suspend fun login(request: LoginRequest): AuthApiResponse? {
        return runCatching {
            val response = client.post(LoginPath) {
                contentType(ContentType.Application.Json)
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Login response status=${response.status}" }
            val authPayload = if (response.status == HttpStatusCode.OK) {
                val body = response.body<AuthorizedResponse>()
                val tokens = Tokens(
                    refreshToken = body.refreshToken,
                    accessToken = body.accessToken,
                    accessExpiresAt = body.accessExpiresAt,
                    refreshExpiresAt = body.refreshExpiresAt,
                )
                tokens to body.userType
            } else {
                null
            }
            AuthApiResponse(
                status = response.status,
                tokens = authPayload?.first,
                userType = authPayload?.second
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Login request failed" }
            null
        }
    }

    private companion object {
        private const val TAG = "ktor-LoginApi"
        const val LoginPath = "/auth/login"
    }
}
