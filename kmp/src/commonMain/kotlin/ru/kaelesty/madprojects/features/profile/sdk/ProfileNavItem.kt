package ru.kaelesty.madprojects.features.profile.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.ui.ProfileScreen
import ru.kaelesty.madprojects.navigation.NavItem

class ProfileNavItem(
    private val authContext: AuthContext,
) : NavItem {

    override fun applyOn(builder: NavGraphBuilder, navController: NavController) = with(builder) {
        composable<Route.Profile> {
            ProfileScreen(authContext)
        }
    }

    @Serializable
    sealed interface Route {
        @Serializable data object Profile : Route
    }
}
