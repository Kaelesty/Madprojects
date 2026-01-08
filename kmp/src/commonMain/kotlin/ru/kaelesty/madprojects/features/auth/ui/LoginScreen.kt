package ru.kaelesty.madprojects.feature.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem
import ru.kaelesty.madprojects.features.auth.ui.LoginViewModel
import ru.kaelesty.madprojects.ui.cards.HeaderCard
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.fields.AppPasswordField
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.strings.StringResources

@Composable
fun LoginScreen(
    navigator: AuthNavItem.Navigator,
    str: StringResources = StringResources,
) {
    val vm = koinViewModel<LoginViewModel>()
    val state by vm.state.collectAsState()
    LaunchedEffect(vm) { vm.events.collect { if (it is LoginViewModel.Event.Successful) navigator.toProfile() } }

    Surface(color = Palette.Background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 280.dp, max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeaderCard(headerText = str.AppName) {
                    Text(text = str.LoginTitle, color = Palette.OnCard, fontFamily = Roboto, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))

                    AppTextField(
                        label = str.EmailLabel,
                        value = state.email,
                        onValueChange = vm::setEmail,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = str.EmailPlaceholder
                    )

                    Spacer(Modifier.height(12.dp))

                    AppPasswordField(
                        label = str.PasswordLabel,
                        value = state.password,
                        onValueChange = vm::setPassword,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = str.PasswordPlaceholder
                    )

                    val msg = state.errorMessage
                    if (msg != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(text = msg, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontFamily = Roboto, textAlign = TextAlign.Center)
                    }

                    Spacer(Modifier.height(16.dp))

                    PrimaryActionButton(
                        text = str.LoginButton,
                        onClick = vm::submit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
