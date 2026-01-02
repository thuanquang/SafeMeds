package com.safemed.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.data.model.ScanHistory
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.SafeMedTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Colors
private val BackgroundLight = Color(0xFFF9FAFB)
private val CardBackground = Color.White
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)
private val SuccessGreen = Color(0xFF10B981)
private val WarningRed = Color(0xFFEF4444)

/**
 * Màn hình Lịch sử quét thuốc
 * Hiển thị danh sách các lần quét thuốc thành công
 * 
 * @param onNavigateBack Callback quay lại màn hình trước
 * @param onNavigateToResult Callback điều hướng tới màn hình kết quả với mã đã quét
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToResult: (String) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Xử lý trạng thái xóa
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                snackbarHostState.showSnackbar("Đã xóa thành công")
                viewModel.resetDeleteState()
            }
            is DeleteState.Error -> {
                snackbarHostState.showSnackbar((deleteState as DeleteState.Error).message)
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    // Dialog xác nhận xóa tất cả
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Xóa tất cả lịch sử?") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ lịch sử quét? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Xóa tất cả", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lịch sử quét",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Danh sách thuốc đã xác thực",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    // Nút xóa tất cả (chỉ hiển thị khi có dữ liệu)
                    if (uiState is HistoryUiState.Success) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Xóa tất cả",
                                tint = Color.Red.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is HistoryUiState.Loading -> {
                    LoadingHistoryContent()
                }
                is HistoryUiState.Empty -> {
                    EmptyHistoryContent()
                }
                is HistoryUiState.Success -> {
                    HistoryListContent(
                        historyList = state.historyList,
                        onItemClick = { history ->
                            // Navigate tới ScanResultScreen với scannedCode
                            onNavigateToResult(history.scannedCode)
                        },
                        onDeleteClick = { history ->
                            viewModel.deleteHistory(history.historyId)
                        }
                    )
                }
                is HistoryUiState.Error -> {
                    ErrorHistoryContent(
                        message = state.message,
                        onRetry = { viewModel.loadHistory() }
                    )
                }
            }

            // Loading overlay khi đang xóa
            if (deleteState is DeleteState.Deleting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldGreen)
                }
            }
        }
    }
}

/**
 * Loading Content
 */
@Composable
private fun LoadingHistoryContent() {
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
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Đang tải lịch sử...",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Empty State Content
 */
@Composable
private fun EmptyHistoryContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(color = EmeraldGreen.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Chưa có lịch sử quét",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bắt đầu quét thuốc để xác thực và lưu lại lịch sử.\nCác lần quét thành công sẽ hiển thị ở đây.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Error Content
 */
@Composable
private fun ErrorHistoryContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Không thể tải lịch sử",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onRetry) {
                Text("Thử lại", color = EmeraldGreen)
            }
        }
    }
}

/**
 * History List Content
 */
@Composable
private fun HistoryListContent(
    historyList: List<ScanHistory>,
    onItemClick: (ScanHistory) -> Unit,
    onDeleteClick: (ScanHistory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header info
        item {
            val authenticCount = historyList.count { it.result == "authentic" }
            val notFoundCount = historyList.count { it.result == "not_found" }
            Text(
                text = "${historyList.size} lần quét (${authenticCount} xác thực, ${notFoundCount} không tìm thấy)",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(
            items = historyList,
            key = { it.historyId }
        ) { history ->
            HistoryItemCard(
                history = history,
                onClick = { onItemClick(history) },
                onDeleteClick = { onDeleteClick(history) }
            )
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * History Item Card
 */
@Composable
private fun HistoryItemCard(
    history: ScanHistory,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Dialog xác nhận xóa item
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa khỏi lịch sử?") },
            text = { Text("Bạn có chắc chắn muốn xóa \"${history.medicineName}\" khỏi lịch sử?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Xóa", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val isAuthentic = history.result == "authentic"
        val statusColor = if (isAuthentic) SuccessGreen else WarningRed
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Medicine Name
                Text(
                    text = history.medicineName.ifBlank { "Không có tên" },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Scanned Code
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = history.scannedCode,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Timestamp
                Text(
                    text = formatTimestamp(history.getTimestampMillis()),
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            // Status icon & delete button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isAuthentic) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = if (isAuthentic) "Đã xác thực" else "Không tìm thấy",
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp to readable string
 * Format: dd/MM/yyyy HH:mm
 */
private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Không xác định"
    }
}

/**
 * Preview - Empty State
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistoryScreenEmptyPreview() {
    SafeMedTheme {
        EmptyHistoryContent()
    }
}

/**
 * Preview - Loading State
 */
@Preview(showBackground = true)
@Composable
private fun HistoryScreenLoadingPreview() {
    SafeMedTheme {
        LoadingHistoryContent()
    }
}
