package ru.kaelesty.madprojects.features.projectcreate.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.utils.KLogger

class VerifyRepoLinkApi(
    private val client: HttpClient,
) {
    suspend fun verify(accessToken: String, repoLink: String): VerifyRepoLinkResponse? {
        return runCatching {
            val response = client.get(VerifyRepoLinkPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter(RepoLinkParam, repoLink)
                expectSuccess = false
            }
            KLogger.d(TAG) { "Verify repo link response status=${response.status}" }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            VerifyRepoLinkResponse(
                status = response.status,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Verify repo link request failed" }
            null
        }
    }

    data class VerifyRepoLinkResponse(
        val status: HttpStatusCode,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-VerifyRepoLinkApi"
        private const val VerifyRepoLinkPath = "/github/verifyRepoLink"
        private const val RepoLinkParam = "repolink"
    }
}
