package com.safemed.ui.screen

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
 * Data class chứa trạng thái UI của màn hình profile
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val isLogoutSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel quản lý logic màn hình profile
 * Xử lý đăng xuất và clear credentials
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Đăng xuất người dùng
     * Clear Firebase Auth và Credential Manager
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.signOut()
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isLogoutSuccess = true
                        ) 
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Đăng xuất thất bại: ${exception.localizedMessage}"
                        )
                    }
                }
        }
    }

    /**
     * Reset trạng thái sau khi navigate
     */
    fun onNavigateHandled() {
        _uiState.update { it.copy(isLogoutSuccess = false) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
