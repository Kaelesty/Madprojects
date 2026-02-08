package ru.kaelesty.madprojects.features.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.utils.KLogger

class ProjectKanbanApi(
    private val client: HttpClient,
) {
    suspend fun createKardChat(accessToken: String, projectId: String, kardId: Int): CreateKardChatResponse? {
        return runCatching {
            val response = client.post(CreateKardChatPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                parameter(KardIdParam, kardId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Create kard chat response status=${response.status}" }
            val chatId = if (response.status == HttpStatusCode.OK) {
                response.body<Map<String, String>>()["chatId"]
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            CreateKardChatResponse(
                status = response.status,
                chatId = chatId,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Create kard chat request failed" }
            null
        }
    }

    data class CreateKardChatResponse(
        val status: HttpStatusCode,
        val chatId: String? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-ProjectKanbanApi"
        private const val CreateKardChatPath = "/project/createKardChat"
        private const val ProjectIdParam = "projectId"
        private const val KardIdParam = "kardId"
    }
}
