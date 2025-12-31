package com.safemed.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safemed.ui.theme.EmeraldGreen

data class FAQ(
    val question: String,
    val answer: String
)

private val faqList = listOf(
    FAQ(
        question = "Làm sao để quét mã vạch thuốc?",
        answer = "Trên màn hình chính, nhấn vào nút \"Quét\" ở thanh điều hướng dưới cùng. Hướng camera vào mã vạch trên hộp thuốc và giữ yên trong vài giây để ứng dụng nhận diện."
    ),
    FAQ(
        question = "Thông tin thuốc có chính xác không?",
        answer = "Thông tin thuốc trong SafeMed được lấy từ các nguồn đáng tin cậy như Cục Quản lý Dược Việt Nam. Tuy nhiên, bạn nên tham khảo ý kiến bác sĩ hoặc dược sĩ trước khi sử dụng thuốc."
    ),
    FAQ(
        question = "Làm sao để tìm nhà thuốc gần tôi?",
        answer = "Nhấn vào tab \"Nhà thuốc\" trên thanh điều hướng. Ứng dụng sẽ sử dụng vị trí của bạn để hiển thị các nhà thuốc gần nhất. Bạn cần cho phép ứng dụng truy cập vị trí."
    ),
    FAQ(
        question = "Tôi có thể xem lại lịch sử quét không?",
        answer = "Có. Vào mục \"Hồ sơ\" > \"Lịch sử scan\" để xem danh sách các loại thuốc bạn đã quét trước đó."
    ),
    FAQ(
        question = "Làm sao để đổi mật khẩu?",
        answer = "Vào \"Hồ sơ\" > \"Đổi mật khẩu\". Nhập mật khẩu hiện tại và mật khẩu mới, sau đó nhấn \"Lưu thay đổi\"."
    ),
    FAQ(
        question = "Ứng dụng có miễn phí không?",
        answer = "SafeMed hoàn toàn miễn phí với các tính năng cơ bản. Một số tính năng nâng cao có thể yêu cầu gói Premium trong tương lai."
    ),
    FAQ(
        question = "Làm sao để xóa tài khoản?",
        answer = "Vào \"Hồ sơ\" > \"Bảo mật nâng cao\" > \"Xóa tài khoản\". Lưu ý: Thao tác này không thể hoàn tác."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onNavigateBack: () -> Unit = {}
) {
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trung tâm hỗ trợ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Contact options
            item {
                Text(
                    text = "Liên hệ hỗ trợ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Phone,
                        title = "Hotline",
                        subtitle = "1900-xxxx",
                        onClick = { /* TODO: Open phone dialer */ }
                    )
                    ContactCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Email,
                        title = "Email",
                        subtitle = "support@safemed.vn",
                        onClick = { /* TODO: Open email client */ }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Chat,
                        title = "Chat trực tuyến",
                        subtitle = "8:00 - 22:00",
                        onClick = { /* TODO: Open chat */ }
                    )
                    ContactCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Send,
                        title = "Zalo OA",
                        subtitle = "@SafeMed",
                        onClick = { /* TODO: Open Zalo */ }
                    )
                }
            }

            // FAQ Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Câu hỏi thường gặp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            items(faqList.size) { index ->
                FAQItem(
                    faq = faqList[index],
                    isExpanded = expandedFaqIndex == index,
                    onClick = {
                        expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                    }
                )
            }

            // Feedback section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Góp ý & Phản hồi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            item {
                FeedbackCard()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ContactCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FAQItem(
    faq: FAQ,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) 
                EmeraldGreen.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = EmeraldGreen
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard() {
    var feedbackText by remember { mutableStateOf("") }
    var showThankYou by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showThankYou) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldGreen
                    )
                    Text(
                        text = "Cảm ơn bạn đã gửi góp ý!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = "Bạn có góp ý hoặc phản hồi gì cho chúng tôi không?",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nhập nội dung góp ý...") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        cursorColor = EmeraldGreen
                    )
                )

                Button(
                    onClick = {
                        // TODO: Submit feedback
                        showThankYou = true
                        feedbackText = ""
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = feedbackText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gửi góp ý")
                }
            }
        }
    }
}
