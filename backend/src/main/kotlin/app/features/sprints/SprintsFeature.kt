package app.features.sprints

import app.openapi.annotations.*
import domain.KanbanRepository
import domain.activity.ActivityRepo
import domain.activity.ActivityType
import domain.project.ProjectRepo
import domain.sprints.SprintMeta
import domain.sprints.SprintView
import domain.sprints.SprintsRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kaelesty.madprojects.api.sprints.CreateSprintRequest
import ru.kaelesty.madprojects.api.sprints.UpdateSprintRequest

interface SprintsFeature {

    suspend fun createSprint(rc: RoutingContext)

    suspend fun getProjectSprints(rc: RoutingContext)

    suspend fun finishSprint(rc: RoutingContext)

    suspend fun getSprint(rc: RoutingContext)

    suspend fun updateSprint(rc: RoutingContext)
}

class SprintsFeatureImpl(
    private val projectRepo: ProjectRepo,
    private val sprintsRepo: SprintsRepo,
    private val activityRepo: ActivityRepo,
    private val kanbanRepo: KanbanRepository
) : SprintsFeature {

    @ApiOperation(method = "POST", path = "/sprint/update", summary = "Update sprint", tags = ["sprints"])
    @ApiSecurity(name = "auth-jwt")
    @ApiRequestBody(type = UpdateSprintRequest::class, description = "Updated sprint data")
    @ApiResponse(code = 200, description = "Sprint updated")
    @ApiResponse(code = 404, description = "Sprint not found or user has no access")
    override suspend fun updateSprint(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val request = call.receive<UpdateSprintRequest>()

            val projectId = sprintsRepo.getSprintProjectId(request.sprintId)
            if (!projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            sprintsRepo.updateSprint(request)
            call.respond(HttpStatusCode.OK)
        }
    }

    @ApiOperation(method = "GET", path = "/sprint/get", summary = "Get sprint", tags = ["sprints"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "sprintId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Sprint returned", type = SprintView::class)
    @ApiResponse(code = 404, description = "Sprint not found or user has no access")
    override suspend fun getSprint(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val sprintId = call.parameters["sprintId"]
            if (sprintId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val projectId = sprintsRepo.getSprintProjectId(sprintId)
            if (!projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val sprint = sprintsRepo.getSprint(sprintId)
            val kanban = kanbanRepo.getKanban(projectId.toInt(), onlyKardIds = sprint.kardIds, userId.toInt())
            call.respondText(
                text = Json.encodeToString(
                    SprintView(
                        meta = sprint.meta,
                        kanban = kanban
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    @ApiOperation(method = "POST", path = "/sprint/finish", summary = "Finish sprint", tags = ["sprints"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "sprintId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Sprint finished")
    @ApiResponse(code = 404, description = "Sprint not found or user has no access")
    override suspend fun finishSprint(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val sprintId = call.parameters["sprintId"]
            if (sprintId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val projectId = sprintsRepo.getSprintProjectId(sprintId)
            if (!projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val sprint = sprintsRepo.getSprint(sprintId)

            sprintsRepo.finishSprint(sprintId)
            activityRepo.recordActivity(
                projectId = projectId,
                actorId = userId,
                targetTitle = sprint.meta.title,
                targetId = sprintId,
                type = ActivityType.SprintFinish
            )
            call.respond(HttpStatusCode.OK)
        }
    }

    @ApiOperation(method = "GET", path = "/sprint/getListByProject", summary = "List project sprints", tags = ["sprints"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project sprints returned")
    @ApiResponse(code = 404, description = "Project not found or user has no access")
    override suspend fun getProjectSprints(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val response = sprintsRepo.getProjectSprints(projectId)
            call.respondText(
                text = Json.encodeToString(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    @ApiOperation(method = "POST", path = "/sprint/create", summary = "Create sprint", tags = ["sprints"])
    @ApiSecurity(name = "auth-jwt")
    @ApiRequestBody(type = CreateSprintRequest::class, description = "Sprint creation payload")
    @ApiResponse(code = 200, description = "Sprint created")
    @ApiResponse(code = 404, description = "Project not found or user has no access")
    override suspend fun createSprint(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<CreateSprintRequest>()
            if (!projectRepo.checkUserInProject(userId, request.projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val sprintId = sprintsRepo.createSprint(request)

            activityRepo.recordActivity(
                projectId = request.projectId,
                actorId = userId,
                targetTitle = request.title,
                targetId = sprintId,
                type = ActivityType.SprintStart
            )

            call.respondText(
                text = Json.encodeToString(
                    mapOf("sprintId" to sprintId)
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}
