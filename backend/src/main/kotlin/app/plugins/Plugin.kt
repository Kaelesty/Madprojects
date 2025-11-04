package app.plugins

import io.ktor.server.routing.Route

interface Plugin {
    fun setup(route: Route)
}