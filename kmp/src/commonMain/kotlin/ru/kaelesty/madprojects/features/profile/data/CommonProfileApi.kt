package ru.kaelesty.madprojects.features.profile.data

import domain.profile.CommonProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.utils.KLogger

class CommonProfileApi(
    private val client: HttpClient,
) {
    suspend fun getCommonProfile(accessToken: String): CommonProfileApiResponse? {
        return runCatching {
            val response = client.get(CommonProfilePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                expectSuccess = false
            }
            KLogger.d(TAG) { "Common profile response status=${response.status}" }
            val profile = if (response.status == HttpStatusCode.OK) {
                response.body<CommonProfileResponse>()
            } else {
                null
            }
            CommonProfileApiResponse(status = response.status, profile = profile)
        }.getOrElse {
            KLogger.e(TAG, it) { "Common profile request failed" }
            null
        }
    }

    data class CommonProfileApiResponse(
        val status: HttpStatusCode,
        val profile: CommonProfileResponse? = null,
    )

    private companion object {
        private const val TAG = "ktor-CommonProfileApi"
        const val CommonProfilePath = "/commonProfile"
    }
}
