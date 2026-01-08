package ru.kaelesty.madprojects.features.auth.domain

import domain.auth.UserType

interface AuthController {

    fun onUnauthorizedResponse()

    fun onAuthorized(tokens: Tokens, userType: UserType)
}
