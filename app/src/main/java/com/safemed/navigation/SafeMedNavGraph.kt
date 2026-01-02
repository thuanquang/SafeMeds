package com.safemed.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.safemed.ui.screen.DebugScreen
import com.safemed.ui.screen.HistoryScreen
import com.safemed.ui.screen.HomeScreen
import com.safemed.ui.screen.LoginScreen
import com.safemed.ui.screen.MapScreen
import com.safemed.ui.screen.ProfileScreen
import com.safemed.ui.screen.RegisterScreen
import com.safemed.ui.screen.ScanResultScreen
import com.safemed.ui.screen.ScanScreen

/**
 * NavHost chính của ứng dụng SafeMed
 * Quản lý navigation giữa các màn hình
 *
 * @param startDestination Điểm bắt đầu (Login hoặc Home tùy theo auth state)
 */
@Composable
fun SafeMedNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppDestination.Login.route
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
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AppDestination.Register.route)
                }
            )
        }

        // Màn hình đăng ký
        composable(AppDestination.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Màn hình chính
        composable(AppDestination.Home.route) {
            HomeScreen(
                onNavigateToMap = { navController.navigate(AppDestination.Map.route) },
                onNavigateToScan = { navController.navigate(AppDestination.Scan.route) },
                onNavigateToProfile = { navController.navigate(AppDestination.Profile.route) },
                onNavigateToDebug = { navController.navigate(AppDestination.Debug.route) }
            )
        }

        // Màn hình bản đồ
        composable(AppDestination.Map.route) {
            MapScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Màn hình quét thuốc
        composable(AppDestination.Scan.route) {
            ScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResult = { scannedCode ->
                    navController.navigate(AppDestination.ScanResult.createRoute(scannedCode))
                },
                onNavigateToHistory = {
                    navController.navigate(AppDestination.History.route)
                }
            )
        }

        // Màn hình kết quả quét
        composable(
            route = AppDestination.ScanResult.route,
            arguments = listOf(
                navArgument("scannedCode") { type = NavType.StringType },
                navArgument("fromHistory") { 
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val scannedCode = backStackEntry.arguments?.getString("scannedCode") ?: ""
            ScanResultScreen(
                scannedCode = scannedCode,
                onNavigateBack = { navController.popBackStack() },
                onScanAgain = {
                    // Pop về ScanScreen và reset state
                    navController.popBackStack(AppDestination.Scan.route, inclusive = false)
                },
                onGoHome = {
                    // Pop về Home screen
                    navController.popBackStack(AppDestination.Home.route, inclusive = false)
                },
                onNavigateToHistory = {
                    navController.navigate(AppDestination.History.route)
                }
            )
        }

        // Màn hình lịch sử quét
        composable(AppDestination.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResult = { scannedCode ->
                    // fromHistory = true để không tạo duplicate history
                    navController.navigate(AppDestination.ScanResult.createRoute(scannedCode, fromHistory = true))
                }
            )
        }

        // Màn hình hồ sơ
        composable(AppDestination.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình Debug (chỉ dùng trong development)
        composable(AppDestination.Debug.route) {
            DebugScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

