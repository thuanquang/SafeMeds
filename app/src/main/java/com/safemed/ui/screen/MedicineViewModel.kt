package com.safemed.ui.screen

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.model.Medicine
import com.safemed.data.repository.HistoryRepository
import com.safemed.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Sealed class đại diện cho các trạng thái UI của màn hình kết quả quét
 */
sealed class MedicineLookupState {
    /** Trạng thái ban đầu - chưa bắt đầu tra cứu */
    object Idle : MedicineLookupState()
    
    /** Đang tra cứu dữ liệu từ Firestore */
    object Loading : MedicineLookupState()
    
    /** Tra cứu thành công - tìm thấy thuốc trong CSDL Bộ Y tế */
    data class Success(
        val medicine: Medicine,
        val verificationTime: String
    ) : MedicineLookupState()
    
    /** Không tìm thấy thuốc trong CSDL */
    data class NotFound(
        val scannedCode: String,
        val verificationTime: String
    ) : MedicineLookupState()
    
    /** Lỗi khi tra cứu (mạng, Firestore, etc.) */
    data class Error(
        val message: String,
        val scannedCode: String
    ) : MedicineLookupState()
}

/**
 * Data class chứa toàn bộ state của màn hình kết quả
 */
data class ScanResultUiState(
    val lookupState: MedicineLookupState = MedicineLookupState.Idle,
    val scannedCode: String = ""
)

/**
 * ViewModel quản lý logic tra cứu và xác thực thuốc
 * Sử dụng StateFlow để reactive với Compose UI
 * 
 * @param medicineRepository Repository để truy vấn Firestore
 * @param savedStateHandle Handle để lấy navigation arguments
 */
@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val historyRepository: HistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "MedicineViewModel"
    }

    private val _uiState = MutableStateFlow(ScanResultUiState())
    val uiState: StateFlow<ScanResultUiState> = _uiState.asStateFlow()

    // Flag để đảm bảo chỉ lưu lịch sử 1 lần - persist across navigation
    private var historySaved = false

    // Lấy scannedCode từ navigation argument
    private val scannedCode: String = savedStateHandle.get<String>("scannedCode") ?: ""
    
    // Kiểm tra xem đang xem lại từ History hay quét mới
    private val fromHistory: Boolean = savedStateHandle.get<Boolean>("fromHistory") ?: false

    init {
        // Tự động tra cứu khi ViewModel được khởi tạo
        if (scannedCode.isNotBlank()) {
            _uiState.update { it.copy(scannedCode = scannedCode) }
            lookupMedicine(scannedCode)
        }
    }

    /**
     * Chuẩn hóa mã quét
     * Delegate cho repository để đảm bảo logic nhất quán
     */
    fun normalizeCode(code: String): String {
        return medicineRepository.normalizeCode(code)
    }

    /**
     * Tra cứu thuốc từ Firestore
     * Gọi hàm này khi cần tra cứu lại hoặc tra cứu mã mới
     * 
     * @param code Mã quét được (SDK hoặc Barcode)
     */
    fun lookupMedicine(code: String) {
        val normalizedCode = normalizeCode(code)
        
        viewModelScope.launch {
            // Chuyển sang trạng thái Loading
            _uiState.update { 
                it.copy(
                    lookupState = MedicineLookupState.Loading,
                    scannedCode = normalizedCode
                )
            }

            // Thực hiện tra cứu
            medicineRepository.lookupMedicine(normalizedCode)
                .onSuccess { medicine ->
                    val verificationTime = getCurrentTimeFormatted()
                    
                    if (medicine != null) {
                        _uiState.update {
                            it.copy(
                                lookupState = MedicineLookupState.Success(
                                    medicine = medicine,
                                    verificationTime = verificationTime
                                )
                            )
                        }
                        // Tự động lưu vào lịch sử
                        saveToHistoryIfNeeded(medicine, normalizedCode)
                    } else {
                        _uiState.update {
                            it.copy(
                                lookupState = MedicineLookupState.NotFound(
                                    scannedCode = normalizedCode,
                                    verificationTime = verificationTime
                                )
                            )
                        }
                        // Lưu vào lịch sử với kết quả not_found
                        saveNotFoundToHistoryIfNeeded(normalizedCode)
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            lookupState = MedicineLookupState.Error(
                                message = mapErrorMessage(exception),
                                scannedCode = normalizedCode
                            )
                        )
                    }
                }
        }
    }

    /**
     * Lưu thuốc đã xác thực thành công vào lịch sử (chỉ 1 lần)
     * Bỏ qua nếu đang xem lại từ History
     */
    private fun saveToHistoryIfNeeded(medicine: Medicine, scannedCode: String) {
        // Không lưu nếu đang xem lại từ History
        if (fromHistory) {
            Log.d(TAG, "Viewing from history, skipping save...")
            return
        }
        
        if (historySaved) {
            Log.d(TAG, "History already saved, skipping...")
            return
        }
        
        historySaved = true
        viewModelScope.launch {
            historyRepository.addToHistory(medicine, scannedCode)
                .onSuccess { historyId ->
                    Log.d(TAG, "History saved successfully: $historyId")
                }
                .onFailure { error ->
                    Log.e(TAG, "Error saving history", error)
                    historySaved = false // Reset để có thể thử lại
                }
        }
    }

    /**
     * Lưu thuốc không tìm thấy vào lịch sử (chỉ 1 lần)
     * Bỏ qua nếu đang xem lại từ History
     */
    private fun saveNotFoundToHistoryIfNeeded(scannedCode: String) {
        // Không lưu nếu đang xem lại từ History
        if (fromHistory) {
            Log.d(TAG, "Viewing from history, skipping save...")
            return
        }
        
        if (historySaved) {
            Log.d(TAG, "History already saved, skipping...")
            return
        }
        
        historySaved = true
        viewModelScope.launch {
            historyRepository.addNotFoundToHistory(scannedCode)
                .onSuccess { historyId ->
                    Log.d(TAG, "NotFound history saved successfully: $historyId")
                }
                .onFailure { error ->
                    Log.e(TAG, "Error saving not found history", error)
                    historySaved = false // Reset để có thể thử lại
                }
        }
    }

    /**
     * Retry tra cứu khi gặp lỗi
     */
    fun retry() {
        val code = _uiState.value.scannedCode
        if (code.isNotBlank()) {
            lookupMedicine(code)
        }
    }

    /**
     * Reset state về ban đầu
     */
    fun resetState() {
        _uiState.update { ScanResultUiState() }
    }

    /**
     * Format thời gian hiện tại theo định dạng Việt Nam
     */
    private fun getCurrentTimeFormatted(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        return dateFormat.format(Date())
    }

    /**
     * Map exception sang thông báo lỗi thân thiện với người dùng
     */
    private fun mapErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("network", ignoreCase = true) == true ||
            exception.message?.contains("internet", ignoreCase = true) == true ->
                "Không có kết nối mạng. Vui lòng kiểm tra và thử lại."
            
            exception.message?.contains("timeout", ignoreCase = true) == true ->
                "Kết nối quá chậm. Vui lòng thử lại."
            
            exception.message?.contains("permission", ignoreCase = true) == true ->
                "Không có quyền truy cập dữ liệu. Vui lòng liên hệ hỗ trợ."
            
            else -> "Đã xảy ra lỗi: ${exception.localizedMessage ?: "Không xác định"}"
        }
    }
}
