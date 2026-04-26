package app.openapi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OpenApiDocument(
    val openapi: String = "3.1.1",
    val info: OpenApiInfo = OpenApiInfo(),
    val servers: List<OpenApiServer> = emptyList(),
    val paths: Map<String, Map<String, OpenApiOperation>> = emptyMap(),
    val components: OpenApiComponents = OpenApiComponents(),
)

@Serializable
data class OpenApiInfo(
    val title: String = "API Documentation",
    val version: String = "1.0.0",
    val description: String? = null,
)

@Serializable
data class OpenApiServer(
    val url: String,
    val description: String? = null,
)

@Serializable
data class OpenApiComponents(
    val schemas: Map<String, JsonElement> = emptyMap(),
    val securitySchemes: Map<String, OpenApiSecurityScheme> = emptyMap(),
)

@Serializable
data class OpenApiOperation(
    val summary: String? = null,
    val description: String? = null,
    val operationId: String? = null,
    val tags: List<String> = emptyList(),
    val parameters: List<OpenApiParameter> = emptyList(),
    val requestBody: OpenApiRequestBody? = null,
    val responses: Map<String, OpenApiResponse> = emptyMap(),
    val security: List<Map<String, List<String>>> = emptyList(),
)

@Serializable
data class OpenApiParameter(
    val name: String,
    @SerialName("in")
    val location: String,
    val required: Boolean,
    val description: String? = null,
    val schema: JsonElement,
)

@Serializable
data class OpenApiRequestBody(
    val required: Boolean,
    val description: String? = null,
    val content: Map<String, OpenApiMediaType>,
)

@Serializable
data class OpenApiResponse(
    val description: String,
    val content: Map<String, OpenApiMediaType> = emptyMap(),
)

@Serializable
data class OpenApiMediaType(
    val schema: JsonElement? = null,
)

@Serializable
data class OpenApiSecurityScheme(
    val type: String,
    val description: String? = null,
    @SerialName("in")
    val location: String? = null,
    val name: String? = null,
    val scheme: String? = null,
    val bearerFormat: String? = null,
)
