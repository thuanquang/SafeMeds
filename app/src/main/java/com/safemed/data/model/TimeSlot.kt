package com.safemed.data.model

import android.content.Context
import com.safemed.R

/**
 * Enum đại diện cho 4 buổi trong ngày để nhắc nhở uống thuốc
 * Mỗi buổi có khung giờ mặc định
 */
enum class TimeSlot(
    val displayNameResId: Int,
    val defaultHour: Int,
    val defaultMinute: Int,
    val startHour: Int,
    val endHour: Int
) {
    MORNING(
        displayNameResId = R.string.reminder_morning,
        defaultHour = 7,
        defaultMinute = 0,
        startHour = 5,
        endHour = 11
    ),
    NOON(
        displayNameResId = R.string.reminder_noon,
        defaultHour = 12,
        defaultMinute = 0,
        startHour = 11,
        endHour = 14
    ),
    AFTERNOON(
        displayNameResId = R.string.reminder_afternoon,
        defaultHour = 17,
        defaultMinute = 0,
        startHour = 14,
        endHour = 18
    ),
    EVENING(
        displayNameResId = R.string.reminder_evening,
        defaultHour = 20,
        defaultMinute = 0,
        startHour = 18,
        endHour = 23
    );

    /**
     * Get localized display name
     */
    fun getDisplayName(context: Context): String {
        return context.getString(displayNameResId)
    }

    /**
     * Format thời gian mặc định thành chuỗi HH:mm
     */
    fun getDefaultTimeString(): String {
        return String.format("%02d:%02d", defaultHour, defaultMinute)
    }

    companion object {
        /**
         * Tìm TimeSlot phù hợp với giờ cho trước
         */
        fun fromHour(hour: Int): TimeSlot? {
            return entries.find { hour in it.startHour until it.endHour }
        }
    }
}
