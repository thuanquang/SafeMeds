package com.safemed.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safemed.data.repository.FirestoreSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebugUiState(
    val isLoading: Boolean = false,
    val message: String = "",
    val isError: Boolean = false,
    val hasData: Boolean? = null
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val seeder: FirestoreSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    init {
        checkData()
    }

    fun checkData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val hasData = seeder.hasData()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasData = hasData,
                message = if (hasData) "Firestore đã có dữ liệu" else "Firestore chưa có dữ liệu"
            )
        }
    }

    fun seedAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "Đang thêm dữ liệu...")
            seeder.seedAllData()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "✅ Đã thêm dữ liệu mẫu thành công!",
                        isError = false,
                        hasData = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "❌ Lỗi: ${e.message}",
                        isError = true
                    )
                }
        }
    }

    fun seedPharmacies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "Đang thêm nhà thuốc...")
            seeder.seedPharmacies()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "✅ Đã thêm 5 nhà thuốc mẫu!",
                        isError = false
                    )
                    checkData()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "❌ Lỗi: ${e.message}",
                        isError = true
                    )
                }
        }
    }

    fun seedMedicines() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "Đang thêm thuốc...")
            seeder.seedMedicines()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "✅ Đã thêm 6 loại thuốc mẫu!",
                        isError = false
                    )
                    checkData()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "❌ Lỗi: ${e.message}",
                        isError = true
                    )
                }
        }
    }

    fun clearPharmacies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            seeder.clearCollection("pharmacies")
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "🗑️ Đã xóa tất cả nhà thuốc",
                        isError = false
                    )
                    checkData()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "❌ Lỗi: ${e.message}",
                        isError = true
                    )
                }
        }
    }

    fun clearMedicines() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            seeder.clearCollection("medicines")
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "🗑️ Đã xóa tất cả thuốc",
                        isError = false
                    )
                    checkData()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "❌ Lỗi: ${e.message}",
                        isError = true
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛠️ Debug Tools") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        uiState.isError -> MaterialTheme.colorScheme.errorContainer
                        uiState.hasData == true -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Trạng thái Firestore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (uiState.isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Đang xử lý...")
                        }
                    } else {
                        Text(
                            text = uiState.message.ifEmpty { "Chưa kiểm tra" },
                            color = if (uiState.isError) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.checkData() },
                        enabled = !uiState.isLoading
                    ) {
                        Text("🔄 Kiểm tra lại")
                    }
                }
            }

            // Seed Data Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📥 Thêm dữ liệu mẫu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { viewModel.seedAllData() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("🚀 Thêm TẤT CẢ dữ liệu mẫu")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.seedPharmacies() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading
                        ) {
                            Text("🏥 Nhà thuốc")
                        }
                        OutlinedButton(
                            onClick = { viewModel.seedMedicines() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading
                        ) {
                            Text("💊 Thuốc")
                        }
                    }
                }
            }

            // Clear Data Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🗑️ Xóa dữ liệu (Cẩn thận!)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearPharmacies() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Xóa Nhà thuốc")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearMedicines() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Xóa Thuốc")
                        }
                    }
                }
            }

            // Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ℹ️ Thông tin Collections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("• pharmacies: 5 nhà thuốc TP.HCM")
                    Text("• medicines: 6 loại thuốc (4 thật, 2 giả)")
                    Text("• users: Tự động tạo khi đăng ký")
                    Text("• scan_history: Tạo khi quét thuốc")
                }
            }
        }
    }
}