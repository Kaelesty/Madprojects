package app.openapi.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiOperation(
    val method: String,
    val path: String,
    val summary: String = "",
    val description: String = "",
    val tags: Array<String> = [],
    val operationId: String = "",
)

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiQueryParam(
    val name: String,
    val type: KClass<*>,
    val required: Boolean = false,
    val description: String = "",
)

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiPathParam(
    val name: String,
    val type: KClass<*>,
    val description: String = "",
)

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiHeaderParam(
    val name: String,
    val type: KClass<*> = String::class,
    val required: Boolean = false,
    val description: String = "",
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiRequestBody(
    val type: KClass<*> = Unit::class,
    val required: Boolean = true,
    val description: String = "",
    val contentType: String = "application/json",
    val schemaRef: String = "",
    val schemaJson: String = "",
)

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiResponse(
    val code: Int,
    val description: String,
    val type: KClass<*> = Unit::class,
    val contentType: String = "application/json",
    val schemaRef: String = "",
    val schemaJson: String = "",
)

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiSecurity(
    val name: String,
    val scopes: Array<String> = [],
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiRouteRef(
    val handler: String,
)
