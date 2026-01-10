package ru.kaelesty.madprojects.features.projectcreate.data

import domain.project.AvailableCurator
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.utils.KLogger

class CuratorsApi(
    private val client: HttpClient,
) {
    suspend fun getCurators(accessToken: String): CuratorsApiResponse? {
        return runCatching {
            val response = client.get(CuratorsPath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                expectSuccess = false
            }
            KLogger.d(TAG) { "Curators response status=${response.status}" }
            val curators = if (response.status == HttpStatusCode.OK) {
                response.body<List<AvailableCurator>>()
            } else {
                null
            }
            val errorMessage = if (response.status != HttpStatusCode.OK) {
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
            CuratorsApiResponse(
                status = response.status,
                curators = curators,
                errorMessage = errorMessage,
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Curators request failed" }
            null
        }
    }

    data class CuratorsApiResponse(
        val status: HttpStatusCode,
        val curators: List<AvailableCurator>? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-CuratorsApi"
        const val CuratorsPath = "/project/curators"
    }
}
