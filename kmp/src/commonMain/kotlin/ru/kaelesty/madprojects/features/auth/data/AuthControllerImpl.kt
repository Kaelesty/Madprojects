package ru.kaelesty.madprojects.features.auth.data

import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.AuthController
import ru.kaelesty.madprojects.features.auth.domain.Tokens

class AuthControllerImpl: AuthController {

    override val authContext: AuthContext
        get() = TODO("Not yet implemented")

    override fun onUnauthorizedResponse() {
        TODO("Not yet implemented")
    }

    override fun onAuthorized(tokens: Tokens) {
        TODO("Not yet implemented")
    }
}