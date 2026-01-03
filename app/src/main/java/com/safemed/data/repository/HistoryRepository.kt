package com.safemed.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.safemed.data.model.Medicine
import com.safemed.data.model.ScanHistory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository xử lý lịch sử quét thuốc từ Firebase Firestore
 * Collection: history
 * 
 * Composite Index cần tạo:
 * - userId (Ascending) + timestamp (Descending)
 * 
 * Chiến lược:
 * - Lưu snapshot thông tin thuốc để hiển thị nhanh
 * - Sử dụng StateFlow để cập nhật realtime
 * - Hỗ trợ phân trang (pagination) với limit
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "HistoryRepository"
        private const val HISTORY_COLLECTION = "history"
        private const val DEFAULT_PAGE_SIZE = 20
    }

    /**
     * Lấy ID của người dùng hiện tại
     */
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Thêm một bản ghi vào lịch sử quét
     * Tự động lấy userId từ FirebaseAuth
     * 
     * @param medicine Thông tin thuốc đã xác thực thành công
     * @param scannedCode Mã thực tế người dùng đã quét
     * @return Result<String> - ID của document vừa tạo
     */
    suspend fun addToHistory(medicine: Medicine, scannedCode: String): Result<String> {
        val userId = getCurrentUserId()
        
        if (userId == null) {
            Log.e(TAG, "User not logged in, cannot save history")
            return Result.failure(Exception("Người dùng chưa đăng nhập"))
        }

        return try {
            val historyEntry = hashMapOf(
                "userId" to userId,
                "medicineId" to medicine.documentId,
                "scannedCode" to scannedCode,
                "medicineName" to medicine.tenThuoc,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "result" to "authentic"
            )

            val documentRef = db.collection(HISTORY_COLLECTION)
                .add(historyEntry)
                .await()

            Log.d(TAG, "History added successfully: ${documentRef.id}")
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding history", e)
            Result.failure(e)
        }
    }

    /**
     * Thêm thuốc không tìm thấy vào lịch sử
     * 
     * @param scannedCode Mã người dùng đã quét
     * @return Result<String> - ID của document vừa tạo
     */
    suspend fun addNotFoundToHistory(scannedCode: String): Result<String> {
        val userId = getCurrentUserId()
        
        if (userId == null) {
            Log.e(TAG, "User not logged in, cannot save history")
            return Result.failure(Exception("Người dùng chưa đăng nhập"))
        }

        return try {
            val historyEntry = hashMapOf(
                "userId" to userId,
                "medicineId" to "",
                "scannedCode" to scannedCode,
                "medicineName" to "Không xác thực được",
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "result" to "not_found"
            )

            val documentRef = db.collection(HISTORY_COLLECTION)
                .add(historyEntry)
                .await()

            Log.d(TAG, "NotFound history added successfully: ${documentRef.id}")
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding not found history", e)
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách lịch sử quét của người dùng hiện tại
     * Sắp xếp theo timestamp giảm dần (mới nhất lên đầu)
     * 
     * @param limit Số lượng bản ghi tối đa (mặc định 20)
     * @return Flow<List<ScanHistory>> - Danh sách lịch sử realtime
     */
    fun getHistory(limit: Int = DEFAULT_PAGE_SIZE): Flow<List<ScanHistory>> = callbackFlow {
        val userId = getCurrentUserId()
        
        if (userId == null) {
            Log.e(TAG, "User not logged in, cannot get history")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Fetching history for userId: $userId")

        // Query đơn giản hơn - không dùng orderBy để tránh cần composite index
        // Sắp xếp sẽ được thực hiện ở client-side
        val listenerRegistration = db.collection(HISTORY_COLLECTION)
            .whereEqualTo("userId", userId)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to history: ${error.message}", error)
                    // Nếu lỗi do thiếu index, log rõ ràng
                    if (error.message?.contains("index") == true) {
                        Log.e(TAG, "FIRESTORE INDEX REQUIRED! Check Logcat for the link to create index.")
                    }
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val historyList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ScanHistory::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document ${doc.id}", e)
                        null
                    }
                }?.sortedByDescending { it.getTimestampMillis() } ?: emptyList()

                Log.d(TAG, "History updated: ${historyList.size} items for user $userId")
                trySend(historyList)
            }

        awaitClose {
            listenerRegistration.remove()
            Log.d(TAG, "History listener removed")
        }
    }

    /**
     * Lấy một bản ghi lịch sử theo ID
     * 
     * @param historyId ID của bản ghi
     * @return Result<ScanHistory?> - Bản ghi lịch sử
     */
    suspend fun getHistoryById(historyId: String): Result<ScanHistory?> {
        return try {
            val document = db.collection(HISTORY_COLLECTION)
                .document(historyId)
                .get()
                .await()

            val history = document.toObject(ScanHistory::class.java)
            Log.d(TAG, "History fetched: ${history?.medicineName}")
            Result.success(history)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching history by ID", e)
            Result.failure(e)
        }
    }

    /**
     * Xóa một bản ghi lịch sử
     * Chỉ cho phép xóa nếu userId khớp với người dùng hiện tại
     * 
     * @param historyId ID của bản ghi cần xóa
     * @return Result<Unit>
     */
    suspend fun deleteHistory(historyId: String): Result<Unit> {
        val userId = getCurrentUserId()
        
        if (userId == null) {
            return Result.failure(Exception("Người dùng chưa đăng nhập"))
        }

        return try {
            // Kiểm tra quyền sở hữu trước khi xóa
            val document = db.collection(HISTORY_COLLECTION)
                .document(historyId)
                .get()
                .await()

            val history = document.toObject(ScanHistory::class.java)
            
            if (history?.userId != userId) {
                return Result.failure(Exception("Không có quyền xóa bản ghi này"))
            }

            db.collection(HISTORY_COLLECTION)
                .document(historyId)
                .delete()
                .await()

            Log.d(TAG, "History deleted: $historyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting history", e)
            Result.failure(e)
        }
    }

    /**
     * Xóa toàn bộ lịch sử của người dùng hiện tại
     * 
     * @return Result<Int> - Số lượng bản ghi đã xóa
     */
    suspend fun clearAllHistory(): Result<Int> {
        val userId = getCurrentUserId()
        
        if (userId == null) {
            return Result.failure(Exception("Người dùng chưa đăng nhập"))
        }

        return try {
            val documents = db.collection(HISTORY_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val batch = db.batch()
            documents.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(TAG, "All history cleared: ${documents.size()} items")
            Result.success(documents.size())
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing history", e)
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách lịch sử với phân trang (Pagination)
     * Sử dụng cho lazy loading khi danh sách lớn
     * 
     * @param lastTimestamp Timestamp của bản ghi cuối cùng đã load
     * @param limit Số lượng bản ghi mỗi trang
     * @return Result<List<ScanHistory>>
     */
    suspend fun getHistoryPaginated(
        lastTimestamp: com.google.firebase.Timestamp? = null,
        limit: Int = DEFAULT_PAGE_SIZE
    ): Result<List<ScanHistory>> {
        val userId = getCurrentUserId()
        
        if (userId == null) {
            return Result.failure(Exception("Người dùng chưa đăng nhập"))
        }

        return try {
            var query = db.collection(HISTORY_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            // Nếu có lastTimestamp, bắt đầu từ đó
            if (lastTimestamp != null) {
                query = query.startAfter(lastTimestamp)
            }

            val documents = query.get().await()
            
            val historyList = documents.mapNotNull { doc ->
                doc.toObject(ScanHistory::class.java)
            }

            Log.d(TAG, "History paginated: ${historyList.size} items")
            Result.success(historyList)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paginated history", e)
            Result.failure(e)
        }
    }
}
