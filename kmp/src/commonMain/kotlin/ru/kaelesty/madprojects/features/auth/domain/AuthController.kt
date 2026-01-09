package ru.kaelesty.madprojects.features.auth.domain

import domain.auth.UserType
import ru.kaelesty.madprojects.api.auth.Tokens

interface AuthController {

    fun onUnauthorizedResponse()

    fun onAuthorized(tokens: Tokens, userType: UserType)
}
