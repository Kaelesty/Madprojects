package app.features.curatorship

import app.openapi.annotations.*
import domain.CuratorshipRepo
import domain.curatorship.CheckCuratorshipUseCase
import domain.profile.ProfileRepo
import domain.project.ProjectRepo
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

interface CuratorshipFeature {

    suspend fun approveProject(rc: RoutingContext)

    suspend fun disapproveProject(rc: RoutingContext)

    suspend fun retrySubmission(rc: RoutingContext)

    suspend fun getPendingProjects(rc: RoutingContext)

    suspend fun getUnmarkedProjects(rc: RoutingContext)
}

class CuratorshipFeatureImpl(
    private val profileRepo: ProfileRepo,
    private val curatorshipRepo: CuratorshipRepo,
    private val projectRepo: ProjectRepo,
): CuratorshipFeature {

    @ApiOperation(method = "GET", path = "/curatorship/getUnmarkedProjects", summary = "Get unmarked projects", tags = ["curatorship"])
    @ApiSecurity(name = "auth-jwt")
    @ApiResponse(code = 200, description = "Unmarked projects returned")
    @ApiResponse(code = 423, description = "User is not a curator")
    override suspend fun getUnmarkedProjects(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }
            val projects = curatorshipRepo.getUnmarkedProjects(userId)
            call.respondText(
                text = Json.encodeToString(projects),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    @ApiOperation(method = "GET", path = "/curatorship/getPendingProjects", summary = "Get pending projects", tags = ["curatorship"])
    @ApiSecurity(name = "auth-jwt")
    @ApiResponse(code = 200, description = "Pending projects returned")
    @ApiResponse(code = 423, description = "User is not a curator")
    override suspend fun getPendingProjects(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }
            val projects = curatorshipRepo.getPendingProjects(userId)
            call.respondText(
                text = Json.encodeToString(projects),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    @ApiOperation(method = "POST", path = "/curatorship/retrySubmission", summary = "Retry project submission", tags = ["curatorship"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Submission retried")
    @ApiResponse(code = 404, description = "Project not found or user has no access")
    override suspend fun retrySubmission(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            curatorshipRepo.retrySubmission(projectId)
            call.respond(HttpStatusCode.OK)
        }
    }

    @ApiOperation(method = "POST", path = "/curatorship/approve", summary = "Approve project", tags = ["curatorship"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project approved")
    @ApiResponse(code = 404, description = "Project not found")
    @ApiResponse(code = 423, description = "User is not a curator")
    override suspend fun approveProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }
            curatorshipRepo.approveProject(userId, projectId)
            call.respond(HttpStatusCode.OK)
        }
    }

    @ApiOperation(method = "POST", path = "/curatorship/disapprove", summary = "Disapprove project", tags = ["curatorship"])
    @ApiSecurity(name = "auth-jwt")
    @ApiRequestBody(type = DisapproveProjectRequest::class, description = "Disapproval payload")
    @ApiResponse(code = 200, description = "Project disapproved")
    @ApiResponse(code = 423, description = "User is not a curator")
    override suspend fun disapproveProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<DisapproveProjectRequest>()
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }

            curatorshipRepo.disapproveProject(userId, request.projectId, request.message)
            call.respond(HttpStatusCode.OK)
        }
    }
}
