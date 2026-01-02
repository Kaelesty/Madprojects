package ru.kaelesty.madprojects.features.checkin.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import ru.kaelesty.madprojects.features.checkin.sdk.CheckInNavItem
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.HeaderCard
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun CheckInScreen(
    navigator: CheckInNavItem.Navigator,
    strings: CheckInStrings = CheckInStrings,
) {
    val vm = koinViewModel<CheckInViewModel>()
    val state by vm.state.collectAsState()

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (event is CheckInViewModel.Event.Connected) {
                navigator.toMain()
            }
        }
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
                when (state) {
                    CheckInViewModel.State.Checking -> {
                        HeaderCard(headerText = StringResources.AppName) {
                            Text(
                                text = strings.CheckingTitle,
                                style = TextStyle(
                                    color = Palette.OnCard,
                                    fontFamily = Roboto,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Palette.AccentBlue,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    CheckInViewModel.State.Failed -> {
                        HeaderCard(headerText = StringResources.AppName) {
                            Text(
                                text = strings.ErrorTitle,
                                style = TextStyle(
                                    color = Palette.OnCard,
                                    fontFamily = Roboto,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(Modifier.height(16.dp))

                            PrimaryActionButton(
                                text = strings.RetryButton,
                                onClick = vm::check,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
