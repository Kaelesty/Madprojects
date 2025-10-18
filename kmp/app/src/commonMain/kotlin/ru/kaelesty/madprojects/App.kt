package ru.kaelesty.madprojects

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ru.kaelesty.madprojects.features.auth.sdk.AuthNavigation
import ru.kaelesty.madprojects.navigation.FeatureNavigation

@Composable
fun App(
    featureNavigationList: List<FeatureNavigation>
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = AuthNavigation.Route.Login) {
        featureNavigationList.forEach { featureNavigation ->
            featureNavigation.applyOn(this@NavHost, navController)
        }
    }
}
