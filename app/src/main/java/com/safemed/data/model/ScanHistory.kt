package com.safemed.data.model

/**
 * Data class đại diện cho collection "scan_history" trong Firestore
 * Liên kết logic (Relation) giữa User và Medicine
 * Lưu lịch sử quét thuốc của người dùng
 */
data class ScanHistory(
    val historyId: String = "",
    val userId: String = "",       // Foreign Key tới collection Users
    val medicineId: String = "",   // Foreign Key tới collection Medicines
    val scanTime: Long = System.currentTimeMillis(),
    val result: String = ""        // Kết quả: "authentic", "counterfeit", "unknown"
)
