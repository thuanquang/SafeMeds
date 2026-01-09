package com.safemed.ui.screen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.safemed.R

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
    onNavigateToReminder: () -> Unit = {}, 
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val currentUser = Firebase.auth.currentUser
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    
    val guestText = stringResource(R.string.profile_guest)
    val stitchBg = colorResource(id = R.color.stitch_bg)

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(uiState.isLogoutSuccess) {
        if (uiState.isLogoutSuccess) {
            onLogout()
            viewModel.onNavigateHandled()
        }
    }

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
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.back),
                        tint = colorResource(id = R.color.stitch_text_primary)
                    )
                }
            }
            
            // Header Card
            ProfileHeaderCard(
                displayName = uiState.displayName.ifEmpty { currentUser?.displayName ?: guestText },
                email = uiState.email.ifEmpty { currentUser?.email ?: "" },
                avatarUrl = if (uiState.avatarUrl.isNullOrEmpty()) {
                    currentUser?.photoUrl?.toString()
                } else {
                    uiState.avatarUrl
                }
            )

            // Grid Row 1 (Personal Info & Scan History)
            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Personal Info
                DashboardCard(
                    onClick = onNavigateToUpdateProfile,
                    modifier = Modifier.weight(1f),
                    color = Color.White
                ) {
                   DashboardCardContent(
                       icon = Icons.Default.Edit,
                       iconBg = colorResource(id = R.color.stitch_lime),
                       iconTint = colorResource(id = R.color.stitch_text_primary),
                       title = stringResource(R.string.profile_personal_info),
                       subtitle = stringResource(R.string.profile_edit_label),
                       showArrow = true
                   )
                }
                
                // Scan History
                DashboardCard(
                    onClick = onNavigateToScanHistory,
                    modifier = Modifier.weight(1f),
                    color = colorResource(id = R.color.stitch_find_bg).copy(alpha = 0.5f)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background decoration
                        Icon(
                             imageVector = Icons.Default.History,
                             contentDescription = null,
                             modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(x=20.dp, y=20.dp),
                             tint = Color.Black.copy(alpha = 0.05f)
                        )
                        DashboardCardContent(
                            icon = Icons.Default.History,
                            iconBg = Color.White,
                            iconTint = colorResource(id = R.color.stitch_text_primary),
                            title = stringResource(R.string.profile_scan_history),
                            subtitle = stringResource(R.string.profile_view_log),
                            showArrow = false
                        )
                    }
                }
            }

            // Grid Row 2 (Reminders, Security, Settings)
            Row(
                 modifier = Modifier.fillMaxWidth().height(120.dp),
                 horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reminders
                SmallDashboardCard(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.profile_reminders_short),
                    onClick = onNavigateToReminder,
                    modifier = Modifier.weight(1f)
                )
                // Security
                SmallDashboardCard(
                    icon = Icons.Default.Security,
                    label = stringResource(R.string.profile_security_short),
                    onClick = onNavigateToSecurity,
                    modifier = Modifier.weight(1f)
                )
                // Settings
                SmallDashboardCard(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.profile_settings_short),
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Menu List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    ProfileListItem(
                        icon = Icons.Default.Lock,
                        text = stringResource(R.string.profile_change_password),
                        onClick = onNavigateToChangePassword
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.Gray.copy(alpha = 0.1f))
                    
                    ProfileListItem(
                        icon = Icons.Default.Description,
                        text = stringResource(R.string.profile_terms),
                        onClick = onNavigateToTerms
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.Gray.copy(alpha = 0.1f))
                    
                    ProfileListItem(
                        icon = Icons.Default.Help,
                        text = stringResource(R.string.profile_help_center),
                        onClick = onNavigateToSupport
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout Button
            Button(
                onClick = viewModel::logout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF5350),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_logout_caps),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    displayName: String,
    email: String,
    avatarUrl: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.stitch_dark_green)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // Add a slightly transparent ring around avatar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(6.dp, colorResource(id = R.color.stitch_find_bg), CircleShape)
                )

                if (!avatarUrl.isNullOrEmpty()) {
                    if (avatarUrl.startsWith("data:image")) {
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
                                modifier = Modifier.fillMaxSize().padding(6.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.stitch_text_primary)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize().padding(6.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.stitch_text_primary)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DashboardCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color,
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun DashboardCardContent(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    showArrow: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (showArrow) {
                 Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.rotate(-45f).size(20.dp)
                 )
            }
        }
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.stitch_text_primary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}


@Composable
private fun SmallDashboardCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
             Icon(
                 imageVector = icon,
                 contentDescription = null,
                 tint = colorResource(id = R.color.stitch_text_primary),
                 modifier = Modifier.size(28.dp)
             )
             Spacer(modifier = Modifier.height(12.dp))
             Text(
                 text = label,
                 style = MaterialTheme.typography.labelMedium,
                 fontWeight = FontWeight.Bold,
                 color = colorResource(id = R.color.stitch_text_primary)
             )
        }
    }
}

@Composable
private fun ProfileListItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorResource(id = R.color.stitch_text_primary)
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
