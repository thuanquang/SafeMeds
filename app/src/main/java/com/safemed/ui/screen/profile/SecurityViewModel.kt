package com.safemed.ui.screen.profile

import android.app.Activity
import android.app.Application
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.safemed.data.repository.AuthRepository
import com.safemed.data.repository.UserPreferencesRepository
import com.safemed.data.repository.FirebaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LoginHistoryItem(
    val deviceName: String,
    val location: String,
    val timestamp: Long,
    val isCurrent: Boolean = false
)

data class SecurityUiState(
    val biometricEnabled: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val loginNotificationEnabled: Boolean = true,
    val isBiometricAvailable: Boolean = false,
    val biometricStatusMessage: String = "",
    val loginHistory: List<LoginHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showBiometricVerifyDialog: Boolean = false,
    val showLoginHistoryDialog: Boolean = false,
    val showDevicesDialog: Boolean = false,
    val message: String? = null,
    val accountDeleted: Boolean = false,
    val requireReauth: Boolean = false,
    val reauthLoading: Boolean = false,
    val reauthError: String? = null,
    val hasPasswordProvider: Boolean = true,
    val hasGoogleProvider: Boolean = false
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val firebaseHelper: FirebaseHelper
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        checkBiometricAvailability()
        loadSettings()
        loadLoginHistory()
        checkAuthProviders()
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(getApplication())
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                _uiState.update { 
                    it.copy(
                        isBiometricAvailable = true,
                        biometricStatusMessage = "Thiết bị hỗ trợ xác thực sinh trắc học"
                    ) 
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                _uiState.update { 
                    it.copy(
                        isBiometricAvailable = false,
                        biometricStatusMessage = "Thiết bị không có phần cứng sinh trắc học"
                    ) 
                }
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                _uiState.update { 
                    it.copy(
                        isBiometricAvailable = false,
                        biometricStatusMessage = "Phần cứng sinh trắc học hiện không khả dụng"
                    ) 
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                _uiState.update { 
                    it.copy(
                        isBiometricAvailable = false,
                        biometricStatusMessage = "Chưa đăng ký vân tay/Face ID trên thiết bị"
                    ) 
                }
            }
            else -> {
                _uiState.update { 
                    it.copy(
                        isBiometricAvailable = false,
                        biometricStatusMessage = "Không thể sử dụng xác thực sinh trắc học"
                    ) 
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.biometricEnabled,
                userPreferencesRepository.twoFactorEnabled,
                userPreferencesRepository.loginNotificationEnabled
            ) { biometric, twoFactor, loginNotification ->
                _uiState.value.copy(
                    biometricEnabled = biometric,
                    twoFactorEnabled = twoFactor,
                    loginNotificationEnabled = loginNotification
                )
            }.collect { newState ->
                _uiState.update { 
                    it.copy(
                        biometricEnabled = newState.biometricEnabled,
                        twoFactorEnabled = newState.twoFactorEnabled,
                        loginNotificationEnabled = newState.loginNotificationEnabled
                    )
                }
            }
        }
    }

    private fun loadLoginHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            firebaseHelper.getLoginHistory { history ->
                val items = history.map { data ->
                    LoginHistoryItem(
                        deviceName = data["deviceName"] as? String ?: "Unknown Device",
                        location = data["location"] as? String ?: "Unknown",
                        timestamp = data["timestamp"] as? Long ?: 0L,
                        isCurrent = data["isCurrent"] as? Boolean ?: false
                    )
                }.sortedByDescending { it.timestamp }
                
                _uiState.update { 
                    it.copy(
                        loginHistory = items,
                        isLoading = false
                    ) 
                }
            }
        }
    }

    // ===== Biometric =====
    
    fun requestBiometricToggle(enable: Boolean) {
        if (enable && _uiState.value.isBiometricAvailable) {
            // Request biometric verification before enabling
            _uiState.update { it.copy(showBiometricVerifyDialog = true) }
        } else {
            // Disable directly
            setBiometricEnabled(false)
        }
    }

    fun onBiometricVerified(success: Boolean) {
        _uiState.update { it.copy(showBiometricVerifyDialog = false) }
        if (success) {
            setBiometricEnabled(true)
            _uiState.update { it.copy(message = "Đã bật xác thực sinh trắc học") }
        } else {
            _uiState.update { it.copy(message = "Xác thực không thành công") }
        }
    }

    private fun setBiometricEnabled(enabled: Boolean) {
        userPreferencesRepository.setBiometricEnabled(enabled)
        _uiState.update { it.copy(biometricEnabled = enabled) }
    }

    // ===== Two Factor =====
    
    fun toggleTwoFactor(enabled: Boolean) {
        // Note: Full 2FA requires backend implementation
        // For now, just save preference and show message
        userPreferencesRepository.setTwoFactorEnabled(enabled)
        _uiState.update { 
            it.copy(
                twoFactorEnabled = enabled,
                message = if (enabled) {
                    "Tính năng 2FA đang được phát triển"
                } else {
                    "Đã tắt xác thực 2 yếu tố"
                }
            ) 
        }
    }

    // ===== Login Notification =====
    
    fun toggleLoginNotification(enabled: Boolean) {
        userPreferencesRepository.setLoginNotificationEnabled(enabled)
        _uiState.update { it.copy(loginNotificationEnabled = enabled) }
    }

    // ===== Login History =====
    
    fun showLoginHistoryDialog() {
        loadLoginHistory()
        _uiState.update { it.copy(showLoginHistoryDialog = true) }
    }

    fun dismissLoginHistoryDialog() {
        _uiState.update { it.copy(showLoginHistoryDialog = false) }
    }

    // ===== Devices =====
    
    fun showDevicesDialog() {
        _uiState.update { it.copy(showDevicesDialog = true) }
    }

    fun dismissDevicesDialog() {
        _uiState.update { it.copy(showDevicesDialog = false) }
    }

    // ===== Delete Account =====
    
    fun showDeleteAccountDialog() {
        _uiState.update { it.copy(showDeleteAccountDialog = true) }
    }

    fun dismissDeleteAccountDialog() {
        _uiState.update { it.copy(showDeleteAccountDialog = false) }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteAccountDialog = false) }
            
            authRepository.deleteAccount()
                .onSuccess {
                    // Clear local preferences
                    userPreferencesRepository.clearAllSettings()
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            accountDeleted = true,
                            message = "Tài khoản đã được xóa"
                        ) 
                    }
                }
                .onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("requires-recent-login") == true ||
                        exception.message?.contains("re-authenticate") == true -> {
                            _uiState.update { it.copy(requireReauth = true) }
                            "Vui lòng đăng nhập lại để xác nhận xóa tài khoản"
                        }
                        else -> "Không thể xóa tài khoản: ${exception.message}"
                    }
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            message = errorMessage
                        ) 
                    }
                }
        }
    }

    fun onReauthCompleted() {
        _uiState.update { it.copy(requireReauth = false, reauthLoading = false, reauthError = null) }
        // Retry delete after reauth
        deleteAccount()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearReauthFlag() {
        _uiState.update { it.copy(requireReauth = false, reauthLoading = false, reauthError = null) }
    }
    
    fun clearReauthError() {
        _uiState.update { it.copy(reauthError = null) }
    }
    
    fun reauthWithPassword(password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(reauthLoading = true, reauthError = null) }
            try {
                val user = Firebase.auth.currentUser
                if (user == null) {
                    _uiState.update { it.copy(reauthLoading = false, reauthError = "Không tìm thấy tài khoản") }
                    return@launch
                }
                val email = user.email ?: ""
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()
                onReauthCompleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(reauthLoading = false, reauthError = "Mật khẩu không chính xác") }
            }
        }
    }
    
    fun reauthWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(reauthLoading = true, reauthError = null) }
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(activity.getString(com.safemed.R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(activity, gso)
                val account = googleSignInClient.silentSignIn().await()
                val idToken = account.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    Firebase.auth.currentUser?.reauthenticate(credential)?.await()
                    onReauthCompleted()
                } else {
                    _uiState.update { it.copy(reauthLoading = false, reauthError = "Không thể xác thực với Google") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(reauthLoading = false, reauthError = "Xác thực Google thất bại: ${e.message}") }
            }
        }
    }
    
    private fun checkAuthProviders() {
        val user = Firebase.auth.currentUser
        user?.let {
            val hasPassword = it.providerData.any { p -> p.providerId == "password" }
            val hasGoogle = it.providerData.any { p -> p.providerId == "google.com" }
            _uiState.update { state -> state.copy(hasPasswordProvider = hasPassword, hasGoogleProvider = hasGoogle) }
        }
    }
}
