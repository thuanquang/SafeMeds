package com.safemed.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.SafeMedTheme
import kotlinx.coroutines.delay
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
    IDLE,       // Chờ người dùng nhấn nút
    SCANNING,   // Đang quét (hiển thị loading)
    COMPLETED   // Quét xong, chuyển màn hình
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToResult: (String) -> Unit = {} // Callback với mã quét được
) {
    val context = LocalContext.current
    
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

    // Xử lý simulate scan - delay 3 giây rồi navigate
    LaunchedEffect(scanState) {
        if (scanState == ScanState.SCANNING) {
            delay(3000) // Đợi 3 giây
            scanState = ScanState.COMPLETED
            // Navigate với mã giả lập
            onNavigateToResult("SAFEMED-DEMO-12345")
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
                    // Đã có quyền - hiển thị camera preview
                    CameraPreviewView(
                        modifier = Modifier.fillMaxSize()
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

                // Hiển thị loading khi đang quét
                if (scanState == ScanState.SCANNING) {
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
                                text = "Đang xác thực...",
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

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
                text = "Đặt camera phía trên mã vạch hoặc QR code\ntrên bao bì thuốc để xác thực",
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            // Spacer to push button to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Action Button
            Button(
                onClick = {
                    if (scanState == ScanState.IDLE) {
                        scanState = ScanState.SCANNING
                    }
                },
                enabled = scanState == ScanState.IDLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    contentColor = TextWhite,
                    disabledContainerColor = EmeraldGreen.copy(alpha = 0.5f),
                    disabledContentColor = TextWhite.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (scanState == ScanState.SCANNING) "Đang quét..." else "Bắt đầu quét",
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
 * Camera Preview sử dụng CameraX
 * Được tách riêng để quản lý lifecycle tốt hơn
 */
@Composable
private fun CameraPreviewView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Executor cho ImageAnalysis
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

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
                try {
                    // Unbind tất cả use cases trước khi bind mới
                    provider.unbindAll()

                    // Preview use case
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    // ImageAnalysis use case (khung sườn cho ML Kit)
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                // TODO: Tích hợp ML Kit Barcode Scanning ở đây
                                // Khung sườn cho việc xử lý barcode:
                                // val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                // val mediaImage = imageProxy.image
                                // if (mediaImage != null) {
                                //     val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                                //     barcodeScanner.process(inputImage)
                                //         .addOnSuccessListener { barcodes ->
                                //             for (barcode in barcodes) {
                                //                 // Xử lý barcode được quét
                                //             }
                                //         }
                                //         .addOnCompleteListener {
                                //             imageProxy.close()
                                //         }
                                // }
                                
                                // Tạm thời chỉ close imageProxy
                                imageProxy.close()
                            }
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

                    Log.d(TAG, "Camera use cases bound successfully")
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

