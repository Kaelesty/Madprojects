package ru.kaelesty.madprojects.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

interface NavItem {
    fun applyOn(builder: NavGraphBuilder, navController: NavController)
}

