package ru.kaelesty.madprojects.ui.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val canExpand = enabled && options.isNotEmpty()
    val isExpanded = expanded && canExpand

    LaunchedEffect(canExpand) {
        if (!canExpand) {
            expanded = false
        }
    }

    Column(modifier) {
        Text(
            text = label,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 12.sp
        )

        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { if (canExpand) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected?.let(optionLabel).orEmpty(),
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
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
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
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
                    disabledTextColor = Palette.FieldText,
                    focusedPlaceholderColor = Palette.FieldPlaceholder,
                    unfocusedPlaceholderColor = Palette.FieldPlaceholder,
                    disabledPlaceholderColor = Palette.FieldPlaceholder
                )
            )

            val menuShape = RoundedCornerShape(10.dp)
            val menuColors = MaterialTheme.colorScheme.copy(
                surface = Palette.Background,
                surfaceTint = Color.Transparent,
            )
            MaterialTheme(colorScheme = menuColors) {
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Palette.Background, menuShape),
                ) {
                    options.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = optionLabel(option),
                                    color = Palette.OnCard,
                                    fontFamily = Roboto,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                onSelect(option)
                                expanded = false
                            }
                        )
                        if (index < options.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Palette.Divider)
                            )
                        }
                    }
                }
            }
        }
    }
}
