---
name: annotate-openapi-endpoint
description: "Add custom OpenAPI annotations to backend API handlers or named route-wrapper functions in the Madprojects backend. Use when Codex needs to cover an existing endpoint with `app.openapi.annotations` without changing endpoint behavior: choose handler-vs-wrapper annotation placement, add request/response/parameter/security metadata, satisfy the KSP processor constraints, and validate that `/openapi.json` still builds correctly."
---

# Annotate OpenAPI Endpoint

Cover one backend endpoint at a time with the custom OpenAPI framework implemented in this repo.

Prefer annotating the feature handler directly. Use route-wrapper mode only when the route metadata must live on a named routing function and it is worth extracting the current inline lambda.

## Quick workflow

1. Find the route registration in `backend/src/main/kotlin/app/Application.kt` and the handler it calls.
2. Decide annotation target:
   - Use handler-level annotations for existing `suspend fun x(rc: RoutingContext)` methods in `backend/src/main/kotlin/app/features/...`.
   - Use a named route-wrapper only if the endpoint currently needs route-level metadata outside the handler and you are willing to replace the inline lambda with a named function.
3. Read the available annotation contracts in `backend-openapi-api/src/main/kotlin/app/openapi/annotations/ApiAnnotations.kt`.
4. Add the smallest correct annotation set:
   - Always add `@ApiOperation`.
   - Add `@ApiQueryParam`, `@ApiPathParam`, `@ApiHeaderParam` only for real inputs.
   - Add `@ApiRequestBody` only when the endpoint consumes a body.
   - Add one or more `@ApiResponse`.
   - Add `@ApiSecurity` for authenticated endpoints, usually `auth-jwt`.
   - Add `@ApiRouteRef` only on a named route-wrapper function.
5. Build and validate with `./gradlew :backend:build`.

## Prefer handler-level annotations

Use handler-level annotations by default because the current codebase mostly registers routes with inline lambdas in `Application.kt`.

Typical shape:

```kotlin
@ApiOperation(
    method = "POST",
    path = "/auth/login",
    summary = "Authenticate user"
)
@ApiRequestBody(type = LoginRequest::class, description = "Credentials")
@ApiResponse(code = 200, description = "Authenticated", type = AuthorizedResponse::class)
@ApiResponse(code = 403, description = "Invalid credentials")
suspend fun login(rc: RoutingContext)
```

Use the real route path from `Application.kt`, not a guessed path.

## Use route-wrapper mode only when needed

The v1 framework does not support anonymous route lambdas. If wrapper-level annotations are required, first extract a named routing function, then annotate it.

Pattern:

```kotlin
@ApiOperation(method = "GET", path = "/example", summary = "Example")
@ApiRouteRef(handler = "app.features.example.ExampleFeatureImpl.getExample")
fun Routing.installExampleRoute(feature: ExampleFeature) {
    get("/example") {
        feature.getExample(this)
    }
}
```

Rules:

- Keep `@ApiOperation` on the wrapper.
- Keep params/request body/responses on the handler.
- Do not duplicate full operation definitions on both wrapper and handler unless they are linked by `@ApiRouteRef`.

## Annotation rules that matter

Follow these processor constraints exactly. They are enforced at compile time by `backend-openapi-processor`.

- Path must start with `/`.
- `@ApiPathParam` names must match `{placeholders}` in `@ApiOperation.path` exactly.
- `@ApiRequestBody` must define exactly one schema source:
  `type`, `schemaRef`, or `schemaJson`.
- `@ApiResponse` may omit schema entirely for empty/no-body responses.
- If a response/request schema is type-based, the type must be supported by the processor.
- Do not declare the same response code twice on one function.
- Do not create duplicate `method + path` combinations across annotated operations.

## Supported automatic schema generation

Prefer `type = SomeDto::class` when the DTO fits the v1 processor.

Supported:

- Kotlin primitives, `String`, `Boolean`
- nullable fields
- enums
- `@Serializable` data classes
- nested `@Serializable` data classes
- `List<T>`, `Set<T>`
- `Map<String, T>`

Not supported automatically:

- sealed or polymorphic hierarchies
- unresolved generics
- inheritance-driven schema composition
- map keys other than `String`

If the DTO is unsupported, switch to:

- `schemaRef = "ExistingSchemaName"` when you need a component reference
- `schemaJson = """{...}"""` when you need an inline manual schema

## How to derive annotations from existing code

Read the handler body before annotating.

- Derive query parameters from `call.parameters[...]` or `call.request.queryParameters[...]`.
- Derive header parameters from `call.request.headers[...]`.
- Derive request body from `call.receive<T>()`.
- Derive success and error responses from `call.respond(...)`, `call.respondText(...)`, `call.respondRedirect(...)`, and explicit `HttpStatusCode` branches.
- Add `@ApiSecurity(name = "auth-jwt")` if the route is inside `authenticate("auth-jwt")`.

Prefer documenting only observable behavior. Do not invent responses that the endpoint does not actually return.

## Minimal completion checklist

- Import annotations from `app.openapi.annotations`.
- Use the exact route path and HTTP method.
- Keep endpoint behavior unchanged unless extracting a named wrapper is required.
- Keep descriptions short and factual.
- Use handler-level annotations unless wrapper mode is clearly justified.
- Rebuild with `./gradlew :backend:build`.
- If the endpoint is the first documented one, optionally inspect `/openapi.json` after the build or run backend tests.
