package ru.kaelesty.madprojects.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

data class AppDropdownMenuItem(
    val text: String,
    val onClick: () -> Unit,
)

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppDropdownMenuItem>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val menuColors = MaterialTheme.colorScheme.copy(
        surface = Palette.Background,
        surfaceTint = Color.Transparent,
    )
    MaterialTheme(colorScheme = menuColors) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.background(Palette.Background, shape),
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.text,
                            color = Palette.OnCard,
                            fontFamily = Roboto,
                            fontSize = 14.sp,
                        )
                    },
                    onClick = item.onClick,
                )
                if (index < items.lastIndex) {
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
