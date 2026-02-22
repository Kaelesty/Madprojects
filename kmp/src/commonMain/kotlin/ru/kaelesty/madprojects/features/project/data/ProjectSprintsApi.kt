package ru.kaelesty.madprojects.features.project.data

import io.ktor.client.call.body
import domain.sprints.ProfileSprint
import domain.sprints.SprintView
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.activity.ActivityResponse
import ru.kaelesty.madprojects.api.sprints.CreateSprintRequest
import ru.kaelesty.madprojects.api.sprints.UpdateSprintRequest
import ru.kaelesty.madprojects.utils.KLogger
import shared_domain.entities.KanbanState

class ProjectSprintsApi(
    private val client: HttpClient,
) {
    suspend fun getProjectSprints(accessToken: String, projectId: String): ProjectSprintsResponse? {
        return runCatching {
            val response = client.get(SprintsListPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Sprints list response status=${response.status}" }
            val sprints = if (response.status == HttpStatusCode.OK) {
                response.body<List<ProfileSprint>>()
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            ProjectSprintsResponse(
                status = response.status,
                sprints = sprints,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Sprints list request failed" }
            null
        }
    }

    suspend fun getSprint(accessToken: String, sprintId: String): SprintResponse? {
        return runCatching {
            val response = client.get(SprintDetailsPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(SprintIdParam, sprintId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Sprint details response status=${response.status}" }
            val sprint = if (response.status == HttpStatusCode.OK) {
                response.body<SprintView>()
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            SprintResponse(
                status = response.status,
                sprint = sprint,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Sprint details request failed" }
            null
        }
    }

    suspend fun createSprint(accessToken: String, request: CreateSprintRequest): CreateSprintResponse? {
        return runCatching {
            val response = client.post(SprintCreatePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Create sprint response status=${response.status}" }
            val sprintId = if (response.status == HttpStatusCode.OK) {
                response.body<Map<String, String>>()["sprintId"]
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            CreateSprintResponse(
                status = response.status,
                sprintId = sprintId,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Create sprint request failed" }
            null
        }
    }

    suspend fun updateSprint(accessToken: String, request: UpdateSprintRequest): SimpleResponse? {
        return runCatching {
            val response = client.post(SprintUpdatePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Update sprint response status=${response.status}" }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            SimpleResponse(
                status = response.status,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Update sprint request failed" }
            null
        }
    }

    suspend fun finishSprint(accessToken: String, sprintId: String): SimpleResponse? {
        return runCatching {
            val response = client.post(SprintFinishPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(SprintIdParam, sprintId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Finish sprint response status=${response.status}" }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            SimpleResponse(
                status = response.status,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Finish sprint request failed" }
            null
        }
    }

    suspend fun getProjectKards(accessToken: String, projectId: String): ProjectKardsResponse? {
        return runCatching {
            val response = client.get(ProjectKardsPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Project kards response status=${response.status}" }
            val kards = if (response.status == HttpStatusCode.OK) {
                response.body<List<KanbanState.Kard>>()
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            ProjectKardsResponse(
                status = response.status,
                kards = kards,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Project kards request failed" }
            null
        }
    }

    suspend fun getProjectActivities(
        accessToken: String,
        projectId: String,
        count: Int? = null,
    ): ProjectActivitiesResponse? {
        return runCatching {
            val response = client.get(ProjectActivitiesPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                if (count != null) {
                    parameter(CountParam, count)
                }
                expectSuccess = false
            }
            KLogger.d(TAG) { "Project activities response status=${response.status}" }
            val body = if (response.status == HttpStatusCode.OK) {
                response.body<ActivityResponse>()
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            ProjectActivitiesResponse(
                status = response.status,
                body = body,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Project activities request failed" }
            null
        }
    }

    data class ProjectSprintsResponse(
        val status: HttpStatusCode,
        val sprints: List<ProfileSprint>? = null,
        val errorMessage: String? = null,
    )

    data class SprintResponse(
        val status: HttpStatusCode,
        val sprint: SprintView? = null,
        val errorMessage: String? = null,
    )

    data class CreateSprintResponse(
        val status: HttpStatusCode,
        val sprintId: String? = null,
        val errorMessage: String? = null,
    )

    data class SimpleResponse(
        val status: HttpStatusCode,
        val errorMessage: String? = null,
    )

    data class ProjectKardsResponse(
        val status: HttpStatusCode,
        val kards: List<KanbanState.Kard>? = null,
        val errorMessage: String? = null,
    )

    data class ProjectActivitiesResponse(
        val status: HttpStatusCode,
        val body: ActivityResponse? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-ProjectSprintsApi"
        private const val SprintsListPath = "/sprint/getListByProject"
        private const val SprintCreatePath = "/sprint/create"
        private const val SprintUpdatePath = "/sprint/update"
        private const val SprintFinishPath = "/sprint/finish"
        private const val SprintDetailsPath = "/sprint/get"
        private const val ProjectKardsPath = "/project/kards"
        private const val ProjectActivitiesPath = "/project/activity/get"
        private const val ProjectIdParam = "projectId"
        private const val SprintIdParam = "sprintId"
        private const val CountParam = "count"
    }
}
