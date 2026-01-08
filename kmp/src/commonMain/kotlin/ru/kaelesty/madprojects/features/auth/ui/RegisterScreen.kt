package ru.kaelesty.madprojects.features.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.auth.UserType
import org.koin.compose.viewmodel.koinViewModel
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.HeaderCard
import ru.kaelesty.madprojects.ui.fields.AppDropdownField
import ru.kaelesty.madprojects.ui.fields.AppPasswordField
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun RegisterScreen(
    navigator: AuthNavItem.Navigator,
    strings: RegisterStrings = RegisterStrings,
) {
    val vm = koinViewModel<RegisterViewModel>()
    val state by vm.state.collectAsState()
    val errorText = strings.errorMessage(state.error)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (event is RegisterViewModel.Event.Successful) {
                navigator.toProfile()
            }
        }
    }

    val dataLabel = if (state.userType == UserType.Common) {
        strings.GroupLabel
    } else {
        strings.PositionLabel
    }
    val dataPlaceholder = if (state.userType == UserType.Common) {
        strings.GroupPlaceholder
    } else {
        strings.PositionPlaceholder
    }

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
                HeaderCard(headerText = StringResources.AppName) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = strings.Title,
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(
                                color = Palette.OnCard,
                                fontFamily = Roboto,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        if (state.isFirstPage) {
                            AppTextField(
                                label = strings.UsernameLabel,
                                value = state.username,
                                onValueChange = vm::setUsername,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = strings.UsernamePlaceholder
                            )

                            Spacer(Modifier.height(12.dp))

                            AppTextField(
                                label = strings.LastNameLabel,
                                value = state.lastName,
                                onValueChange = vm::setLastName,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(12.dp))

                            AppTextField(
                                label = strings.FirstNameLabel,
                                value = state.firstName,
                                onValueChange = vm::setFirstName,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(12.dp))

                            AppTextField(
                                label = strings.SecondNameLabel,
                                value = state.secondName,
                                onValueChange = vm::setSecondName,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (errorText != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = errorText,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                    fontFamily = Roboto,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            PrimaryActionButton(
                                text = strings.NextButton,
                                onClick = vm::nextPage,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            AppDropdownField(
                                label = strings.UserTypeLabel,
                                options = UserType.values().toList(),
                                selected = state.userType,
                                onSelect = vm::setUserType,
                                optionLabel = strings::userTypeLabel,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            AppTextField(
                                label = dataLabel,
                                value = state.data,
                                onValueChange = vm::setData,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = dataPlaceholder
                            )

                            Spacer(Modifier.height(12.dp))

                            AppTextField(
                                label = strings.EmailLabel,
                                value = state.email,
                                onValueChange = vm::setEmail,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = strings.EmailPlaceholder,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                            )

                            Spacer(Modifier.height(12.dp))

                            AppPasswordField(
                                label = strings.PasswordLabel,
                                value = state.password,
                                onValueChange = vm::setPassword,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = strings.PasswordPlaceholder,
                                imeAction = ImeAction.Done
                            )

                            if (errorText != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = errorText,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                    fontFamily = Roboto,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            PrimaryActionButton(
                                text = strings.RegisterButton,
                                onClick = vm::submit,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            TextButton(
                                onClick = vm::previousPage,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Palette.AccentBlue
                                )
                            ) {
                                Text(
                                    text = strings.BackButton,
                                    fontFamily = Roboto
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

