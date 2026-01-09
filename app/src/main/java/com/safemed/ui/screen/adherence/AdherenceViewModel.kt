package com.safemed.ui.screen.adherence

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.model.MedicationReminder
import com.safemed.data.model.ReminderLog
import com.safemed.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AdherenceUiState(
    val adherenceScore: Int = 0,
    val adherenceLevel: AdherenceLevel = AdherenceLevel.Good,
    val weeklyStats: List<DailyAdherence> = emptyList(),
    val history: List<AdherenceLog> = emptyList(),
    val isLoading: Boolean = false
)

enum class AdherenceLevel {
    Excellent, Good, Average, Poor
}

data class DailyAdherence(
    val day: String,
    val percentage: Float
)

data class AdherenceLog(
    val id: String,
    val medicineName: String,
    val time: String,
    val status: AdherenceStatus,
    val dateLabel: String
)

enum class AdherenceStatus {
    Taken, Missed, Skipped
}

@HiltViewModel
class AdherenceViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdherenceUiState())
    val uiState: StateFlow<AdherenceUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Combine flows for realtime updates
                kotlinx.coroutines.flow.combine(
                    reminderRepository.getRemindersFlow(),
                    reminderRepository.getReminderLogsFlow(limit = 100)
                ) { reminders, logs ->
                    Pair(reminders, logs)
                }.collect { (reminders, logs) ->
                    processAdherenceData(reminders, logs)
                }
            } catch (e: Exception) {
                Log.e("AdherenceVM", "Error loading data", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun processAdherenceData(
        reminders: List<MedicationReminder>,
        logs: List<ReminderLog>
    ) {
        // --- Process History List ---
        val reminderMap = reminders.associateBy { it.reminderId }
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val historyList = logs.map { log ->
            val reminder = reminderMap[log.reminderId]
            val date = log.actionTime?.toDate() ?: Date()
            val dateStr = checkDateLabel(date, dateFormatter)
            
            AdherenceLog(
                id = log.logId,
                medicineName = reminder?.medicineName ?: "Unknown Medicine",
                time = timeFormatter.format(date),
                status = mapStatus(log.actionTaken),
                dateLabel = dateStr
            )
        }

        // --- Process Weekly Stats & Score ---
        var grandTotalExpected = 0
        var grandTotalTaken = 0
        val weeklyStats = mutableListOf<DailyAdherence>()
        val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())

        // Helper to calculate expected doses up to current time
        fun countExpectedForDate(reminders: List<MedicationReminder>, date: Calendar, isToday: Boolean): Int {
            val dayOfWeek = date.get(Calendar.DAY_OF_WEEK) - 1
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentTimeValue = currentHour * 60 + currentMinute

            var count = 0
            
            for (reminder in reminders) {
                // Skip if not active or not scheduled for this day
                if (!reminder.isActive) continue
                if (reminder.selectedDays.isNotEmpty() && !reminder.selectedDays.contains(dayOfWeek)) continue
                
                // Check each slot
                fun checkSlot(timeStr: String?) {
                    if (timeStr.isNullOrBlank()) return
                    
                    if (isToday) {
                        try {
                            val parts = timeStr.trim().split(":")
                            if (parts.size >= 2) {
                                val h = parts[0].toInt()
                                val m = parts[1].toInt()
                                val slotTimeValue = h * 60 + m
                                
                                // Only count if the time has passed (or is now)
                                if (slotTimeValue <= currentTimeValue) {
                                    count++
                                }
                            }
                        } catch (e: Exception) {
                            // If format invalid, ignore
                        }
                    } else {
                        // Past days: count everything
                        count++
                    }
                }

                checkSlot(reminder.morningTime)
                checkSlot(reminder.noonTime)
                checkSlot(reminder.afternoonTime)
                checkSlot(reminder.eveningTime)
            }
            return count
        }

        for (i in 6 downTo 0) {
            val targetDate = Calendar.getInstance()
            targetDate.add(Calendar.DAY_OF_YEAR, -i)
            val isToday = (i == 0)
            
            val startOfDay = targetDate.clone() as Calendar
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)
            
            val endOfDay = targetDate.clone() as Calendar
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)
            
            // Calculate Expected considering current time for today
            val totalExpected = countExpectedForDate(reminders, targetDate, isToday)

            val takenCount = logs.count { log ->
                val logTime = log.actionTime?.toDate()?.time ?: 0
                logTime >= startOfDay.timeInMillis && 
                logTime <= endOfDay.timeInMillis &&
                log.actionTaken == "taken"
            }
            
            grandTotalExpected += totalExpected
            grandTotalTaken += takenCount

            val percentage = if (totalExpected > 0) {
                (takenCount.toFloat() / totalExpected).coerceIn(0f, 1f)
            } else {
                if (takenCount > 0) 1f else 0f
            }

            weeklyStats.add(DailyAdherence(
                day = dayFormatter.format(targetDate.time),
                percentage = percentage
            ))
        }
        
        // Calculate Overall Score (Weighted Average)
        val score = if (grandTotalExpected > 0) {
            ((grandTotalTaken.toFloat() / grandTotalExpected) * 100).toInt().coerceIn(0, 100)
        } else {
            100 // Default to 100% if no medications expected
        }

        val level = when {
            score >= 90 -> AdherenceLevel.Excellent
            score >= 75 -> AdherenceLevel.Good
            score >= 50 -> AdherenceLevel.Average
            else -> AdherenceLevel.Poor
        }

        _uiState.update { 
            it.copy(
                adherenceScore = score,
                adherenceLevel = level,
                weeklyStats = weeklyStats,
                history = historyList,
                isLoading = false
            )
        }
    }



    private fun checkDateLabel(date: Date, formatter: SimpleDateFormat): String {
        val today = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        calendar.time = date

        return when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hôm nay"
            
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "Hôm qua"
            
            else -> formatter.format(date)
        }
    }

    private fun mapStatus(action: String): AdherenceStatus {
        return when (action.lowercase()) {
            "taken" -> AdherenceStatus.Taken
            "missed" -> AdherenceStatus.Missed
            "skipped", "dismissed" -> AdherenceStatus.Skipped
            else -> AdherenceStatus.Missed
        }
    }
}
