package app.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

class Ping: Plugin {
    override fun setup(route: Route) {
        with(route) {
            ping()
        }
    }

    private fun Route.ping() = get("/ping") {
        val time = System.currentTimeMillis()
        call.respond(
            status = HttpStatusCode.OK,
            message = time.toString()
        )
    }
}