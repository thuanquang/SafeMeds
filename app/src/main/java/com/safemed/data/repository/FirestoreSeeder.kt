package com.safemed.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.safemed.data.model.Medicine
import com.safemed.data.model.Pharmacy
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeder class để thêm dữ liệu mẫu vào Firestore
 * Chỉ sử dụng trong quá trình development/testing
 */
@Singleton
class FirestoreSeeder @Inject constructor() {

    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val TAG = "FirestoreSeeder"
    }

    /**
     * Thêm dữ liệu mẫu cho tất cả collections
     */
    suspend fun seedAllData(): Result<Unit> {
        return try {
            seedPharmacies()
            seedMedicines()
            Log.d(TAG, "All sample data seeded successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed data", e)
            Result.failure(e)
        }
    }

    /**
     * Thêm dữ liệu mẫu nhà thuốc (khu vực TP.HCM)
     */
    suspend fun seedPharmacies(): Result<Unit> {
        val pharmacies = listOf(
            Pharmacy(
                pharmacyId = "pharmacy_001",
                name = "Nhà thuốc Long Châu - Nguyễn Văn Linh",
                address = "123 Nguyễn Văn Linh, Phường Tân Phong, Quận 7, TP.HCM",
                latitude = 10.7326,
                longitude = 106.7196,
                phone = "0901234567",
                isOpen = true
            ),
            Pharmacy(
                pharmacyId = "pharmacy_002",
                name = "Nhà thuốc An Khang - Lê Văn Việt",
                address = "456 Lê Văn Việt, Phường Hiệp Phú, TP. Thủ Đức, TP.HCM",
                latitude = 10.8412,
                longitude = 106.7820,
                phone = "0907654321",
                isOpen = true
            ),
            Pharmacy(
                pharmacyId = "pharmacy_003",
                name = "Nhà thuốc Pharmacity - Điện Biên Phủ",
                address = "789 Điện Biên Phủ, Phường 15, Quận Bình Thạnh, TP.HCM",
                latitude = 10.8012,
                longitude = 106.7105,
                phone = "0909876543",
                isOpen = false
            ),
            Pharmacy(
                pharmacyId = "pharmacy_004",
                name = "Nhà thuốc Medicare - UEH",
                address = "59C Nguyễn Đình Chiểu, Phường 6, Quận 3, TP.HCM",
                latitude = 10.7834,
                longitude = 106.6932,
                phone = "0281234567",
                isOpen = true
            ),
            Pharmacy(
                pharmacyId = "pharmacy_005",
                name = "Nhà thuốc Việt Pháp - Nguyễn Thị Minh Khai",
                address = "123 Nguyễn Thị Minh Khai, Phường Bến Thành, Quận 1, TP.HCM",
                latitude = 10.7721,
                longitude = 106.6922,
                phone = "0283456789",
                isOpen = true
            )
        )

        return try {
            pharmacies.forEach { pharmacy ->
                db.collection("pharmacies")
                    .document(pharmacy.pharmacyId)
                    .set(pharmacy)
                    .await()
                Log.d(TAG, "Added pharmacy: ${pharmacy.name}")
            }
            Log.d(TAG, "Seeded ${pharmacies.size} pharmacies")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed pharmacies", e)
            Result.failure(e)
        }
    }

    /**
     * Thêm dữ liệu mẫu thuốc (có barcode thật để test scanner)
     */
    suspend fun seedMedicines(): Result<Unit> {
        val medicines = listOf(
            Medicine(
                medicineId = "med_001",
                name = "Panadol Extra",
                brand = "GSK (GlaxoSmithKline)",
                barcode = "8934868010012",
                isAuthentic = true,
                imageUrl = "https://cdn.nhathuoclongchau.com.vn/unsafe/800x0/filters:quality(95)/https://cms-prod.s3-sgn09.fptcloud.com/DSC_09829_6a6298b3ae.jpg"
            ),
            Medicine(
                medicineId = "med_002",
                name = "Vitamin C 500mg",
                brand = "DHG Pharma",
                barcode = "8936067690015",
                isAuthentic = true,
                imageUrl = "https://cdn.nhathuoclongchau.com.vn/unsafe/800x0/filters:quality(95)/https://cms-prod.s3-sgn09.fptcloud.com/00030194_vitamin_c_500mg_dhg_100v_7622_6282_large_93dd6282fa.jpg"
            ),
            Medicine(
                medicineId = "med_003",
                name = "Efferalgan 500mg",
                brand = "Sanofi",
                barcode = "8935049000123",
                isAuthentic = true,
                imageUrl = "https://cdn.nhathuoclongchau.com.vn/unsafe/800x0/filters:quality(95)/https://cms-prod.s3-sgn09.fptcloud.com/00501236_efferalgan_500mg_upsa_16v_7198_6093_large_a39379bdb0.jpg"
            ),
            Medicine(
                medicineId = "med_004",
                name = "Hapacol 650mg",
                brand = "DHG Pharma",
                barcode = "8936067690022",
                isAuthentic = true,
                imageUrl = "https://cdn.nhathuoclongchau.com.vn/unsafe/800x0/filters:quality(95)/https://cms-prod.s3-sgn09.fptcloud.com/00030382_hapacol_650_dhg_10vi_x_10vien_1588_6157_large_e4c3c2caec.jpg"
            ),
            Medicine(
                medicineId = "med_005",
                name = "Thuốc giả - Panadol Fake",
                brand = "Unknown",
                barcode = "0000000000000",
                isAuthentic = false,
                imageUrl = ""
            ),
            Medicine(
                medicineId = "med_006",
                name = "Thuốc nghi ngờ - Test Product",
                brand = "Không rõ nguồn gốc",
                barcode = "1111111111111",
                isAuthentic = false,
                imageUrl = ""
            )
        )

        return try {
            medicines.forEach { medicine ->
                db.collection("medicines")
                    .document(medicine.medicineId)
                    .set(medicine)
                    .await()
                Log.d(TAG, "Added medicine: ${medicine.name}")
            }
            Log.d(TAG, "Seeded ${medicines.size} medicines")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed medicines", e)
            Result.failure(e)
        }
    }

    /**
     * Xóa tất cả dữ liệu trong một collection
     * CHỈ DÙNG TRONG DEVELOPMENT!
     */
    suspend fun clearCollection(collectionName: String): Result<Unit> {
        return try {
            val documents = db.collection(collectionName).get().await()
            documents.forEach { doc ->
                db.collection(collectionName).document(doc.id).delete().await()
            }
            Log.d(TAG, "Cleared collection: $collectionName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear collection: $collectionName", e)
            Result.failure(e)
        }
    }

    /**
     * Kiểm tra xem đã có dữ liệu trong Firestore chưa
     */
    suspend fun hasData(): Boolean {
        return try {
            val pharmacies = db.collection("pharmacies").limit(1).get().await()
            val medicines = db.collection("medicines").limit(1).get().await()
            !pharmacies.isEmpty || !medicines.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check data", e)
            false
        }
    }
}