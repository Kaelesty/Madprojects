package ru.kaelesty.madprojects.features.project.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.ui.ProjectScreen
import ru.kaelesty.madprojects.navigation.NavItem
import ru.kaelesty.madprojects.utils.KLogger

class ProjectNavItem(
    private val authContext: AuthContext,
) : NavItem {

    class Navigator(
        private val navController: NavController,
    ) {
        fun back() {
            val from = navController.currentBackStackEntry?.destination?.route ?: "unknown"
            KLogger.d(TAG) { "navigate: $from -> back" }
            navController.popBackStack()
        }
    }

    override fun applyOn(builder: NavGraphBuilder, navController: NavController) = with(builder) {
        val navigator = Navigator(navController)
        composable<Route.Project> { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")
            if (projectId == null) {
                return@composable
            }
            ProjectScreen(
                authContext = authContext,
                projectId = projectId,
                navigator = navigator
            )
        }
    }

    @Serializable
    sealed interface Route {
        @Serializable data class Project(val projectId: String) : Route
    }
}

private const val TAG = "ProjectNavItem"
