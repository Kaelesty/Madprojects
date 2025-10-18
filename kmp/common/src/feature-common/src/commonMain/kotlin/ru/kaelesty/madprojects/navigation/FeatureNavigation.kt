package ru.kaelesty.madprojects.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

interface FeatureNavigation {
    fun applyOn(builder: NavGraphBuilder, navController: NavController)
}
