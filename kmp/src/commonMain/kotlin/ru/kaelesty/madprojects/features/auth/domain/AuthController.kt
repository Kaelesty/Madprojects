package ru.kaelesty.madprojects.features.auth.domain

interface AuthController {

    fun onUnauthorizedResponse()

    fun onAuthorized(tokens: Tokens)
}