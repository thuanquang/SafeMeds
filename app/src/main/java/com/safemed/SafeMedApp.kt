package com.safemed

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.safemed.navigation.AppDestination
import com.safemed.navigation.SafeMedNavGraph
import com.safemed.ui.theme.SafeMedTheme

@Composable
fun SafeMedApp() {
    SafeMedTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            
            // Kiểm tra trạng thái đăng nhập để xác định startDestination
            val startDestination = remember {
                if (Firebase.auth.currentUser != null) {
                    AppDestination.Home.route
                } else {
                    AppDestination.Login.route
                }
            }
            
            SafeMedNavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}

