package com.safemed.navigation

/**
 * Sealed class định nghĩa các điểm đến (destinations) trong ứng dụng
 * Sử dụng cho Navigation Compose
 */
sealed class AppDestination(val route: String) {
    /** Màn hình đăng nhập */
    data object Login : AppDestination("login")
    
    /** Màn hình đăng ký */
    data object Register : AppDestination("register")
    
    /** Màn hình chính */
    data object Home : AppDestination("home")
    
    /** Màn hình bản đồ tìm nhà thuốc */
    data object Map : AppDestination("map")
    
    /** Màn hình quét xác thực thuốc */
    data object Scan : AppDestination("scan")
    
    /** Màn hình hồ sơ người dùng */
    data object Profile : AppDestination("profile")
}

