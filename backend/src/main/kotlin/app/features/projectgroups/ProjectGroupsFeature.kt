package app.features.projectgroups

import app.openapi.annotations.*
import domain.profile.ProfileRepo
import domain.project.ProjectStatus
import domain.projectgroups.ProjectsGroupRepo
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kaelesty.madprojects.api.projectgroups.CreateProjectGroupRequest
import ru.kaelesty.madprojects.api.projectgroups.GroupProjectsResponse

interface ProjectGroupsFeature {

    suspend fun createProjectsGroup(rc: RoutingContext)

    suspend fun getCuratorProjectGroups(rc: RoutingContext)

    suspend fun getGroupProjects(rc: RoutingContext)

    suspend fun getCuratorProjects(rc: RoutingContext)

    suspend fun deleteProjectGroup(rc: RoutingContext)
}

class ProjectGroupsFeatureImpl(
    private val projectsGroupRepo: ProjectsGroupRepo,
    private val profileRepo: ProfileRepo,
): ProjectGroupsFeature {

    @ApiOperation(method = "POST", path = "/projectGroup/delete", summary = "Delete project group", tags = ["project-groups"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectGroupId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project group deleted")
    @ApiResponse(code = 400, description = "Project group id is missing or user is not the owner")
    override suspend fun deleteProjectGroup(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val projectGroupId = call.parameters["projectGroupId"]?.let {
                if (it == "null") null else it
            }
            if (projectGroupId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "No group id",
                )
                return
            }

            val isGroupOwner = projectsGroupRepo.checkIsCuratorGroupOwner(userId, projectGroupId)

            if (!isGroupOwner) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "User is not group owner"
                )
                return
            }

            projectsGroupRepo.deleteProjectGroup(projectGroupId)

            call.respond(
                HttpStatusCode.OK
            )
        }

    }

    @ApiOperation(method = "GET", path = "/curatorship/getProjects", summary = "Get curator projects", tags = ["project-groups"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectGroupId", type = String::class, required = false)
    @ApiQueryParam(name = "status", type = String::class, required = false)
    @ApiQueryParam(name = "marked", type = Boolean::class, required = false)
    @ApiQueryParam(name = "mark", type = Int::class, required = false)
    @ApiResponse(code = 200, description = "Curator projects returned")
    @ApiResponse(code = 423, description = "User is not a curator")
    override suspend fun getCuratorProjects(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }

            val projectGroupId = call.parameters["projectGroupId"]?.let {
                if (it == "null") null else it
            }
            val status = call.parameters["status"]?.let {
                if (it == "null") null else ProjectStatus.valueOf(it)
            }
            val marked = call.parameters["marked"]?.let {
                when (it) {
                    "null" -> null
                    "true" -> true
                    "false" -> false
                    else -> null
                }
            }
            val mark = call.parameters["mark"]?.let {
                if (it == "null") null else it.toInt()
            }

            val projects = projectsGroupRepo.getCuratorProjects(
                curatorId = userId,
                projectGroupId = projectGroupId,
                status = status,
                marked = marked,
                mark = mark,
            )

            call.respondText(
                text = Json.encodeToString(projects),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    @ApiOperation(method = "POST", path = "/projectgroup/create", summary = "Create project group", tags = ["project-groups"])
    @ApiSecurity(name = "auth-jwt")
    @ApiRequestBody(type = CreateProjectGroupRequest::class, description = "Project group creation payload")
    @ApiResponse(code = 200, description = "Project group created")
    @ApiResponse(code = 423, description = "User is not a curator")
    override suspend fun createProjectsGroup(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<CreateProjectGroupRequest>()
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }
            val new = projectsGroupRepo.createProjectsGroup(
                title = request.title,
                curatorId = userId
            )
            call.respondText(
                text = Json.encodeToString(new),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    @ApiOperation(method = "GET", path = "/projectgroup/getCuratorGroups", summary = "Get curator project groups", tags = ["project-groups"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "curatorId", type = String::class, required = false)
    @ApiResponse(code = 200, description = "Project groups returned")
    @ApiResponse(code = 404, description = "Curator not found")
    override suspend fun getCuratorProjectGroups(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val param = call.parameters["curatorId"]

            val curatorId = if (param == null || param == "null") userId else param

            if (curatorId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val groups = projectsGroupRepo.getCuratorProjectGroups(curatorId)
            call.respondText(
                text = Json.encodeToString(groups),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    @ApiOperation(method = "GET", path = "/projectgroup/getGroupProjects", summary = "Get project group projects", tags = ["project-groups"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "groupId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project group projects returned", type = GroupProjectsResponse::class)
    @ApiResponse(code = 404, description = "Project group not found or user has no access")
    @ApiResponse(code = 423, description = "User is not a curator")
    override suspend fun getGroupProjects(rc: RoutingContext) {
        with (rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val groupId = call.parameters["groupId"]
            if (groupId == null || !projectsGroupRepo.checkIsCuratorGroupOwner(userId, groupId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }
            val groupTitle = projectsGroupRepo.getGroupTitle(groupId)
            val projects = projectsGroupRepo.getGroupProjects(groupId)
            val response = GroupProjectsResponse(
                title = groupTitle,
                projects = projects
            )
            call.respondText(
                text = Json.encodeToString(response),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }
}
