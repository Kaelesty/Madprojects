package app.features.database

import app.openapi.annotations.*
import domain.database.lRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface lFeature {
    suspend fun getStudentsWithoutProjects(rc: RoutingContext)
    suspend fun getProjectsWithoutCompletedSprints(rc: RoutingContext)
    suspend fun getAllUsers(rc: RoutingContext)
    suspend fun getUnapprovedProjects(rc: RoutingContext)
    suspend fun getProjectsWithoutRepo(rc: RoutingContext)
    suspend fun getLowRatingGroups(rc: RoutingContext)
    suspend fun getOverdueSprints(rc: RoutingContext)
    suspend fun getUsersWithoutGitHub(rc: RoutingContext)
    suspend fun approveProject(rc: RoutingContext)
    suspend fun deleteProject(rc: RoutingContext)
}

class lFeatureImpl(
    private val lRepo: lRepo,
    private val validator: KeyValidator
) : lFeature {

    @ApiOperation(method = "GET", path = "/db/l/getStudentsWithoutProjects", summary = "Get students without projects", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Students without projects returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getStudentsWithoutProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getStudentsWithoutProjects()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/l/getProjectsWithoutCompletedSprints", summary = "Get projects without completed sprints", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Projects without completed sprints returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getProjectsWithoutCompletedSprints(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getProjectsWithoutCompletedSprints()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/l/getAllUsers", summary = "Get all users", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "All users returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getAllUsers(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getAllUsers()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/l/getUnapprovedProjects", summary = "Get unapproved projects", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Unapproved projects returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getUnapprovedProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getUnapprovedProjects()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/l/getProjectsWithoutRepo", summary = "Get projects without repositories", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Projects without repositories returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getProjectsWithoutRepo(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getProjectsWithoutRepo()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/l/getLowRatingGroups", summary = "Get low-rating groups", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Low-rating groups returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getLowRatingGroups(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getLowRatingGroups()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/l/getOverdueSprints", summary = "Get overdue sprints", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Overdue sprints returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getOverdueSprints(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getOverdueSprints()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/l/getUsersWithoutGitHub", summary = "Get users without GitHub connection", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Users without GitHub connection returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getUsersWithoutGitHub(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getUsersWithoutGitHub()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "POST", path = "/db/l/approveProject", summary = "Approve project", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "projectId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "Project approved")
    @ApiResponse(code = 400, description = "Project id is missing")
    @ApiResponse(code = 403, description = "Access key does not grant admin rights")
    @ApiResponse(code = 500, description = "Project could not be approved")
    override suspend fun approveProject(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()

        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }

        if (projectId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "Missing projectId")
            return
        }

        val result = lRepo.approveProject(projectId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "DELETE", path = "/db/l/deleteProject", summary = "Delete project", tags = ["db-l"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "projectId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "Project deleted")
    @ApiResponse(code = 400, description = "Project id is missing")
    @ApiResponse(code = 403, description = "Access key does not grant admin rights")
    @ApiResponse(code = 500, description = "Project could not be deleted")
    override suspend fun deleteProject(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()

        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }

        if (projectId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "Missing projectId")
            return
        }

        val result = lRepo.deleteProject(projectId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }
}
