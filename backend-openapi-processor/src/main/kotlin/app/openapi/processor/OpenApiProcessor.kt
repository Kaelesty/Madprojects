package app.openapi.processor

import app.openapi.annotations.ApiHeaderParam
import app.openapi.annotations.ApiOperation
import app.openapi.annotations.ApiPathParam
import app.openapi.annotations.ApiQueryParam
import app.openapi.annotations.ApiRequestBody
import app.openapi.annotations.ApiResponse
import app.openapi.annotations.ApiRouteRef
import app.openapi.annotations.ApiSecurity
import app.openapi.model.OpenApiComponents
import app.openapi.model.OpenApiDocument
import app.openapi.model.OpenApiInfo
import app.openapi.model.OpenApiMediaType
import app.openapi.model.OpenApiOperation
import app.openapi.model.OpenApiParameter
import app.openapi.model.OpenApiRequestBody
import app.openapi.model.OpenApiResponse
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class OpenApiProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return OpenApiProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}

private class OpenApiProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) {
            return emptyList()
        }

        val document = OpenApiCollector(
            resolver = resolver,
            logger = logger,
        ).collect()

        GeneratedRegistryWriter(codeGenerator).write(document)
        generated = true
        return emptyList()
    }
}

internal class OpenApiCollector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) {

    private val schemaResolver = SchemaResolver(logger)

    fun collect(): OpenApiDocument {
        val functions = collectAnnotatedFunctions()
        val docsByName = functions.associateBy(
            keySelector = { qualifiedNameOf(it) },
            valueTransform = { parseFunction(it) },
        )
        val referencedHandlers = docsByName.values.mapNotNull { it.routeRef }.toSet()

        val operations = mutableListOf<EffectiveOperation>()
        for (doc in docsByName.values.sortedBy { it.qualifiedName }) {
            if (doc.routeRef != null) {
                operations += mergeWrapper(doc, docsByName[doc.routeRef])
                continue
            }

            if (doc.qualifiedName in referencedHandlers) {
                continue
            }

            if (doc.operation != null) {
                operations += buildStandalone(doc)
                continue
            }

            if (doc.hasApiDetails()) {
                fail(
                    node = doc.symbol,
                    message = "API detail annotations require @ApiOperation or a wrapper linked via @ApiRouteRef.",
                )
            }
        }

        duplicateMethodPath(operations.map { it.method to it.path })?.let { (method, path) ->
            val source = operations.first { it.method == method && it.path == path }.source
            fail(
                node = source,
                message = "Duplicate OpenAPI operation for $method $path.",
            )
        }

        val paths = operations
            .groupBy { it.path }
            .mapValues { (_, pathOperations) ->
                pathOperations.associate { op ->
                    op.method.lowercase() to OpenApiOperation(
                        summary = op.summary,
                        description = op.description,
                        operationId = op.operationId,
                        tags = op.tags,
                        parameters = op.parameters.map { parameter ->
                            OpenApiParameter(
                                name = parameter.name,
                                location = parameter.location,
                                required = parameter.required,
                                description = parameter.description.takeIf { it.isNotBlank() },
                                schema = parameter.schema,
                            )
                        },
                        requestBody = op.requestBody?.let { body ->
                            OpenApiRequestBody(
                                required = body.required,
                                description = body.description.takeIf { it.isNotBlank() },
                                content = mapOf(body.contentType to OpenApiMediaType(body.schema)),
                            )
                        },
                        responses = op.responses.associate { response ->
                            response.code.toString() to OpenApiResponse(
                                description = response.description,
                                content = response.schema?.let { schema ->
                                    mapOf(response.contentType to OpenApiMediaType(schema))
                                } ?: emptyMap(),
                            )
                        },
                        security = op.security.map { security ->
                            mapOf(security.name to security.scopes)
                        },
                    )
                }
            }

        return OpenApiDocument(
            info = OpenApiInfo(),
            paths = paths,
            components = OpenApiComponents(
                schemas = schemaResolver.components(),
                securitySchemes = emptyMap(),
            ),
        )
    }

    private fun collectAnnotatedFunctions(): List<KSFunctionDeclaration> {
        val annotationNames = listOf(
            ApiOperation::class.qualifiedName!!,
            ApiQueryParam::class.qualifiedName!!,
            ApiPathParam::class.qualifiedName!!,
            ApiHeaderParam::class.qualifiedName!!,
            ApiRequestBody::class.qualifiedName!!,
            ApiResponse::class.qualifiedName!!,
            ApiSecurity::class.qualifiedName!!,
            ApiRouteRef::class.qualifiedName!!,
        )

        return annotationNames
            .asSequence()
            .flatMap { resolver.getSymbolsWithAnnotation(it).asSequence() }
            .filterIsInstance<KSFunctionDeclaration>()
            .associateBy { qualifiedNameOf(it) }
            .values
            .toList()
    }

    private fun buildStandalone(doc: ParsedFunction): EffectiveOperation {
        validateStandaloneSignature(doc)

        val operation = doc.operation ?: fail(
            node = doc.symbol,
            message = "Standalone API operation is missing @ApiOperation.",
        )

        val method = normalizeMethod(operation.method, doc.symbol)
        val path = normalizePath(operation.path, doc.symbol)
        runCatching { validatePathTemplate(path, doc.pathParams.map { it.name }.toSet()) }
            .getOrElse { fail(doc.symbol, it.message ?: "Invalid path template.") }
        validateResponseDuplicates(doc.responses, doc.symbol)

        return EffectiveOperation(
            source = doc.symbol,
            method = method,
            path = path,
            summary = operation.summary.ifBlank { null },
            description = operation.description.ifBlank { null },
            operationId = operation.operationId.ifBlank { null },
            tags = operation.tags,
            parameters = buildParameters(doc),
            requestBody = buildRequestBody(doc.requestBody, doc.symbol),
            responses = buildResponses(doc.responses, doc.symbol),
            security = doc.security,
        )
    }

    private fun mergeWrapper(wrapper: ParsedFunction, handler: ParsedFunction?): EffectiveOperation {
        validateWrapperSignature(wrapper)

        val operation = wrapper.operation ?: fail(
            node = wrapper.symbol,
            message = "Route wrapper functions using @ApiRouteRef must also declare @ApiOperation.",
        )
        val target = handler ?: fail(
            node = wrapper.symbol,
            message = "Referenced handler `${wrapper.routeRef}` was not found among annotated functions.",
        )

        validateHandlerSignature(target)
        if (wrapper.queryParams.isNotEmpty() || wrapper.pathParams.isNotEmpty() || wrapper.headerParams.isNotEmpty() ||
            wrapper.requestBody != null || wrapper.responses.isNotEmpty()
        ) {
            fail(
                node = wrapper.symbol,
                message = "Route wrappers linked via @ApiRouteRef must not declare params, request body, or responses directly.",
            )
        }

        val method = normalizeMethod(operation.method, wrapper.symbol)
        val path = normalizePath(operation.path, wrapper.symbol)
        runCatching { validatePathTemplate(path, target.pathParams.map { it.name }.toSet()) }
            .getOrElse { fail(wrapper.symbol, it.message ?: "Invalid path template.") }
        validateResponseDuplicates(target.responses, target.symbol)

        val targetOperation = target.operation
        return EffectiveOperation(
            source = wrapper.symbol,
            method = method,
            path = path,
            summary = targetOperation?.summary?.ifBlank { null } ?: operation.summary.ifBlank { null },
            description = targetOperation?.description?.ifBlank { null } ?: operation.description.ifBlank { null },
            operationId = targetOperation?.operationId?.ifBlank { null } ?: operation.operationId.ifBlank { null },
            tags = if (targetOperation != null && targetOperation.tags.isNotEmpty()) targetOperation.tags else operation.tags,
            parameters = buildParameters(target),
            requestBody = buildRequestBody(target.requestBody, target.symbol),
            responses = buildResponses(target.responses, target.symbol),
            security = if (wrapper.security.isNotEmpty()) wrapper.security else target.security,
        )
    }

    private fun buildParameters(doc: ParsedFunction): List<ParameterDoc> {
        return buildList {
            doc.pathParams.forEach { add(it.toParameter("path")) }
            doc.queryParams.forEach { add(it.toParameter("query")) }
            doc.headerParams.forEach { add(it.toParameter("header")) }
        }
    }

    private fun ParsedParam.toParameter(location: String): ParameterDoc {
        return ParameterDoc(
            name = name,
            location = location,
            required = required || location == "path",
            description = description,
            schema = schemaResolver.resolveParameterSchema(type, source),
        )
    }

    private fun buildRequestBody(body: ParsedRequestBody?, node: KSNode): RequestBodyDoc? {
        if (body == null) {
            return null
        }
        return RequestBodyDoc(
            required = body.required,
            description = body.description,
            contentType = body.contentType,
            schema = resolveSchemaSpec(body.schema, node, SchemaUsage.REQUEST_BODY),
        )
    }

    private fun buildResponses(responses: List<ParsedResponse>, node: KSNode): List<ResponseDoc> {
        if (responses.isEmpty()) {
            return listOf(ResponseDoc(code = 200, description = "OK", contentType = "application/json", schema = null))
        }

        return responses.map { response ->
            ResponseDoc(
                code = response.code,
                description = response.description,
                contentType = response.contentType,
                schema = if (response.schema.isEmpty()) {
                    null
                } else {
                    resolveSchemaSpec(response.schema, node, SchemaUsage.RESPONSE)
                },
            )
        }
    }

    private fun resolveSchemaSpec(schema: SchemaSpec, node: KSNode, usage: SchemaUsage): JsonElement {
        return when (schema.mode()) {
            SchemaMode.AUTO -> schemaResolver.resolveType(schema.type!!, node)
            SchemaMode.REF -> JsonObject(mapOf("\$ref" to JsonPrimitive("#/components/schemas/${schema.schemaRef}")))
            SchemaMode.JSON -> runCatching { Json.parseToJsonElement(schema.schemaJson) }
                .getOrElse {
                    fail(node, "Invalid schemaJson: ${it.message}")
                }
            SchemaMode.NONE,
            SchemaMode.INVALID -> fail(node, "Schema source is missing or invalid for $usage.")
        }
    }

    private fun validateResponseDuplicates(responses: List<ParsedResponse>, node: KSNode) {
        val duplicates = responses.groupBy { it.code }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            fail(node, "Duplicate response codes are not allowed: ${duplicates.sorted().joinToString()}.")
        }
    }

    private fun validateStandaloneSignature(doc: ParsedFunction) {
        if (!doc.isHandlerLike() && !doc.isRouteWrapperLike()) {
            fail(
                doc.symbol,
                "Standalone documented functions must be handler-like (suspend fun x(RoutingContext)) or route-wrapper-like (function with Routing receiver/parameter).",
            )
        }
        if (doc.isHandlerLike()) {
            validateHandlerSignature(doc)
        }
    }

    private fun validateHandlerSignature(doc: ParsedFunction) {
        if (!doc.isHandlerLike()) {
            fail(
                doc.symbol,
                "Handler functions must be `suspend fun x(rc: io.ktor.server.routing.RoutingContext)`.",
            )
        }
    }

    private fun validateWrapperSignature(doc: ParsedFunction) {
        if (!doc.isRouteWrapperLike()) {
            fail(
                doc.symbol,
                "Route wrapper functions must have a Routing receiver or Routing parameter.",
            )
        }
    }

    private fun parseFunction(symbol: KSFunctionDeclaration): ParsedFunction {
        return ParsedFunction(
            symbol = symbol,
            qualifiedName = qualifiedNameOf(symbol),
            operation = symbol.findAnnotation(ApiOperation::class.qualifiedName!!)?.let(::parseOperation),
            routeRef = symbol.findAnnotation(ApiRouteRef::class.qualifiedName!!)?.argument<String>("handler")?.takeIf { it.isNotBlank() },
            queryParams = symbol.findAnnotations(ApiQueryParam::class.qualifiedName!!).map(::parseQueryParam),
            pathParams = symbol.findAnnotations(ApiPathParam::class.qualifiedName!!).map(::parsePathParam),
            headerParams = symbol.findAnnotations(ApiHeaderParam::class.qualifiedName!!).map(::parseHeaderParam),
            requestBody = symbol.findAnnotation(ApiRequestBody::class.qualifiedName!!)?.let(::parseRequestBody),
            responses = symbol.findAnnotations(ApiResponse::class.qualifiedName!!).map(::parseResponse),
            security = symbol.findAnnotations(ApiSecurity::class.qualifiedName!!).map(::parseSecurity),
        )
    }

    private fun parseOperation(annotation: KSAnnotation): ParsedOperation {
        return ParsedOperation(
            method = annotation.argument("method"),
            path = annotation.argument("path"),
            summary = annotation.argument("summary"),
            description = annotation.argument("description"),
            tags = annotation.argument<List<String>>("tags"),
            operationId = annotation.argument("operationId"),
        )
    }

    private fun parseQueryParam(annotation: KSAnnotation): ParsedParam {
        return ParsedParam(
            source = annotation,
            name = annotation.argument("name"),
            type = annotation.kClassArgument("type"),
            required = annotation.argument("required"),
            description = annotation.argument("description"),
        )
    }

    private fun parsePathParam(annotation: KSAnnotation): ParsedParam {
        return ParsedParam(
            source = annotation,
            name = annotation.argument("name"),
            type = annotation.kClassArgument("type"),
            required = true,
            description = annotation.argument("description"),
        )
    }

    private fun parseHeaderParam(annotation: KSAnnotation): ParsedParam {
        return ParsedParam(
            source = annotation,
            name = annotation.argument("name"),
            type = annotation.kClassArgument("type"),
            required = annotation.argument("required"),
            description = annotation.argument("description"),
        )
    }

    private fun parseRequestBody(annotation: KSAnnotation): ParsedRequestBody {
        return ParsedRequestBody(
            required = annotation.argument("required"),
            description = annotation.argument("description"),
            contentType = annotation.argument<String>("contentType").ifBlank { "application/json" },
            schema = SchemaSpec(
                type = annotation.kClassArgumentOrNull("type")?.takeUnless { it.isUnitType() },
                schemaRef = annotation.argument("schemaRef"),
                schemaJson = annotation.argument("schemaJson"),
            ).validated(annotation, SchemaUsage.REQUEST_BODY, logger),
        )
    }

    private fun parseResponse(annotation: KSAnnotation): ParsedResponse {
        return ParsedResponse(
            code = annotation.argument("code"),
            description = annotation.argument("description"),
            contentType = annotation.argument<String>("contentType").ifBlank { "application/json" },
            schema = SchemaSpec(
                type = annotation.kClassArgumentOrNull("type")?.takeUnless { it.isUnitType() },
                schemaRef = annotation.argument("schemaRef"),
                schemaJson = annotation.argument("schemaJson"),
            ).validated(annotation, SchemaUsage.RESPONSE, logger),
        )
    }

    private fun parseSecurity(annotation: KSAnnotation): SecurityDoc {
        return SecurityDoc(
            name = annotation.argument("name"),
            scopes = annotation.argument<List<String>>("scopes"),
        )
    }

    private fun normalizeMethod(method: String, node: KSNode): String {
        val normalized = method.trim().uppercase()
        if (normalized !in HTTP_METHODS) {
            fail(node, "Unsupported HTTP method `$method`.")
        }
        return normalized
    }

    private fun normalizePath(path: String, node: KSNode): String {
        val normalized = path.trim()
        if (!normalized.startsWith("/")) {
            fail(node, "OpenAPI path must start with `/`.")
        }
        return normalized
    }

    private fun fail(node: KSNode, message: String): Nothing {
        logger.error(message, node)
        throw ProcessorException(message)
    }
}

internal class SchemaResolver(
    private val logger: KSPLogger,
) {

    private val componentNamesByType = linkedMapOf<String, String>()
    private val componentSchemas = linkedMapOf<String, JsonElement>()

    fun components(): Map<String, JsonElement> = componentSchemas.toMap()

    fun resolveParameterSchema(type: KSType, node: KSNode): JsonElement {
        return resolveType(type, node)
    }

    fun resolveType(type: KSType, node: KSNode): JsonElement {
        val nullable = type.nullability == Nullability.NULLABLE
        val nonNullType = type.makeNotNullable()
        val declaration = nonNullType.declaration
        val schema = when (val qualifiedName = declaration.qualifiedName?.asString()) {
            "kotlin.String", "java.lang.String" -> typedSchema("string")
            "kotlin.Boolean", "java.lang.Boolean" -> typedSchema("boolean")
            "kotlin.Int", "java.lang.Integer" -> typedSchema("integer")
            "kotlin.Long", "java.lang.Long" -> typedSchema("integer", format = "int64")
            "kotlin.Short", "java.lang.Short" -> typedSchema("integer")
            "kotlin.Double", "java.lang.Double" -> typedSchema("number", format = "double")
            "kotlin.Float", "java.lang.Float" -> typedSchema("number", format = "float")
            "kotlin.collections.List",
            "kotlin.collections.MutableList",
            "kotlin.collections.Set",
            "kotlin.collections.MutableSet" -> resolveArraySchema(nonNullType, node)

            "kotlin.collections.Map",
            "kotlin.collections.MutableMap" -> resolveMapSchema(nonNullType, node)

            else -> resolveNamedType(nonNullType, declaration, qualifiedName, node)
        }

        return if (nullable) {
            nullableSchema(schema)
        } else {
            schema
        }
    }

    private fun resolveArraySchema(type: KSType, node: KSNode): JsonElement {
        val argument = type.arguments.singleOrNull()?.requireType(node, "Collections must declare an item type.")
        val itemType = argument ?: return JsonNull
        return JsonObject(
            mapOf(
                "type" to JsonPrimitive("array"),
                "items" to resolveType(itemType, node),
            )
        )
    }

    private fun resolveMapSchema(type: KSType, node: KSNode): JsonElement {
        val keyType = type.arguments.getOrNull(0)?.requireType(node, "Maps must declare a String key type.")
        val valueType = type.arguments.getOrNull(1)?.requireType(node, "Maps must declare a value type.")
        if (keyType == null || valueType == null) {
            return JsonNull
        }
        if (keyType.makeNotNullable().declaration.qualifiedName?.asString() != "kotlin.String") {
            fail(node, "Only Map<String, T> is supported in OpenAPI schema generation.")
        }
        return JsonObject(
            mapOf(
                "type" to JsonPrimitive("object"),
                "additionalProperties" to resolveType(valueType, node),
            )
        )
    }

    private fun resolveNamedType(
        type: KSType,
        declaration: KSDeclaration,
        qualifiedName: String?,
        node: KSNode,
    ): JsonElement {
        val classDeclaration = declaration as? KSClassDeclaration
            ?: fail(node, "Unsupported schema declaration `${declaration.simpleName.asString()}`.")

        return when {
            classDeclaration.classKind == ClassKind.ENUM_CLASS -> componentRef(type, classDeclaration, buildEnumSchema(classDeclaration))
            classDeclaration.classKind == ClassKind.CLASS &&
                classDeclaration.modifiers.contains(Modifier.DATA) &&
                classDeclaration.hasAnnotation("kotlinx.serialization.Serializable") ->
                componentRef(type, classDeclaration, buildSerializableDataClassSchema(classDeclaration, node))

            else -> fail(
                node,
                "Unsupported schema type `${qualifiedName ?: declaration.simpleName.asString()}`. Use schemaRef or schemaJson override.",
            )
        }
    }

    private fun buildEnumSchema(classDeclaration: KSClassDeclaration): JsonElement {
        val values = classDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.ENUM_ENTRY }
            .map { JsonPrimitive(it.simpleName.asString()) }
            .toList()

        return JsonObject(
            mapOf(
                "type" to JsonPrimitive("string"),
                "enum" to JsonArray(values),
            )
        )
    }

    private fun buildSerializableDataClassSchema(classDeclaration: KSClassDeclaration, node: KSNode): JsonElement {
        if (classDeclaration.typeParameters.isNotEmpty()) {
            fail(node, "Generic serializable DTOs are not supported in v1 OpenAPI generation.")
        }

        val ctorDefaults = classDeclaration.primaryConstructor
            ?.parameters
            ?.associate { it.name?.asString().orEmpty() to it.hasDefault }
            .orEmpty()

        val properties = linkedMapOf<String, JsonElement>()
        val required = mutableListOf<JsonPrimitive>()
        for (property in classDeclaration.getAllProperties()) {
            if (property.isStatic()) {
                continue
            }
            properties[property.simpleName.asString()] = resolveType(property.type.resolve(), property)
            val hasDefault = ctorDefaults[property.simpleName.asString()] == true
            if (property.type.resolve().nullability != Nullability.NULLABLE && !hasDefault) {
                required += JsonPrimitive(property.simpleName.asString())
            }
        }

        val entries = linkedMapOf<String, JsonElement>(
            "type" to JsonPrimitive("object"),
            "properties" to JsonObject(properties),
        )
        if (required.isNotEmpty()) {
            entries["required"] = JsonArray(required)
        }
        return JsonObject(entries)
    }

    private fun componentRef(type: KSType, declaration: KSClassDeclaration, schema: JsonElement): JsonElement {
        val key = canonicalTypeKey(type)
        val componentName = componentNamesByType.getOrPut(key) {
            val simpleName = declaration.simpleName.asString()
            val qualifiedName = declaration.qualifiedName?.asString().orEmpty()
            val conflict = componentNamesByType.values.contains(simpleName)
            if (conflict) qualifiedName.replace('.', '_') else simpleName
        }
        componentSchemas.putIfAbsent(componentName, schema)
        return JsonObject(
            mapOf("\$ref" to JsonPrimitive("#/components/schemas/$componentName"))
        )
    }

    private fun canonicalTypeKey(type: KSType): String {
        val declaration = type.declaration.qualifiedName?.asString().orEmpty()
        if (type.arguments.isEmpty()) {
            return declaration
        }
        val arguments = type.arguments.joinToString(",") { argument ->
            argument.type?.resolve()?.let(::canonicalTypeKey) ?: "*"
        }
        return "$declaration<$arguments>"
    }

    private fun typedSchema(type: String, format: String? = null): JsonElement {
        val entries = linkedMapOf<String, JsonElement>("type" to JsonPrimitive(type))
        if (format != null) {
            entries["format"] = JsonPrimitive(format)
        }
        return JsonObject(entries)
    }

    private fun nullableSchema(schema: JsonElement): JsonElement {
        return JsonObject(
            mapOf(
                "anyOf" to JsonArray(
                    listOf(
                        schema,
                        JsonObject(mapOf("type" to JsonPrimitive("null"))),
                    )
                )
            )
        )
    }

    private fun fail(node: KSNode, message: String): Nothing {
        logger.error(message, node)
        throw ProcessorException(message)
    }
}

internal class GeneratedRegistryWriter(
    private val codeGenerator: CodeGenerator,
) {

    fun write(document: OpenApiDocument) {
        val json = Json {
            prettyPrint = true
            explicitNulls = false
            encodeDefaults = true
        }
        val encoded = json.encodeToString(document)
        val dependencies = Dependencies(false)
        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = GENERATED_PACKAGE,
            fileName = GENERATED_FILE,
        ).bufferedWriter().use { writer ->
            val documentJsonChunks = encoded.chunked(8_000)
            writer.appendLine("package $GENERATED_PACKAGE")
            writer.appendLine()
            writer.appendLine("import app.openapi.model.OpenApiDocument")
            writer.appendLine("import kotlinx.serialization.json.Json")
            writer.appendLine()
            writer.appendLine("object GeneratedOpenApiRegistry {")
            writer.appendLine("    private val DOCUMENT_JSON: String by lazy {")
            writer.appendLine("        buildString(${encoded.length}) {")
            documentJsonChunks.forEach { chunk ->
                writer.appendLine("            append(${chunk.asKotlinStringLiteral()})")
            }
            writer.appendLine("        }")
            writer.appendLine("    }")
            writer.appendLine()
            writer.appendLine("    val document: OpenApiDocument by lazy {")
            writer.appendLine("        Json.decodeFromString<OpenApiDocument>(DOCUMENT_JSON)")
            writer.appendLine("    }")
            writer.appendLine("}")
        }
    }
}

internal data class ParsedFunction(
    val symbol: KSFunctionDeclaration,
    val qualifiedName: String,
    val operation: ParsedOperation?,
    val routeRef: String?,
    val queryParams: List<ParsedParam>,
    val pathParams: List<ParsedParam>,
    val headerParams: List<ParsedParam>,
    val requestBody: ParsedRequestBody?,
    val responses: List<ParsedResponse>,
    val security: List<SecurityDoc>,
) {

    fun hasApiDetails(): Boolean {
        return queryParams.isNotEmpty() ||
            pathParams.isNotEmpty() ||
            headerParams.isNotEmpty() ||
            requestBody != null ||
            responses.isNotEmpty() ||
            security.isNotEmpty()
    }

    fun isHandlerLike(): Boolean {
        val parameter = symbol.parameters.singleOrNull() ?: return false
        val parameterType = parameter.type.resolve().declaration.qualifiedName?.asString()
        return symbol.modifiers.contains(Modifier.SUSPEND) &&
            symbol.extensionReceiver == null &&
            parameterType == ROUTING_CONTEXT
    }

    fun isRouteWrapperLike(): Boolean {
        val receiverType = symbol.extensionReceiver?.resolve()?.declaration?.qualifiedName?.asString()
        if (receiverType == ROUTING) {
            return true
        }
        return symbol.parameters.any { it.type.resolve().declaration.qualifiedName?.asString() == ROUTING }
    }
}

internal data class ParsedOperation(
    val method: String,
    val path: String,
    val summary: String,
    val description: String,
    val tags: List<String>,
    val operationId: String,
)

internal data class ParsedParam(
    val source: KSNode,
    val name: String,
    val type: KSType,
    val required: Boolean,
    val description: String,
)

internal data class ParsedRequestBody(
    val required: Boolean,
    val description: String,
    val contentType: String,
    val schema: SchemaSpec,
)

internal data class ParsedResponse(
    val code: Int,
    val description: String,
    val contentType: String,
    val schema: SchemaSpec,
)

internal data class ParameterDoc(
    val name: String,
    val location: String,
    val required: Boolean,
    val description: String,
    val schema: JsonElement,
)

internal data class RequestBodyDoc(
    val required: Boolean,
    val description: String,
    val contentType: String,
    val schema: JsonElement,
)

internal data class ResponseDoc(
    val code: Int,
    val description: String,
    val contentType: String,
    val schema: JsonElement?,
)

internal data class SecurityDoc(
    val name: String,
    val scopes: List<String>,
)

internal data class EffectiveOperation(
    val source: KSNode,
    val method: String,
    val path: String,
    val summary: String?,
    val description: String?,
    val operationId: String?,
    val tags: List<String>,
    val parameters: List<ParameterDoc>,
    val requestBody: RequestBodyDoc?,
    val responses: List<ResponseDoc>,
    val security: List<SecurityDoc>,
)

internal data class SchemaSpec(
    val type: KSType?,
    val schemaRef: String,
    val schemaJson: String,
) {

    fun mode(): SchemaMode {
        val modes = buildList {
            if (type != null) add(SchemaMode.AUTO)
            if (schemaRef.isNotBlank()) add(SchemaMode.REF)
            if (schemaJson.isNotBlank()) add(SchemaMode.JSON)
        }
        return when {
            modes.size == 1 -> modes.single()
            modes.isEmpty() -> SchemaMode.NONE
            else -> SchemaMode.INVALID
        }
    }

    fun isEmpty(): Boolean = mode() == SchemaMode.NONE
}

internal enum class SchemaMode {
    NONE,
    AUTO,
    REF,
    JSON,
    INVALID,
}

internal enum class SchemaUsage {
    REQUEST_BODY,
    RESPONSE,
}

internal fun SchemaSpec.validated(annotation: KSAnnotation, usage: SchemaUsage, logger: KSPLogger): SchemaSpec {
    val error = validateSchemaSpec(this, usage)
    if (error != null) {
        logger.error(error, annotation)
        throw ProcessorException(error)
    }
    return this
}

internal class ProcessorException(message: String) : RuntimeException(message)

private val PATH_PARAM_REGEX = "\\{([^}/]+)}".toRegex()
private val HTTP_METHODS = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
private const val ROUTING_CONTEXT = "io.ktor.server.routing.RoutingContext"
private const val ROUTING = "io.ktor.server.routing.Routing"
private const val GENERATED_PACKAGE = "app.openapi.generated"
private const val GENERATED_FILE = "GeneratedOpenApiRegistry"

private fun qualifiedNameOf(function: KSFunctionDeclaration): String {
    val parent = function.parentDeclaration?.qualifiedName?.asString()
    val name = function.simpleName.asString()
    return if (parent != null) "$parent.$name" else name
}

internal fun validatePathTemplate(path: String, declaredParams: Set<String>) {
    val placeholders = PATH_PARAM_REGEX.findAll(path).map { it.groupValues[1] }.toSet()
    if (placeholders != declaredParams) {
        throw IllegalArgumentException(
            "Path placeholders $placeholders do not match declared @ApiPathParam names $declaredParams."
        )
    }
}

internal fun duplicateMethodPath(operations: Iterable<Pair<String, String>>): Pair<String, String>? {
    val seen = mutableSetOf<Pair<String, String>>()
    for (operation in operations) {
        if (!seen.add(operation)) {
            return operation
        }
    }
    return null
}

internal fun validateSchemaSpec(schema: SchemaSpec, usage: SchemaUsage): String? {
    return when (schema.mode()) {
        SchemaMode.INVALID -> "Exactly one schema source must be provided: type, schemaRef, or schemaJson."
        SchemaMode.NONE -> if (usage == SchemaUsage.REQUEST_BODY) {
            "@ApiRequestBody requires type, schemaRef, or schemaJson."
        } else {
            null
        }
        SchemaMode.AUTO,
        SchemaMode.REF,
        SchemaMode.JSON -> null
    }
}

private fun KSFunctionDeclaration.findAnnotation(qualifiedName: String): KSAnnotation? {
    return annotations.firstOrNull { it.annotationName() == qualifiedName }
}

private fun KSFunctionDeclaration.findAnnotations(qualifiedName: String): List<KSAnnotation> {
    return annotations.filter { it.annotationName() == qualifiedName }.toList()
}

private fun KSAnnotation.annotationName(): String? {
    return annotationType.resolve().declaration.qualifiedName?.asString()
}

private inline fun <reified T> KSAnnotation.argument(name: String): T {
    @Suppress("UNCHECKED_CAST")
    return arguments.first { it.name?.asString() == name }.value as T
}

private fun KSAnnotation.kClassArgument(name: String): KSType {
    return kClassArgumentOrNull(name)
        ?: throw ProcessorException("Expected KClass argument `$name`.")
}

private fun KSAnnotation.kClassArgumentOrNull(name: String): KSType? {
    return arguments.firstOrNull { it.name?.asString() == name }?.value as? KSType
}

private fun KSTypeArgument.requireType(node: KSNode, message: String): KSType {
    return type?.resolve() ?: throw ProcessorException("$message at ${node.location}")
}

private fun KSType.isUnitType(): Boolean {
    return declaration.qualifiedName?.asString() == "kotlin.Unit"
}

private fun KSClassDeclaration.hasAnnotation(qualifiedName: String): Boolean {
    return annotations.any { it.annotationName() == qualifiedName }
}

private fun KSPropertyDeclaration.isStatic(): Boolean {
    return extensionReceiver != null
}

private fun JsonElement.asKotlinStringLiteral(): String {
    return toString().asKotlinStringLiteral()
}

private fun String.asKotlinStringLiteral(): String {
    return buildString(length + 2) {
        append('"')
        this@asKotlinStringLiteral.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '$' -> append("\\$")
                else -> append(char)
            }
        }
        append('"')
    }
}
