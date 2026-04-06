package ru.kaelesty.madprojects.features.project.data

import domain.project.Project
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.api.invites.ProjectInviteResponse
import ru.kaelesty.madprojects.api.project.UpdateProjectMetaRequest
import ru.kaelesty.madprojects.utils.KLogger

class ProjectInfoApi(
    private val client: HttpClient,
) {
    suspend fun getProject(accessToken: String, projectId: String): ProjectResponse? {
        return runCatching {
            val response = client.get(ProjectGetPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Get project response status=${response.status}" }
            val project = if (response.status == HttpStatusCode.OK) {
                response.body<Project>()
            } else {
                null
            }
            ProjectResponse(
                status = response.status,
                project = project,
                errorMessage = response.errorBodyOrNull()
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Get project request failed" }
            null
        }
    }

    suspend fun getInvite(accessToken: String, projectId: String): InviteResponse? {
        return runCatching {
            val response = client.get(InvitesGetPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Get invite response status=${response.status}" }
            val invite = if (response.status == HttpStatusCode.OK) {
                response.body<ProjectInviteResponse>()
            } else {
                null
            }
            InviteResponse(
                status = response.status,
                invite = invite,
                errorMessage = response.errorBodyOrNull()
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Get invite request failed" }
            null
        }
    }

    suspend fun refreshInvite(accessToken: String, projectId: String): InviteResponse? {
        return runCatching {
            val response = client.post(InvitesRefreshPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Refresh invite response status=${response.status}" }
            val invite = if (response.status == HttpStatusCode.OK) {
                response.body<ProjectInviteResponse>()
            } else {
                null
            }
            InviteResponse(
                status = response.status,
                invite = invite,
                errorMessage = response.errorBodyOrNull()
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Refresh invite request failed" }
            null
        }
    }

    suspend fun updateProjectMeta(accessToken: String, request: UpdateProjectMetaRequest): SimpleResponse? {
        return runCatching {
            val response = client.post(ProjectUpdatePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Update project meta response status=${response.status}" }
            SimpleResponse(response.status, response.errorBodyOrNull())
        }.getOrElse {
            KLogger.e(TAG, it) { "Update project meta request failed" }
            null
        }
    }

    suspend fun removeMember(accessToken: String, projectId: String, memberId: String): SimpleResponse? {
        return runCatching {
            val response = client.post(ProjectMemberRemovePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                parameter(MemberIdParam, memberId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Remove member response status=${response.status}" }
            SimpleResponse(response.status, response.errorBodyOrNull())
        }.getOrElse {
            KLogger.e(TAG, it) { "Remove member request failed" }
            null
        }
    }

    suspend fun addRepository(accessToken: String, projectId: String, repoLink: String): SimpleResponse? {
        return runCatching {
            val response = client.post(ProjectRepoAddPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                parameter(RepoLinkParam, repoLink)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Add repository response status=${response.status}" }
            SimpleResponse(response.status, response.errorBodyOrNull())
        }.getOrElse {
            KLogger.e(TAG, it) { "Add repository request failed" }
            null
        }
    }

    suspend fun removeRepository(accessToken: String, projectId: String, repoId: String): SimpleResponse? {
        return runCatching {
            val response = client.post(ProjectRepoRemovePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                parameter(RepoIdParam, repoId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Remove repository response status=${response.status}" }
            SimpleResponse(response.status, response.errorBodyOrNull())
        }.getOrElse {
            KLogger.e(TAG, it) { "Remove repository request failed" }
            null
        }
    }

    suspend fun deleteProject(accessToken: String, projectId: String): SimpleResponse? {
        return runCatching {
            val response = client.post(ProjectDeletePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Delete project response status=${response.status}" }
            SimpleResponse(response.status, response.errorBodyOrNull())
        }.getOrElse {
            KLogger.e(TAG, it) { "Delete project request failed" }
            null
        }
    }

    suspend fun setProjectMark(accessToken: String, projectId: String, mark: Int): SimpleResponse? {
        return runCatching {
            val response = client.post(ProjectMarkSetPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                parameter(MarkParam, mark)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Set project mark response status=${response.status}" }
            SimpleResponse(response.status, response.errorBodyOrNull())
        }.getOrElse {
            KLogger.e(TAG, it) { "Set project mark request failed" }
            null
        }
    }

    suspend fun approveProject(accessToken: String, projectId: String): SimpleResponse? {
        return runCatching {
            val response = client.post(CuratorshipApprovePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Approve project response status=${response.status}" }
            SimpleResponse(response.status, response.errorBodyOrNull())
        }.getOrElse {
            KLogger.e(TAG, it) { "Approve project request failed" }
            null
        }
    }

    suspend fun disapproveProject(accessToken: String, projectId: String, message: String): SimpleResponse? {
        return runCatching {
            val response = client.post(CuratorshipDisapprovePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(DisapproveProjectRequest(projectId = projectId, message = message))
                expectSuccess = false
            }
            KLogger.d(TAG) { "Disapprove project response status=${response.status}" }
            SimpleResponse(response.status, response.errorBodyOrNull())
        }.getOrElse {
            KLogger.e(TAG, it) { "Disapprove project request failed" }
            null
        }
    }

    data class ProjectResponse(
        val status: HttpStatusCode,
        val project: Project? = null,
        val errorMessage: String? = null,
    )

    data class InviteResponse(
        val status: HttpStatusCode,
        val invite: ProjectInviteResponse? = null,
        val errorMessage: String? = null,
    )

    data class SimpleResponse(
        val status: HttpStatusCode,
        val errorMessage: String? = null,
    )

    private suspend fun io.ktor.client.statement.HttpResponse.errorBodyOrNull(): String? {
        return if (status == HttpStatusCode.OK) null else bodyAsText().trim().takeIf { it.isNotEmpty() }
    }

    private companion object {
        private const val TAG = "ktor-ProjectInfoApi"

        private const val ProjectGetPath = "/project/get"
        private const val ProjectUpdatePath = "/project/update"
        private const val ProjectMemberRemovePath = "/project/member/remove"
        private const val ProjectRepoAddPath = "/project/repo/add"
        private const val ProjectRepoRemovePath = "/project/repo/remove"
        private const val ProjectDeletePath = "/project/delete"
        private const val ProjectMarkSetPath = "/project/mark/set"
        private const val CuratorshipApprovePath = "/curatorship/approve"
        private const val CuratorshipDisapprovePath = "/curatorship/disapprove"
        private const val InvitesGetPath = "/invites/get"
        private const val InvitesRefreshPath = "/invites/refresh"

        private const val ProjectIdParam = "projectId"
        private const val MarkParam = "mark"
        private const val MemberIdParam = "memberId"
        private const val RepoIdParam = "repoId"
        private const val RepoLinkParam = "repoLink"
    }
}

@Serializable
private data class DisapproveProjectRequest(
    val projectId: String,
    val message: String,
)
