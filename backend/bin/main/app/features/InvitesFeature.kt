package app.features

import app.LogManager
import app.openapi.annotations.*
import domain.InvitesRepo
import domain.activity.ActivityRepo
import domain.activity.ActivityType
import domain.profile.ProfileRepo
import domain.project.ProjectRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kaelesty.madprojects.api.invites.ProjectInviteResponse
import ru.kaelesty.madprojects.api.invites.UseInviteResponse

interface InvitesFeature {

    suspend fun getProjectInvite(rc: RoutingContext)

    suspend fun refreshProjectInvite(rc: RoutingContext)

    suspend fun useInvite(rc: RoutingContext)
}

class InvitesFeatureImpl(
    private val invitesRepo: InvitesRepo,
    private val projectRepo: ProjectRepo,
    private val profileRepo: ProfileRepo,
    private val activityRepo: ActivityRepo,
): InvitesFeature {

    @ApiOperation(method = "GET", path = "/invites/get", summary = "Get project invite", tags = ["invites"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project invite loaded", type = ProjectInviteResponse::class)
    @ApiResponse(code = 404, description = "Project not found or user is not a member")
    override suspend fun getProjectInvite(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val invite = invitesRepo.getProjectInvite(projectId)
            call.respondText(
                text = Json.encodeToString(
                    ProjectInviteResponse(invite)
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    @ApiOperation(method = "POST", path = "/invites/refresh", summary = "Refresh project invite", tags = ["invites"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project invite refreshed", type = ProjectInviteResponse::class)
    @ApiResponse(code = 404, description = "Project not found or user is not a member")
    override suspend fun refreshProjectInvite(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val invite = invitesRepo.refreshProjectInvite(projectId)
            call.respondText(
                text = Json.encodeToString(
                    ProjectInviteResponse(invite)
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    @ApiOperation(method = "POST", path = "/invites/use", summary = "Use project invite", tags = ["invites"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "invite", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Invite applied", type = UseInviteResponse::class)
    @ApiResponse(code = 404, description = "Invite not found")
    override suspend fun useInvite(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val invite = call.parameters["invite"]
            LogManager.emitError(
                "InvitesFeature.useInvite request: userId=$userId inviteLength=${invite?.length ?: 0}"
            )

            if (invite == null) {
                LogManager.emitError("InvitesFeature.useInvite missing invite: userId=$userId")
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val projectId = invitesRepo.useInvite(invite, userId)

            if (projectId == null) {
                LogManager.emitError(
                    "InvitesFeature.useInvite failed: userId=$userId inviteLength=${invite.length} projectId=null"
                )
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val memberProfile = profileRepo.getSharedById(userId)

            activityRepo.recordActivity(
                projectId = projectId,
                actorId = null,
                targetTitle = if (memberProfile != null) "${memberProfile.lastName} ${memberProfile.firstName}" else "",
                targetId = userId,
                type = ActivityType.MemberAdd
            )

            LogManager.emitError(
                "InvitesFeature.useInvite success: userId=$userId projectId=$projectId"
            )

            call.respondText(
                text = Json.encodeToString(
                    UseInviteResponse(projectId)
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}
