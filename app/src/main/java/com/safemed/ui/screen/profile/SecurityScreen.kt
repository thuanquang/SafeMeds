package com.safemed.ui.screen.profile

import android.app.Activity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.ui.component.ReauthDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onNavigateBack: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Colors
    val stitchBg = colorResource(id = R.color.stitch_bg)
    val bgCard = colorResource(id = R.color.bg_card)
    val textPrimary = colorResource(id = R.color.stitch_text_primary)
    val textSecondary = colorResource(id = R.color.stitch_text_secondary)
    val stitchDarkGreen = colorResource(id = R.color.stitch_dark_green)
    val stitchLime = colorResource(id = R.color.stitch_lime)

    // Navigate when account is deleted
    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) {
            onAccountDeleted()
        }
    }

    // Show snackbar messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // Biometric prompt launcher
    val biometricPromptLauncher = remember {
        { onSuccess: () -> Unit, onError: () -> Unit ->
            val activity = context as? FragmentActivity
            if (activity != null) {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            onError()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // Don't call onError here, let user retry
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(context.getString(R.string.security_biometric))
                    .setSubtitle(context.getString(R.string.security_biometric_desc))
                    .setNegativeButtonText(context.getString(R.string.btn_cancel))
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                onError()
            }
        }
    }

    // Handle biometric verification request
    LaunchedEffect(uiState.showBiometricVerifyDialog) {
        if (uiState.showBiometricVerifyDialog) {
            biometricPromptLauncher(
                { viewModel.onBiometricVerified(true) },
                { viewModel.onBiometricVerified(false) }
            )
        }
    }

    Scaffold(
        containerColor = stitchBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.security_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = textPrimary
                    ) 
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .background(bgCard, CircleShape)
                            .clip(CircleShape)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = stitchBg
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
                // ===== AUTHENTICATION =====
                SecuritySectionTitle(stringResource(R.string.security_authentication), textSecondary)

                SecurityGroup(bgCard) {
                    SecuritySwitchRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.security_biometric),
                        checked = uiState.biometricEnabled,
                        onCheckedChange = { viewModel.requestBiometricToggle(it) },
                        textPrimary = textPrimary,
                        trackColor = stitchDarkGreen
                    )
                    
                    if (!uiState.isBiometricAvailable) {
                        Text(
                            text = uiState.biometricStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 12.dp)
                        )
                    }

                    HorizontalDivider(color = stitchBg, thickness = 1.dp)

                    SecuritySwitchRow(
                        icon = Icons.Outlined.Security,
                        title = stringResource(R.string.security_2fa),
                        checked = uiState.twoFactorEnabled,
                        onCheckedChange = { viewModel.toggleTwoFactor(it) },
                        textPrimary = textPrimary,
                        trackColor = stitchDarkGreen
                    )
                    Text(
                        text = stringResource(R.string.security_2fa_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                    )
                }

                // ===== LOGIN NOTIFICATION =====
                SecuritySectionTitle(stringResource(R.string.security_login_notification), textSecondary)
                
                SecurityGroup(bgCard) {
                    SecuritySwitchRow(
                        icon = Icons.Outlined.NotificationsActive,
                        title = stringResource(R.string.security_login_notification),
                        checked = uiState.loginNotificationEnabled,
                        onCheckedChange = { viewModel.toggleLoginNotification(it) },
                        textPrimary = textPrimary,
                        trackColor = stitchDarkGreen
                    )
                    Text(
                        text = stringResource(R.string.security_login_notification_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                    )
                }

                // ===== SESSION MANAGEMENT =====
                SecuritySectionTitle(stringResource(R.string.security_session), textSecondary)

                SecurityGroup(bgCard) {
                    SecurityItemRow(
                        icon = Icons.Outlined.Devices,
                        title = stringResource(R.string.security_devices),
                        onClick = { viewModel.showDevicesDialog() },
                        textPrimary = textPrimary
                    )
                    HorizontalDivider(color = stitchBg, thickness = 1.dp)
                    SecurityItemRow(
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.security_login_history),
                        onClick = { viewModel.showLoginHistoryDialog() },
                        textPrimary = textPrimary
                    )
                }

                // ===== DANGER ZONE =====
                SecuritySectionTitle(stringResource(R.string.security_danger), Color.Red.copy(alpha = 0.8f))

                SecurityGroup(bgCard) {
                    SecurityItemRow(
                        icon = Icons.Outlined.DeleteForever,
                        title = stringResource(R.string.security_delete_account),
                        onClick = { viewModel.showDeleteAccountDialog() },
                        textPrimary = Color.Red,
                        iconColor = Color.Red,
                        iconBgRequest = Color.Red.copy(alpha = 0.1f)
                    )
                    Text(
                        text = stringResource(R.string.security_delete_account_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(50.dp))
            }

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = stitchDarkGreen
                )
            }
        }
    }

    // Delete Account Confirmation Dialog
    if (uiState.showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAccountDialog() },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = Color.Red) },
            title = { Text(stringResource(R.string.security_delete_confirm_title)) },
            text = { Text(stringResource(R.string.security_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAccount() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(R.string.security_delete_account))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteAccountDialog() }) {
                    Text(stringResource(R.string.btn_cancel), color = textPrimary)
                }
            },
            containerColor = bgCard,
            titleContentColor = textPrimary,
            textContentColor = textSecondary
        )
    }

    // Login History Dialog
    if (uiState.showLoginHistoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLoginHistoryDialog() },
            title = { Text(stringResource(R.string.security_login_history)) },
            text = {
                if (uiState.loginHistory.isEmpty()) {
                    Text(
                        stringResource(R.string.security_no_data),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = textSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(uiState.loginHistory) { item ->
                            LoginHistoryItem(item, stitchDarkGreen, textPrimary, textSecondary)
                            HorizontalDivider(color = stitchBg)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissLoginHistoryDialog() }) {
                    Text(stringResource(R.string.btn_close), color = stitchDarkGreen)
                }
            },
            containerColor = bgCard,
            titleContentColor = textPrimary,
            textContentColor = textSecondary
        )
    }

    // Devices Dialog
    if (uiState.showDevicesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDevicesDialog() },
            title = { Text(stringResource(R.string.security_devices)) },
            text = {
                val currentDevices = uiState.loginHistory.filter { it.isCurrent }
                val otherDevices = uiState.loginHistory.filter { !it.isCurrent }

                Column {
                    if (currentDevices.isNotEmpty()) {
                        Text(
                            stringResource(R.string.security_current_device),
                            style = MaterialTheme.typography.labelMedium,
                            color = stitchDarkGreen
                        )
                        currentDevices.forEach { device ->
                            DeviceItem(device, isCurrent = true, stitchDarkGreen, textPrimary, textSecondary)
                        }
                    }

                    if (otherDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.security_devices),
                            style = MaterialTheme.typography.labelMedium,
                            color = textSecondary
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(otherDevices.take(5)) { device ->
                                DeviceItem(device, isCurrent = false, stitchDarkGreen, textPrimary, textSecondary)
                            }
                        }
                    }

                    if (currentDevices.isEmpty() && otherDevices.isEmpty()) {
                        Text(
                            stringResource(R.string.security_no_data),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = textSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDevicesDialog() }) {
                    Text(stringResource(R.string.btn_close), color = stitchDarkGreen)
                }
            },
            containerColor = bgCard,
            titleContentColor = textPrimary,
            textContentColor = textSecondary
        )
    }

    // Re-authentication Dialog
    ReauthDialog(
        isVisible = uiState.requireReauth,
        hasPasswordProvider = uiState.hasPasswordProvider,
        hasGoogleProvider = uiState.hasGoogleProvider,
        isLoading = uiState.reauthLoading,
        errorMessage = uiState.reauthError,
        onDismiss = { viewModel.clearReauthFlag() },
        onReauthWithPassword = { password -> viewModel.reauthWithPassword(password) },
        onReauthWithGoogle = { activity -> viewModel.reauthWithGoogle(activity) },
        onClearError = { viewModel.clearReauthError() }
    )
}

// ================= Components =================

@Composable
private fun SecuritySectionTitle(title: String, color: Color) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SecurityGroup(bgColor: Color, content: @Composable ColumnScope.() -> Unit) {
     Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SecurityItemRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    textPrimary: Color,
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
            color = textPrimary,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SecuritySwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textPrimary: Color,
    trackColor: Color,
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
            color = textPrimary,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
             colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = trackColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E7EB),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun LoginHistoryItem(
    item: LoginHistoryItem,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Smartphone,
            contentDescription = null,
            tint = if (item.isCurrent) primaryColor else secondaryTextColor
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                if (item.isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = primaryColor.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = formatTimestamp(item.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
        }
    }
}

@Composable
private fun DeviceItem(
    item: LoginHistoryItem, 
    isCurrent: Boolean,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) primaryColor.copy(alpha = 0.1f) 
                           else Color.Transparent
        ),
        border = if(!isCurrent) androidx.compose.foundation.BorderStroke(1.dp, secondaryTextColor.copy(alpha=0.2f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Smartphone,
                contentDescription = null,
                tint = if (isCurrent) primaryColor else secondaryTextColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = item.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
