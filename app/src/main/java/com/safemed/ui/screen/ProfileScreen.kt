package com.safemed.ui.screen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.safemed.R
import coil.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.safemed.ui.component.MembershipBadge
import com.safemed.ui.component.MembershipTier
import com.safemed.ui.component.ProfileMenuItem
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.EmeraldGreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToUpdateProfile: () -> Unit = {},
    onNavigateToScanHistory: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToReminder: () -> Unit = {}, // Thêm navigation đến màn hình nhắc nhở
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val currentUser = Firebase.auth.currentUser
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val guestText = stringResource(R.string.profile_guest)

    // Reload profile when screen is resumed (e.g., after avatar update)
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // Handle navigation khi đăng xuất thành công
    LaunchedEffect(uiState.isLogoutSuccess) {
        if (uiState.isLogoutSuccess) {
            onLogout()
            viewModel.onNavigateHandled()
        }
    }

    // Hiển thị error message
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ===== Header Section với gradient background =====
            ProfileHeader(
                displayName = uiState.displayName.ifEmpty { currentUser?.displayName ?: guestText },
                email = uiState.email.ifEmpty { currentUser?.email ?: "" },
                // Prioritize Firestore avatar over Google avatar to show custom uploads
                // Only use Google avatar as fallback if Firestore has no avatar
                avatarUrl = if (uiState.avatarUrl.isNullOrEmpty()) {
                    currentUser?.photoUrl?.toString()
                } else {
                    uiState.avatarUrl
                },
                onNavigateBack = onNavigateBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== Menu Items Section =====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // Cập nhật thông tin cá nhân
                    ProfileMenuItem(
                        icon = Icons.Default.Refresh,
                        title = stringResource(R.string.profile_update_info),
                        onClick = onNavigateToUpdateProfile
                    )

                    // Lịch sử scan
                    ProfileMenuItem(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.profile_scan_history),
                        onClick = onNavigateToScanHistory
                    )

                    // Nhắc nhở uống thuốc
                    ProfileMenuItem(
                        icon = Icons.Default.Notifications,
                        title = "Nhắc nhở uống thuốc",
                        onClick = onNavigateToReminder
                    )

                    // Bảo mật nâng cao
                    ProfileMenuItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.profile_security),
                        onClick = onNavigateToSecurity
                    )

                    // Điều khoản và chính sách
                    ProfileMenuItem(
                        icon = Icons.Default.Description,
                        title = stringResource(R.string.profile_terms),
                        onClick = onNavigateToTerms
                    )

                    // Trung tâm chăm sóc
                    ProfileMenuItem(
                        icon = Icons.Default.HeadsetMic,
                        title = stringResource(R.string.profile_support),
                        onClick = onNavigateToSupport
                    )

                    // Đổi mật khẩu
                    ProfileMenuItem(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.profile_change_password),
                        onClick = onNavigateToChangePassword
                    )

                    // Cài đặt
                    ProfileMenuItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.profile_settings),
                        onClick = onNavigateToSettings,
                        showDivider = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Logout Button =====
            Button(
                onClick = viewModel::logout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF5350),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.profile_logout),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Profile Header với gradient background và avatar
 */
@Composable
private fun ProfileHeader(
    displayName: String,
    email: String,
    avatarUrl: String?,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(EmeraldGreen, EmeraldGreenDark)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // SafeMed Logo
                    Text(
                        text = "💚",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Column {
                        Text(
                            text = "SafeMed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dược phẩm an toàn",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Back to Home button
                TextButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.profile_go_home))
                }
            }

            // Avatar và thông tin user
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        !avatarUrl.isNullOrEmpty() && avatarUrl.startsWith("data:image") -> {
                            // Base64 image
                            val bitmap = remember(avatarUrl) {
                                try {
                                    val base64Data = avatarUrl.substringAfter("base64,")
                                    val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = displayName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                        !avatarUrl.isNullOrEmpty() -> {
                            // HTTP URL image
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Name
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Email
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Membership Badge
                MembershipBadge(tier = MembershipTier.BRONZE)
            }
        }
    }
}

