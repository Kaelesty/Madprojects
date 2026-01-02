package ru.kaelesty.madprojects.features.auth.sdk

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpCallValidatorConfig
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthController

class UnauthorizedHandler(
    private val authController: AuthController
) {

    fun configure(
        config: HttpCallValidatorConfig,
    ) {
        config.validateResponse { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                authController.onUnauthorizedResponse()
                throw ClientRequestException(response, "Unauthorized")
            }
        }
    }

}
