package com.safemed.ui.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.model.Medicine
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanResultUiState())
    val uiState: StateFlow<ScanResultUiState> = _uiState.asStateFlow()

    // Lấy scannedCode từ navigation argument
    private val scannedCode: String = savedStateHandle.get<String>("scannedCode") ?: ""

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
                    
                    _uiState.update {
                        it.copy(
                            lookupState = if (medicine != null) {
                                MedicineLookupState.Success(
                                    medicine = medicine,
                                    verificationTime = verificationTime
                                )
                            } else {
                                MedicineLookupState.NotFound(
                                    scannedCode = normalizedCode,
                                    verificationTime = verificationTime
                                )
                            }
                        )
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
