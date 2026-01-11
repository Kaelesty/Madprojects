package ru.kaelesty.madprojects.features.profile.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.ui.ProfileScreen
import ru.kaelesty.madprojects.features.project.sdk.ProjectNavItem
import ru.kaelesty.madprojects.features.projectcreate.sdk.ProjectCreateNavItem
import ru.kaelesty.madprojects.navigation.NavItem
import ru.kaelesty.madprojects.utils.KLogger

class ProfileNavItem(
    private val authContext: AuthContext,
) : NavItem {

    class Navigator(
        private val navController: NavController,
    ) {
        fun toCreateProject() {
            logNavigate(ProjectCreateNavItem.Route.CreateProject)
            navController.navigate(ProjectCreateNavItem.Route.CreateProject)
        }
        fun toProject(projectId: String) {
            logNavigate(ProjectNavItem.Route.Project(projectId))
            navController.navigate(ProjectNavItem.Route.Project(projectId))
        }

        private fun logNavigate(target: Any) {
            val from = navController.currentBackStackEntry?.destination?.route ?: "unknown"
            KLogger.d(TAG) { "navigate: $from -> $target" }
        }
    }

    override fun applyOn(builder: NavGraphBuilder, navController: NavController) = with(builder) {
        val navigator = Navigator(navController)
        composable<Route.Profile> {
            ProfileScreen(authContext, navigator)
        }
    }

    @Serializable
    sealed interface Route {
        @Serializable data object Profile : Route
    }
}

private const val TAG = "ProfileNavItem"
