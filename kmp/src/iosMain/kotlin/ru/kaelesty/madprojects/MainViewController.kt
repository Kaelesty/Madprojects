package ru.kaelesty.madprojects

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem
import ru.kaelesty.madprojects.features.checkin.sdk.CheckInNavItem

fun MainViewController(): UIViewController = ComposeUIViewController {
    App(
        navItems = listOf(
            CheckInNavItem(),
            AuthNavItem(),
        )
    )
}

