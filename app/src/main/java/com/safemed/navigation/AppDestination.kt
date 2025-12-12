package com.safemed.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")
    data object Home : AppDestination("home")
    data object Map : AppDestination("map")
    data object Scan : AppDestination("scan")
    data object Chat : AppDestination("chat")
    data object Profile : AppDestination("profile")
}

/**
 * Bottom navigation items for the main app navigation
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = AppDestination.Home.route,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    SCAN(
        route = AppDestination.Scan.route,
        label = "Scan",
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner
    ),
    CHAT(
        route = AppDestination.Chat.route,
        label = "Chat",
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    ),
    PROFILE(
        route = AppDestination.Profile.route,
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}


