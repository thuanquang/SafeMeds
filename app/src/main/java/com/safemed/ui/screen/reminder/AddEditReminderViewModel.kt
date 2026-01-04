package com.safemed.ui.screen.reminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.safemed.alarm.ReminderAlarmManager
import com.safemed.data.model.MedicationReminder
import com.safemed.data.model.Medicine
import com.safemed.data.model.TimeSlot
import com.safemed.data.repository.MedicineRepository
import com.safemed.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * UI State cho màn hình thêm/sửa nhắc nhở
 */
data class AddEditReminderUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    
    // Time slots (null = không bật)
    val morningTime: String? = null,
    val noonTime: String? = null,
    val afternoonTime: String? = null,
    val eveningTime: String? = null,
    
    // Day selection (empty = mỗi ngày)
    val selectedDays: List<Int> = emptyList(),
    val isEveryday: Boolean = true,
    
    // Repeat settings
    val repeatCount: Int = 0, // 0 = forever
    val repeatUntilDate: Long? = null,
    
    // Reminder type
    val isDetailedReminder: Boolean = false,
    
    // Medicine info (for detailed reminder)
    val medicineId: String? = null,
    val medicineName: String = "",
    val dosage: String = "",
    val note: String = "",
    
    // Medicine search
    val medicineSearchQuery: String = "",
    val searchResults: List<Medicine> = emptyList(),
    val isSearching: Boolean = false,
    
    // Snooze duration
    val snoozeDuration: Int = 10, // minutes
    
    // Messages
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * ViewModel cho màn hình thêm/sửa nhắc nhở uống thuốc
 */
@HiltViewModel
class AddEditReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val medicineRepository: MedicineRepository,
    private val reminderAlarmManager: ReminderAlarmManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reminderId: String? = savedStateHandle["reminderId"]

    private val _uiState = MutableStateFlow(AddEditReminderUiState())
    val uiState: StateFlow<AddEditReminderUiState> = _uiState.asStateFlow()

    init {
        if (reminderId != null) {
            loadReminder(reminderId)
        }
    }

    /**
     * Load reminder để edit
     */
    private fun loadReminder(reminderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = reminderRepository.getReminderById(reminderId)
            result.onSuccess { reminder ->
                reminder?.let {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isEditMode = true,
                            morningTime = it.morningTime,
                            noonTime = it.noonTime,
                            afternoonTime = it.afternoonTime,
                            eveningTime = it.eveningTime,
                            selectedDays = it.selectedDays,
                            isEveryday = it.selectedDays.isEmpty(),
                            repeatCount = it.repeatCount,
                            repeatUntilDate = it.repeatUntilDate?.toDate()?.time,
                            isDetailedReminder = it.isDetailedReminder,
                            medicineId = it.medicineId,
                            medicineName = it.medicineName ?: "",
                            dosage = it.dosage ?: "",
                            note = it.note ?: "",
                            snoozeDuration = it.snoozeDuration
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không thể tải nhắc nhở"
                    )
                }
            }
        }
    }

    /**
     * Set thời gian cho một buổi
     */
    fun setTimeForSlot(slot: TimeSlot, time: String?) {
        _uiState.update { state ->
            when (slot) {
                TimeSlot.MORNING -> state.copy(morningTime = time)
                TimeSlot.NOON -> state.copy(noonTime = time)
                TimeSlot.AFTERNOON -> state.copy(afternoonTime = time)
                TimeSlot.EVENING -> state.copy(eveningTime = time)
            }
        }
    }

    /**
     * Toggle buổi bật/tắt
     */
    fun toggleTimeSlot(slot: TimeSlot) {
        val currentState = _uiState.value
        val currentTime = when (slot) {
            TimeSlot.MORNING -> currentState.morningTime
            TimeSlot.NOON -> currentState.noonTime
            TimeSlot.AFTERNOON -> currentState.afternoonTime
            TimeSlot.EVENING -> currentState.eveningTime
        }

        val newTime = if (currentTime == null) {
            slot.getDefaultTimeString()
        } else {
            null
        }

        setTimeForSlot(slot, newTime)
    }

    /**
     * Toggle chọn ngày
     */
    fun toggleDay(day: Int) {
        _uiState.update { state ->
            val newDays = if (day in state.selectedDays) {
                state.selectedDays - day
            } else {
                state.selectedDays + day
            }
            state.copy(
                selectedDays = newDays,
                isEveryday = newDays.isEmpty()
            )
        }
    }

    /**
     * Toggle mỗi ngày
     */
    fun setEveryday(everyday: Boolean) {
        _uiState.update { state ->
            state.copy(
                isEveryday = everyday,
                selectedDays = if (everyday) emptyList() else state.selectedDays
            )
        }
    }

    /**
     * Toggle loại nhắc nhở (chi tiết / chung)
     */
    fun setDetailedReminder(detailed: Boolean) {
        _uiState.update { it.copy(isDetailedReminder = detailed) }
    }

    /**
     * Set tên thuốc tự nhập
     */
    fun setMedicineName(name: String) {
        _uiState.update { it.copy(medicineName = name, medicineId = null) }
    }

    /**
     * Set liều lượng
     */
    fun setDosage(dosage: String) {
        _uiState.update { it.copy(dosage = dosage) }
    }

    /**
     * Set ghi chú
     */
    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    /**
     * Set snooze duration
     */
    fun setSnoozeDuration(duration: Int) {
        _uiState.update { it.copy(snoozeDuration = duration) }
    }

    /**
     * Search medicines
     */
    fun searchMedicine(query: String) {
        _uiState.update { it.copy(medicineSearchQuery = query) }

        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }

            // Tìm thuốc trong Firestore
            val result = medicineRepository.lookupMedicine(query)
            result.onSuccess { medicine ->
                val results = if (medicine != null) listOf(medicine) else emptyList()
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    /**
     * Select medicine từ search results
     */
    fun selectMedicine(medicine: Medicine) {
        _uiState.update {
            it.copy(
                medicineId = medicine.documentId,
                medicineName = medicine.tenThuoc,
                medicineSearchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    /**
     * Clear search results
     */
    fun clearSearch() {
        _uiState.update {
            it.copy(
                medicineSearchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    /**
     * Validate và lưu reminder
     */
    fun saveReminder() {
        val state = _uiState.value

        // Validate: phải có ít nhất một buổi được bật
        if (state.morningTime == null && state.noonTime == null &&
            state.afternoonTime == null && state.eveningTime == null
        ) {
            _uiState.update {
                it.copy(errorMessage = "Vui lòng chọn ít nhất một buổi để nhắc nhở")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val reminder = MedicationReminder(
                reminderId = reminderId ?: "",
                morningTime = state.morningTime,
                noonTime = state.noonTime,
                afternoonTime = state.afternoonTime,
                eveningTime = state.eveningTime,
                selectedDays = if (state.isEveryday) emptyList() else state.selectedDays,
                repeatCount = state.repeatCount,
                repeatUntilDate = state.repeatUntilDate?.let { Timestamp(java.util.Date(it)) },
                isDetailedReminder = state.isDetailedReminder,
                medicineId = state.medicineId,
                medicineName = if (state.isDetailedReminder) state.medicineName.takeIf { it.isNotBlank() } else null,
                dosage = if (state.isDetailedReminder) state.dosage.takeIf { it.isNotBlank() } else null,
                note = state.note.takeIf { it.isNotBlank() },
                snoozeDuration = state.snoozeDuration,
                isActive = true,
                timezone = TimeZone.getDefault().id
            )

            val result = if (state.isEditMode && reminderId != null) {
                reminderRepository.updateReminder(reminder)
            } else {
                reminderRepository.createReminder(reminder).map { }
            }

            result.onSuccess {
                // Schedule local alarms
                val savedReminder = if (state.isEditMode) {
                    reminder
                } else {
                    // Get the created reminder with its ID
                    reminderRepository.getActiveReminders().getOrNull()?.lastOrNull() ?: reminder
                }
                reminderAlarmManager.scheduleReminder(savedReminder)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Không thể lưu nhắc nhở"
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
     * Get time for slot
     */
    fun getTimeForSlot(slot: TimeSlot): String? {
        return when (slot) {
            TimeSlot.MORNING -> _uiState.value.morningTime
            TimeSlot.NOON -> _uiState.value.noonTime
            TimeSlot.AFTERNOON -> _uiState.value.afternoonTime
            TimeSlot.EVENING -> _uiState.value.eveningTime
        }
    }
}
