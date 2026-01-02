package com.safemed.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.model.Medicine
import com.safemed.data.model.ScanHistory
import com.safemed.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sealed class đại diện cho các trạng thái UI của màn hình lịch sử
 */
sealed class HistoryUiState {
    /** Đang tải dữ liệu */
    object Loading : HistoryUiState()
    
    /** Tải thành công - có dữ liệu */
    data class Success(val historyList: List<ScanHistory>) : HistoryUiState()
    
    /** Danh sách trống */
    object Empty : HistoryUiState()
    
    /** Lỗi khi tải dữ liệu */
    data class Error(val message: String) : HistoryUiState()
}

/**
 * ViewModel quản lý logic lịch sử quét thuốc
 * Sử dụng StateFlow để reactive với Compose UI
 * Hỗ trợ realtime updates từ Firestore
 * 
 * @param historyRepository Repository để truy vấn Firestore
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // State để lưu history item được chọn (để xem chi tiết)
    private val _selectedHistory = MutableStateFlow<ScanHistory?>(null)
    val selectedHistory: StateFlow<ScanHistory?> = _selectedHistory.asStateFlow()

    // State cho thao tác xóa
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Tải danh sách lịch sử từ Firestore
     * Sử dụng Flow để nhận realtime updates
     */
    fun loadHistory() {
        viewModelScope.launch {
            android.util.Log.d("HistoryViewModel", "Loading history...")
            _uiState.value = HistoryUiState.Loading

            historyRepository.getHistory()
                .catch { e ->
                    android.util.Log.e("HistoryViewModel", "Error loading history", e)
                    _uiState.value = HistoryUiState.Error(
                        mapErrorMessage(e)
                    )
                }
                .collect { historyList ->
                    android.util.Log.d("HistoryViewModel", "Received ${historyList.size} history items")
                    _uiState.value = if (historyList.isEmpty()) {
                        HistoryUiState.Empty
                    } else {
                        HistoryUiState.Success(historyList)
                    }
                }
        }
    }

    /**
     * Thêm một bản ghi vào lịch sử quét
     * Được gọi sau khi xác thực thuốc thành công
     * 
     * @param medicine Thông tin thuốc
     * @param scannedCode Mã đã quét
     */
    fun addToHistory(medicine: Medicine, scannedCode: String) {
        viewModelScope.launch {
            historyRepository.addToHistory(medicine, scannedCode)
                .onSuccess { historyId ->
                    // History sẽ tự động cập nhật qua Flow listener
                    android.util.Log.d("HistoryViewModel", "History added: $historyId")
                }
                .onFailure { error ->
                    android.util.Log.e("HistoryViewModel", "Error adding history", error)
                }
        }
    }

    /**
     * Chọn một history item để xem chi tiết
     */
    fun selectHistory(history: ScanHistory) {
        _selectedHistory.value = history
    }

    /**
     * Clear history item đã chọn
     */
    fun clearSelectedHistory() {
        _selectedHistory.value = null
    }

    /**
     * Xóa một bản ghi lịch sử
     * 
     * @param historyId ID của bản ghi cần xóa
     */
    fun deleteHistory(historyId: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting

            historyRepository.deleteHistory(historyId)
                .onSuccess {
                    _deleteState.value = DeleteState.Success
                    // Reset state sau 2 giây
                    kotlinx.coroutines.delay(2000)
                    _deleteState.value = DeleteState.Idle
                }
                .onFailure { error ->
                    _deleteState.value = DeleteState.Error(mapErrorMessage(error))
                }
        }
    }

    /**
     * Xóa toàn bộ lịch sử
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting

            historyRepository.clearAllHistory()
                .onSuccess { count ->
                    _deleteState.value = DeleteState.Success
                    android.util.Log.d("HistoryViewModel", "Cleared $count history items")
                    // Reset state sau 2 giây
                    kotlinx.coroutines.delay(2000)
                    _deleteState.value = DeleteState.Idle
                }
                .onFailure { error ->
                    _deleteState.value = DeleteState.Error(mapErrorMessage(error))
                }
        }
    }

    /**
     * Reset delete state về Idle
     */
    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    /**
     * Map exception thành message thân thiện cho người dùng
     */
    private fun mapErrorMessage(error: Throwable): String {
        return when {
            error.message?.contains("PERMISSION_DENIED") == true -> 
                "Bạn không có quyền truy cập dữ liệu này"
            error.message?.contains("UNAVAILABLE") == true -> 
                "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng."
            error.message?.contains("NOT_FOUND") == true -> 
                "Không tìm thấy dữ liệu"
            else -> 
                error.message ?: "Đã xảy ra lỗi không xác định"
        }
    }
}

/**
 * Sealed class cho trạng thái xóa
 */
sealed class DeleteState {
    object Idle : DeleteState()
    object Deleting : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}
