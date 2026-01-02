package com.safemed.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class đại diện cho collection "medicines" trong Firestore
 * Dữ liệu được cào từ Bộ Y tế - dùng để xác thực thuốc chính hãng
 * 
 * Các trường trong Firestore:
 * - sdk: Số đăng ký thuốc (VDxxxxxx - không có dấu gạch ngang)
 * - barcode: Mã vạch sản phẩm (EAN-13)
 * - ten_thuoc: Tên thuốc
 * - hoat_chat: Hoạt chất
 * - ham_luong: Hàm lượng
 * - dang_bao_che: Dạng bào chế
 * - quy_cach: Quy cách đóng gói
 * - han_sd_sdk: Hạn sử dụng số đăng ký
 * - nha_san_xuat: Nhà sản xuất
 * - nuoc_san_xuat: Nước sản xuất
 * - tuoi_tho: Tuổi thọ/Hạn sử dụng
 */
data class Medicine(
    @DocumentId
    val documentId: String = "",
    
    @get:PropertyName("sdk")
    @set:PropertyName("sdk")
    var sdk: String = "",                    // Số đăng ký thuốc (VDxxxxxx)
    
    @get:PropertyName("barcode")
    @set:PropertyName("barcode")
    var barcode: String = "",                // Mã vạch (EAN-13)
    
    @get:PropertyName("ten_thuoc")
    @set:PropertyName("ten_thuoc")
    var tenThuoc: String = "",               // Tên thuốc
    
    @get:PropertyName("hoat_chat")
    @set:PropertyName("hoat_chat")
    var hoatChat: String = "",               // Hoạt chất
    
    @get:PropertyName("ham_luong")
    @set:PropertyName("ham_luong")
    var hamLuong: String = "",               // Hàm lượng
    
    @get:PropertyName("dang_bao_che")
    @set:PropertyName("dang_bao_che")
    var dangBaoChe: String = "",             // Dạng bào chế
    
    @get:PropertyName("quy_cach")
    @set:PropertyName("quy_cach")
    var quyCach: String = "",                // Quy cách đóng gói
    
    @get:PropertyName("han_sd_sdk")
    @set:PropertyName("han_sd_sdk")
    var hanSdSdk: String = "",               // Hạn sử dụng số đăng ký
    
    @get:PropertyName("nha_san_xuat")
    @set:PropertyName("nha_san_xuat")
    var nhaSanXuat: String = "",             // Nhà sản xuất
    
    @get:PropertyName("nuoc_san_xuat")
    @set:PropertyName("nuoc_san_xuat")
    var nuocSanXuat: String = "",            // Nước sản xuất
    
    @get:PropertyName("tuoi_tho")
    @set:PropertyName("tuoi_tho")
    var tuoiTho: String = ""                 // Tuổi thọ/Hạn sử dụng sản phẩm
) {
    /**
     * Kiểm tra thuốc có hợp lệ (có thông tin đầy đủ)
     */
    fun isValid(): Boolean = tenThuoc.isNotBlank() && (sdk.isNotBlank() || barcode.isNotBlank())
    
    /**
     * Kiểm tra SDK còn hạn (đơn giản hóa - so sánh năm)
     */
    fun isSdkValid(): Boolean {
        if (hanSdSdk.isBlank()) return true // Không có thông tin thì coi như còn hạn
        return try {
            // Format: DD/MM/YYYY hoặc YYYY
            val year = if (hanSdSdk.contains("/")) {
                hanSdSdk.split("/").last().toIntOrNull() ?: 0
            } else {
                hanSdSdk.toIntOrNull() ?: 0
            }
            year >= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        } catch (e: Exception) {
            true
        }
    }
}
