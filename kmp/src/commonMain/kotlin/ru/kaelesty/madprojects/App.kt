package ru.kaelesty.madprojects

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavItem
import ru.kaelesty.madprojects.navigation.NavItem

@Composable
fun App(
    navItems: List<NavItem>
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = AuthNavItem.Route.Hello) {
        navItems.forEach { navItem ->
            navItem.applyOn(this@NavHost, navController)
        }
    }
}

