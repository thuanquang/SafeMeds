package com.safemed.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safemed.ui.component.ProfileMenuItemWithSwitch
import com.safemed.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    // States for toggles
    var isDarkMode by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var autoSaveHistory by remember { mutableStateOf(true) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Tiếng Việt") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
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
            // ===== Giao diện =====
            SettingsSectionHeader(title = "Giao diện")
            
            SettingsItem(
                icon = Icons.Outlined.Language,
                title = "Ngôn ngữ",
                subtitle = selectedLanguage,
                onClick = { showLanguageDialog = true }
            )
            
            ProfileMenuItemWithSwitch(
                icon = Icons.Outlined.DarkMode,
                title = "Chế độ tối",
                checked = isDarkMode,
                onCheckedChange = { isDarkMode = it }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ===== Thông báo =====
            SettingsSectionHeader(title = "Thông báo")

            ProfileMenuItemWithSwitch(
                icon = Icons.Outlined.Notifications,
                title = "Thông báo đẩy",
                checked = notificationEnabled,
                onCheckedChange = { notificationEnabled = it }
            )

            ProfileMenuItemWithSwitch(
                icon = Icons.Outlined.VolumeUp,
                title = "Âm thanh thông báo",
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it }
            )

            ProfileMenuItemWithSwitch(
                icon = Icons.Outlined.Vibration,
                title = "Rung thông báo",
                checked = vibrationEnabled,
                onCheckedChange = { vibrationEnabled = it }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ===== Quét & Lịch sử =====
            SettingsSectionHeader(title = "Quét & Lịch sử")

            ProfileMenuItemWithSwitch(
                icon = Icons.Outlined.History,
                title = "Tự động lưu lịch sử quét",
                checked = autoSaveHistory,
                onCheckedChange = { autoSaveHistory = it }
            )

            SettingsItem(
                icon = Icons.Outlined.DeleteSweep,
                title = "Xóa lịch sử quét",
                subtitle = "Xóa tất cả lịch sử quét thuốc",
                onClick = { /* TODO: Show confirmation dialog */ }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ===== Bộ nhớ & Cache =====
            SettingsSectionHeader(title = "Bộ nhớ")

            SettingsItem(
                icon = Icons.Outlined.CleaningServices,
                title = "Xóa bộ nhớ đệm",
                subtitle = "Giải phóng dung lượng ứng dụng",
                onClick = { /* TODO: Clear cache */ }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ===== Về ứng dụng =====
            SettingsSectionHeader(title = "Về ứng dụng")

            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "Phiên bản",
                subtitle = "1.0.0 (Build 1)",
                onClick = { }
            )

            SettingsItem(
                icon = Icons.Outlined.Star,
                title = "Đánh giá ứng dụng",
                subtitle = "Đánh giá SafeMed trên Play Store",
                onClick = { /* TODO: Open Play Store */ }
            )

            SettingsItem(
                icon = Icons.Outlined.Share,
                title = "Chia sẻ ứng dụng",
                subtitle = "Giới thiệu SafeMed cho bạn bè",
                onClick = { /* TODO: Share app */ }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Chọn ngôn ngữ") },
            text = {
                Column {
                    LanguageOption(
                        language = "Tiếng Việt",
                        flag = "🇻🇳",
                        isSelected = selectedLanguage == "Tiếng Việt",
                        onClick = {
                            selectedLanguage = "Tiếng Việt"
                            showLanguageDialog = false
                        }
                    )
                    LanguageOption(
                        language = "English",
                        flag = "🇺🇸",
                        isSelected = selectedLanguage == "English",
                        onClick = {
                            selectedLanguage = "English"
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Đóng", color = EmeraldGreen)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = EmeraldGreen,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguageOption(
    language: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (isSelected) EmeraldGreen.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = flag,
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = language,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = EmeraldGreen
                )
            }
        }
    }
}
