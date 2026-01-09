package com.safemed.ui.screen.reminder

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.data.model.TimeSlot
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    onNavigateBack: () -> Unit,
    reminderId: String? = null,
    viewModel: AddEditReminderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Colors
    val stitchBg = colorResource(id = R.color.stitch_bg)
    val bgCard = colorResource(id = R.color.bg_card)
    val textPrimary = colorResource(id = R.color.stitch_text_primary)
    val textSecondary = colorResource(id = R.color.stitch_text_secondary)
    val stitchLime = colorResource(id = R.color.stitch_lime)
    val stitchDarkGreen = colorResource(id = R.color.stitch_dark_green)

    // Navigate back on save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    // Show error
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = stitchBg,
        topBar = {
             CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = if (uiState.isEditMode) cleanStringResource(R.string.reminder_edit_title) else cleanStringResource(R.string.reminder_create_new),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = textPrimary
                    ) 
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .background(bgCard, CircleShape)
                            .clip(CircleShape)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = stitchBg
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
             Button(
                onClick = viewModel::saveReminder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = stitchLime,
                    contentColor = stitchDarkGreen,
                    disabledContainerColor = stitchLime.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = stitchDarkGreen,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.reminder_save).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = stitchDarkGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Section Title: TIME
                SectionTitle(cleanStringResource(R.string.reminder_time_section), textSecondary)
                
                // Time Slots Grid
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TimeSlotCard(
                            modifier = Modifier.weight(1f),
                            slot = TimeSlot.MORNING,
                            time = uiState.morningTime,
                            icon = Icons.Outlined.WbTwilight,
                            bgCard = bgCard,
                            accentColor = stitchDarkGreen,
                            onToggle = { viewModel.toggleTimeSlot(TimeSlot.MORNING) },
                            onTimeSelected = { viewModel.setTimeForSlot(TimeSlot.MORNING, it) }
                        )
                        TimeSlotCard(
                            modifier = Modifier.weight(1f),
                            slot = TimeSlot.NOON,
                            time = uiState.noonTime,
                            icon = Icons.Outlined.WbSunny,
                             bgCard = bgCard,
                            accentColor = stitchDarkGreen,
                            onToggle = { viewModel.toggleTimeSlot(TimeSlot.NOON) },
                            onTimeSelected = { viewModel.setTimeForSlot(TimeSlot.NOON, it) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TimeSlotCard(
                            modifier = Modifier.weight(1f),
                            slot = TimeSlot.AFTERNOON,
                            time = uiState.afternoonTime,
                            icon = Icons.Outlined.WbCloudy,
                             bgCard = bgCard,
                            accentColor = stitchDarkGreen,
                            onToggle = { viewModel.toggleTimeSlot(TimeSlot.AFTERNOON) },
                            onTimeSelected = { viewModel.setTimeForSlot(TimeSlot.AFTERNOON, it) }
                        )
                        TimeSlotCard(
                            modifier = Modifier.weight(1f),
                            slot = TimeSlot.EVENING,
                            time = uiState.eveningTime,
                            icon = Icons.Outlined.NightsStay,
                             bgCard = bgCard,
                            accentColor = stitchDarkGreen,
                            onToggle = { viewModel.toggleTimeSlot(TimeSlot.EVENING) },
                            onTimeSelected = { viewModel.setTimeForSlot(TimeSlot.EVENING, it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Day Selection Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = bgCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                         Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 16.dp, 16.dp, 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(stitchLime.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.DateRange, contentDescription = null, tint = stitchDarkGreen)
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cleanStringResource(R.string.reminder_day_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                            
                            // "Hàng ngày" text and switch
                             Text(
                                text = "Hàng ngày", 
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isEveryday) stitchDarkGreen else textSecondary,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            Switch(
                                checked = uiState.isEveryday,
                                onCheckedChange = { viewModel.setEveryday(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = stitchDarkGreen, // Fixed color
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE5E7EB),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                        
                        // Expanded Day Selection
                        if (!uiState.isEveryday) {
                             HorizontalDivider(color = stitchBg, thickness = 1.dp)
                             DaySelectionChips(
                                selectedDays = uiState.selectedDays,
                                onToggleDay = viewModel::toggleDay,
                                activeColor = stitchLime,
                                activeTextColor = stitchDarkGreen,
                                inactiveBg = stitchBg
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Section Title: TYPE
                SectionTitle(cleanStringResource(R.string.reminder_type_section), textSecondary)

                // Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(bgCard, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(
                                if (!uiState.isDetailedReminder) stitchLime else Color.Transparent, 
                                RoundedCornerShape(24.dp)
                            )
                            .clickable { viewModel.setDetailedReminder(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_type_general_short),
                            fontWeight = FontWeight.Bold,
                            color = if (!uiState.isDetailedReminder) stitchDarkGreen else textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(
                                if (uiState.isDetailedReminder) stitchLime else Color.Transparent, 
                                RoundedCornerShape(24.dp)
                            )
                            .clickable { viewModel.setDetailedReminder(true) },
                        contentAlignment = Alignment.Center
                    ) {
                         Text(
                            text = stringResource(R.string.reminder_type_detailed_short),
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isDetailedReminder) stitchDarkGreen else textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                if(uiState.isDetailedReminder) {
                     Spacer(modifier = Modifier.height(16.dp))
                     Card(
                        colors = CardDefaults.cardColors(containerColor = bgCard),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                             OutlinedTextField(
                                value = uiState.medicineName,
                                onValueChange = { viewModel.setMedicineName(it) },
                                label = { Text(stringResource(R.string.reminder_medicine_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = stitchDarkGreen,
                                    focusedLabelColor = stitchDarkGreen,
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.dosage,
                                onValueChange = { viewModel.setDosage(it) },
                                label = { Text(stringResource(R.string.reminder_dosage)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = stitchDarkGreen,
                                    focusedLabelColor = stitchDarkGreen,
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                             Spacer(modifier = Modifier.height(12.dp))
                             OutlinedTextField(
                                value = uiState.note,
                                onValueChange = { viewModel.setNote(it) },
                                label = { Text(stringResource(R.string.reminder_note)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = stitchDarkGreen,
                                    focusedLabelColor = stitchDarkGreen,
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Section Title: SNOOZE
                SectionTitle(cleanStringResource(R.string.reminder_snooze_section), textSecondary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val options = listOf(5, 10, 15, 30)
                    options.forEach { minutes ->
                        val isSelected = uiState.snoozeDuration == minutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(
                                    if (isSelected) stitchLime else bgCard,
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = if (isSelected) Color.Transparent else Color(0xFFE5E7EB),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setSnoozeDuration(minutes) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${minutes}p",
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) stitchDarkGreen else textPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Space for bottom bar
            }
        }
    }
}

@Composable
fun cleanStringResource(id: Int): String {
    return stringResource(id)
        .replace("➕", "")
        .replace("✏️", "")
        .replace("⏰", "")
        .replace("📅", "")
        .replace("💊", "")
        .trim()
}

@Composable
fun SectionTitle(title: String, color: Color) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun TimeSlotCard(
    modifier: Modifier = Modifier,
    slot: TimeSlot,
    time: String?,
    icon: ImageVector,
    bgCard: Color,
    accentColor: Color,
    onToggle: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val isActive = time != null
    
    Card(
        colors = CardDefaults.cardColors(containerColor = bgCard),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .height(160.dp)
            .clickable { onToggle() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFF3F4F6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if(isActive) accentColor else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = slot.getDisplayName(context),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.stitch_text_primary)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(20.dp))
                    .clickable(enabled = isActive) {
                         // Time picker logic
                         if (isActive) {
                             val parts = time!!.split(":")
                             val hour = parts[0].toIntOrNull() ?: slot.defaultHour
                             val minute = parts[1].toIntOrNull() ?: 0
                             TimePickerDialog(
                                 context,
                                 { _, selectedHour, selectedMinute ->
                                     val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                     onTimeSelected(formattedTime)
                                 },
                                 hour,
                                 minute,
                                 true
                             ).show()
                         }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = time ?: "-- : --",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if(isActive) accentColor else Color.Gray
                )
            }
        }
    }
}

@Composable
fun DaySelectionChips(
    selectedDays: List<Int>,
    onToggleDay: (Int) -> Unit,
    activeColor: Color,
    activeTextColor: Color,
    inactiveBg: Color
) {
    val dayNames = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7") // Short names
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dayNames.forEachIndexed { index, name -> 
            val isSelected = selectedDays.contains(index)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isSelected) activeColor else inactiveBg,
                        CircleShape
                    )
                    .clickable { onToggleDay(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) activeTextColor else Color.Gray
                )
            }
        }
    }
}
