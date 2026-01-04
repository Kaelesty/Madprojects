package ru.kaelesty.madprojects.features.auth.data

import androidx.compose.runtime.ProvidedValue
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

    private val tokens = MutableStateFlow<Tokens?>(null)
    override val isAuthenticated: StateFlow<Boolean>
        get() = tokens.map { it != null }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    fun provideTokens(newValue: Tokens) {
        scope.launch {
            tokens.emit(newValue)
        }
    }

    fun invalidateTokens() {
        scope.launch {
            tokens.emit(null)
        }
    }
}