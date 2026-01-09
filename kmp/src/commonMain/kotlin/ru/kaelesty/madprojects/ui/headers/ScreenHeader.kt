package ru.kaelesty.madprojects.ui.headers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.JustAnotherHand
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    logoText: String = StringResources.HeaderLogo,
    logoFont: FontFamily = JustAnotherHand,
    headerHeight: Dp = 64.dp,
    logoOffsetY: Dp = 10.dp,
) {
    Column(
        modifier = modifier.background(Palette.CardSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .zIndex(1f)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = logoText,
                    color = Palette.OnCard,
                    fontFamily = logoFont,
                    fontSize = 80.sp,
                    lineHeight = 36.sp,
                    modifier = Modifier
                        .offset(y = logoOffsetY)
                        .wrapContentHeight(unbounded = true)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier
                        .offset(y = logoOffsetY)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .zIndex(0f)
                .background(
                    Brush.horizontalGradient(
                        listOf(Palette.AccentBlue, Palette.AccentRed)
                    )
                )
        )
    }
}
