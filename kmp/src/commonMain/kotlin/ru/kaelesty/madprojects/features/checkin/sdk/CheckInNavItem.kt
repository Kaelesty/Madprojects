package ru.kaelesty.madprojects.features.checkin.sdk

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem
import ru.kaelesty.madprojects.features.checkin.ui.CheckInScreen
import ru.kaelesty.madprojects.navigation.NavItem
import ru.kaelesty.madprojects.utils.KLogger

class CheckInNavItem : NavItem {

    class Navigator(
        private val navController: NavController,
    ) {
        fun toCheckIn() {
            logNavigate(Route.CheckIn)
            navController.navigate(Route.CheckIn)
        }
        fun toMain() {
            logNavigate(AuthNavItem.Route.Hello)
            navController.navigate(AuthNavItem.Route.Hello) {
                popUpTo(Route.CheckIn) { inclusive = true }
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
        composable<Route.CheckIn> {
            CheckInScreen(navigator)
        }
    }

    @Serializable
    sealed interface Route {
        @Serializable data object CheckIn : Route
    }
}

private const val TAG = "CheckInNavItem"
