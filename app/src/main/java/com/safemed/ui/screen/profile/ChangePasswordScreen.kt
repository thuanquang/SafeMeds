package com.safemed.ui.screen.profile

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.ui.component.ReauthDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Colors
    val stitchBg = colorResource(id = R.color.stitch_bg)
    val stitchDarkGreen = colorResource(id = R.color.stitch_dark_green)
    val stitchLime = colorResource(id = R.color.stitch_lime)
    val stitchTextPrimary = colorResource(id = R.color.stitch_text_primary)
    val stitchTextSecondary = colorResource(id = R.color.stitch_text_secondary)
    val bgCard = colorResource(id = R.color.bg_card)

    // Password visibility states
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            uiState.successMessage?.let {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.onNavigateHandled()
        }
    }

    // Show error
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Re-authentication Dialog
    ReauthDialog(
        isVisible = uiState.showReauthDialog,
        hasPasswordProvider = uiState.hasPasswordProvider,
        hasGoogleProvider = uiState.hasGoogleProvider,
        isLoading = uiState.isReauthenticating,
        errorMessage = uiState.reauthError,
        onDismiss = viewModel::hideReauthDialog,
        onReauthWithPassword = viewModel::reauthWithPassword,
        onReauthWithGoogle = { activity -> viewModel.reauthWithGoogle(activity) },
        onClearError = viewModel::clearReauthError
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.hasPasswordProvider) stringResource(R.string.change_password_title) else stringResource(R.string.change_password_set_password),
                        fontWeight = FontWeight.Bold,
                        color = stitchTextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = stitchTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = stitchBg
                )
            )
        },
        containerColor = stitchBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Icon
             Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(stitchLime, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = stitchDarkGreen
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title and description
            Text(
                text = if (uiState.hasPasswordProvider) {
                    stringResource(R.string.change_password_title)
                } else {
                    stringResource(R.string.change_password_set_password)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = stitchTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (uiState.hasPasswordProvider) {
                    stringResource(R.string.change_password_desc_change)
                } else {
                    stringResource(R.string.change_password_desc_set)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = stitchTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== Form Section =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = bgCard
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Current Password (chỉ hiển thị nếu user đã có password)
                    if (uiState.hasPasswordProvider) {
                        StitchOutlinedTextField(
                            value = uiState.currentPassword,
                            onValueChange = viewModel::onCurrentPasswordChange,
                            label = stringResource(R.string.change_password_current),
                            isVisible = currentPasswordVisible,
                            onVisibilityChange = { currentPasswordVisible = !currentPasswordVisible },
                            enabled = !uiState.isLoading,
                            stitchDarkGreen = stitchDarkGreen,
                            stitchTextPrimary = stitchTextPrimary
                        )
                    }

                    // New Password
                    StitchOutlinedTextField(
                        value = uiState.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        label = stringResource(R.string.change_password_new),
                        isVisible = newPasswordVisible,
                        onVisibilityChange = { newPasswordVisible = !newPasswordVisible },
                        enabled = !uiState.isLoading,
                        supportingText = stringResource(R.string.change_password_hint_min),
                        stitchDarkGreen = stitchDarkGreen,
                        stitchTextPrimary = stitchTextPrimary
                    )

                    // Confirm Password
                    StitchOutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = stringResource(R.string.change_password_confirm),
                        isVisible = confirmPasswordVisible,
                        onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible },
                        enabled = !uiState.isLoading,
                        isError = uiState.confirmPassword.isNotEmpty() && uiState.newPassword != uiState.confirmPassword,
                        errorMessage = if (uiState.confirmPassword.isNotEmpty() && uiState.newPassword != uiState.confirmPassword) {
                             stringResource(R.string.change_password_error_mismatch)
                        } else null,
                        stitchDarkGreen = stitchDarkGreen,
                        stitchTextPrimary = stitchTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // ===== Save Button =====
            Button(
                onClick = viewModel::savePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading && 
                          uiState.newPassword.length >= 6 && 
                          uiState.newPassword == uiState.confirmPassword &&
                          (uiState.hasPasswordProvider.not() || uiState.currentPassword.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = stitchLime,
                    contentColor = stitchDarkGreen,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = stitchDarkGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.processing))
                } else {
                    Text(
                        text = if (uiState.hasPasswordProvider) stringResource(R.string.change_password_title) else stringResource(R.string.change_password_set_password),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StitchOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    stitchDarkGreen: Color,
    stitchTextPrimary: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Hide" else "Show",
                    tint = stitchDarkGreen
                )
            }
        },
        supportingText = {
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else if (supportingText != null) {
                Text(supportingText)
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = stitchDarkGreen,
            focusedLabelColor = stitchDarkGreen,
            cursorColor = stitchDarkGreen,
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            focusedTextColor = stitchTextPrimary,
            unfocusedTextColor = stitchTextPrimary
        )
    )
}
