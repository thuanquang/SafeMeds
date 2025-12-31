package com.safemed.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.safemed.data.model.User
import com.safemed.util.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository xử lý Profile operations
 * - Upload avatar lên Firebase Storage
 * - Update user profile trong Firestore
 * - Lấy thông tin user
 */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    companion object {
        private const val TAG = "ProfileRepository"
        private const val USERS_COLLECTION = "users"
        private const val AVATARS_FOLDER = "avatars"
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
     * Upload avatar lên Firebase Storage
     * @param imageUri Uri của ảnh từ gallery/camera
     * @return URL của ảnh đã upload
     */
    suspend fun uploadAvatar(imageUri: Uri): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            // Compress ảnh trước khi upload
            val compressedData = ImageUtils.compressImage(context, imageUri)
                ?: return Result.failure(Exception("Failed to compress image"))
            
            Log.d(TAG, "Compressed image size: ${ImageUtils.getCompressedSizeKB(compressedData)} KB")
            
            // Upload lên Storage với path: avatars/{userId}.jpg
            val storageRef = storage.reference.child("$AVATARS_FOLDER/$userId.jpg")
            storageRef.putBytes(compressedData).await()
            
            // Lấy download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Avatar uploaded successfully: $downloadUrl")
            
            // Update avatarUrl trong Firestore
            db.collection(USERS_COLLECTION).document(userId)
                .update("avatarUrl", downloadUrl)
                .await()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload avatar", e)
            Result.failure(e)
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
     * Xóa avatar khỏi Storage
     */
    suspend fun deleteAvatar(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val storageRef = storage.reference.child("$AVATARS_FOLDER/$userId.jpg")
            storageRef.delete().await()
            
            // Clear avatarUrl trong Firestore
            db.collection(USERS_COLLECTION).document(userId)
                .update("avatarUrl", "")
                .await()
            
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
}
