package app.plugins.analytics

import app.GithubTokenUtil
import app.openapi.annotations.*
import app.plugins.Plugin
import domain.BranchesRepo
import domain.project.ProjectRepo
import domain.projectgroups.ProjectInGroupMember
import domain.projectgroups.ProjectsGroupRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kaelesty.madprojects.api.analytics.MemberWithMark
import ru.kaelesty.madprojects.api.analytics.ProjectTitleToId
import ru.kaelesty.madprojects.api.analytics.ProjectWithCommitsCount
import ru.kaelesty.madprojects.api.analytics.ProjectWithMark
import ru.kaelesty.madprojects.api.analytics.ProjectWithStatus

class AnalyticsPlugin(
    private val projectRepo: ProjectRepo,
    private val projectGroupsRepo: ProjectsGroupRepo,
    private val branchesRepo: BranchesRepo,
    private val tokenUtil: GithubTokenUtil,
    private val excelWizard: ExcelWizard,
): Plugin {
    override fun setup(route: Route): Unit = with(route) {
        getGroups()
        getProjectStatusesInProjectGroup()
        getProjectStatusesInProjectGroupByProject()
        getUserCommits()
        getProjectGroupCommits()
        getProjectCommitsCount()
        getGroupMarks()
        getProjectGroupMarks()
    }

    private fun Route.getGroups() = get("/analytics/getGroups") {
        getGroupsHandler(this)
    }

    private fun Route.getGroupMarks() = get("/analytics/groupMarks") {
        getGroupMarksHandler(this)
    }

    private fun Route.getProjectStatusesInProjectGroupByProject() = get("/analytics/projectStatusesByProjectId") {
        getProjectStatusesInProjectGroupByProjectHandler(this)
    }

    private fun Route.getProjectGroupCommits() = get("/analytics/projectGroupCommits") {
        getProjectGroupCommitsHandler(this)
    }

    private fun Route.getUserCommits() = get("/analytics/userCommits") {
        getUserCommitsHandler(this)
    }

    private fun Route.getProjectGroupMarks() = get("/analytics/projectGroupMarks") {
        getProjectGroupMarksHandler(this)
    }

    private fun Route.getProjectCommitsCount() = get("/analytics/projectCommitsCount") {
        getProjectCommitsCountHandler(this)
    }

    private fun Route.getProjectStatusesInProjectGroup() = get("/analytics/projectStatuses") {
        getProjectStatusesInProjectGroupHandler(this)
    }

    @ApiOperation(method = "GET", path = "/analytics/getGroups", summary = "Get member groups for project group", tags = ["analytics"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectGroupId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Member groups returned")
    @ApiResponse(code = 404, description = "Project group not found or user has no access")
    private suspend fun getGroupsHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val projectGroupId = rc.call.parameters["projectGroupId"]

        if (projectGroupId == null ||
            !projectGroupsRepo.checkIsCuratorGroupOwner(userId, projectGroupId)
        ) {
            rc.call.respond(HttpStatusCode.Companion.NotFound)
            return
        }

        val groups = projectGroupsRepo.getGroupProjects(projectGroupId)
            .flatMap { projectInGroup ->
                projectRepo.getProject(projectInGroup.id, userId).let { _ ->
                    projectInGroup.members.map {
                        it.group
                    }
                }
            }
            .distinct()
        rc.call.respondText(
            text = Json.Default.encodeToString(mapOf("groups" to groups)),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Companion.OK
        )
    }

    @ApiOperation(method = "GET", path = "/analytics/groupMarks", summary = "Download group marks workbook", tags = ["analytics"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectGroupId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Group marks workbook", contentType = "application/octet-stream")
    @ApiResponse(code = 404, description = "Project group not found or user has no access")
    private suspend fun getGroupMarksHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val projectGroupId = rc.call.parameters["projectGroupId"]

        if (projectGroupId == null ||
            !projectGroupsRepo.checkIsCuratorGroupOwner(userId, projectGroupId)
        ) {
            rc.call.respond(HttpStatusCode.NotFound)
            return
        }

        val groupName = projectGroupsRepo.getGroupTitle(projectGroupId)
        rc.call.response.header(
            "Content-Disposition",
            "attachment; filename=\"${groupName}.xlsx\""
        )

        val userIds = mutableListOf<ProjectInGroupMember>()
        val data = projectGroupsRepo.getGroupProjects(projectGroupId)
            .flatMap { projectInGroup ->
                projectRepo.getProject(projectInGroup.id, userId).let { project ->
                    projectInGroup.members
                        .map {
                            it to project.mark
                        }
                }
            }
            .sortedByDescending { it.second }
            .filter {
                if (!userIds.contains(it.first)) {
                    userIds.add(it.first)
                    true
                } else {
                    false
                }
            }
            .map {
                MemberWithMark(
                    firstName = it.first.firstName,
                    lastName = it.first.lastName,
                    secondName = it.first.secondName,
                    mark = it.second,
                    group = it.first.group,
                )
            }

        rc.call.respondBytes(
            bytes = excelWizard.excelify(data),
            contentType = ContentType.Application.OctetStream,
            status = HttpStatusCode.OK,
        )
    }

    @ApiOperation(method = "GET", path = "/analytics/projectStatusesByProjectId", summary = "Get project statuses by project id", tags = ["analytics"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project statuses returned")
    @ApiResponse(code = 404, description = "Project group not found or user has no access")
    private suspend fun getProjectStatusesInProjectGroupByProjectHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val projectId = rc.call.parameters["projectId"]

        if (projectId == null) {
            rc.call.respond(HttpStatusCode.Companion.NotFound)
            return
        }

        val groupId = projectGroupsRepo.getGroupId(projectId)

        if (!projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            rc.call.respond(HttpStatusCode.Companion.NotFound)
            return
        }

        val projectIds = projectGroupsRepo.getGroupProjects(groupId).map { it.id }
        val response = projectIds.map {
            val project = projectRepo.getProject(it, userId)
            val status = projectRepo.getProjectStatus(it)
            ProjectWithStatus(
                id = project.id,
                title = project.meta.title,
                status = status.name,
            )
        }
        rc.call.respondText(
            text = Json.Default.encodeToString(response),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    @ApiOperation(method = "GET", path = "/analytics/projectGroupCommits", summary = "Get project group commit counters", tags = ["analytics"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "groupId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project group commit counters returned")
    @ApiResponse(code = 404, description = "Project group not found or user has no access")
    private suspend fun getProjectGroupCommitsHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val groupId = rc.call.parameters["groupId"]

        if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            rc.call.respond(HttpStatusCode.Companion.NotFound)
            return
        }

        val response = projectGroupsRepo.getGroupProjectsAnalytics(groupId)
            .map {
                ProjectWithCommitsCount(
                    id = it.id,
                    title = it.title,
                    count = 0,
                )
            }

        rc.call.respondText(
            text = Json.Default.encodeToString(response),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    @ApiOperation(method = "GET", path = "/analytics/userCommits", summary = "Get user commits by project", tags = ["analytics"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "User commits returned")
    @ApiResponse(code = 404, description = "Project not found")
    @ApiResponse(code = 425, description = "GitHub token is not available")
    private suspend fun getUserCommitsHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()

        val projectId = rc.call.parameters["projectId"]
        if (projectId == null) {
            rc.call.respond(HttpStatusCode.Companion.NotFound)
            return
        }

        val githubToken = tokenUtil.getGithubAccessToken(userId)
        if (githubToken == null) {
            rc.call.respond(HttpStatusCode.Companion.TooEarly)
            return
        }
        val commiters = branchesRepo.getCommitsCount(projectId, githubToken)
        rc.call.respondText(
            text = Json.encodeToString(commiters),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    @ApiOperation(method = "GET", path = "/analytics/projectGroupMarks", summary = "Get project group marks", tags = ["analytics"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "groupId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project group marks returned")
    private suspend fun getProjectGroupMarksHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val groupId = rc.call.parameters["groupId"]

        if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            rc.call.respond(HttpStatusCode.Companion.OK)
            return
        }

        val response = projectGroupsRepo.getGroupProjectsAnalytics(groupId)
            .map {
                ProjectWithMark(
                    id = it.id,
                    title = it.title,
                    mark = it.mark,
                )
            }

        rc.call.respondText(
            text = Json.Default.encodeToString(response),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    @ApiOperation(method = "GET", path = "/analytics/projectCommitsCount", summary = "Get total project commits count", tags = ["analytics"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project commits count returned")
    @ApiResponse(code = 404, description = "Project or group not found, or user has no access")
    @ApiResponse(code = 425, description = "GitHub token is not available")
    private suspend fun getProjectCommitsCountHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val projectId = rc.call.parameters["projectId"]

        if (projectId == null) {
            rc.call.respond(HttpStatusCode.Companion.NotFound)
            return
        }

        val groupId = runCatching { projectGroupsRepo.getGroupId(projectId) }.getOrNull()
        if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            rc.call.respond(HttpStatusCode.Companion.NotFound)
            return
        }

        val githubToken = tokenUtil.getGithubAccessToken(userId)
        if (githubToken == null) {
            rc.call.respond(HttpStatusCode.Companion.TooEarly)
            return
        }

        val response = mapOf(
            "count" to branchesRepo.getCommitsCount(projectId, githubToken).sumOf { it.commitsCount }
        )

        rc.call.respondText(
            text = Json.Default.encodeToString(response),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    @ApiOperation(method = "GET", path = "/analytics/projectStatuses", summary = "Get project statuses in group", tags = ["analytics"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "groupId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project statuses returned")
    @ApiResponse(code = 404, description = "Project group not found or user has no access")
    private suspend fun getProjectStatusesInProjectGroupHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val groupId = rc.call.parameters["groupId"]

        if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            rc.call.respond(HttpStatusCode.Companion.NotFound)
            return
        }
        val projectStatuses = projectGroupsRepo.getGroupProjectsAnalytics(groupId).map {
            ProjectTitleToId(
                title = it.title,
                statusName = it.status.name
            )
        }

        rc.call.respondText(
            text = Json.Default.encodeToString(projectStatuses),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }
}
