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
 * Sử dụng Hilt để dependency injection
 * 
 * Thiết kế theo Clean Architecture:
 * - UI State được quản lý bởi StateFlow
 * - Có thể dễ dàng tích hợp UseCase và Repository sau này
 * - Placeholder cho việc gọi API (Supabase/Firebase)
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    // TODO: Inject AuthRepository khi tích hợp backend
    // private val authRepository: AuthRepository
) : ViewModel() {

    // StateFlow cho UI state
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /**
     * Cập nhật họ tên khi người dùng nhập
     */
    fun onFullNameChange(fullName: String) {
        _uiState.update { currentState ->
            currentState.copy(
                fullName = fullName,
                fullNameError = null,
                generalError = null
            )
        }
    }

    /**
     * Cập nhật email khi người dùng nhập
     */
    fun onEmailChange(email: String) {
        _uiState.update { currentState ->
            currentState.copy(
                email = email,
                emailError = null,
                generalError = null
            )
        }
    }

    /**
     * Cập nhật số điện thoại khi người dùng nhập
     */
    fun onPhoneChange(phone: String) {
        _uiState.update { currentState ->
            currentState.copy(
                phone = phone,
                phoneError = null,
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
                confirmPasswordError = null,
                generalError = null
            )
        }
    }

    /**
     * Cập nhật xác nhận mật khẩu khi người dùng nhập
     */
    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { currentState ->
            currentState.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = null,
                generalError = null
            )
        }
    }

    /**
     * Cập nhật trạng thái đồng ý điều khoản
     */
    fun onAgreeToTermsChange(agree: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                agreeToTerms = agree,
                termsError = null
            )
        }
    }

    /**
     * Validate form đăng ký
     * @return true nếu form hợp lệ
     */
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
     * Xử lý đăng ký tài khoản
     * Placeholder: Sẽ gọi API khi tích hợp backend
     */
    fun onRegisterClick() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            try {
                // TODO: Gọi API đăng ký thực tế
                // val result = authRepository.register(
                //     fullName = _uiState.value.fullName,
                //     email = _uiState.value.email,
                //     phone = _uiState.value.phone,
                //     password = _uiState.value.password
                // )
                
                // Simulate network delay (placeholder)
                delay(1500)

                // Placeholder: Giả lập đăng ký thành công
                _uiState.update { it.copy(isLoading = false, isRegisterSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        generalError = "Đăng ký thất bại. Vui lòng thử lại."
                    ) 
                }
            }
        }
    }

    /**
     * Xử lý đăng ký với Google
     * Placeholder: Sẽ tích hợp Google Sign-In SDK
     */
    fun onGoogleSignUpClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // TODO: Tích hợp Google Sign-In
                // val googleIdToken = googleSignInClient.signIn()
                // val result = authRepository.registerWithGoogle(googleIdToken)
                
                delay(1500)
                _uiState.update { it.copy(isLoading = false, isRegisterSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        generalError = "Đăng ký với Google thất bại."
                    ) 
                }
            }
        }
    }

    /**
     * Reset trạng thái sau khi navigate
     */
    fun onNavigateHandled() {
        _uiState.update { it.copy(isRegisterSuccess = false) }
    }
}
