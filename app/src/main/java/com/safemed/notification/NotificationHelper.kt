package com.safemed.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safemed.MainActivity
import com.safemed.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class để tạo và hiển thị notifications
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotificationHelper"
        
        const val CHANNEL_ID = "medication_reminders"
        const val CHANNEL_NAME = "Nhắc nhở uống thuốc"
        const val CHANNEL_DESCRIPTION = "Thông báo nhắc nhở uống thuốc hàng ngày"

        const val ACTION_MARK_TAKEN = "com.safemed.ACTION_MARK_TAKEN"
        const val ACTION_SNOOZE = "com.safemed.ACTION_SNOOZE"

        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TIME_SLOT = "time_slot"
        const val EXTRA_SNOOZE_DURATION = "snooze_duration"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Tạo notification channel (required cho Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "Created notification channel: $CHANNEL_ID")
        }
    }

    /**
     * Hiển thị notification nhắc nhở uống thuốc
     */
    fun showMedicationReminder(
        reminderId: String,
        timeSlot: String,
        title: String,
        body: String,
        medicineName: String?,
        dosage: String?,
        snoozeDuration: Int
    ) {
        val notificationId = generateNotificationId(reminderId, timeSlot)

        // Intent để mở app khi tap vào notification
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "reminder_detail")
            putExtra("remind_id", reminderId)
            putExtra("time_slot", timeSlot)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Đã uống
        val takenIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TIME_SLOT, timeSlot)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Nhắc lại sau
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TIME_SLOT, timeSlot)
            putExtra(EXTRA_SNOOZE_DURATION, snoozeDuration)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_medication)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_check_circle,
                context.getString(R.string.reminder_action_taken),
                takenPendingIntent
            )
            .addAction(
                R.drawable.ic_notification_medication,
                "${context.getString(R.string.reminder_action_snooze)} ${context.getString(R.string.notification_action_snooze_minutes, snoozeDuration)}",
                snoozePendingIntent
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Showed notification for reminder: $reminderId, slot: $timeSlot")
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to show notification", e)
        }
    }

    /**
     * Hủy notification
     */
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
        Log.d(TAG, "Cancelled notification: $notificationId")
    }

    /**
     * Generate unique notification ID
     */
    private fun generateNotificationId(reminderId: String, timeSlot: String): Int {
        return "${reminderId}_${timeSlot}".hashCode() and 0x7FFFFFFF // Ensure positive
    }
}
