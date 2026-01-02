@file:Suppress("FunctionName")

package ru.kaelesty.madprojects.features.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.HeaderCard
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun HelloScreen(
    navigator: AuthNavItem.Navigator,
    str: StringResources = StringResources
) {
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
                    Text(
                        text = str.GreetingTitle,
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
                        text = str.LoginButton,
                        onClick = { navigator.toLogin() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    PrimaryActionButton(
                        text = str.RegisterButton,
                        onClick = { navigator.toRegister() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
