package ru.kaelesty.madprojects.features.auth.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import ru.kaelesty.madprojects.api.auth.AuthorizedResponse
import ru.kaelesty.madprojects.api.auth.Tokens
import domain.auth.RegisterRequest
import ru.kaelesty.madprojects.features.auth.data.AuthApiResponse
import ru.kaelesty.madprojects.utils.KLogger

class RegisterApi(
    private val client: HttpClient,
) {
    suspend fun register(request: RegisterRequest): AuthApiResponse? {
        return runCatching {
            val response = client.post(RegisterPath) {
                contentType(ContentType.Application.Json)
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Register response status=${response.status}" }
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
            KLogger.e(TAG, it) { "Register request failed" }
            null
        }
    }

    private companion object {
        private const val TAG = "ktor-RegisterApi"
        const val RegisterPath = "/auth/register"
    }
}
