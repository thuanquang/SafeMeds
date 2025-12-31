package com.safemed.data.model

/**
 * Data class đại diện cho collection "medicines" trong Firestore
 * Dữ liệu cho module Scanner - xác thực thuốc
 */
data class Medicine(
    val medicineId: String = "",
    val name: String = "",
    val brand: String = "",
    val barcode: String = "",      // Dùng để đối chiếu với ML Kit Barcode Scanner
    val isAuthentic: Boolean = true,
    val imageUrl: String = ""
)
