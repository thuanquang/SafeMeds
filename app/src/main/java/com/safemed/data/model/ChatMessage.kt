package com.safemed.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Enum định nghĩa vai trò của tin nhắn trong cuộc hội thoại
 */
enum class MessageRole {
    USER,
    ASSISTANT
}

/**
 * Data class đại diện cho một tin nhắn trong cuộc hội thoại chatbot
 * Được lưu trữ trong Firestore tại: users/{userId}/chat_history/{messageId}
 */
data class ChatMessage(
    @DocumentId
    val messageId: String = "",
    
    @get:PropertyName("role")
    @set:PropertyName("role")
    var role: String = MessageRole.ASSISTANT.name,
    
    @get:PropertyName("content")
    @set:PropertyName("content")
    var content: String = "",
    
    @ServerTimestamp
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Timestamp? = null
) {
    /**
     * Lấy timestamp dưới dạng milliseconds
     */
    fun getTimestampMillis(): Long {
        return timestamp?.toDate()?.time ?: System.currentTimeMillis()
    }
    
    /**
     * Kiểm tra xem tin nhắn có phải từ người dùng không
     */
    fun isFromUser(): Boolean {
        return role == MessageRole.USER.name
    }
    
    /**
     * Kiểm tra xem tin nhắn có phải từ AI assistant không
     */
    fun isFromAssistant(): Boolean {
        return role == MessageRole.ASSISTANT.name
    }
    
    companion object {
        /**
         * Tạo tin nhắn từ người dùng
         */
        fun createUserMessage(content: String): ChatMessage {
            return ChatMessage(
                role = MessageRole.USER.name,
                content = content,
                timestamp = Timestamp.now()
            )
        }
        
        /**
         * Tạo tin nhắn từ AI assistant
         */
        fun createAssistantMessage(content: String): ChatMessage {
            return ChatMessage(
                role = MessageRole.ASSISTANT.name,
                content = content,
                timestamp = Timestamp.now()
            )
        }
    }
}
