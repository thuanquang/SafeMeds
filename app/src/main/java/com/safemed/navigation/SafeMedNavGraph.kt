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
import com.safemed.ui.screen.RegisterScreen
import com.safemed.ui.screen.ScanScreen

/**
 * NavHost chính của ứng dụng SafeMed
 * Quản lý navigation giữa các màn hình
 * 
 * @param startDestination Route bắt đầu, mặc định là Login.
 *                         Nếu user đã đăng nhập, sẽ là Home.
 */
@Composable
fun SafeMedNavHost(
    navController: NavHostController,
    startDestination: String = AppDestination.Login.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Màn hình đăng nhập
        composable(AppDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate đến Home và xóa backstack
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // Navigate đến Register
                    navController.navigate(AppDestination.Register.route)
                }
            )
        }
        
        // Màn hình đăng ký
        composable(AppDestination.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Navigate đến Home và xóa backstack
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    // Quay lại màn hình Login
                    navController.popBackStack()
                }
            )
        }
        
        // Màn hình chính
        composable(AppDestination.Home.route) {
            HomeScreen(
                onNavigateToMap = { navController.navigate(AppDestination.Map.route) },
                onNavigateToScan = { navController.navigate(AppDestination.Scan.route) },
                onNavigateToProfile = { navController.navigate(AppDestination.Profile.route) }
            )
        }
        
        // Màn hình bản đồ
        composable(AppDestination.Map.route) {
            MapScreen()
        }
        
        // Màn hình quét thuốc
        composable(AppDestination.Scan.route) {
            ScanScreen()
        }
        
        // Màn hình hồ sơ
        composable(AppDestination.Profile.route) {
            ProfileScreen()
        }
    }
}

