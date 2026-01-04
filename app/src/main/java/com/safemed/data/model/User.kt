package com.safemed.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Data class đại diện cho collection "users" trong Firestore
 * Chứa thông tin tài khoản người dùng
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    
    // Ngôn ngữ người dùng chọn ("vi" hoặc "en"), mặc định là "vi"
    @get:PropertyName("language")
    @set:PropertyName("language")
    var language: String = "vi",
    
    // FCM token để gửi push notification
    @get:PropertyName("fcm_token")
    @set:PropertyName("fcm_token")
    var fcmToken: String? = null
)
