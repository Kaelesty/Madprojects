package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.sdk.ProjectNavItem
import ru.kaelesty.madprojects.kmp.generated.resources.Res
import ru.kaelesty.madprojects.kmp.generated.resources.activity
import ru.kaelesty.madprojects.kmp.generated.resources.github
import ru.kaelesty.madprojects.ui.headers.ScreenHeader
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.utils.KLogger

@OptIn(ExperimentalResourceApi::class)
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
                    onClick = {
                        if (selectedTab != ProjectTab.Github) {
                            KLogger.d(TAG) { "tab switch: ${selectedTab.name} -> Github" }
                        }
                        selectedTab = ProjectTab.Github
                    },
                    icon = {
                        ProjectNavIcon(painter = painterResource(Res.drawable.github))
                    },
                    colors = navigationItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == ProjectTab.Sprints,
                    onClick = {
                        if (selectedTab != ProjectTab.Sprints) {
                            KLogger.d(TAG) { "tab switch: ${selectedTab.name} -> Sprints" }
                        }
                        selectedTab = ProjectTab.Sprints
                    },
                    icon = {
                        ProjectNavIcon(painter = painterResource(Res.drawable.activity))
                    },
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
private fun ProjectNavIcon(painter: Painter) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(LocalContentColor.current)
        )
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Palette.AccentBlue,
    unselectedIconColor = Palette.FieldLabel,
    indicatorColor = Palette.Background,
    disabledIconColor = Palette.FieldLabel.copy(alpha = 0.6f),
    disabledTextColor = Color.Transparent
)

private const val TAG = "ProjectScreen"
