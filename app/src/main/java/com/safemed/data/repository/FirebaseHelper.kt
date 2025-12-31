package com.safemed.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.safemed.data.model.Medicine
import com.safemed.data.model.Pharmacy
import com.safemed.data.model.ScanHistory
import com.safemed.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Helper class (Repository Pattern)
 * Đơn giản hóa việc CRUD và tích hợp với kiến trúc MVVM
 */
@Singleton
class FirebaseHelper @Inject constructor() {
    
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth

    // ==================== AUTHENTICATION ====================

    /**
     * Lấy User ID của người dùng đang đăng nhập
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Kiểm tra trạng thái đăng nhập
     */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Lấy thông tin người dùng hiện tại từ Firestore
     */
    fun getCurrentUser(onResult: (User?) -> Unit) {
        val userId = getCurrentUserId() ?: run {
            onResult(null)
            return
        }
        
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.toObject(User::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /**
     * Tạo hoặc cập nhật thông tin User sau khi đăng ký/đăng nhập
     */
    fun saveUser(user: User, onComplete: (Boolean) -> Unit) {
        db.collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // ==================== PHARMACIES (Bản đồ) ====================

    /**
     * Lấy danh sách nhà thuốc để hiển thị trên Google Maps
     */
    fun getPharmacies(onSuccess: (List<Pharmacy>) -> Unit, onError: (Exception) -> Unit = {}) {
        db.collection("pharmacies")
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Pharmacy::class.java)
                onSuccess(list)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    /**
     * Lấy nhà thuốc theo ID
     */
    fun getPharmacyById(pharmacyId: String, onResult: (Pharmacy?) -> Unit) {
        db.collection("pharmacies")
            .document(pharmacyId)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.toObject(Pharmacy::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // ==================== MEDICINES (Scanner) ====================

    /**
     * Tra cứu thuốc theo Barcode sau khi quét bằng CameraX + ML Kit
     */
    fun findMedicineByBarcode(barcode: String, onResult: (Medicine?) -> Unit) {
        db.collection("medicines")
            .whereEqualTo("barcode", barcode)
            .get()
            .addOnSuccessListener { docs ->
                onResult(docs.toObjects(Medicine::class.java).firstOrNull())
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /**
     * Lấy thuốc theo ID
     */
    fun getMedicineById(medicineId: String, onResult: (Medicine?) -> Unit) {
        db.collection("medicines")
            .document(medicineId)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.toObject(Medicine::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /**
     * Tìm kiếm thuốc theo tên
     */
    fun searchMedicinesByName(query: String, onResult: (List<Medicine>) -> Unit) {
        db.collection("medicines")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { docs ->
                onResult(docs.toObjects(Medicine::class.java))
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // ==================== SCAN HISTORY (Lịch sử quét) ====================

    /**
     * Lưu lịch sử quét (Tạo quan hệ dữ liệu cho ERD)
     */
    fun saveScanHistory(medicineId: String, result: String, onComplete: (Boolean) -> Unit = {}) {
        val userId = getCurrentUserId() ?: run {
            onComplete(false)
            return
        }
        
        val historyId = db.collection("scan_history").document().id
        val history = ScanHistory(
            historyId = historyId,
            userId = userId,
            medicineId = medicineId,
            result = result
        )
        
        db.collection("scan_history")
            .document(historyId)
            .set(history)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Lấy lịch sử quét của người dùng hiện tại
     */
    fun getUserScanHistory(onResult: (List<ScanHistory>) -> Unit) {
        val userId = getCurrentUserId() ?: run {
            onResult(emptyList())
            return
        }
        
        db.collection("scan_history")
            .whereEqualTo("userId", userId)
            .orderBy("scanTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                onResult(docs.toObjects(ScanHistory::class.java))
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    /**
     * Xóa một lịch sử quét
     */
    fun deleteScanHistory(historyId: String, onComplete: (Boolean) -> Unit) {
        db.collection("scan_history")
            .document(historyId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
