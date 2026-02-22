package ru.kaelesty.madprojects.features.project.domain

import domain.project.Project
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.project.UpdateProjectMetaRequest
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.data.ProjectInfoApi
import ru.kaelesty.madprojects.features.projectcreate.domain.VerifyRepoLinkUseCase
import ru.kaelesty.madprojects.utils.KLogger

class ProjectInfoUseCase(
    private val api: ProjectInfoApi,
    private val authContext: AuthContext,
    private val verifyRepoLinkUseCase: VerifyRepoLinkUseCase,
) {
    suspend fun loadProject(projectId: String): ProjectResult {
        KLogger.d(TAG) { "loadProject start: projectId=$projectId" }
        val accessToken = authContext.getAccessToken() ?: return authFail("loadProject")
        val response = api.getProject(accessToken, projectId) ?: return nullResponseFail("loadProject")
        return if (response.status == HttpStatusCode.OK) {
            val project = response.project ?: return ProjectResult.Fail(response.status, response.errorMessage)
            KLogger.i(TAG) { "loadProject success: projectId=${project.id} members=${project.members.size} repos=${project.repos.size} isCreator=${project.isCreator}" }
            ProjectResult.Success(project)
        } else {
            KLogger.w(TAG) { "loadProject failed: status=${response.status} message=${response.errorMessage}" }
            ProjectResult.Fail(response.status, response.errorMessage)
        }
    }

    suspend fun getInvite(projectId: String): InviteResult {
        KLogger.d(TAG) { "getInvite start: projectId=$projectId" }
        val accessToken = authContext.getAccessToken() ?: return inviteAuthFail("getInvite")
        val response = api.getInvite(accessToken, projectId) ?: return inviteNullFail("getInvite")
        return inviteResult("getInvite", response)
    }

    suspend fun refreshInvite(projectId: String): InviteResult {
        KLogger.d(TAG) { "refreshInvite start: projectId=$projectId" }
        val accessToken = authContext.getAccessToken() ?: return inviteAuthFail("refreshInvite")
        val response = api.refreshInvite(accessToken, projectId) ?: return inviteNullFail("refreshInvite")
        return inviteResult("refreshInvite", response)
    }

    suspend fun updateProjectMeta(projectId: String, title: String?, desc: String?): ActionResult {
        KLogger.d(TAG) { "updateProjectMeta start: projectId=$projectId titleChanged=${title != null} descChanged=${desc != null}" }
        val accessToken = authContext.getAccessToken() ?: return actionAuthFail("updateProjectMeta")
        val response = api.updateProjectMeta(accessToken, UpdateProjectMetaRequest(projectId, title, desc))
            ?: return actionNullFail("updateProjectMeta")
        return actionResult("updateProjectMeta", response)
    }

    suspend fun removeMember(projectId: String, memberId: String): ActionResult {
        KLogger.d(TAG) { "removeMember start: projectId=$projectId memberId=$memberId" }
        val accessToken = authContext.getAccessToken() ?: return actionAuthFail("removeMember")
        val response = api.removeMember(accessToken, projectId, memberId) ?: return actionNullFail("removeMember")
        return actionResult("removeMember", response)
    }

    suspend fun verifyRepoLink(link: String): ActionResult {
        return when (val result = verifyRepoLinkUseCase.verify(link)) {
            VerifyRepoLinkUseCase.Result.Success -> {
                KLogger.i(TAG) { "verifyRepoLink success" }
                ActionResult.Success
            }
            is VerifyRepoLinkUseCase.Result.Fail -> {
                KLogger.w(TAG) { "verifyRepoLink failed: status=${result.status} message=${result.message}" }
                ActionResult.Fail(result.status, result.message)
            }
        }
    }

    suspend fun addRepository(projectId: String, repoLink: String): ActionResult {
        KLogger.d(TAG) { "addRepository start: projectId=$projectId linkLength=${repoLink.length}" }
        val accessToken = authContext.getAccessToken() ?: return actionAuthFail("addRepository")
        val response = api.addRepository(accessToken, projectId, repoLink) ?: return actionNullFail("addRepository")
        return actionResult("addRepository", response)
    }

    suspend fun removeRepository(projectId: String, repoId: String): ActionResult {
        KLogger.d(TAG) { "removeRepository start: projectId=$projectId repoId=$repoId" }
        val accessToken = authContext.getAccessToken() ?: return actionAuthFail("removeRepository")
        val response = api.removeRepository(accessToken, projectId, repoId) ?: return actionNullFail("removeRepository")
        return actionResult("removeRepository", response)
    }

    suspend fun deleteProject(projectId: String): ActionResult {
        KLogger.d(TAG) { "deleteProject start: projectId=$projectId" }
        val accessToken = authContext.getAccessToken() ?: return actionAuthFail("deleteProject")
        val response = api.deleteProject(accessToken, projectId) ?: return actionNullFail("deleteProject")
        return actionResult("deleteProject", response)
    }

    sealed interface ProjectResult {
        data class Success(val project: Project) : ProjectResult
        data class Fail(val status: HttpStatusCode?, val message: String?) : ProjectResult
    }

    sealed interface InviteResult {
        data class Success(val invite: String) : InviteResult
        data class Fail(val status: HttpStatusCode?, val message: String?) : InviteResult
    }

    sealed interface ActionResult {
        data object Success : ActionResult
        data class Fail(val status: HttpStatusCode?, val message: String?) : ActionResult
    }

    private fun authFail(op: String): ProjectResult {
        KLogger.w(TAG) { "$op failed: access token missing" }
        return ProjectResult.Fail(null, null)
    }

    private fun nullResponseFail(op: String): ProjectResult {
        KLogger.w(TAG) { "$op failed: response is null" }
        return ProjectResult.Fail(null, null)
    }

    private fun inviteAuthFail(op: String): InviteResult {
        KLogger.w(TAG) { "$op failed: access token missing" }
        return InviteResult.Fail(null, null)
    }

    private fun inviteNullFail(op: String): InviteResult {
        KLogger.w(TAG) { "$op failed: response is null" }
        return InviteResult.Fail(null, null)
    }

    private fun actionAuthFail(op: String): ActionResult {
        KLogger.w(TAG) { "$op failed: access token missing" }
        return ActionResult.Fail(null, null)
    }

    private fun actionNullFail(op: String): ActionResult {
        KLogger.w(TAG) { "$op failed: response is null" }
        return ActionResult.Fail(null, null)
    }

    private fun inviteResult(op: String, response: ProjectInfoApi.InviteResponse): InviteResult {
        return if (response.status == HttpStatusCode.OK) {
            val invite = response.invite?.invite
            if (invite.isNullOrBlank()) {
                KLogger.w(TAG) { "$op failed: invite missing" }
                InviteResult.Fail(response.status, response.errorMessage)
            } else {
                KLogger.i(TAG) { "$op success: inviteLength=${invite.length}" }
                InviteResult.Success(invite)
            }
        } else {
            KLogger.w(TAG) { "$op failed: status=${response.status} message=${response.errorMessage}" }
            InviteResult.Fail(response.status, response.errorMessage)
        }
    }

    private fun actionResult(op: String, response: ProjectInfoApi.SimpleResponse): ActionResult {
        return if (response.status == HttpStatusCode.OK) {
            KLogger.i(TAG) { "$op success" }
            ActionResult.Success
        } else {
            KLogger.w(TAG) { "$op failed: status=${response.status} message=${response.errorMessage}" }
            ActionResult.Fail(response.status, response.errorMessage)
        }
    }

    private companion object {
        private const val TAG = "ProjectInfoUseCase"
    }
}
