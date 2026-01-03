package com.safemed.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.safemed.data.model.Pharmacy
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching pharmacy data from Firestore
 * Provides pharmacy list for the nearby pharmacies map feature
 */
@Singleton
class PharmacyRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_PHARMACIES = "pharmacies"
    }

    /**
     * Fetch all pharmacies from Firestore
     * @return Result containing list of pharmacies or error
     */
    suspend fun getAllPharmacies(): Result<List<Pharmacy>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_PHARMACIES)
                .get()
                .await()
            
            val pharmacies = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Pharmacy::class.java)?.copy(
                    pharmacyId = doc.id
                )
            }
            
            Result.success(pharmacies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch pharmacy by ID
     * @param pharmacyId The pharmacy document ID
     * @return Result containing pharmacy or error
     */
    suspend fun getPharmacyById(pharmacyId: String): Result<Pharmacy?> {
        return try {
            val doc = firestore.collection(COLLECTION_PHARMACIES)
                .document(pharmacyId)
                .get()
                .await()
            
            val pharmacy = doc.toObject(Pharmacy::class.java)?.copy(
                pharmacyId = doc.id
            )
            
            Result.success(pharmacy)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
