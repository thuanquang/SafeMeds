package com.safemed.data.model

/**
 * Data class đại diện cho collection "pharmacies" trong Firestore
 * Dữ liệu cho bản đồ và ORS (tìm kiếm nhà thuốc gần nhất)
 */
data class Pharmacy(
    val pharmacyId: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val isOpen: Boolean = true
)
