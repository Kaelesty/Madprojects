package app.features

import app.openapi.SwaggerDocsConfig
import app.openapi.model.OpenApiSecurityScheme
import app.openapi.model.OpenApiServer
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.client.statement.bodyAsText
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwaggerFeatureTest {

    private val config = SwaggerDocsConfig(
        title = "Test API",
        version = "9.9.9",
        description = "Generated in tests",
        servers = listOf(OpenApiServer(url = "https://example.test")),
        securitySchemes = mapOf(
            "auth-jwt" to OpenApiSecurityScheme(
                type = "http",
                scheme = "bearer",
                bearerFormat = "JWT",
            )
        ),
    )

    @Test
    fun `openapi endpoint returns generated document`() = testApplication {
        application {
            routing {
                SwaggerFeatureImpl(config).install(this)
            }
        }

        val response = client.get("/openapi.json")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue((response.headers["Content-Type"] ?: "").startsWith("application/json"))

        val payload = response.bodyAsText()
        assertTrue(payload.contains("\"openapi\""), payload)
        val body = Json.parseToJsonElement(payload).jsonObject
        assertEquals("3.1.1", body.getValue("openapi").jsonPrimitive.content)
        assertEquals("Test API", body.getValue("info").jsonObject.getValue("title").jsonPrimitive.content)
        assertEquals("9.9.9", body.getValue("info").jsonObject.getValue("version").jsonPrimitive.content)
        assertTrue(body.getValue("paths").jsonObject.isEmpty())
        assertEquals(
            "http",
            body.getValue("components").jsonObject
                .getValue("securitySchemes").jsonObject
                .getValue("auth-jwt").jsonObject
                .getValue("type").jsonPrimitive.content,
        )
    }

    @Test
    fun `swagger endpoint returns html bound to openapi json`() = testApplication {
        application {
            routing {
                SwaggerFeatureImpl(config).install(this)
            }
        }

        val response = client.get("/swagger")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("SwaggerUIBundle"))
        assertTrue(body.contains("url: \"/openapi.json\""))
    }
}
