package com.safemed.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.InputStream
import kotlin.coroutines.resume

/**
 * Kết quả xử lý ảnh
 */
sealed class ProcessingResult {
    data class Success(val scanResult: ScanResult) : ProcessingResult()
    data class Error(val message: String, val exception: Exception? = null) : ProcessingResult()
    data object NotFound : ProcessingResult()
}

/**
 * ImageProcessor - Shared logic xử lý ảnh cho cả Camera và Gallery
 * 
 * Sử dụng ML Kit để:
 * 1. Quét mã vạch (Barcode Scanning)
 * 2. Nhận diện text (OCR) để tìm SĐK
 * 
 * @author SafeMed Team
 */
class ImageProcessor : Closeable {

    companion object {
        private const val TAG = "ImageProcessor"
        
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
    
    // Flag để theo dõi trạng thái đóng
    private var isClosed = false

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
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        Log.d(TAG, "ImageProcessor initialized")
    }

    /**
     * Tạo InputImage từ URI ảnh
     * 
     * Xử lý đầy đủ:
     * 1. Đọc ảnh từ ContentResolver (hỗ trợ content:// URI)
     * 2. Xử lý rotation từ EXIF metadata
     * 3. Chuyển đổi sang InputImage
     * 
     * @param context Context để đọc file
     * @param uri URI của ảnh từ thư viện
     * @return InputImage hoặc null nếu lỗi
     */
    suspend fun createInputImageFromUri(context: Context, uri: Uri): InputImage? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating InputImage from URI: $uri")
                
                // Đọc bitmap từ URI qua ContentResolver
                val bitmap = loadBitmapFromUri(context, uri)
                if (bitmap == null) {
                    Log.e(TAG, "Failed to load bitmap from URI")
                    return@withContext null
                }
                
                Log.d(TAG, "Bitmap loaded: ${bitmap.width}x${bitmap.height}")
                
                // Xử lý rotation từ EXIF
                val rotatedBitmap = handleExifRotation(context, uri, bitmap)
                
                Log.d(TAG, "Creating InputImage from bitmap...")
                InputImage.fromBitmap(rotatedBitmap, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create InputImage from URI: $uri", e)
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Load Bitmap từ URI sử dụng ContentResolver
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // Decode với options để tránh OutOfMemory
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                // Reset stream
                inputStream.close()
                val newStream = context.contentResolver.openInputStream(uri)
                
                // Calculate sample size để giảm kích thước nếu ảnh quá lớn
                options.inJustDecodeBounds = false
                options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
                
                newStream?.use { BitmapFactory.decodeStream(it, null, options) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI", e)
            null
        }
    }
    
    /**
     * Tính toán sample size để giảm kích thước ảnh
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Xử lý rotation từ EXIF metadata
     */
    private fun handleExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            
            if (rotationDegrees != 0f) {
                Log.d(TAG, "Rotating bitmap by $rotationDegrees degrees")
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling EXIF rotation", e)
            bitmap
        }
    }

    /**
     * Xử lý ảnh - Quét cả Barcode và OCR
     * 
     * Logic:
     * 1. Thử quét Barcode trước (nhanh, chính xác)
     * 2. Nếu không có Barcode, thử OCR để tìm SĐK
     * 
     * @param inputImage Ảnh đầu vào từ ML Kit
     * @return ProcessingResult với kết quả quét
     */
    suspend fun processImage(inputImage: InputImage): ProcessingResult {
        if (isClosed) {
            Log.e(TAG, "ImageProcessor is closed!")
            return ProcessingResult.Error("ImageProcessor đã bị đóng")
        }
        
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🔍 Starting image processing...")
        Log.d(TAG, "   Image size: ${inputImage.width}x${inputImage.height}")
        Log.d(TAG, "   Rotation: ${inputImage.rotationDegrees}")
        Log.d(TAG, "═══════════════════════════════════════")
        
        // Bước 1: Thử quét Barcode trước
        Log.d(TAG, "Step 1: Scanning for barcodes...")
        val barcodeResult = scanBarcode(inputImage)
        if (barcodeResult is ProcessingResult.Success) {
            Log.d(TAG, "✅ Barcode found!")
            return barcodeResult
        }
        Log.d(TAG, "❌ No barcode found, trying OCR...")
        
        // Bước 2: Không tìm thấy Barcode, thử OCR
        Log.d(TAG, "Step 2: Running OCR text recognition...")
        val ocrResult = scanText(inputImage)
        
        when (ocrResult) {
            is ProcessingResult.Success -> Log.d(TAG, "✅ SDK found via OCR!")
            is ProcessingResult.NotFound -> Log.d(TAG, "❌ No SDK found in OCR text")
            is ProcessingResult.Error -> Log.e(TAG, "❌ OCR error: ${ocrResult.message}")
        }
        
        return ocrResult
    }

    /**
     * Quét mã vạch từ ảnh
     */
    private suspend fun scanBarcode(inputImage: InputImage): ProcessingResult {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Barcode scanner processing...")
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    Log.d(TAG, "Barcode scan complete. Found ${barcodes.size} barcodes")
                    
                    barcodes.forEachIndexed { index, barcode ->
                        Log.d(TAG, "  Barcode[$index]: format=${barcode.format}, value=${barcode.rawValue}")
                    }
                    
                    val validBarcode = barcodes.firstOrNull { barcode ->
                        val rawValue = barcode.rawValue ?: return@firstOrNull false
                        val isValid = isValidBarcode(rawValue)
                        Log.d(TAG, "  Checking barcode '$rawValue' -> valid: $isValid")
                        isValid
                    }

                    if (validBarcode != null) {
                        val code = validBarcode.rawValue!!
                        val result = createScanResult(code, ScanType.BARCODE)
                        Log.d(TAG, "✅ Valid barcode found: $code")
                        continuation.resume(ProcessingResult.Success(result))
                    } else {
                        Log.d(TAG, "No valid barcode found in ${barcodes.size} detected barcodes")
                        continuation.resume(ProcessingResult.NotFound)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed with exception", e)
                    continuation.resume(ProcessingResult.Error("Lỗi quét mã vạch", e as? Exception))
                }
        }
    }

    /**
     * Quét văn bản OCR để tìm SĐK
     */
    private suspend fun scanText(inputImage: InputImage): ProcessingResult {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Text recognizer processing...")
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    Log.d(TAG, "OCR complete. Text length: ${fullText.length} chars")
                    Log.d(TAG, "OCR full text: $fullText")
                    
                    if (fullText.isBlank()) {
                        Log.d(TAG, "OCR returned empty text!")
                        continuation.resume(ProcessingResult.NotFound)
                        return@addOnSuccessListener
                    }
                    
                    val sdkCode = extractSdkFromText(fullText)

                    if (sdkCode != null) {
                        val result = createScanResult(sdkCode, ScanType.SDK)
                        Log.d(TAG, "✅ SDK found via OCR: $sdkCode")
                        continuation.resume(ProcessingResult.Success(result))
                    } else {
                        Log.d(TAG, "No valid SDK pattern found in OCR text")
                        continuation.resume(ProcessingResult.NotFound)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed with exception", e)
                    continuation.resume(ProcessingResult.Error("Lỗi nhận diện văn bản", e as? Exception))
                }
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
     */
    private fun extractSdkFromText(text: String): String? {
        // Preprocess: uppercase and clean whitespace
        val cleanedText = text
            .uppercase()
            .replace("\\s+".toRegex(), " ")
            .trim()

        Log.d(TAG, "OCR Text (cleaned): ${cleanedText.take(300)}")

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
     * Tạo ScanResult từ mã đã phát hiện
     */
    private fun createScanResult(code: String, type: ScanType): ScanResult {
        val normalizedCode = normalizeCode(code)
        
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "✅ DETECTED: ${type.name}")
        Log.d(TAG, "   Original Code: $code")
        Log.d(TAG, "   Normalized: $normalizedCode")
        Log.d(TAG, "═══════════════════════════════════════")
        
        return ScanResult(
            code = code,
            type = type,
            normalizedCode = normalizedCode
        )
    }

    /**
     * Chuẩn hóa mã để dễ so sánh với database
     */
    private fun normalizeCode(code: String): String {
        return code
            .uppercase()
            .replace("-", "")
            .replace(" ", "")
            .trim()
    }

    /**
     * Giải phóng resources
     */
    override fun close() {
        if (!isClosed) {
            isClosed = true
            barcodeScanner.close()
            textRecognizer.close()
            Log.d(TAG, "ImageProcessor closed")
        }
    }
}
