package app.plugins

import app.openapi.annotations.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get

class Ping: Plugin {
    override fun setup(route: Route) {
        with(route) {
            ping()
        }
    }

    private fun Route.ping() = get("/ping") {
        pingHandler(this)
    }

    @ApiOperation(method = "GET", path = "/ping", summary = "Get authenticated ping timestamp", tags = ["plugins"])
    @ApiSecurity(name = "auth-jwt")
    @ApiResponse(code = 200, description = "Current timestamp", contentType = "text/plain")
    private suspend fun pingHandler(rc: RoutingContext) {
        val time = System.currentTimeMillis()
        rc.call.respond(
            status = HttpStatusCode.OK,
            message = time.toString()
        )
    }
}
