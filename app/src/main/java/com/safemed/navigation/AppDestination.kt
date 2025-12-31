package com.safemed.navigation

/**
 * Sealed class định nghĩa các điểm đến (destinations) trong ứng dụng
 * Mỗi object đại diện cho một màn hình với route tương ứng
 */
sealed class AppDestination(val route: String) {
    // Auth screens
    object Login : AppDestination("login")
    object Register : AppDestination("register")
    
    // Main screens
    object Home : AppDestination("home")
    object Map : AppDestination("map")
    object Scan : AppDestination("scan")
    object ScanResult : AppDestination("scan_result/{scannedCode}") {
        fun createRoute(scannedCode: String): String = "scan_result/$scannedCode"
    }
    object Profile : AppDestination("profile")
    
    // Debug screen (chỉ dùng trong development)
    object Debug : AppDestination("debug")
}

