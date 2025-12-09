package com.safemed

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.safemed.navigation.SafeMedNavHost
import com.safemed.ui.theme.SafeMedTheme

@Composable
fun SafeMedApp() {
    SafeMedTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            SafeMedNavHost(navController = navController)
        }
    }
}

