package ru.kaelesty.madprojects

import domain.auth.UserType
import platform.Foundation.NSUserDefaults
import ru.kaelesty.madprojects.api.auth.Tokens
import ru.kaelesty.madprojects.features.auth.data.storage.AuthStorage

class AppleAuthStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : AuthStorage {

    override suspend fun save(item: AuthStorage.Item) {
        defaults.setObject(item.tokens.refreshToken, KEY_REFRESH_TOKEN)
        defaults.setObject(item.tokens.accessToken, KEY_ACCESS_TOKEN)
        defaults.setDouble(item.tokens.accessExpiresAt.toDouble(), KEY_ACCESS_EXPIRES_AT)
        defaults.setDouble(item.tokens.refreshExpiresAt.toDouble(), KEY_REFRESH_EXPIRES_AT)
        defaults.setObject(item.userType.name, KEY_USER_TYPE)
    }

    override suspend fun load(): AuthStorage.Item? {
        val refreshToken = defaults.stringForKey(KEY_REFRESH_TOKEN) ?: return null
        val accessToken = defaults.stringForKey(KEY_ACCESS_TOKEN) ?: return null
        val userTypeName = defaults.stringForKey(KEY_USER_TYPE) ?: return null
        if (defaults.objectForKey(KEY_ACCESS_EXPIRES_AT) == null) return null
        if (defaults.objectForKey(KEY_REFRESH_EXPIRES_AT) == null) return null

        val userType = runCatching {
            UserType.valueOf(userTypeName)
        }.getOrNull() ?: return null

        return AuthStorage.Item(
            tokens = Tokens(
                refreshToken = refreshToken,
                accessToken = accessToken,
                accessExpiresAt = defaults.doubleForKey(KEY_ACCESS_EXPIRES_AT).toLong(),
                refreshExpiresAt = defaults.doubleForKey(KEY_REFRESH_EXPIRES_AT).toLong(),
            ),
            userType = userType,
        )
    }

    override suspend fun clear() {
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        defaults.removeObjectForKey(KEY_ACCESS_TOKEN)
        defaults.removeObjectForKey(KEY_ACCESS_EXPIRES_AT)
        defaults.removeObjectForKey(KEY_REFRESH_EXPIRES_AT)
        defaults.removeObjectForKey(KEY_USER_TYPE)
    }

    private companion object {
        private const val KEY_REFRESH_TOKEN = "auth.refreshToken"
        private const val KEY_ACCESS_TOKEN = "auth.accessToken"
        private const val KEY_ACCESS_EXPIRES_AT = "auth.accessExpiresAt"
        private const val KEY_REFRESH_EXPIRES_AT = "auth.refreshExpiresAt"
        private const val KEY_USER_TYPE = "auth.userType"
    }
}
