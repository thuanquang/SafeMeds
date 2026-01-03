package com.safemed.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safemed.R
import com.safemed.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terms_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== Điều khoản sử dụng =====
            TermsSection(
                title = "1. Điều khoản sử dụng",
                content = """
                    Bằng việc sử dụng ứng dụng SafeMed, bạn đồng ý tuân thủ các điều khoản và điều kiện sau đây.
                    
                    SafeMed là ứng dụng hỗ trợ tra cứu thông tin thuốc và tìm kiếm nhà thuốc. Thông tin trong ứng dụng chỉ mang tính chất tham khảo, không thay thế cho tư vấn y tế chuyên nghiệp.
                    
                    Người dùng cần tuân thủ pháp luật Việt Nam và không sử dụng ứng dụng cho mục đích bất hợp pháp.
                """.trimIndent()
            )

            // ===== Chính sách bảo mật =====
            TermsSection(
                title = "2. Chính sách bảo mật",
                content = """
                    SafeMed cam kết bảo vệ quyền riêng tư và thông tin cá nhân của người dùng.
                    
                    Dữ liệu thu thập:
                    • Thông tin tài khoản (email, tên)
                    • Lịch sử quét thuốc
                    • Vị trí (khi tìm nhà thuốc gần)
                    
                    Mục đích sử dụng:
                    • Cung cấp dịch vụ tra cứu thuốc
                    • Cải thiện trải nghiệm người dùng
                    • Gửi thông báo quan trọng
                    
                    Chúng tôi không chia sẻ thông tin cá nhân với bên thứ ba mà không có sự đồng ý của bạn.
                """.trimIndent()
            )

            // ===== Quyền sở hữu trí tuệ =====
            TermsSection(
                title = "3. Quyền sở hữu trí tuệ",
                content = """
                    Tất cả nội dung trong ứng dụng SafeMed bao gồm nhưng không giới hạn: thiết kế, logo, văn bản, hình ảnh, và mã nguồn đều thuộc quyền sở hữu của SafeMed.
                    
                    Bạn không được sao chép, phân phối, hoặc sử dụng bất kỳ nội dung nào mà không có sự cho phép bằng văn bản.
                """.trimIndent()
            )

            // ===== Giới hạn trách nhiệm =====
            TermsSection(
                title = "4. Giới hạn trách nhiệm",
                content = """
                    SafeMed cung cấp thông tin thuốc từ các nguồn đáng tin cậy nhưng không đảm bảo tính chính xác 100%.
                    
                    Người dùng cần tham khảo ý kiến bác sĩ hoặc dược sĩ trước khi sử dụng bất kỳ loại thuốc nào.
                    
                    SafeMed không chịu trách nhiệm cho bất kỳ thiệt hại nào phát sinh từ việc sử dụng thông tin trong ứng dụng.
                """.trimIndent()
            )

            // ===== Thay đổi điều khoản =====
            TermsSection(
                title = "5. Thay đổi điều khoản",
                content = """
                    SafeMed có quyền thay đổi các điều khoản này bất kỳ lúc nào. Người dùng sẽ được thông báo về các thay đổi quan trọng.
                    
                    Việc tiếp tục sử dụng ứng dụng sau khi điều khoản thay đổi đồng nghĩa với việc bạn chấp nhận các điều khoản mới.
                """.trimIndent()
            )

            // ===== Liên hệ =====
            TermsSection(
                title = "6. Liên hệ",
                content = """
                    Nếu bạn có bất kỳ câu hỏi nào về điều khoản và chính sách, vui lòng liên hệ:
                    
                    Email: support@safemed.vn
                    Hotline: 1900-xxxx-xx
                    Địa chỉ: 59C Nguyễn Đình Chiểu, P.6, Q.3, TP.HCM
                """.trimIndent()
            )

            // Footer
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Cập nhật lần cuối: 01/01/2025",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "© 2025 SafeMed. Đã đăng ký bản quyền.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TermsSection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreen
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
            )
        }
    }
}
