package ru.kaelesty.madprojects.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.kaelesty.madprojects.features.auth.sdk.UnauthorizedHandler
import ru.kaelesty.madprojects.utils.KLogger

fun createKtorClient(
    baseUrl: String = KtorConfig.BaseUrl,
    unauthorizedHandler: UnauthorizedHandler? = null
): HttpClient {
    return HttpClient {
        KLogger.d(TAG) { "creating client on $baseUrl" }
        val requestLogger = Logger.DEFAULT
        fun logFailure(message: String) {
            requestLogger.log("KtorClient: $message")
        }

        defaultRequest {
            url(baseUrl)
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        HttpResponseValidator {
            unauthorizedHandler?.configure(this)
            handleResponseExceptionWithRequest { cause, request ->
                val status = (cause as? ResponseException)?.response?.status
                val statusLabel = if (status != null) {
                    "${status.value} ${status.description}"
                } else {
                    "no response"
                }
                val reason = cause.message ?: cause::class.simpleName.orEmpty()
                logFailure("${request.method.value} ${request.url} failed ($statusLabel): $reason")
            }
        }
    }
}

private const val TAG = "ktor-createKtorClient"
