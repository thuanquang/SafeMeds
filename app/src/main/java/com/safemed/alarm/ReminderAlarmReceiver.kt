package com.safemed.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.safemed.data.model.TimeSlot
import com.safemed.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver để nhận alarm và hiển thị notification
 * Hoạt động như backup khi FCM không available (offline)
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderAlarmManager: ReminderAlarmManager

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val ACTION_SHOW_REMINDER = "com.safemed.ACTION_SHOW_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received alarm broadcast: ${intent.action}")

        if (intent.action != ACTION_SHOW_REMINDER) {
            return
        }

        val reminderId = intent.getStringExtra(ReminderAlarmManager.EXTRA_REMINDER_ID) ?: return
        val timeSlotName = intent.getStringExtra(ReminderAlarmManager.EXTRA_TIME_SLOT) ?: return
        val title = intent.getStringExtra(ReminderAlarmManager.EXTRA_TITLE) ?: "💊 Nhắc uống thuốc"
        val body = intent.getStringExtra(ReminderAlarmManager.EXTRA_BODY) ?: "Đã đến giờ uống thuốc"
        val medicineName = intent.getStringExtra(ReminderAlarmManager.EXTRA_MEDICINE_NAME)
        val dosage = intent.getStringExtra(ReminderAlarmManager.EXTRA_DOSAGE)
        val snoozeDuration = intent.getIntExtra(ReminderAlarmManager.EXTRA_SNOOZE_DURATION, 10)
        val isSnooze = intent.getBooleanExtra(ReminderAlarmManager.EXTRA_IS_SNOOZE, false)

        Log.d(TAG, "Showing notification for reminder: $reminderId, slot: $timeSlotName, isSnooze: $isSnooze")

        // Hiển thị notification
        notificationHelper.showMedicationReminder(
            reminderId = reminderId,
            timeSlot = timeSlotName,
            title = title,
            body = body,
            medicineName = medicineName,
            dosage = dosage,
            snoozeDuration = snoozeDuration
        )

        // Nếu không phải snooze, schedule alarm cho ngày hôm sau
        if (!isSnooze) {
            // Note: Việc reschedule sẽ được thực hiện trong repository hoặc worker
            // để đảm bảo kiểm tra đúng các điều kiện repeat
            Log.d(TAG, "Regular alarm triggered, will reschedule for next occurrence")
        }
    }
}
