package com.safemed

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.safemed.data.repository.AuthRepository
import com.safemed.data.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_PENDING_EMAIL = "pending_email_for_sign_in"
    }

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved language setting on app start
        applySavedLanguage()

        // Xử lý deep link nếu có (khi app mở lần đầu từ email link)
        handleEmailLinkIntent(intent)

        setContent {
            // Observe dark mode preference
            val isDarkMode by userPreferencesRepository.isDarkMode.collectAsState(
                initial = userPreferencesRepository.getDarkMode()
            )
            
            SafeMedApp(isDarkMode = isDarkMode)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Xử lý deep link khi activity đã mở (app đang chạy)
        handleEmailLinkIntent(intent)
    }

    /**
     * Xử lý Email Link từ Intent
     * Được gọi khi user click vào link trong email
     */
    private fun handleEmailLinkIntent(intent: Intent?) {
        val emailLink = intent?.data?.toString()

        if (emailLink.isNullOrEmpty()) {
            Log.d(TAG, "No email link in intent")
            return
        }

        Log.d(TAG, "Received email link: $emailLink")

        // Kiểm tra xem đây có phải là email sign-in link không
        if (Firebase.auth.isSignInWithEmailLink(emailLink)) {
            Log.d(TAG, "Valid email sign-in link detected")

            // Lấy email đã lưu trước đó
            val pendingEmail = sharedPreferences.getString(KEY_PENDING_EMAIL, null)

            if (pendingEmail.isNullOrEmpty()) {
                Log.w(TAG, "No pending email found. User needs to enter email again.")
                // TODO: Show dialog để user nhập lại email
                return
            }

            // Tiến hành sign-in với email link
            lifecycleScope.launch {
                signInWithEmailLink(pendingEmail, emailLink)
            }
        }
    }

    /**
     * Thực hiện sign-in với email link
     */
    private suspend fun signInWithEmailLink(email: String, emailLink: String) {
        Log.d(TAG, "Signing in with email link for: $email")

        authRepository.signInWithEmailLink(email, emailLink)
            .onSuccess { user ->
                Log.d(TAG, "Email link sign-in successful: ${user.uid}")
                clearPendingEmail()
                // App sẽ tự động navigate dựa trên auth state
            }
            .onFailure { exception ->
                Log.e(TAG, "Email link sign-in failed", exception)
                // TODO: Show error toast/snackbar
            }
    }

    /**
     * Xóa pending email sau khi đăng nhập thành công
     */
    private fun clearPendingEmail() {
        sharedPreferences.edit()
            .remove(KEY_PENDING_EMAIL)
            .apply()
    }

    /**
     * Apply saved language setting from preferences
     */
    private fun applySavedLanguage() {
        val savedLanguage = userPreferencesRepository.getLanguage()
        val localeList = LocaleListCompat.forLanguageTags(savedLanguage)
        AppCompatDelegate.setApplicationLocales(localeList)
        Log.d(TAG, "Applied saved language: $savedLanguage")
    }
}
