package com.safemed.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safemed.MainActivity
import com.safemed.R
import com.safemed.data.repository.ReminderRepository
import com.safemed.notification.NotificationHelper
import com.safemed.notification.ReminderActionReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service
 * 
 * LƯU Ý: Hiện tại app sử dụng LOCAL ALARMS (AlarmManager) làm phương pháp CHÍNH
 * để gửi medication reminders vì:
 * - Không cần Firebase Blaze plan (miễn phí)
 * - Hoạt động offline
 * - Chính xác hơn
 * 
 * Service này vẫn được giữ để:
 * - Nhận FCM token và lưu vào Firestore (cho tương lai nếu cần)
 * - Xử lý các push notification khác (không phải medication reminder)
 */
@AndroidEntryPoint
class SafeMedMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SafeMedMessagingService"
    }

    /**
     * Được gọi khi FCM token mới được tạo hoặc refresh
     * Cập nhật token vào Firestore để Cloud Functions có thể gửi notification
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Lưu token vào Firestore
        serviceScope.launch {
            reminderRepository.updateFcmToken(token)
        }
    }

    /**
     * Được gọi khi nhận được push notification từ FCM
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "=== FCM MESSAGE RECEIVED ===")
        Log.d(TAG, "From: ${message.from}")
        Log.d(TAG, "MessageId: ${message.messageId}")
        Log.d(TAG, "Data payload: ${message.data}")
        Log.d(TAG, "Notification payload: ${message.notification}")

        // Xử lý data payload (DATA-ONLY message từ server)
        val data = message.data
        if (data.isNotEmpty()) {
            Log.d(TAG, "Processing data message with ${data.size} fields")
            handleDataMessage(data)
        } else {
            Log.w(TAG, "Empty data payload - notification may not show correctly")
        }

        // Xử lý notification payload (nếu có - thường không có với data-only message)
        message.notification?.let { notification ->
            Log.d(TAG, "Has notification payload: ${notification.title} - ${notification.body}")
            showNotification(
                title = notification.title ?: "SafeMed",
                body = notification.body ?: "",
                data = data
            )
        }
    }

    /**
     * Xử lý data message từ Cloud Functions
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return

        when (type) {
            "medication_reminder" -> {
                val reminderId = data["reminder_id"] ?: ""
                val timeSlot = data["time_slot"] ?: ""
                val title = data["title"] ?: "💊 Nhắc uống thuốc"
                val body = data["body"] ?: "Đã đến giờ uống thuốc"
                val medicineName = data["medicine_name"]
                val dosage = data["dosage"]
                val snoozeDuration = data["snooze_duration"]?.toIntOrNull() ?: 10

                showMedicationReminderNotification(
                    reminderId = reminderId,
                    timeSlot = timeSlot,
                    title = title,
                    body = body,
                    medicineName = medicineName,
                    dosage = dosage,
                    snoozeDuration = snoozeDuration
                )
            }
        }
    }

    /**
     * Hiển thị notification nhắc nhở uống thuốc với action buttons
     */
    private fun showMedicationReminderNotification(
        reminderId: String,
        timeSlot: String,
        title: String,
        body: String,
        medicineName: String?,
        dosage: String?,
        snoozeDuration: Int
    ) {
        notificationHelper.showMedicationReminder(
            reminderId = reminderId,
            timeSlot = timeSlot,
            title = title,
            body = body,
            medicineName = medicineName,
            dosage = dosage,
            snoozeDuration = snoozeDuration
        )
    }

    /**
     * Hiển thị notification thông thường
     */
    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_medication)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
