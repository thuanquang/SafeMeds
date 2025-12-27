package com.safemed.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.ui.component.*
import com.safemed.ui.theme.SurfaceLight
import com.safemed.ui.theme.TextSecondary

/**
 * Màn hình đăng ký theo thiết kế Figma
 * 
 * @param onRegisterSuccess Callback khi đăng ký thành công, navigate đến Home
 * @param onNavigateToLogin Callback để navigate đến màn hình đăng nhập
 * @param viewModel ViewModel quản lý logic đăng ký, inject bởi Hilt
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {
    // Collect UI state từ ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation khi đăng ký thành công
    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            onRegisterSuccess()
            viewModel.onNavigateHandled()
        }
    }

    // Main content với scroll để hỗ trợ các màn hình nhỏ
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ===== Header Section =====
            Text(
                text = "Đăng ký tài khoản",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tạo tài khoản để sử dụng SafeMed",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== Form Section =====
            // Họ và tên TextField
            SafeMedTextField(
                value = uiState.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = "Họ và tên",
                placeholder = "Nguyễn Văn A",
                isError = uiState.fullNameError != null,
                errorMessage = uiState.fullNameError,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            // Số điện thoại TextField
            SafeMedTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = "Số điện thoại",
                placeholder = "0123 456 789",
                keyboardType = KeyboardType.Phone,
                isError = uiState.phoneError != null,
                errorMessage = uiState.phoneError,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mật khẩu TextField
            SafeMedPasswordField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Mật khẩu",
                placeholder = "Nhập mật khẩu",
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Xác nhận mật khẩu TextField
            SafeMedPasswordField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Xác nhận mật khẩu",
                placeholder = "Nhập lại mật khẩu",
                isError = uiState.confirmPasswordError != null,
                errorMessage = uiState.confirmPasswordError,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== Terms & Conditions Checkbox =====
            SafeMedCheckboxWithLinks(
                checked = uiState.agreeToTerms,
                onCheckedChange = viewModel::onAgreeToTermsChange,
                modifier = Modifier.fillMaxWidth()
            )

            // Terms error
            uiState.termsError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 48.dp, top = 4.dp)
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

            // ===== Register Button =====
            SafeMedPrimaryButton(
                text = "Đăng ký tài khoản",
                onClick = viewModel::onRegisterClick,
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Navigate to Login Link =====
            TextWithLink(
                normalText = "Đã có tài khoản?",
                linkText = "Đăng nhập ngay",
                onLinkClick = onNavigateToLogin
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Divider =====
            DividerWithText(text = "Hoặc đăng ký với")

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Google Sign Up Button =====
            GoogleSignInButton(
                text = "Đăng ký với Google",
                onClick = viewModel::onGoogleSignUpClick,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== Footer =====
            CopyrightFooter(
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
