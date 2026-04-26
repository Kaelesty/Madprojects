package app.features.database

import app.openapi.annotations.*
import domain.database.iRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface iFeature {
    suspend fun getUnassignedCurators(rc: RoutingContext)
    suspend fun getPendingApprovalProjects(rc: RoutingContext)
    suspend fun getProjectsWithExpiredSprints(rc: RoutingContext)
    suspend fun getGroupsWithAllProjectsCompleted(rc: RoutingContext)
    suspend fun getProjectsWithLowMarks(rc: RoutingContext)
    suspend fun getGroupsWithLowAverage(rc: RoutingContext)
    suspend fun getProjectsWithAboveAverageCards(rc: RoutingContext)
    suspend fun getExcellentProjects(rc: RoutingContext)
    suspend fun getUsersWithoutChatParticipation(rc: RoutingContext)
    suspend fun getMostActiveChats(rc: RoutingContext)
}

class iFeatureImpl(
    private val iRepo: iRepo,
    private val validator: KeyValidator,
) : iFeature {

    @ApiOperation(method = "GET", path = "/db/i/getUnassignedCurators", summary = "Get unassigned curators", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Unassigned curators returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getUnassignedCurators(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val curators = iRepo.getUnassignedCurators()
        rc.call.respondText(
            text = Json.encodeToString(curators),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getPendingApprovalProjects", summary = "Get pending approval projects", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Pending approval projects returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getPendingApprovalProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getPendingApprovalProjects()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getProjectsWithExpiredSprints", summary = "Get projects with expired sprints", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Projects with expired sprints returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getProjectsWithExpiredSprints(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getProjectsWithExpiredSprints()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getGroupsWithAllProjectsCompleted", summary = "Get groups with completed projects", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Groups with completed projects returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getGroupsWithAllProjectsCompleted(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val groups = iRepo.getGroupsWithAllProjectsCompleted()
        rc.call.respondText(
            text = Json.encodeToString(groups),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getProjectsWithLowMarks", summary = "Get projects with low marks", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Projects with low marks returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getProjectsWithLowMarks(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getProjectsWithLowMarks()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getGroupsWithLowAverage", summary = "Get groups with low average marks", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Groups with low average marks returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getGroupsWithLowAverage(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val groups = iRepo.getGroupsWithLowAverage()
        rc.call.respondText(
            text = Json.encodeToString(groups),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getProjectsWithAboveAverageCards", summary = "Get projects with above-average cards", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Projects with above-average cards returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getProjectsWithAboveAverageCards(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getProjectsWithAboveAverageCards()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getExcellentProjects", summary = "Get excellent projects", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Excellent projects returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getExcellentProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getExcellentProjects()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getUsersWithoutChatParticipation", summary = "Get users without chat participation", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Users without chat participation returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getUsersWithoutChatParticipation(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val users = iRepo.getUsersWithoutChatParticipation()
        rc.call.respondText(
            text = Json.encodeToString(users),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/i/getMostActiveChats", summary = "Get most active chats", tags = ["db-i"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "limit", type = Int::class, required = false)
    @ApiResponse(code = 200, description = "Most active chats returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getMostActiveChats(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val limit = rc.call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
        val chats = iRepo.getMostActiveChats(limit)
        rc.call.respondText(
            text = Json.encodeToString(chats),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }
}
