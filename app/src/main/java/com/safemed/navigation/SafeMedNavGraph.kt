package com.safemed.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import com.safemed.ui.screen.HomeScreen
import com.safemed.ui.screen.LoginScreen
import com.safemed.ui.screen.MapScreen
import com.safemed.ui.screen.ProfileScreen
import com.safemed.ui.screen.ScanScreen

@Composable
fun SafeMedNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Login.route,
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
                onNavigateToMap = { navController.navigate(AppDestination.Map.route) },
                onNavigateToScan = { navController.navigate(AppDestination.Scan.route) },
                onNavigateToProfile = { navController.navigate(AppDestination.Profile.route) }
            )
        }
        composable(AppDestination.Map.route) {
            MapScreen()
        }
        composable(AppDestination.Scan.route) {
            ScanScreen()
        }
        composable(AppDestination.Profile.route) {
            ProfileScreen()
        }
    }
}

