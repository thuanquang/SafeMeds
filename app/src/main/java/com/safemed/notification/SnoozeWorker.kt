package com.safemed.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safemed.data.model.TimeSlot
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker để trigger snooze notification sau một khoảng thời gian
 * Dùng WorkManager để đảm bảo notification được gửi ngay cả khi app bị kill
 */
@HiltWorker
class SnoozeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SnoozeWorker"
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_TIME_SLOT = "time_slot"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_MEDICINE_NAME = "medicine_name"
        const val KEY_DOSAGE = "dosage"
        const val KEY_SNOOZE_DURATION = "snooze_duration"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SnoozeWorker executing...")

        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val timeSlot = inputData.getString(KEY_TIME_SLOT) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "💊 Nhắc uống thuốc"
        val body = inputData.getString(KEY_BODY) ?: "Đã đến giờ uống thuốc"
        val medicineName = inputData.getString(KEY_MEDICINE_NAME)
        val dosage = inputData.getString(KEY_DOSAGE)
        val snoozeDuration = inputData.getInt(KEY_SNOOZE_DURATION, 10)

        Log.d(TAG, "Showing snooze notification for: $reminderId")

        notificationHelper.showMedicationReminder(
            reminderId = reminderId,
            timeSlot = timeSlot,
            title = title,
            body = body,
            medicineName = medicineName,
            dosage = dosage,
            snoozeDuration = snoozeDuration
        )

        return Result.success()
    }
}
