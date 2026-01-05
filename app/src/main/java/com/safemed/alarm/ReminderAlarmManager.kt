package com.safemed.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.safemed.data.model.MedicationReminder
import com.safemed.data.model.TimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class để schedule/cancel local alarms cho medication reminders
 * Đây là phương pháp CHÍNH để gửi notification nhắc nhở uống thuốc
 * 
 * Ưu điểm:
 * - Hoạt động offline (không cần internet)
 * - Miễn phí hoàn toàn (không cần Firebase Blaze plan)
 * - Chính xác và đáng tin cậy
 * - Không bị delay do network
 */
@Singleton
class ReminderAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) {
    companion object {
        private const val TAG = "ReminderAlarmManager"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TIME_SLOT = "time_slot"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_SNOOZE_DURATION = "snooze_duration"
        const val EXTRA_IS_SNOOZE = "is_snooze"

        // Request code base - mỗi reminder có 4 alarms (4 time slots)
        private const val REQUEST_CODE_MULTIPLIER = 10
    }

    /**
     * Schedule tất cả alarms cho một reminder
     */
    fun scheduleReminder(reminder: MedicationReminder) {
        if (!reminder.isActive) {
            Log.d(TAG, "Reminder ${reminder.reminderId} is not active, skipping")
            return
        }

        reminder.morningTime?.let { time ->
            scheduleAlarmForSlot(reminder, TimeSlot.MORNING, time)
        }
        reminder.noonTime?.let { time ->
            scheduleAlarmForSlot(reminder, TimeSlot.NOON, time)
        }
        reminder.afternoonTime?.let { time ->
            scheduleAlarmForSlot(reminder, TimeSlot.AFTERNOON, time)
        }
        reminder.eveningTime?.let { time ->
            scheduleAlarmForSlot(reminder, TimeSlot.EVENING, time)
        }
    }

    /**
     * Schedule alarm cho một time slot cụ thể
     */
    private fun scheduleAlarmForSlot(
        reminder: MedicationReminder,
        slot: TimeSlot,
        time: String
    ) {
        val (hour, minute) = parseTime(time)
        val triggerTime = getNextTriggerTime(hour, minute, reminder.selectedDays)

        val intent = createAlarmIntent(reminder, slot)
        val requestCode = generateRequestCode(reminder.reminderId, slot)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarms not permitted
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "Scheduled alarm for ${reminder.reminderId} - ${slot.name} at $time (trigger: $triggerTime)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule exact alarm - permission denied", e)
            // Fallback to inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    /**
     * Schedule snooze alarm
     */
    fun scheduleSnooze(
        reminderId: String,
        timeSlot: TimeSlot,
        title: String,
        body: String,
        medicineName: String?,
        dosage: String?,
        snoozeDurationMinutes: Int
    ) {
        val triggerTime = System.currentTimeMillis() + (snoozeDurationMinutes * 60 * 1000L)

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_SHOW_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TIME_SLOT, timeSlot.name)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_SNOOZE_DURATION, snoozeDurationMinutes)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        // Use a unique request code for snooze
        val requestCode = "${reminderId}_snooze_${System.currentTimeMillis()}".hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "Scheduled snooze for $reminderId in $snoozeDurationMinutes minutes")
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule snooze alarm", e)
        }
    }

    /**
     * Cancel tất cả alarms cho một reminder
     */
    fun cancelReminder(reminderId: String) {
        TimeSlot.entries.forEach { slot ->
            cancelAlarmForSlot(reminderId, slot)
        }
        Log.d(TAG, "Cancelled all alarms for reminder: $reminderId")
    }

    /**
     * Cancel alarm cho một time slot cụ thể
     */
    private fun cancelAlarmForSlot(reminderId: String, slot: TimeSlot) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val requestCode = generateRequestCode(reminderId, slot)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Reschedule tất cả active reminders (gọi sau khi boot)
     */
    fun rescheduleAllReminders(reminders: List<MedicationReminder>) {
        reminders.filter { it.isActive }.forEach { reminder ->
            scheduleReminder(reminder)
        }
        Log.d(TAG, "Rescheduled ${reminders.count { it.isActive }} active reminders")
    }

    /**
     * Tạo Intent cho alarm
     */
    private fun createAlarmIntent(reminder: MedicationReminder, slot: TimeSlot): Intent {
        return Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_SHOW_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.reminderId)
            putExtra(EXTRA_TIME_SLOT, slot.name)
            putExtra(EXTRA_TITLE, reminder.getNotificationTitle())
            putExtra(EXTRA_BODY, reminder.getNotificationContent())
            putExtra(EXTRA_MEDICINE_NAME, reminder.medicineName)
            putExtra(EXTRA_DOSAGE, reminder.dosage)
            putExtra(EXTRA_SNOOZE_DURATION, reminder.snoozeDuration)
            putExtra(EXTRA_IS_SNOOZE, false)
        }
    }

    /**
     * Generate unique request code cho PendingIntent
     */
    private fun generateRequestCode(reminderId: String, slot: TimeSlot): Int {
        return "${reminderId}_${slot.ordinal}".hashCode()
    }

    /**
     * Parse time string "HH:mm" thành hour và minute
     */
    private fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        return Pair(
            parts.getOrNull(0)?.toIntOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0
        )
    }

    /**
     * Tính thời điểm trigger tiếp theo
     */
    private fun getNextTriggerTime(hour: Int, minute: Int, selectedDays: List<Int>): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()

        // Nếu thời gian đã qua hôm nay, chuyển sang ngày mai
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Nếu có chọn ngày cụ thể, tìm ngày phù hợp tiếp theo
        if (selectedDays.isNotEmpty()) {
            var daysChecked = 0
            while (daysChecked < 7) {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Convert to 0-6
                if (dayOfWeek in selectedDays) {
                    break
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                daysChecked++
            }
        }

        return calendar.timeInMillis
    }
}
