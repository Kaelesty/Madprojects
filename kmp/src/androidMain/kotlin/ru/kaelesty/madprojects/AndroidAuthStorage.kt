package ru.kaelesty.madprojects

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import domain.auth.UserType
import ru.kaelesty.madprojects.features.auth.data.storage.AuthStorage
import ru.kaelesty.madprojects.utils.KLogger

class AndroidAuthStorage(
    private val context: Context,
): AuthStorage {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun save(item: AuthStorage.Item) {
        KLogger.d(TAG) { "saving with userType=${item.userType.name}" }
        prefs.edit()
            .putString(ACCESS_KEY, item.access)
            .putString(REFRESH_KEY, item.refresh)
            .putString(USER_TYPE_KEY, item.userType.name)
            .apply()
    }

    override suspend fun load(): AuthStorage.Item? {
        val access = prefs.getString(ACCESS_KEY, null)
        val refresh = prefs.getString(REFRESH_KEY, null)
        val userType = runCatching {
            prefs.getString(USER_TYPE_KEY, null)?.let { name ->
                UserType.valueOf(name)
            }
        }.getOrElse {
            KLogger.e(TAG, it) { "exception when decoding userType" }
            null
        }
        if (access == null) {
            KLogger.w(TAG) { "access is null, return null" }
            return null
        }
        if (refresh == null) {
            KLogger.w(TAG) { "refresh is null, return null" }
            return null
        }
        if (userType == null) {
            KLogger.w(TAG) { "userType is null, return null" }
            return null
        }
        return AuthStorage.Item(access, refresh, userType)
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
        KLogger.d(TAG) { "cleared" }
    }

    companion object {
        private const val TAG = "AndroidAuthStorage"

        private const val FILE_NAME = "secure_tokens"
        private const val ACCESS_KEY = "access"
        private const val REFRESH_KEY = "refresh"
        private const val USER_TYPE_KEY = "userType"
    }
}