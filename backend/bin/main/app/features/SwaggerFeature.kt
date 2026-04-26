package app.features

import app.openapi.SwaggerDocsConfig
import app.openapi.annotations.*
import app.openapi.generated.GeneratedOpenApiRegistry
import app.openapi.model.OpenApiDocument
import app.openapi.model.OpenApiInfo
import app.openapi.model.OpenApiServer
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SwaggerFeature {

    fun install(routing: Routing)
}

class SwaggerFeatureImpl(
    private val config: SwaggerDocsConfig,
) : SwaggerFeature {

    private val json = Json {
        prettyPrint = true
        explicitNulls = false
        encodeDefaults = true
    }

    override fun install(routing: Routing) {
        with(routing) {
            get(normalizePath(config.specPath)) {
                serveOpenApiJson(this)
            }

            get(normalizePath(config.uiPath)) {
                serveSwaggerUi(this)
            }
        }
    }

    @ApiOperation(method = "GET", path = "/openapi.json", summary = "Get generated OpenAPI document", tags = ["docs"])
    @ApiResponse(code = 200, description = "OpenAPI document")
    private suspend fun serveOpenApiJson(rc: RoutingContext) {
        rc.call.respondText(
            text = json.encodeToString(mergedDocument()),
            contentType = ContentType.Application.Json,
        )
    }

    @ApiOperation(method = "GET", path = "/swagger", summary = "Get Swagger UI", tags = ["docs"])
    @ApiResponse(code = 200, description = "Swagger UI page", contentType = "text/html")
    private suspend fun serveSwaggerUi(rc: RoutingContext) {
        rc.call.respondText(
            text = swaggerHtml(specUrl = normalizePath(config.specPath)),
            contentType = ContentType.Text.Html,
        )
    }

    private fun mergedDocument(): OpenApiDocument {
        val generated = GeneratedOpenApiRegistry.document
        return generated.copy(
            info = OpenApiInfo(
                title = config.title,
                version = config.version,
                description = config.description.takeIf { it.isNotBlank() },
            ),
            servers = config.servers.ifEmpty {
                listOf(OpenApiServer(url = "http://localhost"))
            },
            components = generated.components.copy(
                securitySchemes = generated.components.securitySchemes + config.securitySchemes,
            ),
        )
    }

    private fun normalizePath(path: String): String {
        return if (path.startsWith('/')) path else "/$path"
    }

    private fun swaggerHtml(specUrl: String): String {
        val escapedSpecUrl = specUrl.replace("\"", "&quot;")
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>${config.title}</title>
                <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
                <style>
                    body {
                        margin: 0;
                        background: #f3f5f7;
                    }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                <script>
                    window.ui = SwaggerUIBundle({
                        url: "$escapedSpecUrl",
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [SwaggerUIBundle.presets.apis],
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
