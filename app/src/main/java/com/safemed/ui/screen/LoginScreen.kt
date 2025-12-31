package com.safemed.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.ui.component.*
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.SurfaceLight
import com.safemed.ui.theme.TextSecondary

/**
 * Màn hình đăng nhập theo thiết kế Figma
 * 
 * @param onLoginSuccess Callback khi đăng nhập thành công, navigate đến Home
 * @param onNavigateToRegister Callback để navigate đến màn hình đăng ký
 * @param viewModel ViewModel quản lý logic đăng nhập, inject bởi Hilt
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    // Lấy Activity context để truyền vào Google Sign-In
    val context = LocalContext.current
    val activity = context as Activity

    // Collect UI state từ ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation khi đăng nhập thành công
    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
            viewModel.onNavigateHandled()
        }
    }

    // Main content với scroll để hỗ trợ các màn hình nhỏ
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // ===== Header Section =====
            Text(
                text = "Đăng nhập",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chào mừng bạn trở lại SafeMed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== Form Section =====
            // Email TextField
            SafeMedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                placeholder = "example@email.com",
                keyboardType = KeyboardType.Email,
                isError = uiState.emailError != null,
                errorMessage = uiState.emailError,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password TextField
            SafeMedPasswordField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Mật khẩu",
                placeholder = "Nhập mật khẩu",
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ===== Remember Me & Forgot Password Row =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeMedCheckbox(
                    checked = uiState.rememberMe,
                    onCheckedChange = viewModel::onRememberMeChange,
                    text = "Ghi nhớ đăng nhập"
                )
                ClickableTextLink(
                    text = "Quên mật khẩu?",
                    onClick = viewModel::onForgotPasswordClick,
                    color = EmeraldGreen
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Error Message =====
            uiState.generalError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ===== Login Button =====
            SafeMedPrimaryButton(
                text = "Đăng nhập",
                onClick = viewModel::onLoginClick,
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Divider =====
            DividerWithText(text = "Hoặc đăng nhập với")

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Google Sign In Button =====
            GoogleSignInButton(
                text = "Đăng nhập với Google",
                onClick = { viewModel.onGoogleSignInClick(activity) },
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== Navigate to Register Link =====
            TextWithLink(
                normalText = "Chưa có tài khoản?",
                linkText = "Đăng ký ngay",
                onLinkClick = onNavigateToRegister
            )

            Spacer(modifier = Modifier.weight(1f))

            // ===== Footer =====
            CopyrightFooter(
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

