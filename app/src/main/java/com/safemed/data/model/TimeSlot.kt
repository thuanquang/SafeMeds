package com.safemed.data.model

/**
 * Enum đại diện cho 4 buổi trong ngày để nhắc nhở uống thuốc
 * Mỗi buổi có khung giờ mặc định
 */
enum class TimeSlot(
    val displayName: String,
    val defaultHour: Int,
    val defaultMinute: Int,
    val startHour: Int,
    val endHour: Int
) {
    MORNING(
        displayName = "Sáng",
        defaultHour = 7,
        defaultMinute = 0,
        startHour = 5,
        endHour = 11
    ),
    NOON(
        displayName = "Trưa",
        defaultHour = 12,
        defaultMinute = 0,
        startHour = 11,
        endHour = 14
    ),
    AFTERNOON(
        displayName = "Chiều",
        defaultHour = 17,
        defaultMinute = 0,
        startHour = 14,
        endHour = 18
    ),
    EVENING(
        displayName = "Tối",
        defaultHour = 20,
        defaultMinute = 0,
        startHour = 18,
        endHour = 23
    );

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
