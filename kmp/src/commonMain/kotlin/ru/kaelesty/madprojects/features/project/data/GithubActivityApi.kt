package ru.kaelesty.madprojects.features.project.data

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
import shared_domain.entities.BranchCommits
import shared_domain.entities.RepoView

class GithubActivityApi(
    private val client: HttpClient,
) {
    suspend fun getProjectRepoBranches(accessToken: String, projectId: String): RepoBranchesResponse? {
        return runCatching {
            val response = client.get(RepoBranchesPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ProjectIdParam, projectId)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Repo branches response status=${response.status}" }
            val repos = if (response.status == HttpStatusCode.OK) {
                response.body<List<RepoView>>()
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            RepoBranchesResponse(
                status = response.status,
                repos = repos,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Repo branches request failed" }
            null
        }
    }

    suspend fun getRepoBranchContent(accessToken: String, sha: String, repoName: String): BranchCommitsResponse? {
        return runCatching {
            val response = client.get(RepoBranchContentPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(ShaParam, sha)
                parameter(RepoNameParam, repoName)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Repo branch content response status=${response.status}" }
            val commits = if (response.status == HttpStatusCode.OK) {
                response.body<BranchCommits>()
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            BranchCommitsResponse(
                status = response.status,
                commits = commits,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Repo branch content request failed" }
            null
        }
    }

    data class RepoBranchesResponse(
        val status: HttpStatusCode,
        val repos: List<RepoView>? = null,
        val errorMessage: String? = null,
    )

    data class BranchCommitsResponse(
        val status: HttpStatusCode,
        val commits: BranchCommits? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-GithubActivityApi"
        private const val RepoBranchesPath = "/github/getProjectRepoBranches"
        private const val RepoBranchContentPath = "/github/getRepoBranchContent"
        private const val ProjectIdParam = "projectId"
        private const val ShaParam = "sha"
        private const val RepoNameParam = "repoName"
    }
}
