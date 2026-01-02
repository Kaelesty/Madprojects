package ru.kaelesty.madprojects.features.auth.domain

import kotlinx.coroutines.flow.StateFlow

interface AuthContext {

    val isAuthenticated: StateFlow<Boolean>
}