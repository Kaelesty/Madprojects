package app.plugins

import app.openapi.annotations.*
import domain.BanhammerRepo
import domain.curatorship.CheckCuratorshipUseCase
import domain.profile.ProfileRepo
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BanHammer(
    private val profileRepo: ProfileRepo,
    private val banhammerRepo: BanhammerRepo,
    private val checkCuratorshipUseCase: CheckCuratorshipUseCase,
): Plugin {

    companion object {
        private const val URL = "/banhammer"
    }

    override fun setup(route: Route) = with(route) {
        banUserInProject()
        unbanUserInProject()
        getBannedUsersInProject()
        Unit
    }

    private fun Route.banUserInProject() = post("$URL/banUserInProject") {
        banUserInProjectHandler(this)
    }

    private fun Route.unbanUserInProject() = post("$URL/unbanUserInProject") {
        unbanUserInProjectHandler(this)
    }

    private fun Route.getBannedUsersInProject() = get("$URL/getBannedUsersInProject") {
        getBannedUsersInProjectHandler(this)
    }

    @ApiOperation(method = "POST", path = "/banhammer/banUserInProject", summary = "Ban user in project", tags = ["plugins"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiQueryParam(name = "victimId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "User banned in project")
    @ApiResponse(code = 400, description = "Required parameters are missing")
    @ApiResponse(code = 404, description = "Curatorship for project was not found")
    @ApiResponse(code = 423, description = "User is not a curator")
    private suspend fun banUserInProjectHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        if (!profileRepo.checkIsCurator(userId)) {
            rc.call.respond(HttpStatusCode.Locked)
            return
        }

        val projectId = rc.call.parameters["projectId"]
        val victimId = rc.call.parameters["victimId"]

        if (projectId == null || victimId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "null params")
            return
        }
        if (!checkCuratorshipUseCase.requireProjectCurator(userId, projectId)) {
            rc.call.respond(HttpStatusCode.NotFound, "not curator")
            return
        }
        banhammerRepo.banUserInProject(victimId, projectId)
        rc.call.respond(HttpStatusCode.OK)
    }

    @ApiOperation(method = "POST", path = "/banhammer/unbanUserInProject", summary = "Unban user in project", tags = ["plugins"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiQueryParam(name = "victimId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "User unbanned in project")
    @ApiResponse(code = 400, description = "Required parameters are missing")
    @ApiResponse(code = 404, description = "Curatorship for project was not found")
    @ApiResponse(code = 423, description = "User is not a curator")
    private suspend fun unbanUserInProjectHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        if (!profileRepo.checkIsCurator(userId)) {
            rc.call.respond(HttpStatusCode.Locked)
            return
        }

        val projectId = rc.call.parameters["projectId"]
        val victimId = rc.call.parameters["victimId"]

        if (projectId == null || victimId == null) {
            rc.call.respond(HttpStatusCode.BadRequest)
            return
        }
        if (!checkCuratorshipUseCase.requireProjectCurator(userId, projectId)) {
            rc.call.respond(HttpStatusCode.NotFound)
            return
        }
        banhammerRepo.unbanUserInProject(victimId, projectId)
        rc.call.respond(HttpStatusCode.OK)
    }

    @ApiOperation(method = "GET", path = "/banhammer/getBannedUsersInProject", summary = "Get banned project users", tags = ["plugins"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Banned users returned")
    @ApiResponse(code = 400, description = "Project id is missing")
    @ApiResponse(code = 404, description = "Curatorship for project was not found")
    @ApiResponse(code = 423, description = "User is not a curator")
    private suspend fun getBannedUsersInProjectHandler(rc: RoutingContext) {
        val principal = rc.call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        if (!profileRepo.checkIsCurator(userId)) {
            rc.call.respond(HttpStatusCode.Locked)
            return
        }

        val projectId = rc.call.parameters["projectId"]

        if (projectId == null) {
            rc.call.respond(HttpStatusCode.BadRequest)
            return
        }
        if (!checkCuratorshipUseCase.requireProjectCurator(userId, projectId)) {
            rc.call.respond(HttpStatusCode.NotFound)
            return
        }
        val users = banhammerRepo.getBannedUsers(projectId)
        rc.call.respondText(
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json,
            text = Json.encodeToString(users)
        )
    }
}
