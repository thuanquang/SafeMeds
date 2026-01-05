package com.safemed

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.safemed.navigation.AppDestination
import com.safemed.navigation.SafeMedNavGraph
import com.safemed.ui.theme.SafeMedTheme

@Composable
fun SafeMedApp(
    isDarkMode: Boolean = false,
    deepLinkIntent: Intent? = null
) {
    SafeMedTheme(darkTheme = isDarkMode) {
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
            
            // Handle deep link from notification
            LaunchedEffect(deepLinkIntent) {
                deepLinkIntent?.let { intent ->
                    val navigateTo = intent.getStringExtra("navigate_to")
                    
                    if (navigateTo == "reminder_detail") {
                        val reminderId = intent.getStringExtra("remind_id")
                        val timeSlot = intent.getStringExtra("time_slot")
                        
                        if (!reminderId.isNullOrEmpty()) {
                            // Navigate to edit reminder screen to view details
                            navController.navigate(AppDestination.EditReminder.createRoute(reminderId))
                        }
                    }
                }
            }
            
            SafeMedNavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}

