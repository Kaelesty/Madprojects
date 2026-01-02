package ru.kaelesty.madprojects.features.checkin.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.utils.KLogger

class CheckInApi(
    private val client: HttpClient,
) {
    suspend fun checkConnection(): Boolean {
        return runCatching {
            val response = client.get(PingPath)
            KLogger.d(TAG) { "Received response with ${response.status}" }
            response.status == HttpStatusCode.OK
        }.getOrElse {
            KLogger.e(TAG, it) { "Exception when checking connection" }
            false
        }
    }

    private companion object {
        private const val TAG = "ktor-CheckInApi"
        const val PingPath = "/checkin"
    }
}
