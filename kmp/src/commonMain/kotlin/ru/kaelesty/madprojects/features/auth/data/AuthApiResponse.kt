package ru.kaelesty.madprojects.features.auth.data

import domain.auth.UserType
import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.api.auth.Tokens

data class AuthApiResponse(
    val status: HttpStatusCode,
    val tokens: Tokens? = null,
    val userType: UserType? = null,
)
