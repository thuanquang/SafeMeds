package com.safemed.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository quản lý user preferences sử dụng SharedPreferences
 * Lưu trữ các cài đặt: dark mode, language, notifications, biometric, etc.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        // Keys
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_TWO_FACTOR_ENABLED = "two_factor_enabled"
        private const val KEY_LOGIN_NOTIFICATION_ENABLED = "login_notification_enabled"
        private const val KEY_AUTO_SAVE_HISTORY = "auto_save_history"

        // Defaults
        private const val DEFAULT_DARK_MODE = false
        private const val DEFAULT_LANGUAGE = "vi" // Vietnamese
        private const val DEFAULT_NOTIFICATION_ENABLED = true
        private const val DEFAULT_SOUND_ENABLED = true
        private const val DEFAULT_VIBRATION_ENABLED = true
        private const val DEFAULT_BIOMETRIC_ENABLED = false
        private const val DEFAULT_TWO_FACTOR_ENABLED = false
        private const val DEFAULT_LOGIN_NOTIFICATION_ENABLED = true
        private const val DEFAULT_AUTO_SAVE_HISTORY = true
    }

    // ===== StateFlows for reactive UI updates =====
    
    private val _isDarkMode = MutableStateFlow(getDarkMode())
    val isDarkMode: Flow<Boolean> = _isDarkMode.asStateFlow()

    private val _language = MutableStateFlow(getLanguage())
    val language: Flow<String> = _language.asStateFlow()

    private val _notificationEnabled = MutableStateFlow(getNotificationEnabled())
    val notificationEnabled: Flow<Boolean> = _notificationEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(getSoundEnabled())
    val soundEnabled: Flow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(getVibrationEnabled())
    val vibrationEnabled: Flow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(getBiometricEnabled())
    val biometricEnabled: Flow<Boolean> = _biometricEnabled.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(getTwoFactorEnabled())
    val twoFactorEnabled: Flow<Boolean> = _twoFactorEnabled.asStateFlow()

    private val _loginNotificationEnabled = MutableStateFlow(getLoginNotificationEnabled())
    val loginNotificationEnabled: Flow<Boolean> = _loginNotificationEnabled.asStateFlow()

    private val _autoSaveHistory = MutableStateFlow(getAutoSaveHistory())
    val autoSaveHistory: Flow<Boolean> = _autoSaveHistory.asStateFlow()

    // ===== Dark Mode =====
    
    fun getDarkMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)
    }

    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    // ===== Language =====
    
    fun getLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(languageCode: String) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, languageCode).apply()
        _language.value = languageCode
    }

    // ===== Notification Settings =====
    
    fun getNotificationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_ENABLED)
    }

    fun setNotificationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
        _notificationEnabled.value = enabled
    }

    fun getSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
    }

    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _soundEnabled.value = enabled
    }

    fun getVibrationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        _vibrationEnabled.value = enabled
    }

    // ===== Security Settings =====
    
    fun getBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, DEFAULT_BIOMETRIC_ENABLED)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _biometricEnabled.value = enabled
    }

    fun getTwoFactorEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_TWO_FACTOR_ENABLED, DEFAULT_TWO_FACTOR_ENABLED)
    }

    fun setTwoFactorEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_TWO_FACTOR_ENABLED, enabled).apply()
        _twoFactorEnabled.value = enabled
    }

    fun getLoginNotificationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_LOGIN_NOTIFICATION_ENABLED, DEFAULT_LOGIN_NOTIFICATION_ENABLED)
    }

    fun setLoginNotificationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_LOGIN_NOTIFICATION_ENABLED, enabled).apply()
        _loginNotificationEnabled.value = enabled
    }

    // ===== Scan History Settings =====
    
    fun getAutoSaveHistory(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_SAVE_HISTORY, DEFAULT_AUTO_SAVE_HISTORY)
    }

    fun setAutoSaveHistory(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_SAVE_HISTORY, enabled).apply()
        _autoSaveHistory.value = enabled
    }

    // ===== Clear All Settings =====
    
    fun clearAllSettings() {
        sharedPreferences.edit().clear().apply()
        // Reset all StateFlows to defaults
        _isDarkMode.value = DEFAULT_DARK_MODE
        _language.value = DEFAULT_LANGUAGE
        _notificationEnabled.value = DEFAULT_NOTIFICATION_ENABLED
        _soundEnabled.value = DEFAULT_SOUND_ENABLED
        _vibrationEnabled.value = DEFAULT_VIBRATION_ENABLED
        _biometricEnabled.value = DEFAULT_BIOMETRIC_ENABLED
        _twoFactorEnabled.value = DEFAULT_TWO_FACTOR_ENABLED
        _loginNotificationEnabled.value = DEFAULT_LOGIN_NOTIFICATION_ENABLED
        _autoSaveHistory.value = DEFAULT_AUTO_SAVE_HISTORY
    }
}
