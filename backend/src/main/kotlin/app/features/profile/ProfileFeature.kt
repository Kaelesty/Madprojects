package app.features.profile

import app.openapi.annotations.*
import domain.GithubTokensRepo
import domain.auth.UserType
import domain.profile.CommonProfileResponse
import domain.profile.ProfileRepo
import domain.project.ProjectRepo
import domain.projectgroups.ProjectsGroupRepo
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
import ru.kaelesty.madprojects.api.profile.CuratorProfileResponse
import ru.kaelesty.madprojects.api.profile.SharedProfileResponse
import ru.kaelesty.madprojects.api.profile.UpdateProfileRequest

interface ProfileFeature {

    suspend fun getCommonProfile(rc: RoutingContext)

    suspend fun updateCommonProfile(rc: RoutingContext)

    suspend fun updateCuratorProfile(rc: RoutingContext)

    suspend fun getSharedProfile(rc: RoutingContext)

    suspend fun getCuratorProfile(rc: RoutingContext)
}

class ProfileFeatureImpl(
    private val profileRepo: ProfileRepo,
    private val githubTokensRepo: GithubTokensRepo,
    private val projectsRepo: ProjectRepo,
    private val projectsGroupRepo: ProjectsGroupRepo,
): ProfileFeature {

    @ApiOperation(method = "POST", path = "/curatorProfile/update", summary = "Update curator profile", tags = ["profile"])
    @ApiSecurity(name = "auth-jwt")
    @ApiRequestBody(type = UpdateProfileRequest::class, description = "Updated curator profile data")
    @ApiResponse(code = 200, description = "Curator profile updated")
    override suspend fun updateCuratorProfile(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<UpdateProfileRequest>()
            with(request) {
                profileRepo.updateCurator(
                    userId, firstName, secondName, lastName, data
                )
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    @ApiOperation(method = "GET", path = "/curatorProfile", summary = "Get curator profile", tags = ["profile"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "profileId", type = String::class, required = false)
    @ApiResponse(code = 200, description = "Curator profile returned", type = CuratorProfileResponse::class)
    @ApiResponse(code = 404, description = "Curator profile not found")
    @ApiResponse(code = 423, description = "Requested profile is not a curator")
    override suspend fun getCuratorProfile(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val param = call.parameters["profileId"]

            val profileId = if (param == null || param == "null") userId else param

            if (!profileRepo.checkIsCurator(profileId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }
            val profile = profileRepo.getCuratorById(profileId)
            if (profile == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val response = CuratorProfileResponse(
                firstName = profile.data.firstName,
                secondName = profile.data.secondName,
                lastName = profile.data.lastName,
                email = profile.data.email,
                grade = profile.grade,
                githubMeta = githubTokensRepo.getUserMeta(userId),
                projectGroups = projectsGroupRepo.getCuratorProjectGroups(userId)
            )

            call.respondText(
                text = Json.encodeToString(response),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    @ApiOperation(method = "GET", path = "/sharedProfile", summary = "Get shared profile", tags = ["profile"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "userId", type = String::class, required = false)
    @ApiResponse(code = 200, description = "Shared profile returned", type = SharedProfileResponse::class)
    @ApiResponse(code = 404, description = "Profile not found")
    override suspend fun getSharedProfile(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val jwtId = principal!!.payload.getClaim("userId").asString()
            val param = call.parameters["userId"]

            val userId = param ?: jwtId

            val githubMeta = githubTokensRepo.getUserMeta(userId)
            val profile = profileRepo.getSharedById(userId)
            if (profile == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val data = when (profile.role) {
                UserType.Common -> {
                    profileRepo.getCommonById(userId)?.group
                }
                UserType.Curator -> {
                    profileRepo.getCuratorById(userId)?.grade
                }
            } ?: "..."

            with(profile) {
                call.respondText(
                    text = Json.encodeToString(
                        SharedProfileResponse(
                            firstName = firstName,
                            secondName = secondName,
                            lastName = lastName,
                            avatar = githubMeta?.githubAvatar,
                            role = role,
                            data = data,
                            githubLink = githubMeta?.profileLink,
                            email = email,
                        )
                    ),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK
                )
            }
        }
    }

    @ApiOperation(method = "POST", path = "/commonProfile/update", summary = "Update common profile", tags = ["profile"])
    @ApiSecurity(name = "auth-jwt")
    @ApiRequestBody(type = UpdateProfileRequest::class, description = "Updated common profile data")
    @ApiResponse(code = 200, description = "Common profile updated")
    override suspend fun updateCommonProfile(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<UpdateProfileRequest>()
            with(request) {
                profileRepo.updateCommon(
                    userId, firstName, secondName, lastName, data
                )
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    @ApiOperation(method = "GET", path = "/commonProfile", summary = "Get common profile", tags = ["profile"])
    @ApiSecurity(name = "auth-jwt")
    @ApiQueryParam(name = "profileId", type = String::class, required = false)
    @ApiResponse(code = 200, description = "Common profile returned", type = CommonProfileResponse::class)
    @ApiResponse(code = 404, description = "Profile not found")
    override suspend fun getCommonProfile(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val param = call.parameters["profileId"]

            val profileId = if (param == null || param == "null") userId else param

            val user = profileRepo.getCommonById(profileId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val githubMeta = githubTokensRepo.getUserMeta(profileId)

            val projects = projectsRepo.getUserProjects(profileId)


            call.respondText(
                text = Json.encodeToString(
                    CommonProfileResponse(
                        firstName = user.data.firstName,
                        lastName = user.data.lastName,
                        secondName = user.data.secondName,
                        email = user.data.email,
                        projects = projects,
                        githubMeta = githubMeta,
                        group = user.group
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}
