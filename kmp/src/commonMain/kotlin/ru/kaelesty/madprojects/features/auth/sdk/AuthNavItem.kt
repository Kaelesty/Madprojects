package ru.kaelesty.madprojects.features.auth.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.auth.ui.HelloScreen
import ru.kaelesty.madprojects.feature.auth.ui.LoginScreen
import ru.kaelesty.madprojects.features.auth.ui.RegisterScreen
import ru.kaelesty.madprojects.features.profile.sdk.ProfileNavItem
import ru.kaelesty.madprojects.navigation.NavItem
import ru.kaelesty.madprojects.utils.KLogger

class AuthNavItem: NavItem {

    class Navigator(
        private val navController: NavController,
    ) {
        fun toRegister() {
            logNavigate(Route.Register)
            navController.navigate(Route.Register)
        }

        fun toLogin() {
            logNavigate(Route.Login)
            navController.navigate(Route.Login)
        }

        fun toHello() {
            logNavigate(Route.Hello)
            navController.navigate(Route.Hello)
        }

        fun toProfile() {
            logNavigate(ProfileNavItem.Route.Profile)
            navController.navigate(ProfileNavItem.Route.Profile) {
                popUpTo(Route.Hello) { inclusive = true }
                launchSingleTop = true
                restoreState = false
            }
        }

        private fun logNavigate(target: Any) {
            val from = navController.currentBackStackEntry?.destination?.route ?: "unknown"
            KLogger.d(TAG) { "navigate: $from -> $target" }
        }
    }

    override fun applyOn(builder: NavGraphBuilder, navController: NavController) = with(builder) {
        val navigator = Navigator(navController)

        composable<Route.Login> {
            LoginScreen(navigator)
        }
        composable<Route.Register> {
            RegisterScreen(navigator)
        }
        composable<Route.Hello> {
            HelloScreen(navigator)
        }
    }

    @Serializable
    sealed interface Route {
        @Serializable data object Hello: Route
        @Serializable data object Login: Route
        @Serializable data object Register: Route
    }
}

private const val TAG = "AuthNavItem"

