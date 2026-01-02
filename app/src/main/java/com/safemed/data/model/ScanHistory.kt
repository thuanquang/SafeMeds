package com.safemed.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class đại diện cho collection "history" trong Firestore
 * Liên kết logic (Relation) giữa User và Medicine
 * Lưu lịch sử quét thuốc của người dùng
 * 
 * Chiến lược: Lưu snapshot thông tin thuốc (medicineName, scannedCode)
 * để hiển thị danh sách nhanh mà không cần join bảng
 */
data class ScanHistory(
    @DocumentId
    val historyId: String = "",
    
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",           // ID của người dùng từ FirebaseAuth
    
    @get:PropertyName("medicineId")
    @set:PropertyName("medicineId")
    var medicineId: String = "",       // ID của document thuốc trong collection medicines
    
    @get:PropertyName("scannedCode")
    @set:PropertyName("scannedCode")
    var scannedCode: String = "",      // Mã thực tế mà người dùng đã quét (Barcode hoặc SĐK)
    
    @get:PropertyName("medicineName")
    @set:PropertyName("medicineName")
    var medicineName: String = "",     // Tên thuốc (Snapshot để hiển thị nhanh)
    
    @ServerTimestamp
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Timestamp? = null,  // Thời gian quét (Server timestamp)
    
    @get:PropertyName("result")
    @set:PropertyName("result")
    var result: String = ""            // Kết quả: "authentic", "not_found", "error"
) {
    /**
     * Lấy thời gian quét dưới dạng milliseconds
     */
    fun getTimestampMillis(): Long {
        return timestamp?.toDate()?.time ?: System.currentTimeMillis()
    }
}
