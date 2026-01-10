package ru.kaelesty.madprojects.features.projectcreate.data

import domain.projectgroups.ProjectGroup
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.utils.KLogger

class ProjectGroupsApi(
    private val client: HttpClient,
) {
    suspend fun getCuratorGroups(accessToken: String, curatorId: String): ProjectGroupsApiResponse? {
        return runCatching {
            val response = client.get(ProjectGroupsPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(CuratorIdParam, curatorId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Project groups response status=${response.status}" }
            val groups = if (response.status == HttpStatusCode.OK) {
                response.body<List<ProjectGroup>>()
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            ProjectGroupsApiResponse(
                status = response.status,
                groups = groups,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Project groups request failed" }
            null
        }
    }

    data class ProjectGroupsApiResponse(
        val status: HttpStatusCode,
        val groups: List<ProjectGroup>? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-ProjectGroupsApi"
        private const val ProjectGroupsPath = "/projectgroup/getCuratorGroups"
        private const val CuratorIdParam = "curatorId"
    }
}
