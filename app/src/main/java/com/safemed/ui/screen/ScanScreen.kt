package com.safemed.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.safemed.scanner.HybridMedicineAnalyzer
import com.safemed.scanner.ImageProcessor
import com.safemed.scanner.ProcessingResult
import com.safemed.scanner.ScanResult
import com.safemed.scanner.ScanType
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.SafeMedTheme
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private const val TAG = "ScanScreen"

// Colors for the scan screen
private val ScanScreenBackground = Color(0xFF000000)
private val CameraPlaceholder = Color(0xFF374151)
private val TextWhite = Color.White
private val TextGray = Color(0xFF9CA3AF)

/**
 * Trạng thái quét của màn hình
 */
private enum class ScanState {
    IDLE,               // Chờ camera tự động quét
    SCANNING,           // Đang xác thực mã (hiển thị loading)
    PROCESSING_IMAGE,   // Đang xử lý ảnh từ gallery
    COMPLETED           // Quét xong, chuyển màn hình
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToResult: (String) -> Unit = {} // Callback với mã quét được
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Permission state - sử dụng native ActivityResultContracts
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Launcher để yêu cầu quyền camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        Log.d(TAG, "Camera permission granted: $isGranted")
    }

    // Scan state
    var scanState by remember { mutableStateOf(ScanState.IDLE) }
    
    // Detected scan result
    var detectedResult by remember { mutableStateOf<ScanResult?>(null) }
    
    // Detected code text to display
    var detectedCodeText by remember { mutableStateOf<String?>(null) }
    
    // Flag để tạm dừng camera khi đang xử lý ảnh từ gallery
    var isCameraPaused by remember { mutableStateOf(false) }
    
    // ImageProcessor cho xử lý ảnh từ gallery (shared logic)
    val imageProcessor = remember { ImageProcessor() }
    
    // Launcher để chọn ảnh từ thư viện
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            Log.d(TAG, "Image selected from gallery: $selectedUri")
            
            // Tạm dừng camera và bắt đầu xử lý ảnh
            isCameraPaused = true
            scanState = ScanState.PROCESSING_IMAGE
            
            coroutineScope.launch {
                processGalleryImage(
                    context = context,
                    uri = selectedUri,
                    imageProcessor = imageProcessor,
                    onResult = { result ->
                        detectedResult = result
                        isCameraPaused = false
                    },
                    onError = { errorMessage ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        scanState = ScanState.IDLE
                        isCameraPaused = false
                    }
                )
            }
        }
    }
    
    // Cleanup ImageProcessor khi Composable bị dispose
    DisposableEffect(Unit) {
        onDispose {
            imageProcessor.close()
        }
    }

    // Xử lý khi phát hiện mã - chuyển sang trạng thái SCANNING và sau đó navigate
    LaunchedEffect(detectedResult) {
        detectedResult?.let { result ->
            if (scanState == ScanState.IDLE || scanState == ScanState.PROCESSING_IMAGE) {
                scanState = ScanState.SCANNING
                detectedCodeText = "${result.type.name}: ${result.code}"
                
                // Delay ngắn để hiển thị animation
                kotlinx.coroutines.delay(1500)
                
                scanState = ScanState.COMPLETED
                // Navigate với mã đã chuẩn hóa
                onNavigateToResult(result.normalizedCode)
            }
        }
    }

    Scaffold(
        topBar = {
            ScanScreenTopBar(onNavigateBack = onNavigateBack)
        },
        containerColor = ScanScreenBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ScanScreenBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacing
            Spacer(modifier = Modifier.height(40.dp))

            // Camera Preview Viewfinder with Scanner Overlay
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Kiểm tra quyền camera
                if (hasCameraPermission) {
                    // Đã có quyền - hiển thị camera preview với HybridScanner
                    CameraPreviewWithScanner(
                        modifier = Modifier.fillMaxSize(),
                        isPaused = isCameraPaused,
                        onScanResult = { result ->
                            detectedResult = result
                        }
                    )
                } else {
                    // Chưa có quyền - hiển thị placeholder và thông báo
                    CameraPermissionPlaceholder(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }

                // Scanner corner overlay (luôn hiển thị trên cùng)
                ScannerCornerOverlay(
                    modifier = Modifier.fillMaxSize()
                )

                // Hiển thị loading khi đang quét hoặc xử lý ảnh
                if (scanState == ScanState.SCANNING || scanState == ScanState.PROCESSING_IMAGE) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
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
                                text = if (scanState == ScanState.PROCESSING_IMAGE) 
                                    "Đang đọc ảnh..." else "Đang xác thực...",
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                            detectedCodeText?.let { code ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = code,
                                    color = EmeraldGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // Nút Upload ảnh
            Spacer(modifier = Modifier.height(16.dp))
            
            GalleryUploadButton(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = scanState == ScanState.IDLE
            )

            // Spacing below camera view
            Spacer(modifier = Modifier.height(32.dp))

            // Instruction Title
            Text(
                text = "Quét mã thuốc",
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Small spacing
            Spacer(modifier = Modifier.height(12.dp))

            // Instruction Body Text
            Text(
                text = "Đưa camera vào mã vạch hoặc số đăng ký (SĐK)\ntrên bao bì thuốc để xác thực tự động",
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            // Spacer to push status to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Status indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
                    .background(
                        color = when (scanState) {
                            ScanState.IDLE -> EmeraldGreen.copy(alpha = 0.2f)
                            ScanState.SCANNING, ScanState.PROCESSING_IMAGE -> EmeraldGreen.copy(alpha = 0.5f)
                            ScanState.COMPLETED -> EmeraldGreen
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (scanState) {
                        ScanState.IDLE -> "🔍 Đang tìm mã thuốc..."
                        ScanState.PROCESSING_IMAGE -> "📷 Đang xử lý ảnh..."
                        ScanState.SCANNING -> "⏳ Đang xác thực..."
                        ScanState.COMPLETED -> "✅ Hoàn tất!"
                    },
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom padding
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Camera Preview với HybridMedicineAnalyzer
 * Tích hợp Barcode Scanning + OCR Text Recognition
 * 
 * @param isPaused Tạm dừng camera khi đang xử lý ảnh từ gallery
 */
@Composable
private fun CameraPreviewWithScanner(
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
    onScanResult: (ScanResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Executor cho ImageAnalysis
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // Hybrid Analyzer
    val hybridAnalyzer = remember {
        HybridMedicineAnalyzer { result ->
            onScanResult(result)
        }
    }

    // Camera Provider state
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Khởi tạo CameraX
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "CameraProvider initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Cleanup khi Composable bị dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Disposing camera resources")
            cameraProvider?.unbindAll()
            hybridAnalyzer.close()
            cameraExecutor.shutdown()
        }
    }

    // AndroidView để hiển thị PreviewView
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            cameraProvider?.let { provider ->
                // Nếu đang pause, unbind tất cả để tiết kiệm tài nguyên
                if (isPaused) {
                    provider.unbindAll()
                    Log.d(TAG, "Camera paused for gallery processing")
                    return@let
                }
                
                try {
                    // Unbind tất cả use cases trước khi bind mới
                    provider.unbindAll()

                    // Preview use case
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    // ImageAnalysis với HybridMedicineAnalyzer
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor, hybridAnalyzer)
                        }

                    // Chọn camera sau (back camera)
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Bind to lifecycle
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    Log.d(TAG, "Camera with HybridScanner bound successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bind camera use cases", e)
                }
            }
        }
    )
}

/**
 * Placeholder hiển thị khi chưa có quyền camera
 */
@Composable
private fun CameraPermissionPlaceholder(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Ứng dụng cần quyền truy cập camera để quét mã thuốc",
                color = TextWhite,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen
                )
            ) {
                Text(
                    text = "Cấp quyền Camera",
                    color = TextWhite
                )
            }
        }
    }
}

/**
 * Nút tải ảnh từ thư viện
 */
@Composable
private fun GalleryUploadButton(
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldGreen.copy(alpha = 0.15f),
            contentColor = EmeraldGreen,
            disabledContainerColor = Color.Gray.copy(alpha = 0.1f),
            disabledContentColor = Color.Gray
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .height(48.dp)
            .border(
                width = 1.dp,
                color = if (enabled) EmeraldGreen.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = "Chọn ảnh từ thư viện",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Tải ảnh lên",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Xử lý ảnh từ thư viện
 * 
 * @param context Context để đọc file
 * @param uri URI của ảnh được chọn
 * @param imageProcessor ImageProcessor dùng chung
 * @param onResult Callback khi tìm thấy mã hợp lệ
 * @param onError Callback khi có lỗi hoặc không tìm thấy mã
 */
private suspend fun processGalleryImage(
    context: android.content.Context,
    uri: Uri,
    imageProcessor: ImageProcessor,
    onResult: (ScanResult) -> Unit,
    onError: (String) -> Unit
) {
    Log.d(TAG, "Processing gallery image: $uri")
    
    // Tạo InputImage từ URI
    val inputImage = imageProcessor.createInputImageFromUri(context, uri)
    
    if (inputImage == null) {
        onError("Không thể đọc ảnh. Vui lòng chọn ảnh khác.")
        return
    }
    
    // Xử lý ảnh với shared logic
    when (val result = imageProcessor.processImage(inputImage)) {
        is ProcessingResult.Success -> {
            Log.d(TAG, "Gallery image processed successfully: ${result.scanResult.code}")
            onResult(result.scanResult)
        }
        is ProcessingResult.NotFound -> {
            Log.d(TAG, "No valid code found in gallery image")
            onError("Không tìm thấy thông tin thuốc trong ảnh này.\nVui lòng chọn ảnh rõ nét hơn có chứa mã vạch hoặc số đăng ký (SĐK).")
        }
        is ProcessingResult.Error -> {
            Log.e(TAG, "Error processing gallery image: ${result.message}", result.exception)
            onError("Lỗi xử lý ảnh: ${result.message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanScreenTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "SafeMed",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Xác thực thuốc chính hãng",
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
}

@Composable
private fun ScannerCornerOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        val cornerLength = 40.dp.toPx()
        val cornerOffset = 24.dp.toPx() // Offset from edges to account for rounded corners
        val color = EmeraldGreen

        // Top-Left Corner (L-shape)
        // Horizontal line
        drawLine(
            color = color,
            start = Offset(cornerOffset, cornerOffset),
            end = Offset(cornerOffset + cornerLength, cornerOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Vertical line
        drawLine(
            color = color,
            start = Offset(cornerOffset, cornerOffset),
            end = Offset(cornerOffset, cornerOffset + cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Top-Right Corner (L-shape)
        // Horizontal line
        drawLine(
            color = color,
            start = Offset(size.width - cornerOffset - cornerLength, cornerOffset),
            end = Offset(size.width - cornerOffset, cornerOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Vertical line
        drawLine(
            color = color,
            start = Offset(size.width - cornerOffset, cornerOffset),
            end = Offset(size.width - cornerOffset, cornerOffset + cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-Left Corner (L-shape)
        // Horizontal line
        drawLine(
            color = color,
            start = Offset(cornerOffset, size.height - cornerOffset),
            end = Offset(cornerOffset + cornerLength, size.height - cornerOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Vertical line
        drawLine(
            color = color,
            start = Offset(cornerOffset, size.height - cornerOffset - cornerLength),
            end = Offset(cornerOffset, size.height - cornerOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-Right Corner (L-shape)
        // Horizontal line
        drawLine(
            color = color,
            start = Offset(size.width - cornerOffset - cornerLength, size.height - cornerOffset),
            end = Offset(size.width - cornerOffset, size.height - cornerOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Vertical line
        drawLine(
            color = color,
            start = Offset(size.width - cornerOffset, size.height - cornerOffset - cornerLength),
            end = Offset(size.width - cornerOffset, size.height - cornerOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@ComposePreview(showBackground = true, showSystemUi = true)
@Composable
private fun ScanScreenPreview() {
    SafeMedTheme {
        ScanScreen()
    }
}

