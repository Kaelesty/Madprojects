package ru.kaelesty.madprojects.features.auth.sdk

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpCallValidatorConfig
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext

class UnauthorizedHandler(
    private val authContext: AuthContext
) {

    fun configure(
        config: HttpCallValidatorConfig,
    ) {
        config.validateResponse { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                authContext.onUnauthorizedResponse()
                throw ClientRequestException(response, "Unauthorized")
            }
        }
    }

}
