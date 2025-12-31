package com.safemed.ui.screen

import android.app.Activity
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.repository.AuthRepository
import com.safemed.data.repository.FirebaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val generalError: String? = null,
    val emailLinkSent: Boolean = false,
    val successMessage: String? = null
)

/**
 * ViewModel quản lý logic màn hình đăng nhập
 * Tích hợp Firebase Authentication với Google Sign-In và Email Link
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sharedPreferences: SharedPreferences,
    private val firebaseHelper: FirebaseHelper
) : ViewModel() {

    companion object {
        private const val KEY_PENDING_EMAIL = "pending_email_for_sign_in"
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Cập nhật email khi người dùng nhập
     */
    fun onEmailChange(email: String) {
        _uiState.update { 
            it.copy(
                email = email, 
                emailError = null, 
                generalError = null,
                successMessage = null
            ) 
        }
    }

    /**
     * Cập nhật mật khẩu khi người dùng nhập
     */
    fun onPasswordChange(password: String) {
        _uiState.update { 
            it.copy(
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
        _uiState.update { it.copy(rememberMe = rememberMe) }
    }

    /**
     * Validate form đăng nhập
     */
    private fun validateForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true

        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Vui lòng nhập email") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
            _uiState.update { it.copy(emailError = "Email không hợp lệ") }
            isValid = false
        }

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
     * Đăng nhập bằng Email/Password
     */
    fun onLoginClick() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            authRepository.signInWithEmailPassword(_uiState.value.email, _uiState.value.password)
                .onSuccess {
                    // Save login history
                    firebaseHelper.saveLoginHistory()
                    _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = mapFirebaseError(exception)
                        )
                    }
                }
        }
    }

    /**
     * Đăng nhập bằng Google
     * @param activity Activity context cần thiết cho Credential Manager
     */
    fun onGoogleSignInClick(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            authRepository.signInWithGoogle(activity)
                .onSuccess {
                    // Save login history
                    firebaseHelper.saveLoginHistory()
                    _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = "Đăng nhập Google thất bại: ${exception.localizedMessage}"
                        )
                    }
                }
        }
    }

    /**
     * Gửi Email Link để đăng nhập (passwordless)
     */
    fun onSendEmailLinkClick() {
        if (_uiState.value.email.isBlank() || 
            !android.util.Patterns.EMAIL_ADDRESS.matcher(_uiState.value.email).matches()) {
            _uiState.update { it.copy(emailError = "Vui lòng nhập email hợp lệ") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            authRepository.sendSignInLinkToEmail(_uiState.value.email)
                .onSuccess {
                    savePendingEmail(_uiState.value.email)
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            emailLinkSent = true,
                            successMessage = "Đã gửi link đăng nhập đến email của bạn!"
                        ) 
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = "Gửi email thất bại: ${exception.localizedMessage}"
                        )
                    }
                }
        }
    }

    /**
     * Reset trạng thái sau khi navigate
     */
    fun onNavigateHandled() {
        _uiState.update { it.copy(isLoginSuccess = false, emailLinkSent = false) }
    }

    /**
     * Xử lý quên mật khẩu
     */
    fun onForgotPasswordClick() {
        if (_uiState.value.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Nhập email để đặt lại mật khẩu") }
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(_uiState.value.email).matches()) {
            _uiState.update { it.copy(emailError = "Email không hợp lệ") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            authRepository.sendPasswordResetEmail(_uiState.value.email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Đã gửi email đặt lại mật khẩu!"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = mapFirebaseError(exception)
                        )
                    }
                }
        }
    }

    /**
     * Lưu email đang chờ xác thực (cho Email Link)
     */
    private fun savePendingEmail(email: String) {
        sharedPreferences.edit()
            .putString(KEY_PENDING_EMAIL, email)
            .apply()
    }

    /**
     * Map Firebase error sang thông báo tiếng Việt
     */
    private fun mapFirebaseError(exception: Throwable): String {
        return when {
            exception.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> 
                "Email hoặc mật khẩu không đúng"
            exception.message?.contains("USER_NOT_FOUND") == true -> 
                "Tài khoản không tồn tại"
            exception.message?.contains("WRONG_PASSWORD") == true -> 
                "Mật khẩu không đúng"
            exception.message?.contains("TOO_MANY_REQUESTS") == true -> 
                "Quá nhiều lần thử. Vui lòng thử lại sau."
            exception.message?.contains("NETWORK") == true -> 
                "Lỗi kết nối mạng"
            exception.message?.contains("USER_DISABLED") == true ->
                "Tài khoản đã bị vô hiệu hóa"
            else -> "Đăng nhập thất bại. Vui lòng thử lại."
        }
    }
}
