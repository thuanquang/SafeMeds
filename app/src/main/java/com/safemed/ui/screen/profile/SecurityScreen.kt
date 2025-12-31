package com.safemed.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safemed.ui.component.ProfileMenuItem
import com.safemed.ui.component.ProfileMenuItemWithSwitch
import com.safemed.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onNavigateBack: () -> Unit = {}
) {
    var biometricEnabled by remember { mutableStateOf(false) }
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var loginNotificationEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bảo mật nâng cao") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ===== Xác thực Section =====
            Text(
                text = "XÁC THỰC",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuItemWithSwitch(
                        icon = Icons.Default.Fingerprint,
                        title = "Đăng nhập bằng vân tay/Face ID",
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )

                    ProfileMenuItemWithSwitch(
                        icon = Icons.Default.Security,
                        title = "Xác thực 2 yếu tố (2FA)",
                        checked = twoFactorEnabled,
                        onCheckedChange = { twoFactorEnabled = it },
                        showDivider = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Thông báo Section =====
            Text(
                text = "THÔNG BÁO BẢO MẬT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuItemWithSwitch(
                        icon = Icons.Default.NotificationsActive,
                        title = "Thông báo đăng nhập mới",
                        checked = loginNotificationEnabled,
                        onCheckedChange = { loginNotificationEnabled = it }
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.Devices,
                        title = "Thiết bị đã đăng nhập",
                        onClick = { /* TODO */ }
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.History,
                        title = "Lịch sử đăng nhập",
                        onClick = { /* TODO */ },
                        showDivider = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Tài khoản Section =====
            Text(
                text = "TÀI KHOẢN",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Xóa tài khoản",
                        onClick = { /* TODO: Show confirmation dialog */ },
                        iconTint = MaterialTheme.colorScheme.error,
                        showDivider = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info note
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EmeraldGreen
                    )
                    Text(
                        text = "Các tính năng bảo mật nâng cao giúp bảo vệ tài khoản của bạn khỏi truy cập trái phép.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
