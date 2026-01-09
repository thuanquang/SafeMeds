package com.safemed.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.model.MedicationReminder
import com.safemed.data.repository.ProfileRepository
import com.safemed.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeScheduleItem(
    val time: String,
    val medicineName: String,
    val isDone: Boolean = false
)

data class HomeUiState(
    val userName: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val scheduleItems: List<HomeScheduleItem> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserName()
        loadTodaySchedule()
    }

    private fun loadTodaySchedule() {
        viewModelScope.launch {
            reminderRepository.getRemindersFlow().collect { reminders ->
                val todayItems = processRemindersForToday(reminders)
                _uiState.update { it.copy(scheduleItems = todayItems) }
            }
        }
    }

    private fun processRemindersForToday(reminders: List<MedicationReminder>): List<HomeScheduleItem> {
        val calendar = Calendar.getInstance()
        // Calendar.SUNDAY = 1, MONDAY = 2...
        // MedicationReminder uses 0 = Sunday, 1 = Monday...
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        
        // Simulating "now" to mark past items as done (optional UI logic)
        val nowHour = calendar.get(Calendar.HOUR_OF_DAY)
        val nowMinute = calendar.get(Calendar.MINUTE)
        val nowTimeVal = nowHour * 60 + nowMinute

        val dailyItems = mutableListOf<HomeScheduleItem>()

        for (reminder in reminders) {
            // Check if active
            if (!reminder.isActive) continue
            
            // Check day of week (if selectedDays is empty, it means every day)
            if (reminder.selectedDays.isNotEmpty() && !reminder.selectedDays.contains(currentDayOfWeek)) {
                continue
            }

            // Helper to add slot
            fun addSlot(timeStr: String?) {
                if (timeStr.isNullOrBlank()) return
                
                try {
                    val parts = timeStr.split(":")
                    if (parts.size == 2) {
                        val h = parts[0].toInt()
                        val m = parts[1].toInt()
                        val timeVal = h * 60 + m
                        
                        // Simple "isDone" logic: if time passed, assume done (placeholder logic)
                        val isDone = timeVal < nowTimeVal
                        
                        dailyItems.add(
                            HomeScheduleItem(
                                time = timeStr,
                                medicineName = reminder.medicineName ?: "Medicine",
                                isDone = isDone
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            addSlot(reminder.morningTime)
            addSlot(reminder.noonTime)
            addSlot(reminder.afternoonTime)
            addSlot(reminder.eveningTime)
        }

        // Sort by time
        return dailyItems.sortedBy { 
            val parts = it.time.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        }
    }

    private fun loadUserName() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.getCurrentUserProfile()
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userName = user?.fullName ?: "",
                            avatarUrl = user?.avatarUrl
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(isLoading = false)
                    }
                }
        }
    }
}
