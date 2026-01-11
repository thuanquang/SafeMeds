package com.safemed.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.model.MedicationReminder
import com.safemed.data.model.ReminderLog
import com.safemed.data.repository.ProfileRepository
import com.safemed.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val scheduleItems: List<HomeScheduleItem> = emptyList(),
    val adherenceScore: Int = 100 // Default, updates closely after load
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHomeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            profileRepository.getUserProfileFlow().flatMapLatest { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userName = user?.fullName ?: "",
                        avatarUrl = user?.avatarUrl
                    )
                }

                if (user == null) {
                   flowOf(Triple(emptyList<HomeScheduleItem>(), 100, false))
                } else {
                    combine(
                        reminderRepository.getRemindersFlow(),
                        reminderRepository.getReminderLogsFlow(limit = 100)
                    ) { reminders, logs ->
                        // Filter logs for today
                        val calendar = Calendar.getInstance()
                        val todayYear = calendar.get(Calendar.YEAR)
                        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
                        
                        val todayLogs = logs.filter { log ->
                            log.actionTime?.toDate()?.let { date ->
                                val logCal = Calendar.getInstance()
                                logCal.time = date
                                logCal.get(Calendar.YEAR) == todayYear && 
                                logCal.get(Calendar.DAY_OF_YEAR) == todayDay
                            } ?: false
                        }
                        
                        // Process Adherence Score
                        val stats = calculateAdherenceScore(reminders, logs) // Using all logs for score
                        val score = stats

                        val todayItems = processRemindersForToday(reminders, todayLogs)
                        Triple(todayItems, score, false)
                    }
                }
            }.collect { (scheduleItems, score, _) ->
                _uiState.update { 
                    it.copy(
                        scheduleItems = scheduleItems,
                        adherenceScore = score
                    ) 
                }
            }
        }
    }
    
    // Quick calculation for adherence score (simplified version of AdherenceViewModel logic)
    private fun calculateAdherenceScore(reminders: List<MedicationReminder>, logs: List<ReminderLog>): Int {
        var grandTotalExpected = 0
        var grandTotalTaken = 0
        val activeReminderIds = reminders.map { it.reminderId }.toSet()
        
        // Helper to check if reminder is active on a specific day of week (0=Sun, ... 6=Sat)
        fun countExpectedForDate(reminders: List<MedicationReminder>, date: Calendar, isToday: Boolean, dailyLogs: List<ReminderLog>): Int {
            val dayOfWeek = date.get(Calendar.DAY_OF_WEEK) - 1
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentTimeValue = currentHour * 60 + currentMinute
            
            // Normalize target date to start of day for creation check
            val startOfTargetDay = date.clone() as Calendar
            startOfTargetDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfTargetDay.set(Calendar.MINUTE, 0)
            startOfTargetDay.set(Calendar.SECOND, 0)
            startOfTargetDay.set(Calendar.MILLISECOND, 0)
            
            var count = 0
            for (reminder in reminders) {
                if (!reminder.isActive) continue
                
                // Check if reminder was created after this target day
                if (reminder.createdAt != null) {
                     val createdCal = Calendar.getInstance()
                     createdCal.time = reminder.createdAt!!.toDate()
                     createdCal.set(Calendar.HOUR_OF_DAY, 0)
                     createdCal.set(Calendar.MINUTE, 0)
                     createdCal.set(Calendar.SECOND, 0)
                     createdCal.set(Calendar.MILLISECOND, 0)
                     
                     if (startOfTargetDay.before(createdCal)) {
                         continue
                     }
                }

                if (reminder.selectedDays.isNotEmpty() && !reminder.selectedDays.contains(dayOfWeek)) continue
                
                fun checkSlot(timeStr: String?, slotName: String) {
                    if (timeStr.isNullOrBlank()) return
                    
                    val isTaken = dailyLogs.any { 
                        it.reminderId == reminder.reminderId && 
                        it.actionTaken == "taken" &&
                        (it.timeSlot.equals(slotName, ignoreCase = true) || it.timeSlot == timeStr)
                    }

                    if (isTaken) {
                        count++
                    } else if (isToday) {
                        try {
                            val parts = timeStr.trim().split(":")
                            if (parts.size >= 2) {
                                val slotTime = parts[0].toInt() * 60 + parts[1].toInt()
                                if (slotTime <= currentTimeValue) count++
                            }
                        } catch (e: Exception) { }
                    } else {
                        count++
                    }
                }
                checkSlot(reminder.morningTime, "MORNING")
                checkSlot(reminder.noonTime, "NOON")
                checkSlot(reminder.afternoonTime, "AFTERNOON")
                checkSlot(reminder.eveningTime, "EVENING")
            }
            return count
        }

        // Calculate for last 7 days (including today)
        for (i in 6 downTo 0) {
            val targetDate = Calendar.getInstance()
            targetDate.add(Calendar.DAY_OF_YEAR, -i)
            val isToday = (i == 0)
            
            val startOfDay = targetDate.clone() as Calendar
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)
            startOfDay.set(Calendar.MILLISECOND, 0)
            
            val endOfDay = targetDate.clone() as Calendar
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)
            endOfDay.set(Calendar.MILLISECOND, 999)
            
            val dailyLogs = logs.filter { log ->
                val logTime = log.actionTime?.toDate()?.time ?: 0
                logTime >= startOfDay.timeInMillis && logTime <= endOfDay.timeInMillis
            }
            
            val totalExpected = countExpectedForDate(reminders, targetDate, isToday, dailyLogs)

            val takenCount = dailyLogs.count { log ->
                log.actionTaken == "taken" &&
                activeReminderIds.contains(log.reminderId)
            }
            
            grandTotalExpected += totalExpected
            grandTotalTaken += takenCount
        }
        
        return if (grandTotalExpected > 0) {
            ((grandTotalTaken.toFloat() / grandTotalExpected) * 100).toInt().coerceIn(0, 100)
        } else {
            100 // Default to 100% if no medications expected
        }
    }

    private fun processRemindersForToday(
        reminders: List<MedicationReminder>,
        todayLogs: List<ReminderLog>
    ): List<HomeScheduleItem> {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        
        val dailyItems = mutableListOf<HomeScheduleItem>()

        for (reminder in reminders) {
            if (!reminder.isActive) continue
            if (reminder.selectedDays.isNotEmpty() && !reminder.selectedDays.contains(currentDayOfWeek)) continue

            fun addSlot(timeStr: String?, slotName: String) {
                if (timeStr.isNullOrBlank()) return
                
                // Check if already taken
                val isTaken = todayLogs.any { 
                    it.reminderId == reminder.reminderId && 
                    // Match either the specific time string or the slot name (e.g. "MORNING")
                    (it.timeSlot.equals(timeStr, ignoreCase = true) || 
                     it.timeSlot.equals(slotName, ignoreCase = true)) &&
                    it.actionTaken == "taken"
                }

                dailyItems.add(
                    HomeScheduleItem(
                        time = timeStr,
                        medicineName = reminder.medicineName ?: "Medicine",
                        isDone = isTaken
                    )
                )
            }

            // Pass the exact time string as slot identifier
            addSlot(reminder.morningTime, "morning")
            addSlot(reminder.noonTime, "noon")
            addSlot(reminder.afternoonTime, "afternoon")
            addSlot(reminder.eveningTime, "evening")
        }

        return dailyItems.sortedBy { 
            val parts = it.time.split(":")
            if(parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
        }
    }


}
