package com.safemed.ui.screen.profile

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.ui.theme.EmeraldGreen

// Colors for Stitch Style
private val BackgroundGray = Color(0xFFF2F4F1)
private val DarkGreenCard = Color(0xFF0D3B35)
private val LimeAccent = Color(0xFFDEFF7D)
private val SectionTitleColor = Color(0xFF6B7280) // Cool Gray
private val TextPrimary = Color(0xFF1F2937)

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

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .clip(CircleShape)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundGray
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                
                // ===== APPEARANCE / GIAO DIỆN =====
                SettingsSectionTitle(stringResource(R.string.settings_appearance))
                SettingsGroup {
                    SettingsItemRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.settings_language),
                        value = viewModel.getLanguageDisplayName(),
                        onClick = { showLanguageDialog = true }
                    )
                    HorizontalDivider(color = BackgroundGray, thickness = 1.dp)
                    SettingsSwitchRow(
                        icon = Icons.Outlined.DarkMode,
                        title = stringResource(R.string.settings_dark_mode),
                        checked = uiState.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }

                // ===== NOTIFICATIONS / THÔNG BÁO =====
                SettingsSectionTitle(stringResource(R.string.settings_notifications))
                SettingsGroup {
                     SettingsSwitchRow(
                        icon = Icons.Outlined.Notifications,
                        title = stringResource(R.string.settings_push_notifications),
                        checked = uiState.notificationEnabled,
                        onCheckedChange = { viewModel.toggleNotification(it) }
                    )
                    HorizontalDivider(color = BackgroundGray, thickness = 1.dp)
                    SettingsSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = stringResource(R.string.settings_sound),
                        checked = uiState.soundEnabled,
                        onCheckedChange = { viewModel.toggleSound(it) }
                    )
                    HorizontalDivider(color = BackgroundGray, thickness = 1.dp)
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Vibration,
                        title = stringResource(R.string.settings_vibration),
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = { viewModel.toggleVibration(it) }
                    )
                }

                // ===== DATA / DỮ LIỆU =====
                SettingsSectionTitle(stringResource(R.string.settings_data))
                
                // Special Card for Auto Save
                HighlightedSettingsSwitchCard(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.settings_auto_save_history),
                    checked = uiState.autoSaveHistory,
                    onCheckedChange = { viewModel.toggleAutoSaveHistory(it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                SettingsGroup {
                    SettingsItemRow(
                        icon = Icons.Outlined.DeleteSweep,
                        title = stringResource(R.string.settings_clear_history),
                        onClick = { viewModel.showClearHistoryDialog() }
                    )
                }

                // ===== OTHER / KHÁC =====
                SettingsSectionTitle(stringResource(R.string.settings_other))
                SettingsGroup {
                     SettingsItemRow(
                        icon = Icons.Outlined.CleaningServices,
                        title = stringResource(R.string.settings_clear_cache),
                        value = uiState.cacheSize,
                        onClick = { viewModel.showClearCacheDialog() }
                    )
                }

                // Extra "About App" items
                Spacer(modifier = Modifier.height(24.dp))
                 SettingsGroup {
                    SettingsItemRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_app_version),
                        value = "1.0.0",
                        showArrow = false,
                        onClick = {}
                    )
                     HorizontalDivider(color = BackgroundGray, thickness = 1.dp)
                    SettingsItemRow(
                        icon = Icons.Outlined.Star,
                        title = stringResource(R.string.settings_rate_app),
                        onClick = { context.startActivity(viewModel.getRateIntent()) }
                    )
                     HorizontalDivider(color = BackgroundGray, thickness = 1.dp)
                    val shareTitle = stringResource(R.string.settings_share_app)
                    SettingsItemRow(
                        icon = Icons.Outlined.Share,
                        title = shareTitle,
                        onClick = { context.startActivity(Intent.createChooser(viewModel.getShareIntent(), shareTitle)) }
                    )
                }
                
                Spacer(modifier = Modifier.height(50.dp))
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

// ================= Components =================

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = SectionTitleColor,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
     Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    showArrow: Boolean = true,
    onClick: () -> Unit,
    iconColor: Color = Color.Gray,
    iconBgRequest: Color? = null
) {
    val iconBg = iconBgRequest ?: Color(0xFFF3F4F6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: Color = Color.Gray,
    iconBgRequest: Color? = null
) {
    val iconBg = iconBgRequest ?: Color(0xFFF3F4F6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
             colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DarkGreenCard,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E7EB),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun HighlightedSettingsSwitchCard(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGreenCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
         Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LimeAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
           
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
             Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                 colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkGreenCard,
                    checkedTrackColor = LimeAccent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray,
                    uncheckedBorderColor = Color.Transparent
                )
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
                .padding(vertical = 12.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = flag,
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = language,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
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
