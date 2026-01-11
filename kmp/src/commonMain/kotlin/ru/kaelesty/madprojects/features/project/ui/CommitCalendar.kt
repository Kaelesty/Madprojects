package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kaelesty.madprojects.ui.theme.Palette

@Composable
fun CommitCalendar(
    month: ProjectViewModel.MonthOption?,
    commits: List<ProjectViewModel.CommitItem>,
    modifier: Modifier = Modifier,
) {
    if (month == null) return

    val dayCounts = remember(month, commits) {
        commits
            .mapNotNull { parseDay(it.rawDate) }
            .groupingBy { it }
            .eachCount()
    }
    val maxCount = remember(dayCounts) { dayCounts.values.maxOrNull() ?: 0 }
    val cells = remember(month) {
        buildCalendarCells(month.year, month.month)
    }
    val rows = remember(cells) { cells.chunked(7) }
    val lightColor = Color(0xFFDDECF8)
    val cellShape = RoundedCornerShape(4.dp)

    val spacing = 6.dp
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellSize = ((maxWidth - spacing * 6) / 7).let { size ->
            if (size < 12.dp) 12.dp else size
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            rows.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    week.forEach { day ->
                        val count = (if (day == null) null else dayCounts[day]) ?: 0
                        val color = if (day == null) {
                            Color.Transparent
                        } else {
                            commitColor(count, maxCount, lightColor)
                        }
                        val ratio = if (maxCount > 0) count.toFloat() / maxCount else 0f
                        val label = if (day == null) {
                            ""
                        } else if (count >= 10) {
                            "9+"
                        } else {
                            count.toString()
                        }
                        val textColor = if (ratio > 0.6f) Color.White else Palette.OnCard
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(color, cellShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (label.isNotBlank()) {
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun commitColor(count: Int, maxCount: Int, lightColor: Color): Color {
    if (maxCount <= 0) return lightColor
    val ratio = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    return lerp(lightColor, Palette.AccentBlue, ratio)
}

private fun buildCalendarCells(year: Int, month: Int): List<Int?> {
    val firstDayOffset = dayOfWeek(year, month, 1)
    val daysInMonth = daysInMonth(year, month)
    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    val cellCount = rows * 7
    return List(cellCount) { index ->
        val day = index - firstDayOffset + 1
        if (day in 1..daysInMonth) day else null
    }
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

private fun dayOfWeek(year: Int, month: Int, day: Int): Int {
    var m = month
    var y = year
    if (m < 3) {
        m += 12
        y -= 1
    }
    val k = y % 100
    val j = y / 100
    val h = (day + (13 * (m + 1)) / 5 + k + (k / 4) + (j / 4) + 5 * j) % 7
    return (h + 5) % 7
}

private fun parseDay(raw: String): Int? {
    if (raw.length < 10) return null
    return raw.substring(8, 10).toIntOrNull()
}
