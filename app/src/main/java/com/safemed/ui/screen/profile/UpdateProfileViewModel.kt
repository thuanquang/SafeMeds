package com.safemed.ui.screen.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.model.User
import com.safemed.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val user: User? = null,
    val fullName: String = "",
    val avatarUrl: String = "",
    val selectedImageUri: Uri? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class UpdateProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateProfileUiState())
    val uiState: StateFlow<UpdateProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * Load thông tin user từ Firestore
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            profileRepository.getCurrentUserProfile()
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            fullName = user?.fullName ?: "",
                            avatarUrl = user?.avatarUrl ?: ""
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Không thể tải thông tin: ${exception.message}"
                        )
                    }
                }
        }
    }

    /**
     * Update tên hiển thị
     */
    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name) }
    }

    /**
     * Chọn ảnh từ gallery
     */
    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    /**
     * Upload avatar và cập nhật profile
     */
    fun saveProfile() {
        val state = _uiState.value
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            
            try {
                // Nếu có ảnh mới được chọn, upload trước
                val finalAvatarUrl = if (state.selectedImageUri != null) {
                    _uiState.update { it.copy(isUploadingAvatar = true) }
                    
                    val uploadResult = profileRepository.uploadAvatar(state.selectedImageUri)
                    
                    _uiState.update { it.copy(isUploadingAvatar = false) }
                    
                    uploadResult.getOrElse {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "Upload ảnh thất bại: ${it.errorMessage}"
                            )
                        }
                        return@launch
                    }
                } else {
                    state.avatarUrl
                }
                
                // Cập nhật profile với tên và avatar URL mới
                val updateResult = if (finalAvatarUrl != state.avatarUrl) {
                    profileRepository.updateProfileWithAvatar(state.fullName, finalAvatarUrl)
                } else {
                    profileRepository.updateProfile(state.fullName)
                }
                
                updateResult
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isSuccess = true,
                                avatarUrl = finalAvatarUrl,
                                selectedImageUri = null
                            )
                        }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "Cập nhật thất bại: ${exception.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isUploadingAvatar = false,
                        errorMessage = "Lỗi: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Xóa avatar hiện tại
     */
    fun deleteAvatar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true) }
            
            profileRepository.deleteAvatar()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            avatarUrl = "",
                            selectedImageUri = null
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            errorMessage = "Xóa ảnh thất bại: ${exception.message}"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onNavigateHandled() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}
