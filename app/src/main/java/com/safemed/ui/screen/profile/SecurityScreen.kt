package com.safemed.ui.screen.profile

import android.app.Activity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.ui.component.ProfileMenuItem
import com.safemed.ui.component.ProfileMenuItemWithSwitch
import com.safemed.ui.component.ReauthDialog
import com.safemed.ui.theme.EmeraldGreen
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_title)) },
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
                // ===== Xác thực sinh trắc học =====
                SecuritySectionHeader(title = stringResource(R.string.security_authentication))

                ProfileMenuItemWithSwitch(
                    icon = Icons.Outlined.Fingerprint,
                    title = stringResource(R.string.security_biometric),
                    checked = uiState.biometricEnabled,
                    onCheckedChange = { viewModel.requestBiometricToggle(it) }
                )

                if (!uiState.isBiometricAvailable) {
                    Text(
                        text = uiState.biometricStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ===== Xác thực 2 yếu tố =====
                SecuritySectionHeader(title = stringResource(R.string.security_2fa))

                ProfileMenuItemWithSwitch(
                    icon = Icons.Outlined.Security,
                    title = stringResource(R.string.security_2fa),
                    checked = uiState.twoFactorEnabled,
                    onCheckedChange = { viewModel.toggleTwoFactor(it) }
                )

                Text(
                    text = stringResource(R.string.security_2fa_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ===== Thông báo đăng nhập =====
                SecuritySectionHeader(title = stringResource(R.string.security_login_notification))

                ProfileMenuItemWithSwitch(
                    icon = Icons.Outlined.NotificationsActive,
                    title = stringResource(R.string.security_login_notification),
                    checked = uiState.loginNotificationEnabled,
                    onCheckedChange = { viewModel.toggleLoginNotification(it) }
                )

                Text(
                    text = stringResource(R.string.security_login_notification_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ===== Quản lý phiên đăng nhập =====
                SecuritySectionHeader(title = stringResource(R.string.security_session))

                ProfileMenuItem(
                    icon = Icons.Outlined.Devices,
                    title = stringResource(R.string.security_devices),
                    onClick = { viewModel.showDevicesDialog() }
                )

                ProfileMenuItem(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.security_login_history),
                    onClick = { viewModel.showLoginHistoryDialog() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ===== Vùng nguy hiểm =====
                SecuritySectionHeader(title = stringResource(R.string.security_danger), isWarning = true)

                ProfileMenuItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = stringResource(R.string.security_delete_account),
                    onClick = { viewModel.showDeleteAccountDialog() },
                    iconTint = Color.Red
                )

                Text(
                    text = stringResource(R.string.security_delete_account_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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

    // Delete Account Confirmation Dialog
    if (uiState.showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAccountDialog() },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = Color.Red) },
            title = { Text(stringResource(R.string.security_delete_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.security_delete_confirm_message))
                }
            },
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
                    Text(stringResource(R.string.btn_cancel))
                }
            }
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
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(uiState.loginHistory) { item ->
                            LoginHistoryItem(item)
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissLoginHistoryDialog() }) {
                    Text(stringResource(R.string.btn_close), color = EmeraldGreen)
                }
            }
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
                            color = EmeraldGreen
                        )
                        currentDevices.forEach { device ->
                            DeviceItem(device, isCurrent = true)
                        }
                    }

                    if (otherDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.security_devices),
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(otherDevices.take(5)) { device ->
                                DeviceItem(device, isCurrent = false)
                            }
                        }
                    }

                    if (currentDevices.isEmpty() && otherDevices.isEmpty()) {
                        Text(
                            stringResource(R.string.security_no_data),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDevicesDialog() }) {
                    Text(stringResource(R.string.btn_close), color = EmeraldGreen)
                }
            }
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

@Composable
private fun SecuritySectionHeader(
    title: String,
    isWarning: Boolean = false
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = if (isWarning) Color.Red else EmeraldGreen,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun LoginHistoryItem(item: LoginHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Smartphone,
            contentDescription = null,
            tint = if (item.isCurrent) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (item.isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Hiện tại",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = formatTimestamp(item.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceItem(item: LoginHistoryItem, isCurrent: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) EmeraldGreen.copy(alpha = 0.1f) 
                           else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Smartphone,
                contentDescription = null,
                tint = if (isCurrent) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = item.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Không xác định"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
