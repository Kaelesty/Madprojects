package app.features

import app.smtp.EmailService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

interface EmailFeature {

    suspend fun sayHello(rc: RoutingContext)
}

class EmailFeatureImpl(
    private val emailService: EmailService
): EmailFeature {

    override suspend fun sayHello(rc: RoutingContext) {
        with(rc) {
            val accessCode = call.parameters["accessCode"]
            if (accessCode != "_sEa1") {
                call.respond(HttpStatusCode.Forbidden)
                return
            }

            val subject = call.parameters["subject"]
            if (subject == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            emailService.sendEmail(
                to = subject,
                subject = "Just saying hello...",
                body = "hello!"
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}