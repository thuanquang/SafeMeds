package com.safemed.ui.screen.profile

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.ui.component.ProfileMenuItemWithSwitch
import com.safemed.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Show snackbar messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ===== Giao diện =====
                SettingsSectionHeader(title = stringResource(R.string.settings_appearance))
                
                SettingsItem(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = viewModel.getLanguageDisplayName(),
                    onClick = { showLanguageDialog = true }
                )
                
                ProfileMenuItemWithSwitch(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(R.string.settings_dark_mode),
                    checked = uiState.isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ===== Thông báo =====
                SettingsSectionHeader(title = stringResource(R.string.settings_notifications))

                ProfileMenuItemWithSwitch(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.settings_push_notifications),
                    checked = uiState.notificationEnabled,
                    onCheckedChange = { viewModel.toggleNotification(it) }
                )

                ProfileMenuItemWithSwitch(
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    title = stringResource(R.string.settings_sound),
                    checked = uiState.soundEnabled,
                    onCheckedChange = { viewModel.toggleSound(it) }
                )

                ProfileMenuItemWithSwitch(
                    icon = Icons.Outlined.Vibration,
                    title = stringResource(R.string.settings_vibration),
                    checked = uiState.vibrationEnabled,
                    onCheckedChange = { viewModel.toggleVibration(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ===== Quét & Lịch sử =====
                SettingsSectionHeader(title = stringResource(R.string.settings_data))

                ProfileMenuItemWithSwitch(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.settings_auto_save_history),
                    checked = uiState.autoSaveHistory,
                    onCheckedChange = { viewModel.toggleAutoSaveHistory(it) }
                )

                SettingsItem(
                    icon = Icons.Outlined.DeleteSweep,
                    title = stringResource(R.string.settings_clear_history),
                    subtitle = "",
                    onClick = { viewModel.showClearHistoryDialog() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ===== Bộ nhớ & Cache =====
                SettingsSectionHeader(title = stringResource(R.string.settings_other))

                SettingsItem(
                    icon = Icons.Outlined.CleaningServices,
                    title = stringResource(R.string.settings_clear_cache),
                    subtitle = stringResource(R.string.settings_cache_size, uiState.cacheSize),
                    onClick = { viewModel.showClearCacheDialog() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ===== Về ứng dụng =====
                SettingsSectionHeader(title = stringResource(R.string.settings_other))

                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_app_version),
                    subtitle = "1.0.0 (Build 1)",
                    onClick = { }
                )

                SettingsItem(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.settings_rate_app),
                    subtitle = "",
                    onClick = { 
                        context.startActivity(viewModel.getRateIntent())
                    }
                )

                val shareTitle = stringResource(R.string.settings_share_app)
                SettingsItem(
                    icon = Icons.Outlined.Share,
                    title = shareTitle,
                    subtitle = "",
                    onClick = { 
                        context.startActivity(Intent.createChooser(viewModel.getShareIntent(), shareTitle))
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = EmeraldGreen
                )
            }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        val activity = context as? Activity
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_select_language)) },
            text = {
                Column {
                    LanguageOption(
                        language = stringResource(R.string.settings_language_vietnamese),
                        flag = "🇻🇳",
                        isSelected = uiState.language == "vi",
                        onClick = {
                            viewModel.setLanguage("vi")
                            showLanguageDialog = false
                            // Recreate activity to apply language change
                            activity?.recreate()
                        }
                    )
                    LanguageOption(
                        language = stringResource(R.string.settings_language_english),
                        flag = "🇺🇸",
                        isSelected = uiState.language == "en",
                        onClick = {
                            viewModel.setLanguage("en")
                            showLanguageDialog = false
                            // Recreate activity to apply language change
                            activity?.recreate()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_close), color = EmeraldGreen)
                }
            }
        )
    }

    // Clear History Confirmation Dialog
    if (uiState.showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearHistoryDialog() },
            icon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = Color.Red) },
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text(stringResource(R.string.settings_confirm_clear_history)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearScanHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearHistoryDialog() }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Clear Cache Confirmation Dialog
    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearCacheDialog() },
            icon = { Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = EmeraldGreen) },
            title = { Text(stringResource(R.string.settings_clear_cache)) },
            text = { Text(stringResource(R.string.settings_confirm_clear_cache)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearCache() },
                    colors = ButtonDefaults.textButtonColors(contentColor = EmeraldGreen)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearCacheDialog() }) {
                    Text(stringResource(R.string.btn_cancel))
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
