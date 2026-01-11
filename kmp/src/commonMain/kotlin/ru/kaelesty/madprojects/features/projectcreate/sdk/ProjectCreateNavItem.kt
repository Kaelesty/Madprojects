package ru.kaelesty.madprojects.features.projectcreate.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.projectcreate.ui.CreateProjectScreen
import ru.kaelesty.madprojects.navigation.NavItem
import ru.kaelesty.madprojects.utils.KLogger

class ProjectCreateNavItem : NavItem {

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
        composable<Route.CreateProject> {
            CreateProjectScreen(navigator)
        }
    }

    @Serializable
    sealed interface Route {
        @Serializable data object CreateProject : Route
    }
}

private const val TAG = "ProjectCreateNavItem"
