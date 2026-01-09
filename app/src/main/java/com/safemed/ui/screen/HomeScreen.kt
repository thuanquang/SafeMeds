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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.safemed.R


@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDebug: () -> Unit = {},
    onNavigateToReminder: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val stitchBg = colorResource(id = R.color.stitch_bg)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = stitchBg,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                HomeHeader(
                    userName = uiState.userName,
                    avatarUrl = uiState.avatarUrl,
                    onProfileClick = onNavigateToProfile
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                MedicationScheduleCard(
                    items = uiState.scheduleItems,
                    onClick = onNavigateToReminder
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                BentoGridActions(
                    onScanClick = onNavigateToScan,
                    onFindClick = onNavigateToMap,
                    onAdherenceClick = {}
                )
                
                Spacer(modifier = Modifier.height(100.dp)) // Space for floating bar
            }

            // Floating AI Bar aligned to bottom
            FloatingAIBar(
                onClick = onNavigateToChat,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 40.dp)
            )
        }
    }
}

@Composable
private fun HomeHeader(userName: String, avatarUrl: String?, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.home_overview),
                style = MaterialTheme.typography.labelMedium,
                color = colorResource(id = R.color.stitch_text_secondary),
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.home_greeting),
                style = MaterialTheme.typography.headlineMedium,
                color = colorResource(id = R.color.stitch_text_primary),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = userName.ifBlank { "..." },
                style = MaterialTheme.typography.headlineMedium,
                color = colorResource(id = R.color.stitch_text_primary).copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
        }
        
        Surface(
            onClick = onProfileClick,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 0.dp,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
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
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profile",
                                tint = colorResource(id = R.color.stitch_text_primary)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = colorResource(id = R.color.stitch_text_primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicationScheduleCard(
    items: List<HomeScheduleItem>,
    onClick: () -> Unit
) {
    val darkGreen = colorResource(id = R.color.stitch_dark_green)
    val lime = colorResource(id = R.color.stitch_lime)
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = darkGreen)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.home_medication) + " " + stringResource(id = R.string.home_schedule),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Timeline Content
            if (items.isEmpty()) {
                Text(
                    text = "No medications scheduled for today",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayItems = items.take(3) // Show max 3 items to fit
                    
                    displayItems.forEachIndexed { index, item ->
                        if (index > 0) {
                             HorizontalDivider(
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).padding(top = 45.dp),
                                thickness = 1.dp,
                                color = Color.White.copy(alpha = 0.2f)
                            )
                        }
                        
                        TimelineItem(
                            time = item.time,
                            name = item.medicineName,
                            isDone = item.isDone,
                            accentColor = lime
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    time: String, 
    name: String, 
    isDone: Boolean, 
    accentColor: Color
) {
    if (isDone) {
        TimelineItem_Done(time, name)
    } else {
        TimelineItem_Active(time, name, accentColor)
    }
}

@Composable
private fun TimelineItem_Done(time: String, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (name.isNotBlank() && !name.equals("medicine", ignoreCase = true)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                modifier = Modifier.widthIn(max = 60.dp)
            )
        }
    }
}

@Composable
private fun TimelineItem_Active(time: String, name: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(accentColor, CircleShape)
                .shadow(elevation = 16.dp, spotColor = accentColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                 imageVector = Icons.Outlined.Medication,
                 contentDescription = null,
                 tint = colorResource(id = R.color.stitch_dark_green),
                 modifier = Modifier.size(28.dp).rotate(-45f)
             )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (name.isNotBlank() && !name.equals("medicine", ignoreCase = true)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 70.dp)
            )
        }
    }
}

@Composable
private fun BentoGridActions(
    onScanClick: () -> Unit,
    onFindClick: () -> Unit,
    onAdherenceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(360.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Scan (Tall)
        Card(
            onClick = onScanClick,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colorResource(id = R.color.stitch_lime), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = stringResource(id = R.string.home_scan),
                        tint = colorResource(id = R.color.stitch_dark_green),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Column {
                    Text(
                        text = "${stringResource(id = R.string.home_scan)}\n${stringResource(id = R.string.home_meds)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.stitch_text_primary),
                        lineHeight = 46.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.home_scan_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(id = R.color.stitch_text_secondary),
                        lineHeight = 22.sp
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.home_start),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.stitch_text_primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = colorResource(id = R.color.stitch_text_primary),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Right Column: Split
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Find Card
            Card(
                onClick = onFindClick,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.stitch_find_bg))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Decorative big icon
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.05f),
                        modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(x = 10.dp, y = 10.dp)
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.home_find),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.stitch_text_primary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.home_find_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorResource(id = R.color.stitch_text_secondary),
                                lineHeight = 16.sp
                            )
                        }
                        
                        Text(
                            text = stringResource(id = R.string.home_search_db),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.stitch_text_secondary),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            // Adherence Card
            Card(
                onClick = onAdherenceClick,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.stitch_lime))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = null,
                            tint = colorResource(id = R.color.stitch_dark_green)
                        )
                        
                        Surface(
                            color = colorResource(id = R.color.stitch_dark_green).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_weekly),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.stitch_dark_green)
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = "98%",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.stitch_dark_green)
                        )
                        Text(
                            text = stringResource(id = R.string.home_adherence),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.stitch_dark_green).copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingAIBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(40.dp),
        color = colorResource(id = R.color.stitch_dark_green),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + Text Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SmartToy,
                        contentDescription = null,
                        tint = colorResource(id = R.color.stitch_lime),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = stringResource(id = R.string.home_ask_ai),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.home_ai_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(id = R.color.stitch_lime)
                    )
                }
            }

            // Action Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colorResource(id = R.color.stitch_lime), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                     contentDescription = null,
                     tint = colorResource(id = R.color.stitch_dark_green),
                     modifier = Modifier.size(24.dp).rotate(-45f)
                 )
            }
        }
    }
}

