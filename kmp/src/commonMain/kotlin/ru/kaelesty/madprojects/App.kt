package ru.kaelesty.madprojects

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collect
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.GithubOauthBridge
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem
import ru.kaelesty.madprojects.features.checkin.sdk.CheckInNavItem
import ru.kaelesty.madprojects.features.profile.sdk.ProfileNavItem
import ru.kaelesty.madprojects.navigation.NavItem
import ru.kaelesty.madprojects.utils.KLogger

@Composable
fun App(
    navItems: List<NavItem>,
    authContext: AuthContext? = null,
) {
    val navController = rememberNavController()

    val isAuthenticated = authContext?.isAuthenticated?.collectAsStateWithLifecycle()?.value

    val authStart = AuthNavItem.Route.Hello
    val startDestination = CheckInNavItem.Route.CheckIn

    LaunchedEffect(navController) {
        GithubOauthBridge.events.collect { result ->
            val from = navController.currentBackStackEntry?.destination?.route ?: "unknown"
            KLogger.i(TAG) { "oauth callback: status=${result.status} reason=${result.reason} -> navigate $from -> ${ProfileNavItem.Route.Profile}" }
            navController.navigate(ProfileNavItem.Route.Profile) {
                launchSingleTop = true
            }
        }
    }

    if (authContext != null) {
        LaunchedEffect(isAuthenticated) {
            if (isAuthenticated == false) {
                val from = navController.currentBackStackEntry?.destination?.route ?: "unknown"
                KLogger.d(TAG) { "navigate: $from -> $authStart (isAuthenticated=$isAuthenticated)" }
                navController.navigate(authStart) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }
            else {
                val from = navController.currentBackStackEntry?.destination?.route ?: "unknown"
                KLogger.d(TAG) { "navigate: $from -> ${ProfileNavItem.Route.Profile} (isAuthenticated=$isAuthenticated)" }
                navController.navigate(ProfileNavItem.Route.Profile) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(TRANSITION_MS),
                initialOffsetX = { it }
            )
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(TRANSITION_MS),
                targetOffsetX = { -it }
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(TRANSITION_MS),
                initialOffsetX = { -it }
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(TRANSITION_MS),
                targetOffsetX = { it }
            )
        }
    ) {
        navItems.forEach { navItem ->
            navItem.applyOn(this, navController)
        }
    }
}

private const val TRANSITION_MS = 800
private const val TAG = "AppNav"
