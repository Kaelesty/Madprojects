package ru.kaelesty.madprojects.features.auth.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem

@Composable
fun RegisterScreen(
    navigator: AuthNavItem.Navigator
) {
    Box {
        BasicText("Register", modifier = Modifier.clickable { navigator.toLogin() })
    }
}

