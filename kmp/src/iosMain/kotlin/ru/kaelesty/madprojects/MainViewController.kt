package ru.kaelesty.madprojects

import androidx.compose.ui.window.ComposeUIViewController
import ru.kaelesty.madprojects.features.auth.data.AuthContextImpl
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem
import ru.kaelesty.madprojects.features.checkin.sdk.CheckInNavItem
import ru.kaelesty.madprojects.features.profile.sdk.ProfileNavItem

fun MainViewController(): UIViewController = ComposeUIViewController {
    val authContext = AuthContextImpl()
    App(
        navItems = listOf(
            CheckInNavItem(),
            AuthNavItem(),
            ProfileNavItem(authContext),
        ),
        authContext = authContext,
    )
}

