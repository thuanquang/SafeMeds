package com.safemed.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.safemed.alarm.ReminderAlarmManager
import com.safemed.data.model.TimeSlot
import com.safemed.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver xử lý các action từ notification buttons
 * - "Đã uống": Log action và dismiss notification
 * - "Nhắc lại sau": Schedule snooze alarm
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderAlarmManager: ReminderAlarmManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ReminderActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")

        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
        val timeSlot = intent.getStringExtra(NotificationHelper.EXTRA_TIME_SLOT) ?: return
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            NotificationHelper.ACTION_MARK_TAKEN -> {
                handleMarkTaken(context, reminderId, timeSlot, notificationId)
            }
            NotificationHelper.ACTION_SNOOZE -> {
                val snoozeDuration = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_DURATION, 10)
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "💊 Nhắc uống thuốc"
                val body = intent.getStringExtra(NotificationHelper.EXTRA_BODY) ?: "Đã đến giờ uống thuốc"
                val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME)
                val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE)
                
                handleSnooze(
                    context = context,
                    reminderId = reminderId,
                    timeSlot = timeSlot,
                    notificationId = notificationId,
                    snoozeDuration = snoozeDuration,
                    title = title,
                    body = body,
                    medicineName = medicineName,
                    dosage = dosage
                )
            }
        }
    }

    /**
     * Xử lý khi user nhấn "Đã uống"
     */
    private fun handleMarkTaken(
        context: Context,
        reminderId: String,
        timeSlot: String,
        notificationId: Int
    ) {
        Log.d(TAG, "Marking reminder as taken: $reminderId, slot: $timeSlot")

        // Dismiss notification
        if (notificationId != -1) {
            notificationHelper.cancelNotification(notificationId)
        }

        // Log action to Firestore
        scope.launch {
            reminderRepository.logReminderAction(
                reminderId = reminderId,
                timeSlot = timeSlot,
                action = "taken"
            )
        }

        // Show confirmation toast
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "✓ Đã ghi nhận uống thuốc", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Xử lý khi user nhấn "Nhắc lại sau"
     */
    private fun handleSnooze(
        context: Context,
        reminderId: String,
        timeSlot: String,
        notificationId: Int,
        snoozeDuration: Int,
        title: String,
        body: String,
        medicineName: String?,
        dosage: String?
    ) {
        Log.d(TAG, "Snoozing reminder: $reminderId for $snoozeDuration minutes")

        // Dismiss current notification
        if (notificationId != -1) {
            notificationHelper.cancelNotification(notificationId)
        }

        // Schedule snooze alarm
        val slot = try {
            TimeSlot.valueOf(timeSlot)
        } catch (e: Exception) {
            TimeSlot.MORNING
        }

        reminderAlarmManager.scheduleSnooze(
            reminderId = reminderId,
            timeSlot = slot,
            title = title,
            body = body,
            medicineName = medicineName,
            dosage = dosage,
            snoozeDurationMinutes = snoozeDuration
        )

        // Log snooze action
        scope.launch {
            reminderRepository.logReminderAction(
                reminderId = reminderId,
                timeSlot = timeSlot,
                action = "snoozed",
                snoozeCount = 1
            )
        }

        // Show confirmation toast
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "⏰ Sẽ nhắc lại sau $snoozeDuration phút", Toast.LENGTH_SHORT).show()
        }
    }
}
