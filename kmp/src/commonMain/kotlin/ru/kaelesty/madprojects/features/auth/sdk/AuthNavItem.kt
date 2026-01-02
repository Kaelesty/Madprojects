package ru.kaelesty.madprojects.features.auth.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.auth.ui.HelloScreen
import ru.kaelesty.madprojects.feature.auth.ui.LoginScreen
import ru.kaelesty.madprojects.features.auth.ui.RegisterScreen
import ru.kaelesty.madprojects.navigation.NavItem

class AuthNavItem: NavItem {

    class Navigator(
        private val navController: NavController,
    ) {
        fun toRegister() = navController.navigate(Route.Register)

        fun toLogin() = navController.navigate(Route.Login)

        fun toHello() = navController.navigate(Route.Hello)
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

