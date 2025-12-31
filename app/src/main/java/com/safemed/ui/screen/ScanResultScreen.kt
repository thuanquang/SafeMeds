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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.SafeMedTheme

// Colors
private val SuccessGreen = Color(0xFF10B981)
private val WarningRed = Color(0xFFEF4444)
private val BackgroundLight = Color(0xFFF9FAFB)
private val CardBackground = Color.White
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)

/**
 * Kết quả xác thực thuốc
 */
data class MedicineVerificationResult(
    val isAuthentic: Boolean,
    val scannedCode: String,
    val medicineName: String,
    val manufacturer: String,
    val batchNumber: String,
    val expiryDate: String,
    val registrationNumber: String,
    val verificationTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scannedCode: String,
    onNavigateBack: () -> Unit = {},
    onScanAgain: () -> Unit = {},
    onGoHome: () -> Unit = {}
) {
    // Demo result - trong thực tế sẽ fetch từ API dựa trên scannedCode
    val result = getDemoResult(scannedCode)

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
                            text = "SafeMed",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Verification Status Card
            VerificationStatusCard(isAuthentic = result.isAuthentic)

            Spacer(modifier = Modifier.height(24.dp))

            // Medicine Details Card
            MedicineDetailsCard(result = result)

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            ActionButtons(
                onScanAgain = onScanAgain,
                onGoHome = onGoHome
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VerificationStatusCard(isAuthentic: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAuthentic) SuccessGreen.copy(alpha = 0.1f) else WarningRed.copy(alpha = 0.1f)
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
                    .background(
                        color = if (isAuthentic) SuccessGreen else WarningRed,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAuthentic) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Text
            Text(
                text = if (isAuthentic) "THUỐC CHÍNH HÃNG" else "CẢNH BÁO",
                color = if (isAuthentic) SuccessGreen else WarningRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isAuthentic) {
                    "Sản phẩm đã được xác thực thành công.\nĐây là thuốc chính hãng."
                } else {
                    "Không thể xác thực sản phẩm.\nVui lòng liên hệ nhà sản xuất."
                },
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun MedicineDetailsCard(result: MedicineVerificationResult) {
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

            DetailRow(label = "Tên thuốc", value = result.medicineName)
            DetailRow(label = "Nhà sản xuất", value = result.manufacturer)
            DetailRow(label = "Số lô", value = result.batchNumber)
            DetailRow(label = "Hạn sử dụng", value = result.expiryDate)
            DetailRow(label = "Số đăng ký", value = result.registrationNumber)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(12.dp))

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
                    text = result.scannedCode,
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
                    text = result.verificationTime,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
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
 * Demo data - trong thực tế sẽ fetch từ backend
 */
private fun getDemoResult(scannedCode: String): MedicineVerificationResult {
    return MedicineVerificationResult(
        isAuthentic = true,
        scannedCode = scannedCode,
        medicineName = "Paracetamol 500mg",
        manufacturer = "Công ty Dược phẩm ABC",
        batchNumber = "LOT2024001",
        expiryDate = "31/12/2026",
        registrationNumber = "VD-12345-20",
        verificationTime = "31/12/2025 14:30"
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ScanResultScreenPreview() {
    SafeMedTheme {
        ScanResultScreen(
            scannedCode = "SAFEMED-DEMO-12345"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerificationStatusCardAuthenticPreview() {
    SafeMedTheme {
        VerificationStatusCard(isAuthentic = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun VerificationStatusCardWarningPreview() {
    SafeMedTheme {
        VerificationStatusCard(isAuthentic = false)
    }
}
