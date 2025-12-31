package com.safemed.data.model

/**
 * Data class đại diện cho collection "users" trong Firestore
 * Chứa thông tin tài khoản người dùng
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
