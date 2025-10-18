package ru.kaelesty.madprojects.features.auth.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.kaelesty.madprojects.features.auth.impl.ui.Login
import ru.kaelesty.madprojects.features.auth.impl.ui.Register
import ru.kaelesty.madprojects.navigation.FeatureNavigation

class AuthNavigation: FeatureNavigation {

    override fun applyOn(builder: NavGraphBuilder, navController: NavController) = with(builder) {
        composable<Route.Login> {
            Login(
                toRegister = {
                    navController.navigate(Route.Register)
                }
            )
        }
        composable<Route.Register> {
            Register(
                toLogin = {
                    navController.navigate(Route.Login)
                }
            )
        }
    }

    sealed interface Route {
        data object Login: Route
        data object Register: Route
    }
}