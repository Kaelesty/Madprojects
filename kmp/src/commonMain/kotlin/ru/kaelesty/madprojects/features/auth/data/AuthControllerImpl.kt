package ru.kaelesty.madprojects.features.auth.data

import domain.auth.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.api.auth.Tokens
import ru.kaelesty.madprojects.features.auth.data.storage.AuthStorage
import ru.kaelesty.madprojects.features.auth.domain.AuthController
import ru.kaelesty.madprojects.utils.KLogger

class AuthControllerImpl(
    private val contextImpl: AuthContextImpl,
    private val storage: AuthStorage,
): AuthController {

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        tryLoadSessionFromStorage()
    }

    override fun onUnauthorizedResponse() {
        // todo try refresh
        contextImpl.invalidateAuth()
    }

    override fun onAuthorized(tokens: Tokens, userType: UserType) {
        scope.launch {
            storage.save(
                item = AuthStorage.Item(
                    tokens = tokens,
                    userType = userType,
                )
            )
        }
        contextImpl.provideAuth(tokens, userType)
    }

    private fun tryLoadSessionFromStorage() {
        scope.launch {
            storage.load()?.let { savedSession ->
                // todo check expiration
                contextImpl.provideAuth(
                    tokens = savedSession.tokens,
                    userType = savedSession.userType,
                )
                KLogger.d(TAG) { "tryLoadSessionFromStorage succeed" }
            } ?: KLogger.d(TAG) { "tryLoadSessionFromStorage failed -> session is null" }
        }
    }

    companion object {
        private const val TAG = "AuthControllerImpl"
    }
}
