package com.safemed.data.model

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import com.safemed.R

/**
 * Data class đại diện cho lịch nhắc nhở uống thuốc
 * Lưu trong Firestore tại: users/{userId}/reminders/{reminderId}
 *
 * Hỗ trợ:
 * - 4 buổi trong ngày (sáng, trưa, chiều, tối) - mỗi buổi có thể bật/tắt riêng
 * - Chọn ngày trong tuần hoặc mỗi ngày
 * - Lặp lại theo số lần hoặc đến ngày kết thúc
 * - Nhắc chi tiết (thuốc + liều) hoặc nhắc chung
 */
data class MedicationReminder(
    @DocumentId
    val reminderId: String = "",

    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    // ===== Thời gian nhắc nhở - format "HH:mm", null nếu không bật buổi đó =====
    @get:PropertyName("morning_time")
    @set:PropertyName("morning_time")
    var morningTime: String? = null,

    @get:PropertyName("noon_time")
    @set:PropertyName("noon_time")
    var noonTime: String? = null,

    @get:PropertyName("afternoon_time")
    @set:PropertyName("afternoon_time")
    var afternoonTime: String? = null,

    @get:PropertyName("evening_time")
    @set:PropertyName("evening_time")
    var eveningTime: String? = null,

    // ===== Ngày trong tuần (0 = Chủ nhật, 1 = Thứ 2, ..., 6 = Thứ 7) =====
    // Rỗng = mỗi ngày
    @get:PropertyName("selected_days")
    @set:PropertyName("selected_days")
    var selectedDays: List<Int> = emptyList(),

    // ===== Lặp lại =====
    // 0 = lặp mãi mãi, > 0 = số lần lặp còn lại
    @get:PropertyName("repeat_count")
    @set:PropertyName("repeat_count")
    var repeatCount: Int = 0,

    // Ngày kết thúc lặp (null = không có ngày kết thúc)
    @get:PropertyName("repeat_until_date")
    @set:PropertyName("repeat_until_date")
    var repeatUntilDate: Timestamp? = null,

    // ===== Thông tin thuốc (nếu nhắc chi tiết) =====
    @get:PropertyName("is_detailed_reminder")
    @set:PropertyName("is_detailed_reminder")
    var isDetailedReminder: Boolean = false,

    // ID thuốc từ collection medicines (nếu chọn từ danh sách)
    @get:PropertyName("medicine_id")
    @set:PropertyName("medicine_id")
    var medicineId: String? = null,

    // Tên thuốc snapshot hoặc tên tự nhập
    @get:PropertyName("medicine_name")
    @set:PropertyName("medicine_name")
    var medicineName: String? = null,

    // Liều lượng (ví dụ: "2 viên", "10ml")
    @get:PropertyName("dosage")
    @set:PropertyName("dosage")
    var dosage: String? = null,

    // Ghi chú thêm
    @get:PropertyName("note")
    @set:PropertyName("note")
    var note: String? = null,

    // ===== Cài đặt snooze =====
    // Thời gian snooze mặc định (phút): 5, 10, 15, 30
    @get:PropertyName("snooze_duration")
    @set:PropertyName("snooze_duration")
    var snoozeDuration: Int = 10,

    // ===== Trạng thái =====
    @get:PropertyName("is_active")
    @set:PropertyName("is_active")
    var isActive: Boolean = true,

    // Timezone của người dùng (ví dụ: "Asia/Ho_Chi_Minh")
    @get:PropertyName("timezone")
    @set:PropertyName("timezone")
    var timezone: String = "Asia/Ho_Chi_Minh",

    @ServerTimestamp
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Timestamp? = null,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Timestamp? = null
) {
    /**
     * Kiểm tra xem có ít nhất một buổi được bật không
     */
    @Exclude
    fun hasActiveTimeSlot(): Boolean {
        return morningTime != null || noonTime != null || afternoonTime != null || eveningTime != null
    }

    /**
     * Lấy danh sách các buổi đã bật
     */
    @Exclude
    fun getActiveTimeSlots(): Map<TimeSlot, String> {
        val result = mutableMapOf<TimeSlot, String>()
        morningTime?.let { result[TimeSlot.MORNING] = it }
        noonTime?.let { result[TimeSlot.NOON] = it }
        afternoonTime?.let { result[TimeSlot.AFTERNOON] = it }
        eveningTime?.let { result[TimeSlot.EVENING] = it }
        return result
    }

    /**
     * Lấy thời gian cho một buổi cụ thể
     */
    @Exclude
    fun getTimeForSlot(slot: TimeSlot): String? {
        return when (slot) {
            TimeSlot.MORNING -> morningTime
            TimeSlot.NOON -> noonTime
            TimeSlot.AFTERNOON -> afternoonTime
            TimeSlot.EVENING -> eveningTime
        }
    }

    /**
     * Set thời gian cho một buổi cụ thể
     */
    @Exclude
    fun setTimeForSlot(slot: TimeSlot, time: String?) {
        when (slot) {
            TimeSlot.MORNING -> morningTime = time
            TimeSlot.NOON -> noonTime = time
            TimeSlot.AFTERNOON -> afternoonTime = time
            TimeSlot.EVENING -> eveningTime = time
        }
    }

    /**
     * Kiểm tra xem hôm nay có trong danh sách ngày được chọn không
     * @param dayOfWeek: 1 = Chủ nhật, 2 = Thứ 2, ..., 7 = Thứ 7 (Calendar.DAY_OF_WEEK)
     */
    @Exclude
    fun isScheduledForDay(dayOfWeek: Int): Boolean {
        // Chuyển đổi từ Calendar.DAY_OF_WEEK (1-7) sang format lưu trữ (0-6)
        val normalizedDay = dayOfWeek - 1
        return selectedDays.isEmpty() || normalizedDay in selectedDays
    }

    /**
     * Lấy nội dung thông báo
     */
    @Exclude
    fun getNotificationContent(): String {
        return if (isDetailedReminder && !medicineName.isNullOrBlank()) {
            buildString {
                append("Đã đến giờ uống ")
                append(medicineName)
                if (!dosage.isNullOrBlank()) {
                    append(" - $dosage")
                }
            }
        } else {
            "Đã đến giờ uống thuốc"
        }
    }

    /**
     * Lấy title thông báo
     */
    @Exclude
    fun getNotificationTitle(): String {
        return if (isDetailedReminder && !medicineName.isNullOrBlank()) {
            "💊 Nhắc uống $medicineName"
        } else {
            "💊 Nhắc uống thuốc"
        }
    }

    /**
     * Format ngày lặp lại để hiển thị (localized)
     */
    @Exclude
    fun getRepeatDisplayText(context: Context): String {
        val dayNames = listOf(
            context.getString(R.string.reminder_day_sun),
            context.getString(R.string.reminder_day_mon),
            context.getString(R.string.reminder_day_tue),
            context.getString(R.string.reminder_day_wed),
            context.getString(R.string.reminder_day_thu),
            context.getString(R.string.reminder_day_fri),
            context.getString(R.string.reminder_day_sat)
        )
        
        return when {
            selectedDays.isEmpty() -> context.getString(R.string.reminder_everyday)
            selectedDays.size == 7 -> context.getString(R.string.reminder_everyday)
            selectedDays.size == 5 && selectedDays.containsAll(listOf(1, 2, 3, 4, 5)) -> context.getString(R.string.reminder_repeat_weekdays)
            selectedDays.size == 2 && selectedDays.containsAll(listOf(0, 6)) -> context.getString(R.string.reminder_repeat_weekends)
            else -> selectedDays.sorted().joinToString(", ") { dayNames.getOrElse(it) { "" } }
        }
    }
}

/**
 * Data class cho thống kê reminder đã uống thuốc
 */
data class ReminderLog(
    @DocumentId
    val logId: String = "",

    @get:PropertyName("reminder_id")
    @set:PropertyName("reminder_id")
    var reminderId: String = "",

    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("time_slot")
    @set:PropertyName("time_slot")
    var timeSlot: String = "",

    @get:PropertyName("scheduled_time")
    @set:PropertyName("scheduled_time")
    var scheduledTime: Timestamp? = null,

    @get:PropertyName("action_taken")
    @set:PropertyName("action_taken")
    var actionTaken: String = "", // "taken", "snoozed", "dismissed"

    @get:PropertyName("action_time")
    @set:PropertyName("action_time")
    var actionTime: Timestamp? = null,

    @get:PropertyName("snooze_count")
    @set:PropertyName("snooze_count")
    var snoozeCount: Int = 0
)
