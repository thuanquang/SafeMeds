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
     * LƯU Ý: Dữ liệu mẫu này chỉ dùng cho development/testing
     * Dữ liệu thực tế sẽ được import từ CSDL Bộ Y tế
     * 
     * Lưu ý: SDK trên Firestore lưu dạng VDxxxxxx (không có dấu -)
     */
    suspend fun seedMedicines(): Result<Unit> {
        val medicines = listOf(
            Medicine(
                sdk = "VD1234520",
                barcode = "8934868010012",
                tenThuoc = "Panadol Extra",
                hoatChat = "Paracetamol, Caffeine",
                hamLuong = "500mg, 65mg",
                dangBaoChe = "Viên nén",
                quyCach = "Hộp 12 vỉ x 10 viên",
                hanSdSdk = "31/12/2028",
                nhaSanXuat = "GSK Consumer Healthcare",
                nuocSanXuat = "Việt Nam",
                tuoiTho = "36 tháng"
            ),
            Medicine(
                sdk = "VD2345621",
                barcode = "8936067690015",
                tenThuoc = "Vitamin C 500mg",
                hoatChat = "Acid Ascorbic",
                hamLuong = "500mg",
                dangBaoChe = "Viên nén",
                quyCach = "Hộp 10 vỉ x 10 viên",
                hanSdSdk = "31/12/2027",
                nhaSanXuat = "DHG Pharma",
                nuocSanXuat = "Việt Nam",
                tuoiTho = "24 tháng"
            ),
            Medicine(
                sdk = "VN3456722",
                barcode = "8935049000123",
                tenThuoc = "Efferalgan 500mg",
                hoatChat = "Paracetamol",
                hamLuong = "500mg",
                dangBaoChe = "Viên sủi",
                quyCach = "Tuýp 10 viên",
                hanSdSdk = "31/12/2026",
                nhaSanXuat = "UPSA SAS",
                nuocSanXuat = "Pháp",
                tuoiTho = "36 tháng"
            ),
            Medicine(
                sdk = "VD4567823",
                barcode = "8936067690022",
                tenThuoc = "Hapacol 650mg",
                hoatChat = "Paracetamol",
                hamLuong = "650mg",
                dangBaoChe = "Viên nén",
                quyCach = "Hộp 10 vỉ x 10 viên",
                hanSdSdk = "31/12/2029",
                nhaSanXuat = "DHG Pharma",
                nuocSanXuat = "Việt Nam",
                tuoiTho = "36 tháng"
            )
        )

        return try {
            medicines.forEach { medicine ->
                // Sử dụng SDK hoặc barcode làm document ID
                val docId = medicine.sdk.ifBlank { medicine.barcode }.ifBlank { "unknown_${System.currentTimeMillis()}" }
                db.collection("medicines")
                    .document(docId)
                    .set(medicine)
                    .await()
                Log.d(TAG, "Added medicine: ${medicine.tenThuoc}")
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