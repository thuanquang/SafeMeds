package com.safemed.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.safemed.data.model.User
import com.safemed.util.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository xử lý Profile operations
 * - Upload avatar dưới dạng Base64 vào Firestore (không cần Firebase Storage)
 * - Update user profile trong Firestore
 * - Lấy thông tin user
 */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ProfileRepository"
        private const val USERS_COLLECTION = "users"
        private const val MAX_AVATAR_SIZE = 256 // pixels - nhỏ hơn để giảm kích thước Base64
        private const val AVATAR_QUALITY = 70 // Quality thấp hơn để giảm kích thước
        private const val MAX_AVATAR_BYTES = 400 * 1024 // 400KB limit cho Base64
    }

    /**
     * Lấy thông tin user hiện tại từ Firestore
     */
    suspend fun getCurrentUserProfile(): Result<User?> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val document = db.collection(USERS_COLLECTION).document(userId).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Upload avatar - lưu dưới dạng Base64 vào Firestore
     * Không cần Firebase Storage (Blaze plan)
     * @param imageUri Uri của ảnh từ gallery/camera
     * @return Data URL của ảnh (data:image/jpeg;base64,...)
     */
    suspend fun uploadAvatar(imageUri: Uri): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            Log.d(TAG, "Starting avatar upload for user: $userId")
            
            // Compress ảnh với kích thước nhỏ hơn cho Base64
            val compressedData = ImageUtils.compressImage(
                context = context,
                imageUri = imageUri,
                maxSize = MAX_AVATAR_SIZE,
                quality = AVATAR_QUALITY
            ) ?: return Result.failure(Exception("Không thể xử lý ảnh"))
            
            Log.d(TAG, "Compressed image size: ${compressedData.size / 1024} KB")
            
            // Kiểm tra kích thước - Firestore document limit là 1MB
            if (compressedData.size > MAX_AVATAR_BYTES) {
                Log.e(TAG, "Image too large: ${compressedData.size} bytes")
                return Result.failure(Exception("Ảnh quá lớn. Vui lòng chọn ảnh nhỏ hơn"))
            }
            
            // Convert to Base64
            val base64String = Base64.encodeToString(compressedData, Base64.NO_WRAP)
            val avatarDataUrl = "data:image/jpeg;base64,$base64String"
            
            Log.d(TAG, "Base64 string length: ${base64String.length}")
            
            // Save to Firestore - use set with merge to create document if not exists
            db.collection(USERS_COLLECTION).document(userId)
                .set(mapOf("avatarUrl" to avatarDataUrl), com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            Log.d(TAG, "Avatar saved to Firestore successfully")
            
            // Also update Firebase Auth profile photo
            try {
                val profileUpdates = userProfileChangeRequest {
                    photoUri = Uri.parse(avatarDataUrl)
                }
                auth.currentUser?.updateProfile(profileUpdates)?.await()
                Log.d(TAG, "Firebase Auth profile updated")
            } catch (e: Exception) {
                // Non-critical, just log
                Log.w(TAG, "Could not update Auth profile photo: ${e.message}")
            }
            
            Result.success(avatarDataUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload avatar: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("PERMISSION_DENIED") == true ->
                    "Không có quyền cập nhật. Vui lòng đăng nhập lại."
                e.message?.contains("NOT_FOUND") == true ->
                    "Không tìm thấy hồ sơ người dùng."
                e.message?.contains("network") == true ->
                    "Lỗi kết nối mạng. Vui lòng kiểm tra internet."
                else -> e.message ?: "Lỗi không xác định"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Cập nhật thông tin profile (fullName, email)
     */
    suspend fun updateProfile(fullName: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val updates = hashMapOf<String, Any>(
                "fullName" to fullName
            )
            
            db.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật cả avatar URL và fullName
     */
    suspend fun updateProfileWithAvatar(fullName: String, avatarUrl: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val updates = hashMapOf<String, Any>(
                "fullName" to fullName,
                "avatarUrl" to avatarUrl
            )
            
            db.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Profile with avatar updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile with avatar", e)
            Result.failure(e)
        }
    }

    /**
     * Xóa avatar - chỉ xóa trong Firestore
     */
    suspend fun deleteAvatar(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            // Clear avatarUrl trong Firestore
            db.collection(USERS_COLLECTION).document(userId)
                .update("avatarUrl", "")
                .await()
            
            // Clear Firebase Auth photo
            try {
                val profileUpdates = userProfileChangeRequest {
                    photoUri = null
                }
                auth.currentUser?.updateProfile(profileUpdates)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear Auth profile photo: ${e.message}")
            }
            
            Log.d(TAG, "Avatar deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete avatar", e)
            Result.failure(e)
        }
    }

    /**
     * Kiểm tra user đã có avatar chưa
     */
    suspend fun hasAvatar(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            val document = db.collection(USERS_COLLECTION).document(userId).get().await()
            val user = document.toObject(User::class.java)
            !user?.avatarUrl.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cập nhật ngôn ngữ người dùng vào Firestore
     * Được gọi khi user thay đổi ngôn ngữ trong Settings
     * @param languageCode "vi" hoặc "en"
     */
    suspend fun updateLanguage(languageCode: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            db.collection(USERS_COLLECTION).document(userId)
                .set(mapOf("language" to languageCode), com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            Log.d(TAG, "Language updated to: $languageCode")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update language", e)
            Result.failure(e)
        }
    }
}
