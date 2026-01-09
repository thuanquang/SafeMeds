package com.safemed.ui.screen.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.safemed.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: UpdateProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val stitchDarkGreen = colorResource(id = R.color.stitch_dark_green)
    val stitchLime = colorResource(id = R.color.stitch_lime)
    val stitchBg = colorResource(id = R.color.stitch_bg)

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        }
    }

    // Check permission and open gallery
    fun openGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                imagePickerLauncher.launch("image/*")
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    // Handle success navigation
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.update_profile_success))
            viewModel.onNavigateHandled()
            onNavigateBack()
        }
    }

    // Show error
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = stitchBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ===== Header Section =====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = stitchDarkGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar (Back Button + Title)
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                         // Back Button Box
                         Box(
                             modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .clip(CircleShape)
                                .clickable { onNavigateBack() },
                             contentAlignment = Alignment.Center
                         ) {
                             Icon(
                                 imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                 contentDescription = stringResource(R.string.back),
                                 tint = Color.White
                             )
                         }
                         
                         // Title
                         Text(
                             text = stringResource(R.string.settings_title_caps),
                             style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Bold,
                             color = Color.White.copy(alpha = 0.9f),
                             letterSpacing = 1.sp,
                             modifier = Modifier.align(Alignment.Center)
                         )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Avatar Section
                    Box(contentAlignment = Alignment.Center) {
                         Box(
                            modifier = Modifier
                                .size(140.dp)
                                .border(3.dp, stitchLime, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)) // Fallback bg
                                .clickable { openGallery() },
                            contentAlignment = Alignment.Center
                         ) {
                            if (uiState.isUploadingAvatar) {
                                CircularProgressIndicator(color = stitchDarkGreen)
                            } else {
                                ProfileAvatarImage(
                                    avatarUrl = uiState.avatarUrl,
                                    selectedUri = uiState.selectedImageUri
                                )
                            }
                         }

                         // Camera Icon Badge
                         Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .size(36.dp)
                                .background(stitchLime, RoundedCornerShape(12.dp))
                                .border(2.dp, stitchDarkGreen, RoundedCornerShape(12.dp))
                                .clickable { openGallery() },
                            contentAlignment = Alignment.Center
                         ) {
                             Icon(
                                 imageVector = Icons.Default.CameraAlt,
                                 contentDescription = "Change photo",
                                 tint = stitchDarkGreen,
                                 modifier = Modifier.size(18.dp)
                             )
                         }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name Display in Header
                    Text(
                        text = if (uiState.fullName.isNotBlank()) uiState.fullName else "User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = stringResource(R.string.update_profile_subtitle_v2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ===== Form Content =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Card (Editable)
                ProfileInfoCard(
                    icon = Icons.Outlined.AccountBox,
                    label = stringResource(R.string.label_fullname_caps)
                ) {
                    BasicTextField(
                        value = uiState.fullName,
                        onValueChange = viewModel::onFullNameChange,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.stitch_text_primary)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Email Card (Read-only)
                ProfileInfoCard(
                    icon = Icons.Default.Email,
                    label = stringResource(R.string.label_email_caps)
                ) {
                    Text(
                        text = uiState.user?.email ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = viewModel::saveProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = stitchLime,
                        contentColor = stitchDarkGreen
                    ),
                    enabled = !uiState.isSaving && uiState.fullName.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                             if (uiState.isSaving) {
                                Text(
                                    text = stringResource(R.string.processing),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                             } else {
                                Text(
                                    text = stringResource(R.string.btn_save_changes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.msg_last_updated),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = stitchDarkGreen.copy(alpha = 0.7f)
                                )
                             }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(stitchDarkGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                             if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                             } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                             }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colorResource(R.color.stitch_bg), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorResource(R.color.stitch_dark_green),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        }
    }
}

@Composable
private fun ProfileAvatarImage(avatarUrl: String, selectedUri: Uri?) {
    val context = LocalContext.current
    if (selectedUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(selectedUri)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else if (avatarUrl.isNotEmpty()) {
        if (avatarUrl.startsWith("data:image")) {
             val bitmap = remember(avatarUrl) {
                try {
                    val base64Data = avatarUrl.substringAfter("base64,")
                    val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) { null }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                 Icon(Icons.Default.CameraAlt, null)
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.CameraAlt, // Placeholder
            contentDescription = null,
            tint = Color.Gray
        )
    }
}
