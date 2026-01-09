package ru.kaelesty.madprojects.features.auth.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.auth.AuthorizedResponse
import ru.kaelesty.madprojects.api.auth.Tokens
import ru.kaelesty.madprojects.features.auth.data.AuthApiResponse
import ru.kaelesty.madprojects.utils.KLogger

class RefreshApi(
    private val client: HttpClient,
) {
    suspend fun refresh(refreshToken: String): AuthApiResponse? {
        return runCatching {
            val response = client.post(RefreshPath) {
                header(HttpHeaders.Authorization, "Bearer $refreshToken")
                expectSuccess = false
            }
            KLogger.d(TAG) { "Refresh response status=${response.status}" }
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
            KLogger.e(TAG, it) { "Refresh request failed" }
            null
        }
    }

    private companion object {
        private const val TAG = "ktor-RefreshApi"
        const val RefreshPath = "/auth/refresh"
    }
}
