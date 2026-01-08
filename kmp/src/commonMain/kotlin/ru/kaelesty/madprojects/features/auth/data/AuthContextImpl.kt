package ru.kaelesty.madprojects.features.auth.data

import domain.auth.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.Tokens

class AuthContextImpl: AuthContext {

    private val scope: CoroutineScope by lazy { CoroutineScope(Dispatchers.Main) }

    private val _tokens = MutableStateFlow<Tokens?>(null)
    private val userTypeState = MutableStateFlow<UserType?>(null)
    override val isAuthenticated: StateFlow<Boolean>
        get() = _tokens.map { it != null }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )
    override val userType: StateFlow<UserType?>
        get() = userTypeState
    override val tokens: StateFlow<Tokens?>
        get() = _tokens

    fun provideAuth(tokens: Tokens, userType: UserType) {
        scope.launch {
            _tokens.emit(tokens)
            userTypeState.emit(userType)
        }
    }

    fun invalidateAuth() {
        scope.launch {
            _tokens.emit(null)
            userTypeState.emit(null)
        }
    }
}
