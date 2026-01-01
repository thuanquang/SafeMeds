package com.safemed.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.data.model.Medicine
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.SafeMedTheme

// Colors
private val SuccessGreen = Color(0xFF10B981)      // Emerald - Thuốc chính hãng
private val WarningRed = Color(0xFFEF4444)        // Red - Không tìm thấy
private val WarningOrange = Color(0xFFF59E0B)     // Orange - Cảnh báo SDK hết hạn
private val InfoBlue = Color(0xFF3B82F6)          // Blue - Thông tin
private val BackgroundLight = Color(0xFFF9FAFB)
private val CardBackground = Color.White
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scannedCode: String,
    onNavigateBack: () -> Unit = {},
    onScanAgain: () -> Unit = {},
    onGoHome: () -> Unit = {},
    viewModel: MedicineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kết quả xác thực",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "SafeMed - Dữ liệu Bộ Y tế",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState.lookupState) {
                is MedicineLookupState.Idle,
                is MedicineLookupState.Loading -> {
                    // Loading State
                    LoadingContent()
                }

                is MedicineLookupState.Success -> {
                    // Found medicine - Authentic
                    SuccessContent(
                        medicine = state.medicine,
                        scannedCode = uiState.scannedCode,
                        verificationTime = state.verificationTime,
                        onScanAgain = onScanAgain,
                        onGoHome = onGoHome
                    )
                }

                is MedicineLookupState.NotFound -> {
                    // Medicine not found
                    NotFoundContent(
                        scannedCode = state.scannedCode,
                        verificationTime = state.verificationTime,
                        onScanAgain = onScanAgain,
                        onGoHome = onGoHome
                    )
                }

                is MedicineLookupState.Error -> {
                    // Error state
                    ErrorContent(
                        message = state.message,
                        scannedCode = state.scannedCode,
                        onRetry = { viewModel.retry() },
                        onScanAgain = onScanAgain,
                        onGoHome = onGoHome
                    )
                }
            }
        }
    }
}

/**
 * Loading Content - Đang xác thực dữ liệu quốc gia
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = EmeraldGreen,
                modifier = Modifier.size(64.dp),
                strokeWidth = 5.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Đang xác thực dữ liệu quốc gia...",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Đối chiếu với CSDL Bộ Y tế",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Success Content - Thuốc chính hãng
 */
@Composable
private fun SuccessContent(
    medicine: Medicine,
    scannedCode: String,
    verificationTime: String,
    onScanAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Verification Status Card - Chính hãng
        AuthenticVerificationCard(isSdkValid = medicine.isSdkValid())

        Spacer(modifier = Modifier.height(24.dp))

        // Medicine Details Card
        MedicineDetailsCard(
            medicine = medicine,
            scannedCode = scannedCode,
            verificationTime = verificationTime
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        ActionButtons(
            onScanAgain = onScanAgain,
            onGoHome = onGoHome
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Not Found Content - Không tìm thấy thuốc
 */
@Composable
private fun NotFoundContent(
    scannedCode: String,
    verificationTime: String,
    onScanAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Not Found Card
        NotFoundVerificationCard()

        Spacer(modifier = Modifier.height(24.dp))

        // Scanned Code Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Thông tin quét",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(16.dp))

                DetailRow(label = "Mã quét được", value = scannedCode)
                DetailRow(label = "Thời gian xác thực", value = verificationTime)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "💡 Gợi ý: Hãy kiểm tra lại mã quét hoặc liên hệ nhà sản xuất để xác minh.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        ActionButtons(
            onScanAgain = onScanAgain,
            onGoHome = onGoHome
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Error Content - Lỗi khi tra cứu
 */
@Composable
private fun ErrorContent(
    message: String,
    scannedCode: String,
    onRetry: () -> Unit,
    onScanAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = WarningOrange.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color = WarningOrange, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "LỖI KẾT NỐI",
                    color = WarningOrange,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                DetailRow(label = "Mã quét được", value = scannedCode)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Retry Button
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WarningOrange,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thử lại",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        ActionButtons(
            onScanAgain = onScanAgain,
            onGoHome = onGoHome
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Card hiển thị trạng thái Thuốc chính hãng
 */
@Composable
private fun AuthenticVerificationCard(isSdkValid: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SuccessGreen.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(color = SuccessGreen, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Text
            Text(
                text = "THUỐC CHÍNH HÃNG",
                color = SuccessGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Badge - Dữ liệu từ Bộ Y tế
            Box(
                modifier = Modifier
                    .background(
                        color = SuccessGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dữ liệu từ Bộ Y tế",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sản phẩm đã được xác thực thành công.\nĐây là thuốc đã đăng ký với Cục Quản lý Dược.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Cảnh báo nếu SDK sắp hết hạn
            if (!isSdkValid) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = WarningOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Số đăng ký có thể đã hết hạn. Kiểm tra thông tin bên dưới.",
                            color = WarningOrange,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card hiển thị trạng thái Không tìm thấy thuốc
 */
@Composable
private fun NotFoundVerificationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningRed.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(color = WarningRed, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Text
            Text(
                text = "KHÔNG TÌM THẤY DỮ LIỆU",
                color = WarningRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mã thuốc không có trong cơ sở dữ liệu\nCục Quản lý Dược - Bộ Y tế.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Warning box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = WarningRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠️ Hãy cẩn thận khi sử dụng sản phẩm này. Liên hệ nhà sản xuất hoặc cơ quan y tế để xác minh.",
                    color = WarningRed,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Card hiển thị thông tin chi tiết thuốc từ Firestore
 */
@Composable
private fun MedicineDetailsCard(
    medicine: Medicine,
    scannedCode: String,
    verificationTime: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Thông tin sản phẩm",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(16.dp))

            // Thông tin cơ bản
            DetailRow(label = "Tên thuốc", value = medicine.tenThuoc.ifBlank { "Không có thông tin" })
            DetailRow(label = "Hoạt chất", value = medicine.hoatChat.ifBlank { "Không có thông tin" })
            DetailRow(label = "Hàm lượng", value = medicine.hamLuong.ifBlank { "Không có thông tin" })
            DetailRow(label = "Dạng bào chế", value = medicine.dangBaoChe.ifBlank { "Không có thông tin" })
            DetailRow(label = "Quy cách đóng gói", value = medicine.quyCach.ifBlank { "Không có thông tin" })
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(8.dp))
            
            // Thông tin nhà sản xuất
            DetailRow(label = "Nhà sản xuất", value = medicine.nhaSanXuat.ifBlank { "Không có thông tin" })
            DetailRow(label = "Nước sản xuất", value = medicine.nuocSanXuat.ifBlank { "Không có thông tin" })
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(8.dp))
            
            // Thông tin đăng ký
            if (medicine.sdk.isNotBlank()) {
                DetailRow(label = "Số đăng ký (SĐK)", value = medicine.sdk)
            }
            if (medicine.barcode.isNotBlank()) {
                DetailRow(label = "Mã vạch", value = medicine.barcode)
            }
            DetailRow(
                label = "Hạn SĐK", 
                value = medicine.hanSdSdk.ifBlank { "Không có thông tin" },
                valueColor = if (!medicine.isSdkValid()) WarningOrange else TextPrimary
            )
            if (medicine.tuoiTho.isNotBlank()) {
                // Thêm "tháng" nếu giá trị chỉ là số, hoặc giữ nguyên nếu đã có đơn vị
                val tuoiThoDisplay = if (medicine.tuoiTho.all { it.isDigit() }) {
                    "${medicine.tuoiTho} tháng"
                } else {
                    medicine.tuoiTho
                }
                DetailRow(label = "Tuổi thọ", value = tuoiThoDisplay)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(12.dp))

            // Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mã quét",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = scannedCode,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Thời gian xác thực",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = verificationTime,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String, 
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun ActionButtons(
    onScanAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary Button - Quét lại
        Button(
            onClick = onScanAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldGreen,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Quét thuốc khác",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Secondary Button - Về trang chủ
        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EmeraldGreen
            )
        ) {
            Text(
                text = "Về trang chủ",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Preview - Loading state
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ScanResultScreenLoadingPreview() {
    SafeMedTheme {
        LoadingContent()
    }
}

/**
 * Preview - Authentic verification card
 */
@Preview(showBackground = true)
@Composable
private fun AuthenticVerificationCardPreview() {
    SafeMedTheme {
        AuthenticVerificationCard(isSdkValid = true)
    }
}

/**
 * Preview - Not found verification card
 */
@Preview(showBackground = true)
@Composable
private fun NotFoundVerificationCardPreview() {
    SafeMedTheme {
        NotFoundVerificationCard()
    }
}
