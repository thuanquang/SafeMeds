package com.safemed.ui.screen.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.alarm.ReminderAlarmManager
import com.safemed.data.model.MedicationReminder
import com.safemed.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State cho màn hình danh sách nhắc nhở
 */
data class ReminderListUiState(
    val reminders: List<MedicationReminder> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel cho màn hình danh sách nhắc nhở uống thuốc
 */
@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmManager: ReminderAlarmManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderListUiState())
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()

    init {
        loadReminders()
    }

    /**
     * Load danh sách reminders từ Firestore (realtime)
     */
    private fun loadReminders() {
        viewModelScope.launch {
            reminderRepository.getRemindersFlow().collect { reminders ->
                _uiState.update {
                    it.copy(
                        reminders = reminders,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Toggle trạng thái active của reminder
     */
    fun toggleReminderActive(reminder: MedicationReminder) {
        viewModelScope.launch {
            val newActiveState = !reminder.isActive
            
            val result = reminderRepository.toggleReminderActive(
                reminderId = reminder.reminderId,
                isActive = newActiveState
            )

            result.onSuccess {
                // Update local alarms
                if (newActiveState) {
                    val updatedReminder = reminder.copy().apply { isActive = true }
                    reminderAlarmManager.scheduleReminder(updatedReminder)
                } else {
                    reminderAlarmManager.cancelReminder(reminder.reminderId)
                }

                _uiState.update {
                    it.copy(successMessage = if (newActiveState) "Đã bật nhắc nhở" else "Đã tắt nhắc nhở")
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Có lỗi xảy ra")
                }
            }
        }
    }

    /**
     * Xóa reminder
     */
    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = reminderRepository.deleteReminder(reminderId)

            result.onSuccess {
                // Cancel alarms
                reminderAlarmManager.cancelReminder(reminderId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Đã xóa nhắc nhở"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Có lỗi xảy ra"
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
