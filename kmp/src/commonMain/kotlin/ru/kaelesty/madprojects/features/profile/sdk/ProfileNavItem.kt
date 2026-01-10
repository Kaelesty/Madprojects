package ru.kaelesty.madprojects.features.profile.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.ui.ProfileScreen
import ru.kaelesty.madprojects.features.projectcreate.sdk.ProjectCreateNavItem
import ru.kaelesty.madprojects.navigation.NavItem

class ProfileNavItem(
    private val authContext: AuthContext,
) : NavItem {

    class Navigator(
        private val navController: NavController,
    ) {
        fun toCreateProject() = navController.navigate(ProjectCreateNavItem.Route.CreateProject)
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
