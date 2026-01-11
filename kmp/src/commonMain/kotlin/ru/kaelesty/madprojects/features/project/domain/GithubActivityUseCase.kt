package ru.kaelesty.madprojects.features.project.domain

import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.data.GithubActivityApi
import ru.kaelesty.madprojects.utils.KLogger
import shared_domain.entities.BranchCommits
import shared_domain.entities.RepoView

class GithubActivityUseCase(
    private val api: GithubActivityApi,
    private val authContext: AuthContext,
) {
    suspend fun loadRepoBranches(projectId: String): RepoBranchesResult {
        KLogger.d(TAG) { "loadRepoBranches start: projectId=$projectId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "loadRepoBranches failed: access token missing" }
            return RepoBranchesResult.Fail(null, null)
        }
        val response = api.getProjectRepoBranches(accessToken, projectId) ?: run {
            KLogger.w(TAG) { "loadRepoBranches failed: response is null" }
            return RepoBranchesResult.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            val repos = response.repos ?: run {
                KLogger.w(TAG) { "loadRepoBranches failed: body missing" }
                return RepoBranchesResult.Fail(response.status, response.errorMessage)
            }
            KLogger.i(TAG) { "loadRepoBranches success: repos=${repos.size}" }
            return RepoBranchesResult.Success(repos)
        }
        KLogger.w(TAG) { "loadRepoBranches failed: status=${response.status} message=${response.errorMessage}" }
        return RepoBranchesResult.Fail(response.status, response.errorMessage)
    }

    suspend fun loadRepoBranchContent(sha: String, repoName: String): BranchCommitsResult {
        KLogger.d(TAG) { "loadRepoBranchContent start: sha=$sha repo=$repoName" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "loadRepoBranchContent failed: access token missing" }
            return BranchCommitsResult.Fail(null, null)
        }
        val response = api.getRepoBranchContent(accessToken, sha, repoName) ?: run {
            KLogger.w(TAG) { "loadRepoBranchContent failed: response is null" }
            return BranchCommitsResult.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            val commits = response.commits ?: run {
                KLogger.w(TAG) { "loadRepoBranchContent failed: body missing" }
                return BranchCommitsResult.Fail(response.status, response.errorMessage)
            }
            KLogger.i(TAG) { "loadRepoBranchContent success: commits=${commits.commits.size}" }
            return BranchCommitsResult.Success(commits)
        }
        KLogger.w(TAG) { "loadRepoBranchContent failed: status=${response.status} message=${response.errorMessage}" }
        return BranchCommitsResult.Fail(response.status, response.errorMessage)
    }

    sealed interface RepoBranchesResult {
        data class Success(val repos: List<RepoView>) : RepoBranchesResult
        data class Fail(val status: HttpStatusCode?, val message: String?) : RepoBranchesResult
    }

    sealed interface BranchCommitsResult {
        data class Success(val commits: BranchCommits) : BranchCommitsResult
        data class Fail(val status: HttpStatusCode?, val message: String?) : BranchCommitsResult
    }

    private companion object {
        private const val TAG = "GithubActivityUseCase"
    }
}
