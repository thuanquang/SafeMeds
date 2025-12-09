package com.safemed.navigation

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")
    data object Home : AppDestination("home")
    data object Map : AppDestination("map")
    data object Scan : AppDestination("scan")
    data object Profile : AppDestination("profile")
}

