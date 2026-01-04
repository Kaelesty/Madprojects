package ru.kaelesty.madprojects.features.auth.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.utils.KLogger

class LoginApi(
    private val client: HttpClient,
) {
    suspend fun login(request: LoginRequest): HttpStatusCode? {
        return runCatching {
            val response = client.post(LoginPath) {
                contentType(ContentType.Application.Json)
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Login response status=${response.status}" }
            response.status
        }.getOrElse {
            KLogger.e(TAG, it) { "Login request failed" }
            null
        }
    }

    @Serializable
    data class LoginRequest(
        val email: String,
        val password: String,
    )

    private companion object {
        private const val TAG = "ktor-LoginApi"
        const val LoginPath = "/auth/login"
    }
}
