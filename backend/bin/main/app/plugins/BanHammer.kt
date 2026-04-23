package app.plugins

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
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        if (!profileRepo.checkIsCurator(userId)) {
            call.respond(HttpStatusCode.Locked)
            return@post
        }

        val projectId = call.parameters["projectId"]
        val victimId = call.parameters["victimId"]

        if (projectId == null || victimId == null) {
            call.respond(HttpStatusCode.BadRequest, "null params")
            return@post
        }
        if (!checkCuratorshipUseCase.requireProjectCurator(userId, projectId)) {
            call.respond(HttpStatusCode.NotFound, "not curator")
            return@post
        }
        banhammerRepo.banUserInProject(victimId, projectId)
        call.respond(HttpStatusCode.OK)
    }

    private fun Route.unbanUserInProject() = post("$URL/unbanUserInProject") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        if (!profileRepo.checkIsCurator(userId)) {
            call.respond(HttpStatusCode.Locked)
            return@post
        }

        val projectId = call.parameters["projectId"]
        val victimId = call.parameters["victimId"]

        if (projectId == null || victimId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        if (!checkCuratorshipUseCase.requireProjectCurator(userId, projectId)) {
            call.respond(HttpStatusCode.NotFound)
            return@post
        }
        banhammerRepo.unbanUserInProject(victimId, projectId)
        call.respond(HttpStatusCode.OK)
    }

    private fun Route.getBannedUsersInProject() = get("$URL/getBannedUsersInProject") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        if (!profileRepo.checkIsCurator(userId)) {
            call.respond(HttpStatusCode.Locked)
            return@get
        }

        val projectId = call.parameters["projectId"]

        if (projectId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }
        if (!checkCuratorshipUseCase.requireProjectCurator(userId, projectId)) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val users = banhammerRepo.getBannedUsers(projectId)
        call.respondText(
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json,
            text = Json.encodeToString(users)
        )
    }
}