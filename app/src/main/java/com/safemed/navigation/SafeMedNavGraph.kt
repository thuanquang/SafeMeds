package com.safemed.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import com.safemed.ui.screen.ChatScreen
import com.safemed.ui.screen.HomeScreen
import com.safemed.ui.screen.LoginScreen
import com.safemed.ui.screen.MapScreen
import com.safemed.ui.screen.ProfileScreen
import com.safemed.ui.screen.ScanScreen

@Composable
fun SafeMedNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppDestination.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.Home.route) {
            HomeScreen(
                onNavigateToMap = { navController.navigate(AppDestination.Map.route) }
            )
        }
        composable(AppDestination.Map.route) {
            MapScreen()
        }
        composable(AppDestination.Scan.route) {
            ScanScreen()
        }
        composable(AppDestination.Chat.route) {
            ChatScreen()
        }
        composable(AppDestination.Profile.route) {
            ProfileScreen()
        }
    }
}

/**
 * List of routes that should show the bottom navigation bar
 */
val bottomNavRoutes = listOf(
    AppDestination.Home.route,
    AppDestination.Scan.route,
    AppDestination.Chat.route,
    AppDestination.Profile.route
)

