package com.safemed.ui.screen.profile

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.repository.UserPreferencesRepository
import com.safemed.data.repository.FirebaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val language: String = "vi",
    val notificationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoSaveHistory: Boolean = true,
    val isLoading: Boolean = false,
    val showClearHistoryDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val message: String? = null,
    val cacheSize: String = "0 KB"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseHelper: FirebaseHelper
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        calculateCacheSize()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Collect each preference individually to avoid combine complexity
            launch {
                userPreferencesRepository.isDarkMode.collect { darkMode ->
                    _uiState.update { it.copy(isDarkMode = darkMode) }
                }
            }
            launch {
                userPreferencesRepository.language.collect { language ->
                    _uiState.update { it.copy(language = language) }
                }
            }
            launch {
                userPreferencesRepository.notificationEnabled.collect { notification ->
                    _uiState.update { it.copy(notificationEnabled = notification) }
                }
            }
            launch {
                userPreferencesRepository.soundEnabled.collect { sound ->
                    _uiState.update { it.copy(soundEnabled = sound) }
                }
            }
            launch {
                userPreferencesRepository.vibrationEnabled.collect { vibration ->
                    _uiState.update { it.copy(vibrationEnabled = vibration) }
                }
            }
            launch {
                userPreferencesRepository.autoSaveHistory.collect { autoSave ->
                    _uiState.update { it.copy(autoSaveHistory = autoSave) }
                }
            }
        }
    }

    private fun calculateCacheSize() {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            val size = getFolderSize(cacheDir)
            val sizeStr = formatSize(size)
            _uiState.update { it.copy(cacheSize = sizeStr) }
        }
    }

    private fun getFolderSize(folder: File): Long {
        var size: Long = 0
        folder.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getFolderSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    // ===== Dark Mode =====
    
    fun toggleDarkMode(enabled: Boolean) {
        userPreferencesRepository.setDarkMode(enabled)
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    // ===== Language =====
    
    fun setLanguage(languageCode: String) {
        userPreferencesRepository.setLanguage(languageCode)
        _uiState.update { it.copy(language = languageCode) }
        
        // Apply language change using AppCompatDelegate
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getLanguageDisplayName(): String {
        return when (_uiState.value.language) {
            "vi" -> "Tiếng Việt"
            "en" -> "English"
            else -> "Tiếng Việt"
        }
    }

    // ===== Notification Settings =====
    
    fun toggleNotification(enabled: Boolean) {
        userPreferencesRepository.setNotificationEnabled(enabled)
        _uiState.update { it.copy(notificationEnabled = enabled) }
    }

    fun toggleSound(enabled: Boolean) {
        userPreferencesRepository.setSoundEnabled(enabled)
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun toggleVibration(enabled: Boolean) {
        userPreferencesRepository.setVibrationEnabled(enabled)
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }

    // ===== Scan History Settings =====
    
    fun toggleAutoSaveHistory(enabled: Boolean) {
        userPreferencesRepository.setAutoSaveHistory(enabled)
        _uiState.update { it.copy(autoSaveHistory = enabled) }
    }

    fun showClearHistoryDialog() {
        _uiState.update { it.copy(showClearHistoryDialog = true) }
    }

    fun dismissClearHistoryDialog() {
        _uiState.update { it.copy(showClearHistoryDialog = false) }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showClearHistoryDialog = false) }
            
            firebaseHelper.deleteScanHistory { success ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = if (success) "Đã xóa lịch sử quét" else "Không thể xóa lịch sử"
                    ) 
                }
            }
        }
    }

    // ===== Cache =====
    
    fun showClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = true) }
    }

    fun dismissClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = false) }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showClearCacheDialog = false) }
            
            try {
                val cacheDir = getApplication<Application>().cacheDir
                deleteRecursively(cacheDir)
                
                // Also clear external cache if exists
                getApplication<Application>().externalCacheDir?.let { externalCache ->
                    deleteRecursively(externalCache)
                }
                
                calculateCacheSize()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "Đã xóa bộ nhớ đệm"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "Không thể xóa bộ nhớ đệm: ${e.message}"
                    ) 
                }
            }
        }
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        file.delete()
    }

    // ===== App Actions =====
    
    fun getShareIntent(): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SafeMed - Tra cứu thuốc an toàn")
            putExtra(
                Intent.EXTRA_TEXT,
                "Khám phá SafeMed - Ứng dụng tra cứu thông tin thuốc và tìm nhà thuốc gần bạn!\n\n" +
                "Tải ngay: https://play.google.com/store/apps/details?id=com.safemed"
            )
        }
    }

    fun getRateIntent(): Intent {
        // Try to open Play Store app first, fallback to browser
        return try {
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.safemed"))
        } catch (e: Exception) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.safemed"))
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
