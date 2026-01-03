package com.safemed.scanner

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kết quả quét thuốc
 * @param code Mã thuốc (barcode hoặc SĐK)
 * @param type Loại mã ("BARCODE" hoặc "SDK")
 * @param normalizedCode Mã đã chuẩn hóa (loại bỏ dấu gạch ngang, viết hoa)
 */
data class ScanResult(
    val code: String,
    val type: ScanType,
    val normalizedCode: String
)

/**
 * Loại mã quét được
 */
enum class ScanType {
    BARCODE,    // Mã vạch (EAN-13, UPC-A, ...)
    SDK         // Số đăng ký thuốc (VN-12345-20, VD-12345-20, ...)
}

/**
 * Interface callback khi phát hiện mã thuốc
 */
fun interface OnMedicineDetectedListener {
    fun onDetected(result: ScanResult)
}

/**
 * Hybrid Medicine Analyzer - Kết hợp Barcode Scanning và OCR
 * 
 * Logic hoạt động:
 * 1. Ưu tiên 1: Quét mã vạch trước (nhanh, chính xác)
 * 2. Ưu tiên 2: Nếu không có barcode, dùng OCR để tìm SĐK
 * 
 * Debounce: Chỉ trả kết quả mới nếu khác mã trước đó trong 2 giây
 * 
 * @param onMedicineDetected Callback khi phát hiện mã thuốc hợp lệ
 */
class HybridMedicineAnalyzer(
    private val onMedicineDetected: OnMedicineDetectedListener
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "SafeMedsScanner"
        
        // Debounce time in milliseconds
        private const val DEBOUNCE_TIME_MS = 2000L
        
        // Regex patterns
        // SĐK format: VN-12345-20, VD-12345-20, VS-123456-24, etc.
        private val SDK_REGEX = Regex(
            pattern = "([A-Z]{2,6})[\\s\\-_]*([0-9]{3,6})[\\s\\-_]*([0-9]{2})",
            option = RegexOption.IGNORE_CASE
        )
        
        // Barcode: 12-13 digit numbers (EAN-13, UPC-A)
        private val BARCODE_REGEX = Regex("^[0-9]{12,13}$")
        
        // Vietnamese SDK prefixes
        private val VALID_SDK_PREFIXES = setOf(
            "VN", "VD", "VS", "VT",  // Thuốc trong nước/nhập khẩu
            "SP", "QLSP",             // Sản phẩm đặc biệt
            "GC", "GCXT"              // Giấy chứng nhận
        )
    }

    // ML Kit clients
    private val barcodeScanner: BarcodeScanner
    private val textRecognizer: TextRecognizer
    
    // Debounce tracking
    private var lastDetectedCode: String? = null
    private var lastDetectionTime: Long = 0L
    
    // Processing flag to prevent concurrent analysis
    private val isProcessing = AtomicBoolean(false)

    init {
        // Configure Barcode Scanner for common medicine barcode formats
        val barcodeOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        
        barcodeScanner = BarcodeScanning.getClient(barcodeOptions)
        
        // Configure Text Recognizer for Latin characters (Vietnamese)
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        Log.d(TAG, "HybridMedicineAnalyzer initialized")
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // Prevent concurrent processing
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            isProcessing.set(false)
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        // Priority 1: Try Barcode scanning first (faster, more accurate)
        scanBarcode(inputImage, imageProxy)
    }

    /**
     * Quét mã vạch - Ưu tiên cao nhất
     */
    private fun scanBarcode(inputImage: InputImage, imageProxy: ImageProxy) {
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val validBarcode = barcodes.firstOrNull { barcode ->
                    val rawValue = barcode.rawValue ?: return@firstOrNull false
                    isValidBarcode(rawValue)
                }

                if (validBarcode != null) {
                    val code = validBarcode.rawValue!!
                    handleDetection(code, ScanType.BARCODE, imageProxy)
                } else {
                    // No valid barcode found, try OCR
                    scanText(inputImage, imageProxy)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scanning failed", e)
                // Fallback to OCR on barcode failure
                scanText(inputImage, imageProxy)
            }
    }

    /**
     * Quét văn bản OCR để tìm SĐK - Ưu tiên thứ 2
     */
    private fun scanText(inputImage: InputImage, imageProxy: ImageProxy) {
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                val sdkCode = extractSdkFromText(fullText)

                if (sdkCode != null) {
                    handleDetection(sdkCode, ScanType.SDK, imageProxy)
                } else {
                    // No valid code found
                    finishAnalysis(imageProxy)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
                finishAnalysis(imageProxy)
            }
    }

    /**
     * Kiểm tra mã vạch hợp lệ
     */
    private fun isValidBarcode(code: String): Boolean {
        return BARCODE_REGEX.matches(code)
    }

    /**
     * Trích xuất SĐK từ văn bản OCR
     * 
     * Tiền xử lý:
     * 1. Viết hoa toàn bộ
     * 2. Loại bỏ khoảng trắng dư thừa
     * 3. Khớp Regex SĐK
     * 4. Validate prefix
     */
    private fun extractSdkFromText(text: String): String? {
        // Preprocess: uppercase and clean whitespace
        val cleanedText = text
            .uppercase()
            .replace("\\s+".toRegex(), " ")
            .trim()

        Log.d(TAG, "OCR Text (cleaned): $cleanedText")

        // Find all potential SDK matches
        val matches = SDK_REGEX.findAll(cleanedText)
        
        for (match in matches) {
            val prefix = match.groupValues[1].uppercase()
            val number = match.groupValues[2]
            val year = match.groupValues[3]
            
            // Validate prefix
            if (prefix in VALID_SDK_PREFIXES) {
                val sdkCode = "$prefix-$number-$year"
                Log.d(TAG, "Found valid SDK: $sdkCode")
                return sdkCode
            }
        }

        return null
    }

    /**
     * Xử lý kết quả phát hiện với Debounce
     */
    private fun handleDetection(code: String, type: ScanType, imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        val normalizedCode = normalizeCode(code)
        
        // Debounce: Skip if same code detected within DEBOUNCE_TIME_MS
        val shouldSkip = normalizedCode == lastDetectedCode && 
                         (currentTime - lastDetectionTime) < DEBOUNCE_TIME_MS
        
        if (shouldSkip) {
            Log.d(TAG, "Debounce: Skipping duplicate detection: $code")
            finishAnalysis(imageProxy)
            return
        }

        // Update debounce tracking
        lastDetectedCode = normalizedCode
        lastDetectionTime = currentTime

        // Create result
        val result = ScanResult(
            code = code,
            type = type,
            normalizedCode = normalizedCode
        )

        // Log result
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "✅ DETECTED: ${type.name}")
        Log.d(TAG, "   Original Code: $code")
        Log.d(TAG, "   Normalized: $normalizedCode")
        Log.d(TAG, "═══════════════════════════════════════")

        // Callback
        onMedicineDetected.onDetected(result)
        
        finishAnalysis(imageProxy)
    }

    /**
     * Chuẩn hóa mã để dễ so sánh với database
     * - Viết hoa
     * - Loại bỏ dấu gạch ngang
     * - Loại bỏ khoảng trắng
     */
    private fun normalizeCode(code: String): String {
        return code
            .uppercase()
            .replace("-", "")
            .replace(" ", "")
            .trim()
    }

    /**
     * Kết thúc phân tích, giải phóng resources
     */
    private fun finishAnalysis(imageProxy: ImageProxy) {
        imageProxy.close()
        isProcessing.set(false)
    }

    /**
     * Reset debounce - gọi khi muốn cho phép quét lại mã cũ
     */
    fun resetDebounce() {
        lastDetectedCode = null
        lastDetectionTime = 0L
        Log.d(TAG, "Debounce reset")
    }

    /**
     * Giải phóng resources khi không dùng nữa
     */
    fun close() {
        barcodeScanner.close()
        textRecognizer.close()
        Log.d(TAG, "HybridMedicineAnalyzer closed")
    }
}
