package ru.kaelesty.madprojects.features.auth.data

import ru.kaelesty.madprojects.features.auth.domain.AuthController
import ru.kaelesty.madprojects.features.auth.domain.Tokens

class AuthControllerImpl(
    private val contextImpl: AuthContextImpl,
): AuthController {

    override fun onUnauthorizedResponse() {
        // todo - try refresh
        contextImpl.invalidateTokens()
    }

    override fun onAuthorized(tokens: Tokens) {
        contextImpl.provideTokens(tokens)
    }
}