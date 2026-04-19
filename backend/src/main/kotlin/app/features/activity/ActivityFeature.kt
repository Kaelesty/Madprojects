package app.features.activity

import app.openapi.annotations.*
import domain.activity.ActivityRepo
import domain.profile.ProfileRepo
import domain.profile.SharedProfile
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
import ru.kaelesty.madprojects.api.activity.ActivityResponse

interface ActivityFeature {

    suspend fun getActivity(rc: RoutingContext)
}

class ActivityFeatureImpl(
    private val profileRepo: ProfileRepo,
    private val activityRepo: ActivityRepo,
    private val projectRepo: ProjectRepo
): ActivityFeature {

    @ApiOperation(method = "GET", path = "/project/activity/get", summary = "Get project activity", tags = ["activity"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "projectId", type = String::class, required = true)
    @ApiQueryParam(name = "count", type = String::class, required = false, description = "Maximum number of activity entries or null")
    @ApiResponse(code = 200, description = "Project activity returned", type = ActivityResponse::class)
    @ApiResponse(code = 404, description = "Project not found or user has no access")
    override suspend fun getActivity(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            val count = call.parameters["count"]
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val activities = activityRepo.getProjectActivity(
                projectId,
                if (count == "null") null else count?.toInt()
            )
            val actors = activities.map { it.actorId }
                .distinct()
                .filterNotNull()
                .map {
                    val profile = profileRepo.getSharedById(it)?.let {
                        SharedProfile(
                            firstName = it.firstName,
                            secondName = it.secondName,
                            lastName = it.lastName,
                        )
                    }
                    if (profile == null) {
                        null
                    }
                    else {
                        it to profile
                    }
                }
                .filterNotNull()

            call.respondText(
                text = Json.encodeToString(
                    ActivityResponse(
                        activities = activities,
                        actors = actors.map { it.first to it.second }.toMap()
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}
