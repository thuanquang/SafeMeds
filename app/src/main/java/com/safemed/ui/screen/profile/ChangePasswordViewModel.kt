package com.safemed.ui.screen.profile

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.repository.AuthRepository
import com.safemed.data.repository.ReauthenticationRequiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val hasPasswordProvider: Boolean = false,
    val hasGoogleProvider: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val showReauthDialog: Boolean = false,
    val isReauthenticating: Boolean = false,
    val reauthError: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    init {
        checkAuthProviders()
    }

    /**
     * Kiểm tra user có những auth provider nào
     */
    private fun checkAuthProviders() {
        _uiState.update {
            it.copy(
                hasPasswordProvider = authRepository.hasPasswordProvider(),
                hasGoogleProvider = authRepository.hasGoogleProvider()
            )
        }
    }

    fun onCurrentPasswordChange(password: String) {
        _uiState.update { it.copy(currentPassword = password, errorMessage = null) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    /**
     * Xử lý đặt/đổi mật khẩu
     * - Google-only user: Link email/password credential (đặt mật khẩu lần đầu)
     * - Email user: Update password (cần re-auth nếu session cũ)
     */
    fun savePassword() {
        val state = _uiState.value

        // Validate
        if (state.newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu phải có ít nhất 6 ký tự") }
            return
        }

        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu xác nhận không khớp") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (state.hasPasswordProvider) {
                // User đã có password -> cần re-auth trước rồi update password
                reauthAndUpdatePassword()
            } else {
                // Google-only user -> link email/password credential
                linkPasswordToGoogleAccount()
            }
        }
    }

    /**
     * Re-authenticate và update password cho user có sẵn password
     */
    private suspend fun reauthAndUpdatePassword() {
        val state = _uiState.value

        // Nếu chưa nhập current password, yêu cầu nhập
        if (state.currentPassword.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Vui lòng nhập mật khẩu hiện tại"
                )
            }
            return
        }

        // Re-authenticate với current password
        val reauthResult = authRepository.reauthenticateWithPassword(state.currentPassword)

        if (reauthResult.isFailure) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Mật khẩu hiện tại không đúng"
                )
            }
            return
        }

        // Update password
        authRepository.updatePassword(state.newPassword)
            .onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = "Đổi mật khẩu thành công!",
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = ""
                    )
                }
            }
            .onFailure { exception ->
                if (exception is ReauthenticationRequiredException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showReauthDialog = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Đổi mật khẩu thất bại: ${exception.message}"
                        )
                    }
                }
            }
    }

    /**
     * Link email/password credential cho Google-only user
     */
    private suspend fun linkPasswordToGoogleAccount() {
        val state = _uiState.value

        authRepository.linkEmailPassword(state.newPassword)
            .onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        hasPasswordProvider = true,
                        successMessage = "Đặt mật khẩu thành công! Bây giờ bạn có thể đăng nhập bằng email và mật khẩu.",
                        newPassword = "",
                        confirmPassword = ""
                    )
                }
            }
            .onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Đặt mật khẩu thất bại: ${exception.message}"
                    )
                }
            }
    }

    // ==================== Re-authentication Dialog ====================

    fun showReauthDialog() {
        _uiState.update { it.copy(showReauthDialog = true, reauthError = null) }
    }

    fun hideReauthDialog() {
        _uiState.update { it.copy(showReauthDialog = false, reauthError = null) }
    }

    fun reauthWithPassword(password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReauthenticating = true, reauthError = null) }

            authRepository.reauthenticateWithPassword(password)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isReauthenticating = false,
                            showReauthDialog = false
                        )
                    }
                    // Retry update password
                    retryUpdatePassword()
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isReauthenticating = false,
                            reauthError = "Mật khẩu không đúng"
                        )
                    }
                }
        }
    }

    fun reauthWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReauthenticating = true, reauthError = null) }

            authRepository.reauthenticateWithGoogle(activity)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isReauthenticating = false,
                            showReauthDialog = false
                        )
                    }
                    // Retry update password
                    retryUpdatePassword()
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isReauthenticating = false,
                            reauthError = "Xác thực Google thất bại: ${exception.message}"
                        )
                    }
                }
        }
    }

    private suspend fun retryUpdatePassword() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true) }

        authRepository.updatePassword(state.newPassword)
            .onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = "Đổi mật khẩu thành công!",
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = ""
                    )
                }
            }
            .onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Đổi mật khẩu thất bại: ${exception.message}"
                    )
                }
            }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearReauthError() {
        _uiState.update { it.copy(reauthError = null) }
    }

    fun onNavigateHandled() {
        _uiState.update { it.copy(isSuccess = false, successMessage = null) }
    }
}
