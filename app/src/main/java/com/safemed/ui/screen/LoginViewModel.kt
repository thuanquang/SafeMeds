package com.safemed.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class chứa trạng thái UI của màn hình đăng nhập
 * Sử dụng StateFlow để reactive với Compose
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
)

/**
 * ViewModel quản lý logic màn hình đăng nhập
 * Sử dụng Hilt để dependency injection
 * 
 * Thiết kế theo Clean Architecture:
 * - UI State được quản lý bởi StateFlow
 * - Có thể dễ dàng tích hợp UseCase và Repository sau này
 * - Placeholder cho việc gọi API (Supabase/Firebase)
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    // TODO: Inject AuthRepository khi tích hợp backend
    // private val authRepository: AuthRepository
) : ViewModel() {

    // StateFlow cho UI state
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Cập nhật email khi người dùng nhập
     */
    fun onEmailChange(email: String) {
        _uiState.update { currentState ->
            currentState.copy(
                email = email,
                emailError = null, // Xóa lỗi khi người dùng bắt đầu nhập lại
                generalError = null
            )
        }
    }

    /**
     * Cập nhật mật khẩu khi người dùng nhập
     */
    fun onPasswordChange(password: String) {
        _uiState.update { currentState ->
            currentState.copy(
                password = password,
                passwordError = null,
                generalError = null
            )
        }
    }

    /**
     * Cập nhật trạng thái ghi nhớ đăng nhập
     */
    fun onRememberMeChange(rememberMe: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(rememberMe = rememberMe)
        }
    }

    /**
     * Validate form đăng nhập
     * @return true nếu form hợp lệ
     */
    private fun validateForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true

        // Validate email
        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Vui lòng nhập email") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
            _uiState.update { it.copy(emailError = "Email không hợp lệ") }
            isValid = false
        }

        // Validate password
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Vui lòng nhập mật khẩu") }
            isValid = false
        } else if (currentState.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Mật khẩu phải có ít nhất 6 ký tự") }
            isValid = false
        }

        return isValid
    }

    /**
     * Xử lý đăng nhập với email và mật khẩu
     * Placeholder: Sẽ gọi API khi tích hợp backend
     */
    fun onLoginClick() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            try {
                // TODO: Gọi API đăng nhập thực tế
                // val result = authRepository.login(email, password)
                
                // Simulate network delay (placeholder)
                delay(1500)

                // Placeholder: Giả lập đăng nhập thành công
                _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        generalError = "Đăng nhập thất bại. Vui lòng thử lại."
                    ) 
                }
            }
        }
    }

    /**
     * Xử lý đăng nhập với Google
     * Placeholder: Sẽ tích hợp Google Sign-In SDK
     */
    fun onGoogleSignInClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // TODO: Tích hợp Google Sign-In
                // val googleIdToken = googleSignInClient.signIn()
                // val result = authRepository.loginWithGoogle(googleIdToken)
                
                delay(1500)
                _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        generalError = "Đăng nhập với Google thất bại."
                    ) 
                }
            }
        }
    }

    /**
     * Reset trạng thái sau khi navigate
     */
    fun onNavigateHandled() {
        _uiState.update { it.copy(isLoginSuccess = false) }
    }

    /**
     * Xử lý quên mật khẩu
     * Placeholder: Sẽ navigate đến màn hình quên mật khẩu
     */
    fun onForgotPasswordClick() {
        // TODO: Navigate to forgot password screen
    }
}
