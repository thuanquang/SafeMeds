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
    object Profile : AppDestination("profile")
    
    // Profile sub-screens
    object UpdateProfile : AppDestination("update_profile")
    object ScanHistory : AppDestination("scan_history")
    object Security : AppDestination("security")
    object Terms : AppDestination("terms")
    object Support : AppDestination("support")
    object ChangePassword : AppDestination("change_password")
    object Settings : AppDestination("settings")
    
    // Debug screen (chỉ dùng trong development)
    object Debug : AppDestination("debug")
}

