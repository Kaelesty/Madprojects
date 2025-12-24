package ru.kaelesty.madprojects.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import ru.kaelesty.madprojects.kmp.generated.resources.Res
import ru.kaelesty.madprojects.kmp.generated.resources.justanotherhand_regular
import ru.kaelesty.madprojects.kmp.generated.resources.roboto_variable
import ru.kaelesty.madprojects.kmp.generated.resources.roboto_variable_italic

// Common (multiplatform) font families backed by Compose Multiplatform resources.
@OptIn(ExperimentalResourceApi::class)
val Roboto: FontFamily
    @Composable get() = FontFamily(
        // Map common weights to the same variable font; renderer selects axis values.
        Font(Res.font.roboto_variable, FontWeight.Thin, FontStyle.Normal),
        Font(Res.font.roboto_variable, FontWeight.ExtraLight, FontStyle.Normal),
        Font(Res.font.roboto_variable, FontWeight.Light, FontStyle.Normal),
        Font(Res.font.roboto_variable, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.roboto_variable, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.roboto_variable, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.roboto_variable, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.roboto_variable, FontWeight.ExtraBold, FontStyle.Normal),
        Font(Res.font.roboto_variable, FontWeight.Black, FontStyle.Normal),
        // Italic axis
        Font(Res.font.roboto_variable_italic, FontWeight.Thin, FontStyle.Italic),
        Font(Res.font.roboto_variable_italic, FontWeight.ExtraLight, FontStyle.Italic),
        Font(Res.font.roboto_variable_italic, FontWeight.Light, FontStyle.Italic),
        Font(Res.font.roboto_variable_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.roboto_variable_italic, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.roboto_variable_italic, FontWeight.SemiBold, FontStyle.Italic),
        Font(Res.font.roboto_variable_italic, FontWeight.Bold, FontStyle.Italic),
        Font(Res.font.roboto_variable_italic, FontWeight.ExtraBold, FontStyle.Italic),
        Font(Res.font.roboto_variable_italic, FontWeight.Black, FontStyle.Italic),
    )

@OptIn(ExperimentalResourceApi::class)
val JustAnotherHand: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.justanotherhand_regular, FontWeight.Normal, FontStyle.Normal)
    )
