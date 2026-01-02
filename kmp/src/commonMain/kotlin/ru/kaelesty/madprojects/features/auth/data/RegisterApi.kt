package ru.kaelesty.madprojects.features.auth.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.auth.domain.UserType
import ru.kaelesty.madprojects.utils.KLogger

class RegisterApi(
    private val client: HttpClient,
) {
    suspend fun register(request: RegisterRequest): HttpStatusCode? {
        return runCatching {
            val response = client.post(RegisterPath) {
                contentType(ContentType.Application.Json)
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Register response status=${response.status}" }
            response.status
        }.getOrElse {
            KLogger.e(TAG, it) { "Register request failed" }
            null
        }
    }

    @Serializable
    data class RegisterRequest(
        val username: String,
        val lastName: String,
        val firstName: String,
        val secondName: String,
        val data: String,
        val email: String,
        val password: String,
        val userType: UserType,
    )

    private companion object {
        private const val TAG = "ktor-RegisterApi"
        const val RegisterPath = "/auth/register"
    }
}
