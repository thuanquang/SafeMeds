package com.safemed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safemed.navigation.AppDestination
import com.safemed.navigation.BottomNavItem
import com.safemed.navigation.SafeMedNavHost
import com.safemed.navigation.bottomNavRoutes
import com.safemed.ui.components.SafeMedBottomNavBar
import com.safemed.ui.theme.SafeMedTheme

@Composable
fun SafeMedApp() {
    SafeMedTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Determine if bottom nav should be shown
            val shouldShowBottomNav = currentRoute in bottomNavRoutes
            
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = shouldShowBottomNav,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        SafeMedBottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { item ->
                                navController.navigate(item.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(AppDestination.Home.route) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                SafeMedNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}


