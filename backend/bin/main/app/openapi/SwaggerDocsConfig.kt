package app.openapi

import app.openapi.model.OpenApiSecurityScheme
import app.openapi.model.OpenApiServer

data class SwaggerDocsConfig(
    val title: String,
    val version: String,
    val description: String = "",
    val servers: List<OpenApiServer> = emptyList(),
    val securitySchemes: Map<String, OpenApiSecurityScheme> = emptyMap(),
    val specPath: String = "/openapi.json",
    val uiPath: String = "/swagger",
)
