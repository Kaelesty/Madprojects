package ru.kaelesty.madprojects.features.auth.domain

import domain.auth.UserType
import kotlinx.coroutines.flow.StateFlow
import ru.kaelesty.madprojects.api.auth.Tokens

interface AuthContext {

    val isAuthenticated: StateFlow<Boolean>

    val userType: StateFlow<UserType?>

    val tokens: StateFlow<Tokens?>
}
