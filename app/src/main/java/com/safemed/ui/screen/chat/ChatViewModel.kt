package com.safemed.ui.screen.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.safemed.R
import com.safemed.data.model.ChatMessage
import com.safemed.data.model.MedicationReminder
import com.safemed.data.model.MessageRole
import com.safemed.data.network.RateLimitExceededException
import com.safemed.data.repository.ChatRepository
import com.safemed.data.repository.OfflineException
import com.safemed.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class for parsing reminder action from AI response
 */
data class ReminderAction(
    @SerializedName("medicine_name") val medicineName: String?,
    @SerializedName("time") val time: String?, // Format: "HH:MM"
    @SerializedName("period") val period: String?, // morning|noon|afternoon|evening
    @SerializedName("note") val note: String? = null
)

/**
 * UI State cho ChatScreen
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel quản lý logic cho ChatScreen
 * 
 * Chức năng:
 * - Load lịch sử chat từ Firestore
 * - Gửi tin nhắn đến Gemini API với debouncing (isSending flag)
 * - Inject welcome message khi chat rỗng
 * - Xử lý offline fallback và rate limit errors
 * - Parse và tạo reminder từ AI response
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val chatRepository: ChatRepository,
    private val reminderRepository: ReminderRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
        private val REMINDER_ACTION_REGEX = Regex("\\[REMINDER_ACTION\\]\\s*([\\s\\S]*?)\\s*\\[/REMINDER_ACTION\\]")
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val context get() = getApplication<Application>()
    private val gson = Gson()

    init {
        loadChatHistory()
    }

    /**
     * Load lịch sử chat từ Firestore
     */
    private fun loadChatHistory() {
        viewModelScope.launch {
            chatRepository.getChatHistoryFlow().collect { messages ->
                _uiState.update {
                    it.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Gửi tin nhắn của người dùng
     * Có debouncing bằng cách check isSending flag
     */
    fun sendMessage(userMessageText: String) {
        // Debouncing: Không cho gửi khi đang xử lý
        if (_uiState.value.isSending || userMessageText.isBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                // Set sending state
                _uiState.update { it.copy(isSending = true, errorMessage = null) }

                // Tạo và lưu tin nhắn của user
                val userMessage = ChatMessage.createUserMessage(userMessageText.trim())
                chatRepository.saveMessage(userMessage)

                // Lấy 10 tin nhắn gần nhất làm context (bao gồm tin vừa gửi)
                val currentMessages = _uiState.value.messages
                val recentMessages = currentMessages.takeLast(10)

                // Gửi đến Gemini API
                val result = chatRepository.sendMessageToGemini(
                    userMessage = userMessageText.trim(),
                    recentMessages = recentMessages
                )

                result.onSuccess { responseText ->
                    // Check và xử lý reminder action trong response
                    processReminderAction(responseText)
                    
                    // Lưu phản hồi từ AI
                    val assistantMessage = ChatMessage.createAssistantMessage(responseText)
                    chatRepository.saveMessage(assistantMessage)
                    Log.d(TAG, "AI response saved successfully")
                }.onFailure { exception ->
                    handleSendError(exception)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                handleSendError(e)
            } finally {
                // Reset sending state
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    /**
     * Parse và tạo reminder từ AI response
     * Format: [REMINDER_ACTION]{"medicine_name":"X","time":"HH:MM","period":"morning|noon|afternoon|evening"}[/REMINDER_ACTION]
     */
    private suspend fun processReminderAction(responseText: String) {
        val match = REMINDER_ACTION_REGEX.find(responseText) ?: return
        
        try {
            val jsonStr = match.groupValues[1].trim()
            val action = gson.fromJson(jsonStr, ReminderAction::class.java)
            
            if (action.medicineName.isNullOrBlank() || action.time.isNullOrBlank()) {
                Log.w(TAG, "Invalid reminder action: missing required fields")
                return
            }

            // Tạo reminder từ action
            val reminder = MedicationReminder().apply {
                medicineName = action.medicineName
                isDetailedReminder = true
                
                // Set time based on period
                when (action.period?.lowercase()) {
                    "morning" -> morningTime = action.time
                    "noon" -> noonTime = action.time
                    "afternoon" -> afternoonTime = action.time
                    "evening" -> eveningTime = action.time
                    else -> {
                        // Auto detect period from time
                        val hour = action.time.split(":").firstOrNull()?.toIntOrNull() ?: 0
                        when {
                            hour in 5..11 -> morningTime = action.time
                            hour in 12..13 -> noonTime = action.time
                            hour in 14..17 -> afternoonTime = action.time
                            else -> eveningTime = action.time
                        }
                    }
                }
                
                note = action.note
                isActive = true
            }

            // Lưu reminder vào Firestore
            val result = reminderRepository.createReminder(reminder)
            result.onSuccess { reminderId ->
                Log.d(TAG, "Reminder created successfully: $reminderId")
                _uiState.update { 
                    it.copy(successMessage = context.getString(R.string.chat_reminder_created))
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to create reminder", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing reminder action", e)
        }
    }

    /**
     * Xử lý các loại lỗi khi gửi tin nhắn
     */
    private suspend fun handleSendError(exception: Throwable) {
        val errorMessage = when (exception) {
            is OfflineException -> {
                // Lưu tin nhắn offline fallback
                val offlineMessage = ChatMessage.createAssistantMessage(
                    context.getString(R.string.chat_offline_message)
                )
                chatRepository.saveMessage(offlineMessage)
                null // Không hiện error vì đã hiện message
            }
            is RateLimitExceededException -> {
                // Lưu tin nhắn rate limit
                val rateLimitMessage = ChatMessage.createAssistantMessage(
                    context.getString(R.string.chat_rate_limit_message)
                )
                chatRepository.saveMessage(rateLimitMessage)
                null // Không hiện error vì đã hiện message
            }
            else -> {
                Log.e(TAG, "Send message error", exception)
                context.getString(R.string.chat_error_message)
            }
        }

        if (errorMessage != null) {
            _uiState.update { it.copy(errorMessage = errorMessage) }
        }
    }

    /**
     * Lấy welcome message dựa trên locale hiện tại
     */
    fun getWelcomeMessage(): String {
        return context.getString(R.string.chat_welcome_message)
    }

    /**
     * Xóa toàn bộ lịch sử chat
     */
    fun clearChatHistory() {
        viewModelScope.launch {
            val result = chatRepository.clearHistory()
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        messages = emptyList(),
                        successMessage = "Chat history cleared"
                    ) 
                }
            }.onFailure { exception ->
                Log.e(TAG, "Error clearing chat history", exception)
                _uiState.update { 
                    it.copy(errorMessage = context.getString(R.string.chat_error_message)) 
                }
            }
        }
    }

    /**
     * Clear error message sau khi đã hiển thị
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success message sau khi đã hiển thị
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
