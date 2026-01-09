package com.safemed.ui.screen.adherence

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.ui.theme.EmeraldGreen
import kotlin.math.roundToInt

// Colors matching Stitch Design System
private val BackgroundGray = Color(0xFFF2F4F1)
private val DarkGreen = Color(0xFF0D3B35)
private val Lime = Color(0xFFDEFF7D)
private val TextPrimary = Color(0xFF1F2937)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdherenceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Thống kê tuân thủ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundGray
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                AdherenceScoreCard(
                    score = uiState.adherenceScore,
                    level = uiState.adherenceLevel
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                WeeklyChartCard(weeklyStats = uiState.weeklyStats)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Lịch sử uống thuốc",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Group by date label for cleaner list? 
            // The ViewModel already returns flat list with dateLabel, let's just group visually here if needed, 
            // or assume backend/VM sorts it. VM provides 'dateLabel' so we can use stickyHeader if we want, 
            // but simple item grouping is fine.
            
            val groupedHistory = uiState.history.groupBy { it.dateLabel }
            groupedHistory.forEach { (date, items) ->
                item {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(items) { log ->
                    AdherenceLogItem(log)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AdherenceScoreCard(score: Int, level: AdherenceLevel) {
    val levelText = when (level) {
        AdherenceLevel.Excellent -> "Tuyệt vời!"
        AdherenceLevel.Good -> "Rất tốt"
        AdherenceLevel.Average -> "Cần cố gắng"
        AdherenceLevel.Poor -> "Cần chú ý hơn"
    }
    
    val animatedScore by animateFloatAsState(
         targetValue = score / 100f, 
         label = "score"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGreen)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tổng quan tuần này",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Lime
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = levelText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            
            // Circular Progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background Track
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress
                    drawArc(
                        color = Lime,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedScore,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyChartCard(weeklyStats: List<DailyAdherence>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Biểu đồ tuần",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.forEach { dayStat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Bar
                        val barHeight = (100 * dayStat.percentage).dp // max 100dp
                        val barColor = if (dayStat.percentage == 1.0f) DarkGreen else if (dayStat.percentage > 0.5f) DarkGreen.copy(alpha = 0.6f) else Color(0xFFFFB4AB)
                        
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(100.dp), // Max height container
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(barHeight) // Actual height
                                    .background(barColor, RoundedCornerShape(6.dp))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = dayStat.day,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdherenceLogItem(log: AdherenceLog) {
    val (icon, color, bg) = when (log.status) {
        AdherenceStatus.Taken -> Triple(Icons.Default.Check, DarkGreen, Color(0xFFE6F4F1))
        AdherenceStatus.Missed -> Triple(Icons.Default.Close, Color.Red, Color(0xFFFDE8E8))
        AdherenceStatus.Skipped -> Triple(Icons.Default.Remove, Color.Gray, Color(0xFFF3F4F6))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.medicineName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = log.time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Text(
                text = when(log.status) {
                    AdherenceStatus.Taken -> "Đã uống"
                    AdherenceStatus.Missed -> "Bỏ lỡ"
                    AdherenceStatus.Skipped -> "Bỏ qua"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
