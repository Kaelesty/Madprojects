package ru.kaelesty.madprojects.features.projectcreate.data

import domain.project.CreateProjectRequest
import domain.project.CreateProjectResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import ru.kaelesty.madprojects.utils.KLogger

class CreateProjectApi(
    private val client: HttpClient,
) {
    suspend fun createProject(accessToken: String, request: CreateProjectRequest): CreateProjectApiResponse? {
        return runCatching {
            val response = client.post(CreateProjectPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Create project response status=${response.status}" }
            val projectId = if (response.status == HttpStatusCode.OK) {
                response.body<CreateProjectResponse>().projectId
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            CreateProjectApiResponse(
                status = response.status,
                projectId = projectId,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Create project request failed" }
            null
        }
    }

    data class CreateProjectApiResponse(
        val status: HttpStatusCode,
        val projectId: String? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-CreateProjectApi"
        const val CreateProjectPath = "/project/create"
    }
}
