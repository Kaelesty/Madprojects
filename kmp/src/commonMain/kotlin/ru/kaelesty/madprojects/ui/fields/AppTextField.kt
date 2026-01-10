package ru.kaelesty.madprojects.ui.fields

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    val shape = RoundedCornerShape(6.dp)
    Column(modifier) {
        Text(
            text = label,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 12.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .shadow(2.dp, shape, clip = false)
                .clip(shape)
                .border(1.dp, Palette.FieldBorder, shape),
            placeholder = {
                if (placeholder != null) Text(placeholder, color = Palette.FieldPlaceholder, fontFamily = Roboto)
            },
            textStyle = TextStyle(color = Palette.FieldText, fontFamily = Roboto, fontSize = 15.sp),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            visualTransformation = VisualTransformation.None,
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.FieldBg,
                unfocusedContainerColor = Palette.FieldBg,
                disabledContainerColor = Palette.FieldBg,
                focusedIndicatorColor = Palette.FieldBorderFocused,
                unfocusedIndicatorColor = Palette.FieldBg,
                disabledIndicatorColor = Palette.FieldBg,
                cursorColor = Palette.Cursor,
                focusedTextColor = Palette.FieldText,
                unfocusedTextColor = Palette.FieldText,
                focusedPlaceholderColor = Palette.FieldPlaceholder,
                unfocusedPlaceholderColor = Palette.FieldPlaceholder
            )
        )
    }
}

@Composable
fun AppPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Done
) {
    var visible by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Column(modifier) {
        Text(
            text = label,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 12.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .shadow(2.dp, shape, clip = false)
                .clip(shape)
                .border(1.dp, Palette.FieldBorder, shape),
            placeholder = {
                if (placeholder != null) Text(placeholder, color = Palette.FieldPlaceholder, fontFamily = Roboto)
            },
            textStyle = TextStyle(color = Palette.FieldText, fontFamily = Roboto, fontSize = 15.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null, tint = Palette.FieldLabel)
                }
            },
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.FieldBg,
                unfocusedContainerColor = Palette.FieldBg,
                disabledContainerColor = Palette.FieldBg,
                focusedIndicatorColor = Palette.FieldBorderFocused,
                unfocusedIndicatorColor = Palette.FieldBg,
                disabledIndicatorColor = Palette.FieldBg,
                cursorColor = Palette.Cursor,
                focusedTextColor = Palette.FieldText,
                unfocusedTextColor = Palette.FieldText,
                focusedPlaceholderColor = Palette.FieldPlaceholder,
                unfocusedPlaceholderColor = Palette.FieldPlaceholder
            )
        )
    }
}
