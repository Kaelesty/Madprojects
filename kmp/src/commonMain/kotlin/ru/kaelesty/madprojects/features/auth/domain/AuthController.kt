package ru.kaelesty.madprojects.features.auth.domain

interface AuthController {

    val authContext: AuthContext

    fun onUnauthorizedResponse()

    fun onAuthorized(tokens: Tokens)
}