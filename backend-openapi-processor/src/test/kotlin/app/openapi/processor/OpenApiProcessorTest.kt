package app.openapi.processor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class OpenApiProcessorTest {

    @Test
    fun `validate path template accepts matching params`() {
        validatePathTemplate(
            path = "/projects/{projectId}/members/{memberId}",
            declaredParams = setOf("projectId", "memberId"),
        )
    }

    @Test
    fun `validate path template rejects mismatched params`() {
        val error = assertFailsWith<IllegalArgumentException> {
            validatePathTemplate(
                path = "/projects/{projectId}",
                declaredParams = setOf("memberId"),
            )
        }

        assertEquals(
            "Path placeholders [projectId] do not match declared @ApiPathParam names [memberId].",
            error.message,
        )
    }

    @Test
    fun `duplicate method path finds repeated operation`() {
        val duplicate = duplicateMethodPath(
            listOf(
                "GET" to "/projects",
                "POST" to "/projects",
                "GET" to "/projects",
            )
        )

        assertEquals("GET" to "/projects", duplicate)
    }

    @Test
    fun `response schema spec may be empty`() {
        val error = validateSchemaSpec(
            schema = SchemaSpec(
                type = null,
                schemaRef = "",
                schemaJson = "",
            ),
            usage = SchemaUsage.RESPONSE,
        )

        assertNull(error)
    }

    @Test
    fun `request body schema spec requires a source`() {
        val error = validateSchemaSpec(
            schema = SchemaSpec(
                type = null,
                schemaRef = "",
                schemaJson = "",
            ),
            usage = SchemaUsage.REQUEST_BODY,
        )

        assertEquals("@ApiRequestBody requires type, schemaRef, or schemaJson.", error)
    }

    @Test
    fun `schema spec rejects multiple sources`() {
        val error = validateSchemaSpec(
            schema = SchemaSpec(
                type = null,
                schemaRef = "ProjectPayload",
                schemaJson = """{"type":"object"}""",
            ),
            usage = SchemaUsage.RESPONSE,
        )

        assertEquals("Exactly one schema source must be provided: type, schemaRef, or schemaJson.", error)
    }

    @Test
    fun `schema spec accepts manual ref override`() {
        val error = validateSchemaSpec(
            schema = SchemaSpec(
                type = null,
                schemaRef = "ProjectPayload",
                schemaJson = "",
            ),
            usage = SchemaUsage.REQUEST_BODY,
        )

        assertNull(error)
    }
}
