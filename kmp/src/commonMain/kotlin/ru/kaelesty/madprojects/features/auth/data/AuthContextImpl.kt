package ru.kaelesty.madprojects.features.auth.data

import domain.auth.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.api.auth.Tokens
import ru.kaelesty.madprojects.features.auth.data.storage.AuthStorage
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.RefreshUseCase
import ru.kaelesty.madprojects.utils.KLogger
import ru.kaelesty.madprojects.utils.nowMillis
import kotlin.time.ExperimentalTime

class AuthContextImpl(
    private val refreshUseCase: RefreshUseCase? = null,
    private val storage: AuthStorage? = null,
): AuthContext {

    private val scope: CoroutineScope by lazy { CoroutineScope(Dispatchers.Main) }

    private val _tokens = MutableStateFlow<Tokens?>(null)
    private val isAuthenticatedState: StateFlow<Boolean> by lazy {
        _tokens
            .map { it != null }
            .distinctUntilChanged()
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )
    }
    private val userTypeState = MutableStateFlow<UserType?>(null)
    override val isAuthenticated: StateFlow<Boolean>
        get() = isAuthenticatedState
    override val userType: StateFlow<UserType?>
        get() = userTypeState

    init {
        KLogger.d(TAG) { "init: loading session from storage" }
        tryLoadSessionFromStorage()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getAccessToken(): String? {
        val tokens = _tokens.value ?: run {
            KLogger.d(TAG) { "getAccessToken: no session" }
            return null
        }
        val now = nowMillis()
        return if (tokens.accessExpiresAt > now) {
            KLogger.d(TAG) { "getAccessToken: token valid, expiresAt=${tokens.accessExpiresAt}" }
            tokens.accessToken
        } else {
            KLogger.w(TAG) { "getAccessToken: access expired, trying refresh" }
            tryRefresh(tokens)
        }
    }

    override fun onUnauthorizedResponse() {
        KLogger.w(TAG) { "onUnauthorizedResponse: invalidating auth" }
        invalidateAuth()
    }

    override fun onAuthorized(tokens: Tokens, userType: UserType) {
        KLogger.i(TAG) { "onAuthorized: userType=$userType accessExpiresAt=${tokens.accessExpiresAt} refreshExpiresAt=${tokens.refreshExpiresAt}" }
        storage?.let { storage ->
            scope.launch {
                storage.save(
                    item = AuthStorage.Item(
                        tokens = tokens,
                        userType = userType,
                    )
                )
            }
        }
        provideAuth(tokens, userType)
    }

    override fun logout() {
        KLogger.i(TAG) { "logout: invalidating auth by user action" }
        invalidateAuth()
    }

    private fun provideAuth(tokens: Tokens, userType: UserType) {
        KLogger.d(TAG) { "provideAuth: userType=$userType" }
        scope.launch {
            _tokens.emit(tokens)
            userTypeState.emit(userType)
        }
    }

    private fun invalidateAuth() {
        KLogger.d(TAG) { "invalidateAuth" }
        scope.launch {
            storage?.clear()
            _tokens.emit(null)
            userTypeState.emit(null)
        }
    }

    private suspend fun tryRefresh(tokens: Tokens): String? {
        if (tokens.refreshExpiresAt <= nowMillis()) {
            KLogger.w(TAG) { "tryRefresh: refresh token expired" }
            invalidateAuth()
            return null
        }
        val useCase = refreshUseCase ?: run {
            KLogger.w(TAG) { "tryRefresh: refresh use case is missing" }
            invalidateAuth()
            return null
        }
        KLogger.d(TAG) { "tryRefresh: calling refresh use case" }
        val result = runCatching {
            useCase.refresh(tokens.refreshToken)
        }.getOrElse { error ->
            KLogger.e(TAG, error) { "refresh failed" }
            invalidateAuth()
            return null
        }
        return when (result) {
            is RefreshUseCase.Result.Success -> {
                KLogger.i(TAG) { "tryRefresh: success, userType=${result.userType}" }
                onAuthorized(result.tokens, result.userType)
                result.tokens.accessToken
            }
            RefreshUseCase.Result.Fail -> {
                KLogger.w(TAG) { "tryRefresh: failed" }
                invalidateAuth()
                null
            }
        }
    }

    private fun tryLoadSessionFromStorage() {
        val storage = storage ?: return
        scope.launch {
            storage.load()?.let { savedSession ->
                // todo check expiration
                provideAuth(
                    tokens = savedSession.tokens,
                    userType = savedSession.userType,
                )
                KLogger.d(TAG) { "tryLoadSessionFromStorage succeed" }
            } ?: KLogger.d(TAG) { "tryLoadSessionFromStorage failed -> session is null" }
        }
    }

    private companion object {
        private const val TAG = "AuthContextImpl"
    }
}
