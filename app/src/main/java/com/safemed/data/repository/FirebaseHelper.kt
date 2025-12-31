package com.safemed.data.repository

import android.util.Log
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

private const val TAG = "FirebaseHelper"

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

    /**
     * Xóa tất cả lịch sử quét của người dùng hiện tại
     */
    fun deleteScanHistory(onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: run {
            onComplete(false)
            return
        }
        
        db.collection("scan_history")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    onComplete(true)
                    return@addOnSuccessListener
                }
                
                val batch = db.batch()
                for (doc in docs.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    // ==================== LOGIN HISTORY (Lịch sử đăng nhập) ====================

    /**
     * Lưu lịch sử đăng nhập
     * @param deviceInfo Thông tin thiết bị (tên, model, OS version)
     */
    fun saveLoginHistory(
        deviceName: String = android.os.Build.MODEL,
        location: String = "Unknown",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val userId = getCurrentUserId()
        Log.d(TAG, "saveLoginHistory called - userId: $userId")
        
        if (userId == null) {
            Log.e(TAG, "saveLoginHistory failed - user not logged in")
            onComplete(false)
            return
        }
        
        val loginData = hashMapOf(
            "deviceName" to deviceName,
            "deviceModel" to android.os.Build.MODEL,
            "manufacturer" to android.os.Build.MANUFACTURER,
            "osVersion" to "Android ${android.os.Build.VERSION.RELEASE}",
            "location" to location,
            "timestamp" to System.currentTimeMillis(),
            "isCurrent" to true
        )
        
        Log.d(TAG, "Saving login data: $loginData")
        
        // Mark all previous logins as not current
        db.collection("users")
            .document(userId)
            .collection("login_history")
            .whereEqualTo("isCurrent", true)
            .get()
            .addOnSuccessListener { docs ->
                Log.d(TAG, "Found ${docs.size()} previous current logins")
                val batch = db.batch()
                for (doc in docs.documents) {
                    batch.update(doc.reference, "isCurrent", false)
                }
                
                // Add new login record
                val newLoginRef = db.collection("users")
                    .document(userId)
                    .collection("login_history")
                    .document()
                batch.set(newLoginRef, loginData)
                
                batch.commit()
                    .addOnSuccessListener { 
                        Log.d(TAG, "Login history saved successfully")
                        onComplete(true) 
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to commit login history batch", e)
                        onComplete(false) 
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to query previous logins, saving directly", e)
                // Still try to save the new login
                db.collection("users")
                    .document(userId)
                    .collection("login_history")
                    .add(loginData)
                    .addOnSuccessListener { 
                        Log.d(TAG, "Login history saved successfully (fallback)")
                        onComplete(true) 
                    }
                    .addOnFailureListener { e2 ->
                        Log.e(TAG, "Failed to save login history (fallback)", e2)
                        onComplete(false) 
                    }
            }
    }

    /**
     * Lấy lịch sử đăng nhập của người dùng hiện tại
     */
    fun getLoginHistory(onResult: (List<Map<String, Any>>) -> Unit) {
        val userId = getCurrentUserId()
        Log.d(TAG, "getLoginHistory called - userId: $userId")
        
        if (userId == null) {
            Log.e(TAG, "getLoginHistory failed - user not logged in")
            onResult(emptyList())
            return
        }
        
        db.collection("users")
            .document(userId)
            .collection("login_history")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { docs ->
                Log.d(TAG, "getLoginHistory success - found ${docs.size()} records")
                val history = docs.documents.mapNotNull { it.data }
                onResult(history)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "getLoginHistory failed", e)
                onResult(emptyList())
            }
    }

    /**
     * Xóa tất cả các phiên đăng nhập khác (đăng xuất từ xa)
     */
    fun logoutOtherDevices(onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: run {
            onComplete(false)
            return
        }
        
        db.collection("users")
            .document(userId)
            .collection("login_history")
            .whereEqualTo("isCurrent", false)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    onComplete(true)
                    return@addOnSuccessListener
                }
                
                val batch = db.batch()
                for (doc in docs.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }
}
