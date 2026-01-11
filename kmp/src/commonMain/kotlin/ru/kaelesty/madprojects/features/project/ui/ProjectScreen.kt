package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.sdk.ProjectNavItem
import ru.kaelesty.madprojects.ui.headers.ScreenHeader
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette

@Composable
fun ProjectScreen(
    authContext: AuthContext,
    projectId: String,
    navigator: ProjectNavItem.Navigator,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ProjectTab.Github) }

    Surface(color = Palette.Background) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = StringResources.ProjectScreenTitle,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    ProjectTab.Github -> ProjectGithubScreen(projectId = projectId)
                    ProjectTab.Sprints -> ProjectSprintsScreen()
                }
            }
            NavigationBar(
                containerColor = Palette.CardSurface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == ProjectTab.Github,
                    onClick = { selectedTab = ProjectTab.Github },
                    icon = { Icon(imageVector = Icons.Default.Code, contentDescription = null) },
                    colors = navigationItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == ProjectTab.Sprints,
                    onClick = { selectedTab = ProjectTab.Sprints },
                    icon = { Icon(imageVector = Icons.Default.Event, contentDescription = null) },
                    colors = navigationItemColors()
                )
            }
        }
    }
}

private enum class ProjectTab {
    Github,
    Sprints,
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Palette.AccentBlue,
    unselectedIconColor = Palette.FieldLabel,
    indicatorColor = Palette.Background,
    disabledIconColor = Palette.FieldLabel.copy(alpha = 0.6f),
    disabledTextColor = Color.Transparent
)
