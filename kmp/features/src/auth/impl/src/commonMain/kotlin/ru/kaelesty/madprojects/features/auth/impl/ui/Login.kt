package ru.kaelesty.madprojects.features.auth.impl.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Login(
    toRegister: () -> Unit
) {
    Box {
        BasicText("Login", modifier = Modifier.onClick { toRegister() })
    }
}