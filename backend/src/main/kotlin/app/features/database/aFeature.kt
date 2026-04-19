package app.features.database

import app.openapi.annotations.*
import domain.database.aRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.server.response.respond

interface aFeature {
    suspend fun getProjectUsers(rc: RoutingContext)
    suspend fun getGroupProjects(rc: RoutingContext)
    suspend fun addProjectToGroup(rc: RoutingContext)
    suspend fun getAllProjectStatuses(rc: RoutingContext)
    suspend fun approveProject(rc: RoutingContext)
    suspend fun createSprintKard(rc: RoutingContext)
    suspend fun moveKard(rc: RoutingContext)
    suspend fun getGroupMarksCsv(rc: RoutingContext)
    suspend fun approveAllGroupProjects(rc: RoutingContext)
    suspend fun removeUserFromProject(rc: RoutingContext)
}

class aFeatureImpl(
    private val aRepo: aRepo,
    private val validator: KeyValidator
) : aFeature {

    @ApiOperation(method = "GET", path = "/db/a/getProjectUsers", summary = "Get project users", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "projectId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "Project users returned")
    @ApiResponse(code = 400, description = "Project id is missing")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getProjectUsers(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing projectId")

        val data = aRepo.getProjectUsers(projectId)
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/a/getGroupProjects", summary = "Get group projects", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "groupId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "Group projects returned")
    @ApiResponse(code = 400, description = "Group id is missing")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getGroupProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val groupId = rc.call.parameters["groupId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing groupId")

        val data = aRepo.getGroupProjects(groupId)
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "POST", path = "/db/a/addProjectToGroup", summary = "Add project to group", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "projectId", type = Int::class, required = true)
    @ApiQueryParam(name = "groupId", type = Int::class, required = true)
    @ApiQueryParam(name = "curatorId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "Project added to group")
    @ApiResponse(code = 400, description = "Required parameters are missing")
    @ApiResponse(code = 403, description = "Access key does not grant admin rights")
    @ApiResponse(code = 500, description = "Project could not be added to group")
    override suspend fun addProjectToGroup(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing projectId")
        val groupId = rc.call.parameters["groupId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing groupId")
        val curatorId = rc.call.parameters["curatorId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing curatorId")

        val result = aRepo.addProjectToGroup(projectId, groupId, curatorId, "pending")
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/a/getAllProjectStatuses", summary = "Get all project statuses", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiResponse(code = 200, description = "Project statuses returned")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getAllProjectStatuses(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = aRepo.getAllProjectStatuses()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "POST", path = "/db/a/approveProject", summary = "Approve project", tags = ["db-a"])
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

        val result = aRepo.approveProject(projectId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "POST", path = "/db/a/createSprintKard", summary = "Create sprint kard", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "name", type = String::class, required = true)
    @ApiQueryParam(name = "authorId", type = Int::class, required = true)
    @ApiQueryParam(name = "desc", type = String::class, required = true)
    @ApiQueryParam(name = "sprintId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "Sprint kard created")
    @ApiResponse(code = 400, description = "Required parameters are missing")
    @ApiResponse(code = 403, description = "Access key does not grant admin rights")
    @ApiResponse(code = 500, description = "Sprint kard could not be created")
    override suspend fun createSprintKard(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        val name = rc.call.parameters["name"]
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing name")
        val authorId = rc.call.parameters["authorId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing authorId")
        val desc = rc.call.parameters["desc"]
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing desc")
        val sprintId = rc.call.parameters["sprintId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing sprintId")

        val result = aRepo.createSprintKard(name, authorId, desc, sprintId, createTimeMillis = System.currentTimeMillis())
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "POST", path = "/db/a/moveKard", summary = "Move kard", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "kardId", type = Int::class, required = true)
    @ApiQueryParam(name = "columnId", type = Int::class, required = true)
    @ApiQueryParam(name = "newOrder", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "Kard moved")
    @ApiResponse(code = 400, description = "Required parameters are missing")
    @ApiResponse(code = 403, description = "Access key does not grant admin rights")
    @ApiResponse(code = 500, description = "Kard could not be moved")
    override suspend fun moveKard(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        val kardId = rc.call.parameters["kardId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing kardId")
        val columnId = rc.call.parameters["columnId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing columnId")
        val newOrder = rc.call.parameters["newOrder"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing newOrder")

        val result = aRepo.moveKard(kardId, columnId, newOrder)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "GET", path = "/db/a/getGroupMarksCsv", summary = "Get group marks export", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "groupId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "Group marks export returned", contentType = "text/plain")
    @ApiResponse(code = 400, description = "Group id is missing")
    @ApiResponse(code = 401, description = "Access key is invalid")
    override suspend fun getGroupMarksCsv(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val groupId = rc.call.parameters["groupId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing groupId")

        val data = aRepo.getGroupMarksCsv(groupId)
        rc.call.respondText(
            text = data.csvData,
            status = HttpStatusCode.OK,
        )
    }

    @ApiOperation(method = "POST", path = "/db/a/approveAllGroupProjects", summary = "Approve all group projects", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "groupId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "All group projects approved")
    @ApiResponse(code = 400, description = "Group id is missing")
    @ApiResponse(code = 403, description = "Access key does not grant admin rights")
    @ApiResponse(code = 500, description = "Projects could not be approved")
    override suspend fun approveAllGroupProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        val groupId = rc.call.parameters["groupId"]?.toIntOrNull()

        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        if (groupId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "Missing groupId")
            return
        }

        val result = aRepo.approveAllGroupProjects(groupId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    @ApiOperation(method = "DELETE", path = "/db/a/removeUserFromProject", summary = "Remove user from project", tags = ["db-a"])
    @ApiQueryParam(name = "key", type = String::class, required = true)
    @ApiQueryParam(name = "userId", type = Int::class, required = true)
    @ApiQueryParam(name = "projectId", type = Int::class, required = true)
    @ApiResponse(code = 200, description = "User removed from project")
    @ApiResponse(code = 400, description = "Required parameters are missing")
    @ApiResponse(code = 403, description = "Access key does not grant admin rights")
    @ApiResponse(code = 500, description = "User could not be removed from project")
    override suspend fun removeUserFromProject(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        val userId = rc.call.parameters["userId"]?.toIntOrNull()
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()

        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        if (userId == null || projectId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "Missing parameters")
            return
        }

        val result = aRepo.removeUserFromProject(userId, projectId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }
}
