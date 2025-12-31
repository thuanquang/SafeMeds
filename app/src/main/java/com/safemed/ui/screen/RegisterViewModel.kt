package com.safemed.ui.screen

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class chứa trạng thái UI của màn hình đăng ký
 * Sử dụng StateFlow để reactive với Compose
 */
data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreeToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val isRegisterSuccess: Boolean = false,
    val fullNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val generalError: String? = null
)

/**
 * ViewModel quản lý logic màn hình đăng ký
 * Tích hợp Firebase Authentication với Google Sign-In
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, fullNameError = null, generalError = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneError = null, generalError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { 
            it.copy(password = password, passwordError = null, confirmPasswordError = null, generalError = null) 
        }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null, generalError = null) }
    }

    fun onAgreeToTermsChange(agree: Boolean) {
        _uiState.update { it.copy(agreeToTerms = agree, termsError = null) }
    }

    private fun validateForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true

        // Validate họ tên
        if (currentState.fullName.isBlank()) {
            _uiState.update { it.copy(fullNameError = "Vui lòng nhập họ và tên") }
            isValid = false
        } else if (currentState.fullName.length < 2) {
            _uiState.update { it.copy(fullNameError = "Họ tên phải có ít nhất 2 ký tự") }
            isValid = false
        }

        // Validate email
        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Vui lòng nhập email") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
            _uiState.update { it.copy(emailError = "Email không hợp lệ") }
            isValid = false
        }

        // Validate số điện thoại
        if (currentState.phone.isBlank()) {
            _uiState.update { it.copy(phoneError = "Vui lòng nhập số điện thoại") }
            isValid = false
        } else if (!currentState.phone.matches(Regex("^[0-9]{10,11}$"))) {
            _uiState.update { it.copy(phoneError = "Số điện thoại không hợp lệ") }
            isValid = false
        }

        // Validate mật khẩu
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Vui lòng nhập mật khẩu") }
            isValid = false
        } else if (currentState.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Mật khẩu phải có ít nhất 6 ký tự") }
            isValid = false
        }

        // Validate xác nhận mật khẩu
        if (currentState.confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Vui lòng xác nhận mật khẩu") }
            isValid = false
        } else if (currentState.password != currentState.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Mật khẩu xác nhận không khớp") }
            isValid = false
        }

        // Validate điều khoản
        if (!currentState.agreeToTerms) {
            _uiState.update { it.copy(termsError = "Bạn cần đồng ý với điều khoản sử dụng") }
            isValid = false
        }

        return isValid
    }

    /**
     * Đăng ký bằng Email/Password
     */
    fun onRegisterClick() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            authRepository.createUserWithEmailPassword(
                email = _uiState.value.email,
                password = _uiState.value.password,
                fullName = _uiState.value.fullName,
                phone = _uiState.value.phone
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isRegisterSuccess = true) }
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
     * Đăng ký bằng Google
     * @param activity Activity context cần thiết cho Credential Manager
     */
    fun onGoogleSignUpClick(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            authRepository.signInWithGoogle(activity)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isRegisterSuccess = true) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = "Đăng ký với Google thất bại: ${exception.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun onNavigateHandled() {
        _uiState.update { it.copy(isRegisterSuccess = false) }
    }

    private fun mapFirebaseError(exception: Throwable): String {
        return when {
            exception.message?.contains("EMAIL_EXISTS") == true ||
            exception.message?.contains("email-already-in-use") == true ->
                "Email này đã được sử dụng"
            exception.message?.contains("WEAK_PASSWORD") == true ->
                "Mật khẩu quá yếu. Vui lòng chọn mật khẩu mạnh hơn."
            exception.message?.contains("INVALID_EMAIL") == true ->
                "Email không hợp lệ"
            exception.message?.contains("NETWORK") == true ->
                "Lỗi kết nối mạng"
            else -> "Đăng ký thất bại. Vui lòng thử lại."
        }
    }
}
