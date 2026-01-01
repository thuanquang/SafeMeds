package com.safemed.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.safemed.data.model.Medicine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository xử lý truy vấn dữ liệu thuốc từ Firebase Firestore
 * Tối ưu cho tốc độ với 50.000+ dòng dữ liệu
 */
@Singleton
class MedicineRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "MedicineRepository"
        private const val MEDICINES_COLLECTION = "medicines"
    }

    /**
     * Chuẩn hóa mã quét để đối chiếu với Firestore
     * - Chuyển thành CHỮ HOA
     * - Loại bỏ khoảng trắng
     * - Loại bỏ dấu gạch ngang (-) vì Firestore lưu SDK dạng VDxxxxxx (không có dấu -)
     * 
     * VD: "VD-12345-20" -> "VD1234520"
     *     "vd 12345 20" -> "VD1234520"
     * 
     * @param code Mã quét thô từ camera
     * @return Mã đã được chuẩn hóa
     */
    fun normalizeCode(code: String): String {
        return code
            .trim()
            .uppercase()                    // Chuyển thành chữ hoa
            .replace("\\s+".toRegex(), "") // Loại bỏ tất cả khoảng trắng
            .replace("-", "")               // Loại bỏ dấu gạch ngang
            .replace("_", "")               // Loại bỏ dấu gạch dưới (nếu có)
    }

    /**
     * Tra cứu thuốc theo mã quét (SDK hoặc Barcode)
     * Thực hiện 2 truy vấn song song để tối ưu tốc độ
     * 
     * @param scannedCode Mã quét được (đã hoặc chưa chuẩn hóa)
     * @return Result<Medicine?> - Success với Medicine nếu tìm thấy, null nếu không
     */
    suspend fun lookupMedicine(scannedCode: String): Result<Medicine?> {
        val normalizedCode = normalizeCode(scannedCode)
        
        Log.d(TAG, "Looking up medicine with code: $scannedCode (normalized: $normalizedCode)")

        return try {
            // Thực hiện 2 truy vấn song song để tìm theo SDK và Barcode
            coroutineScope {
                val sdkQueryDeferred = async {
                    queryByField("sdk", normalizedCode)
                }
                
                val barcodeQueryDeferred = async {
                    queryByField("barcode", normalizedCode)
                }

                // Chờ cả 2 truy vấn hoàn thành
                val sdkResult = sdkQueryDeferred.await()
                val barcodeResult = barcodeQueryDeferred.await()

                // Ưu tiên kết quả từ SDK trước, sau đó đến barcode
                val medicine = sdkResult ?: barcodeResult

                if (medicine != null) {
                    Log.d(TAG, "Found medicine: ${medicine.tenThuoc}")
                    Result.success(medicine)
                } else {
                    Log.d(TAG, "No medicine found for code: $normalizedCode")
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up medicine", e)
            Result.failure(e)
        }
    }

    /**
     * Truy vấn Firestore theo một trường cụ thể
     * Bỏ qua các document có giá trị trống ở trường đó
     * 
     * @param fieldName Tên trường (sdk hoặc barcode)
     * @param value Giá trị cần tìm
     * @return Medicine nếu tìm thấy, null nếu không
     */
    private suspend fun queryByField(fieldName: String, value: String): Medicine? {
        // Bỏ qua truy vấn nếu value rỗng
        if (value.isBlank()) return null

        return try {
            val querySnapshot = db.collection(MEDICINES_COLLECTION)
                .whereEqualTo(fieldName, value)
                .limit(1) // Tối ưu: chỉ lấy 1 kết quả vì mã là duy nhất
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                val medicine = document.toObject(Medicine::class.java)
                
                // Bỏ qua kết quả nếu trường tìm kiếm trống (dữ liệu không hợp lệ)
                if (medicine != null && getFieldValue(medicine, fieldName).isNotBlank()) {
                    medicine
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying by $fieldName", e)
            null
        }
    }

    /**
     * Lấy giá trị của trường theo tên
     */
    private fun getFieldValue(medicine: Medicine, fieldName: String): String {
        return when (fieldName) {
            "sdk" -> medicine.sdk
            "barcode" -> medicine.barcode
            else -> ""
        }
    }

    /**
     * Tra cứu thuốc theo SDK
     * @param sdk Số đăng ký thuốc
     */
    suspend fun lookupBySdk(sdk: String): Result<Medicine?> {
        val normalizedSdk = normalizeCode(sdk)
        return try {
            val medicine = queryByField("sdk", normalizedSdk)
            Result.success(medicine)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tra cứu thuốc theo Barcode
     * @param barcode Mã vạch sản phẩm
     */
    suspend fun lookupByBarcode(barcode: String): Result<Medicine?> {
        val normalizedBarcode = normalizeCode(barcode)
        return try {
            val medicine = queryByField("barcode", normalizedBarcode)
            Result.success(medicine)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
