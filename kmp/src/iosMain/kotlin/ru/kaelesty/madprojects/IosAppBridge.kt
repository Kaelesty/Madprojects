package ru.kaelesty.madprojects

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import ru.kaelesty.madprojects.di.getKoin
import ru.kaelesty.madprojects.di.initKoin
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.GithubOauthBridge
import ru.kaelesty.madprojects.navigation.NavItem

class IosAppBridge {

    fun mainViewController(): UIViewController {
        ensureKoinStarted()
        val koin = getKoin()
        val navItems = koin.getAll<NavItem>()
        val authContext = koin.get<AuthContext>()

        return ComposeUIViewController {
            App(
                navItems = navItems,
                authContext = authContext,
            )
        }
    }

    fun handleOpenUrl(url: String): Boolean {
        ensureKoinStarted()
        return GithubOauthBridge.handleIncomingUrl(url)
    }

    private fun ensureKoinStarted() {
        initKoin(platformModules = listOf(iosModule))
    }
}
