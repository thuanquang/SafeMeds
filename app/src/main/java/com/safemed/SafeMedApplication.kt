package com.safemed

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.messaging.FirebaseMessaging
import com.safemed.data.repository.ReminderRepository
import com.safemed.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SafeMedApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reminderRepository: ReminderRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SafeMedApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel early (before any notification is sent)
        createNotificationChannel()
        
        // Initialize FCM and get token
        initializeFcm()
    }
    
    /**
     * Create notification channel for medication reminders
     * Must be created before any notification is posted (Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationHelper.CHANNEL_ID,
                NotificationHelper.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = NotificationHelper.CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: ${NotificationHelper.CHANNEL_ID}")
        }
    }

    /**
     * Configure WorkManager with HiltWorkerFactory
     * Required for injecting dependencies into Workers
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    /**
     * Initialize Firebase Cloud Messaging and update FCM token
     */
    private fun initializeFcm() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Save token to Firestore
            applicationScope.launch {
                try {
                    reminderRepository.updateFcmToken(token)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update FCM token", e)
                }
            }
        }
    }
}

