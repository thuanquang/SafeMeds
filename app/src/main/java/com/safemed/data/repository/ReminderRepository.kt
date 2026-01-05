package com.safemed.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.safemed.data.model.MedicationReminder
import com.safemed.data.model.ReminderLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository xử lý CRUD cho lịch nhắc nhở uống thuốc
 * Lưu trong Firestore: users/{userId}/reminders
 */
@Singleton
class ReminderRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "ReminderRepository"
        private const val USERS_COLLECTION = "users"
        private const val REMINDERS_COLLECTION = "reminders"
        private const val REMINDER_LOGS_COLLECTION = "reminder_logs"
    }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Lấy collection reference cho reminders của user hiện tại
     */
    private fun getRemindersCollection() = currentUserId?.let {
        db.collection(USERS_COLLECTION).document(it).collection(REMINDERS_COLLECTION)
    }

    /**
     * Lấy tất cả reminders của user hiện tại (realtime)
     */
    fun getRemindersFlow(): Flow<List<MedicationReminder>> = callbackFlow {
        val collection = getRemindersCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = collection
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to reminders", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val reminders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MedicationReminder::class.java)
                } ?: emptyList()

                trySend(reminders)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Lấy tất cả active reminders
     */
    suspend fun getActiveReminders(): Result<List<MedicationReminder>> {
        return try {
            val collection = getRemindersCollection()
                ?: return Result.failure(Exception("User not logged in"))

            val snapshot = collection
                .whereEqualTo("is_active", true)
                .get()
                .await()

            val reminders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MedicationReminder::class.java)
            }

            Result.success(reminders)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active reminders", e)
            Result.failure(e)
        }
    }

    /**
     * Lấy reminder theo ID
     */
    suspend fun getReminderById(reminderId: String): Result<MedicationReminder?> {
        return try {
            val collection = getRemindersCollection()
                ?: return Result.failure(Exception("User not logged in"))

            val doc = collection.document(reminderId).get().await()
            val reminder = doc.toObject(MedicationReminder::class.java)

            Result.success(reminder)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reminder by ID", e)
            Result.failure(e)
        }
    }

    /**
     * Tạo reminder mới
     */
    suspend fun createReminder(reminder: MedicationReminder): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("User not logged in"))
            val collection = getRemindersCollection()
                ?: return Result.failure(Exception("User not logged in"))

            val reminderWithUser = reminder.copy().apply {
                this.userId = userId
                this.createdAt = Timestamp.now()
                this.updatedAt = Timestamp.now()
            }

            val docRef = collection.add(reminderWithUser).await()
            Log.d(TAG, "Created reminder with ID: ${docRef.id}")

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating reminder", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật reminder
     */
    suspend fun updateReminder(reminder: MedicationReminder): Result<Unit> {
        return try {
            val collection = getRemindersCollection()
                ?: return Result.failure(Exception("User not logged in"))

            val updates = mapOf(
                "morning_time" to reminder.morningTime,
                "noon_time" to reminder.noonTime,
                "afternoon_time" to reminder.afternoonTime,
                "evening_time" to reminder.eveningTime,
                "selected_days" to reminder.selectedDays,
                "repeat_count" to reminder.repeatCount,
                "repeat_until_date" to reminder.repeatUntilDate,
                "is_detailed_reminder" to reminder.isDetailedReminder,
                "medicine_id" to reminder.medicineId,
                "medicine_name" to reminder.medicineName,
                "dosage" to reminder.dosage,
                "note" to reminder.note,
                "snooze_duration" to reminder.snoozeDuration,
                "is_active" to reminder.isActive,
                "timezone" to reminder.timezone,
                "updated_at" to Timestamp.now()
            )

            collection.document(reminder.reminderId).update(updates).await()
            Log.d(TAG, "Updated reminder: ${reminder.reminderId}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating reminder", e)
            Result.failure(e)
        }
    }

    /**
     * Xóa reminder
     */
    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        return try {
            val collection = getRemindersCollection()
                ?: return Result.failure(Exception("User not logged in"))

            collection.document(reminderId).delete().await()
            Log.d(TAG, "Deleted reminder: $reminderId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reminder", e)
            Result.failure(e)
        }
    }

    /**
     * Bật/tắt reminder
     */
    suspend fun toggleReminderActive(reminderId: String, isActive: Boolean): Result<Unit> {
        return try {
            val collection = getRemindersCollection()
                ?: return Result.failure(Exception("User not logged in"))

            collection.document(reminderId).update(
                mapOf(
                    "is_active" to isActive,
                    "updated_at" to Timestamp.now()
                )
            ).await()

            Log.d(TAG, "Toggled reminder $reminderId active: $isActive")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling reminder", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật FCM token cho user hiện tại
     */
    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            db.collection(USERS_COLLECTION).document(userId).update(
                mapOf("fcm_token" to token)
            ).await()

            Log.d(TAG, "Updated FCM token for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Lấy FCM token của user hiện tại
     */
    suspend fun getFcmToken(): Result<String?> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val doc = db.collection(USERS_COLLECTION).document(userId).get().await()
            val token = doc.getString("fcm_token")

            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Ghi log khi user tương tác với notification
     */
    suspend fun logReminderAction(
        reminderId: String,
        timeSlot: String,
        action: String, // "taken", "snoozed", "dismissed"
        snoozeCount: Int = 0
    ): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val log = ReminderLog(
                reminderId = reminderId,
                userId = userId,
                timeSlot = timeSlot,
                scheduledTime = Timestamp.now(),
                actionTaken = action,
                actionTime = Timestamp.now(),
                snoozeCount = snoozeCount
            )

            db.collection(REMINDER_LOGS_COLLECTION).add(log).await()
            Log.d(TAG, "Logged reminder action: $action for $reminderId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging reminder action", e)
            Result.failure(e)
        }
    }
}
