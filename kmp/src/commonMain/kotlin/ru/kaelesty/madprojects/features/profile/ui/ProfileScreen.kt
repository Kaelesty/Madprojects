package ru.kaelesty.madprojects.features.profile.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.auth.UserType
import org.koin.compose.viewmodel.koinViewModel
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.HeaderCard
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun ProfileScreen(
    authContext: AuthContext,
) {
    val userType by authContext.userType.collectAsState()
    when (userType) {
        UserType.Common -> CommonProfileScreen()
        UserType.Curator -> CuratorProfileScreen()
        null -> ProfilePlaceholderScreen("profile")
    }
}

@Composable
fun CommonProfileScreen() {
    val vm = koinViewModel<CommonProfileViewModel>()
    val state by vm.state.collectAsState()

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
                    when (val current = state) {
                        CommonProfileViewModel.State.Loading -> {
                            Text(
                                text = "commonProfile",
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
                        is CommonProfileViewModel.State.Loaded -> {
                            Text(
                                text = "commonProfile",
                                style = TextStyle(
                                    color = Palette.OnCard,
                                    fontFamily = Roboto,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(text = "firstName: ${current.profile.firstName}", color = Palette.OnCard, fontFamily = Roboto)
                            Text(text = "lastName: ${current.profile.lastName}", color = Palette.OnCard, fontFamily = Roboto)
                            Text(text = "secondName: ${current.profile.secondName}", color = Palette.OnCard, fontFamily = Roboto)
                            Text(text = "email: ${current.profile.email}", color = Palette.OnCard, fontFamily = Roboto)
                            Text(text = "group: ${current.profile.group}", color = Palette.OnCard, fontFamily = Roboto)
                        }
                        CommonProfileViewModel.State.Error -> {
                            Text(
                                text = "\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438",
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
                                text = "\u041f\u043e\u043f\u0440\u043e\u0431\u043e\u0432\u0430\u0442\u044c \u0441\u043d\u043e\u0432\u0430",
                                onClick = vm::load,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CuratorProfileScreen() {
    ProfilePlaceholderScreen("curatorProfile")
}

@Composable
private fun ProfilePlaceholderScreen(title: String) {
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
                    Text(
                        text = title,
                        style = TextStyle(
                            color = Palette.OnCard,
                            fontFamily = Roboto,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
