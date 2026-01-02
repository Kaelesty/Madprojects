package ru.kaelesty.madprojects.ui.fields

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdownField(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)

    Column(modifier) {
        Text(
            text = label,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 12.sp
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = optionLabel(selected),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .shadow(2.dp, shape, clip = false)
                    .clip(shape)
                    .border(1.dp, Palette.FieldBorder, shape)
                    .menuAnchor(),
                placeholder = {
                    if (placeholder != null) {
                        Text(placeholder, color = Palette.FieldPlaceholder, fontFamily = Roboto)
                    }
                },
                textStyle = TextStyle(
                    color = Palette.FieldText,
                    fontFamily = Roboto,
                    fontSize = 15.sp
                ),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel(option),
                                color = Palette.FieldText,
                                fontFamily = Roboto
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
