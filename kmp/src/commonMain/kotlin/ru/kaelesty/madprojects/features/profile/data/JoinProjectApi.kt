package ru.kaelesty.madprojects.features.profile.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.invites.UseInviteResponse
import ru.kaelesty.madprojects.utils.KLogger

class JoinProjectApi(
    private val client: HttpClient,
) {
    suspend fun join(accessToken: String, invite: String): JoinProjectApiResponse? {
        return runCatching {
            val response = client.post(JoinProjectPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(InviteParam, invite)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Join project response status=${response.status}" }
            val projectId = if (response.status == HttpStatusCode.OK) {
                response.body<UseInviteResponse>().projectId
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            JoinProjectApiResponse(
                status = response.status,
                projectId = projectId,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Join project request failed" }
            null
        }
    }

    data class JoinProjectApiResponse(
        val status: HttpStatusCode,
        val projectId: String? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-JoinProjectApi"
        private const val JoinProjectPath = "/invites/use"
        private const val InviteParam = "invite"
    }
}
