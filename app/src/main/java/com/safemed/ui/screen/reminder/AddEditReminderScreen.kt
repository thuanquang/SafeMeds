package com.safemed.ui.screen.reminder

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.data.model.TimeSlot
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.EmeraldGreenDark

/**
 * Màn hình thêm/sửa nhắc nhở uống thuốc
 */
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            AddEditReminderHeader(
                isEditMode = uiState.isEditMode,
                onNavigateBack = onNavigateBack
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldGreen)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Time Slots Section
                    TimeSlotSection(
                        morningTime = uiState.morningTime,
                        noonTime = uiState.noonTime,
                        afternoonTime = uiState.afternoonTime,
                        eveningTime = uiState.eveningTime,
                        onToggleSlot = viewModel::toggleTimeSlot,
                        onSetTime = viewModel::setTimeForSlot
                    )

                    HorizontalDivider()

                    // Day Selection Section
                    DaySelectionSection(
                        isEveryday = uiState.isEveryday,
                        selectedDays = uiState.selectedDays,
                        onSetEveryday = viewModel::setEveryday,
                        onToggleDay = viewModel::toggleDay
                    )

                    HorizontalDivider()

                    // Reminder Type Section
                    ReminderTypeSection(
                        isDetailedReminder = uiState.isDetailedReminder,
                        medicineName = uiState.medicineName,
                        dosage = uiState.dosage,
                        note = uiState.note,
                        onSetDetailedReminder = viewModel::setDetailedReminder,
                        onSetMedicineName = viewModel::setMedicineName,
                        onSetDosage = viewModel::setDosage,
                        onSetNote = viewModel::setNote
                    )

                    HorizontalDivider()

                    // Snooze Duration Section
                    SnoozeDurationSection(
                        snoozeDuration = uiState.snoozeDuration,
                        onSetSnoozeDuration = viewModel::setSnoozeDuration
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = viewModel::saveReminder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isEditMode) "Cập nhật" else "Lưu nhắc nhở",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Header
 */
@Composable
private fun AddEditReminderHeader(
    isEditMode: Boolean,
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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Quay lại",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isEditMode) "✏️ Sửa nhắc nhở" else "➕ Tạo nhắc nhở mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Section chọn thời gian cho 4 buổi
 */
@Composable
private fun TimeSlotSection(
    morningTime: String?,
    noonTime: String?,
    afternoonTime: String?,
    eveningTime: String?,
    onToggleSlot: (TimeSlot) -> Unit,
    onSetTime: (TimeSlot, String?) -> Unit
) {
    val context = LocalContext.current

    Column {
        Text(
            text = "⏰ Thời gian nhắc nhở",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Chọn các buổi và giờ nhắc nhở. Có thể bỏ qua buổi không cần.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimeSlotPicker(
                slot = TimeSlot.MORNING,
                time = morningTime,
                emoji = "🌅",
                onToggle = { onToggleSlot(TimeSlot.MORNING) },
                onTimeSelected = { onSetTime(TimeSlot.MORNING, it) }
            )
            TimeSlotPicker(
                slot = TimeSlot.NOON,
                time = noonTime,
                emoji = "☀️",
                onToggle = { onToggleSlot(TimeSlot.NOON) },
                onTimeSelected = { onSetTime(TimeSlot.NOON, it) }
            )
            TimeSlotPicker(
                slot = TimeSlot.AFTERNOON,
                time = afternoonTime,
                emoji = "🌤️",
                onToggle = { onToggleSlot(TimeSlot.AFTERNOON) },
                onTimeSelected = { onSetTime(TimeSlot.AFTERNOON, it) }
            )
            TimeSlotPicker(
                slot = TimeSlot.EVENING,
                time = eveningTime,
                emoji = "🌙",
                onToggle = { onToggleSlot(TimeSlot.EVENING) },
                onTimeSelected = { onSetTime(TimeSlot.EVENING, it) }
            )
        }
    }
}

/**
 * Time slot picker component
 */
@Composable
private fun TimeSlotPicker(
    slot: TimeSlot,
    time: String?,
    emoji: String,
    onToggle: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val isActive = time != null

    val backgroundColor = if (isActive) {
        EmeraldGreen.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val borderColor = if (isActive) {
        EmeraldGreen
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onToggle() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = slot.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isActive) {
            TextButton(
                onClick = {
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
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = time!!,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }
        } else {
            Text(
                text = "--:--",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Section chọn ngày trong tuần
 */
@Composable
private fun DaySelectionSection(
    isEveryday: Boolean,
    selectedDays: List<Int>,
    onSetEveryday: (Boolean) -> Unit,
    onToggleDay: (Int) -> Unit
) {
    Column {
        Text(
            text = "📅 Ngày lặp lại",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Everyday toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSetEveryday(!isEveryday) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEveryday,
                onCheckedChange = { onSetEveryday(it) },
                colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mỗi ngày",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (!isEveryday) {
            Spacer(modifier = Modifier.height(12.dp))

            // Day chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val days = listOf(
                    0 to "CN",
                    1 to "T2",
                    2 to "T3",
                    3 to "T4",
                    4 to "T5",
                    5 to "T6",
                    6 to "T7"
                )

                days.forEach { (dayIndex, dayName) ->
                    val isSelected = dayIndex in selectedDays

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onToggleDay(dayIndex) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section chọn loại nhắc nhở (chi tiết hoặc chung)
 */
@Composable
private fun ReminderTypeSection(
    isDetailedReminder: Boolean,
    medicineName: String,
    dosage: String,
    note: String,
    onSetDetailedReminder: (Boolean) -> Unit,
    onSetMedicineName: (String) -> Unit,
    onSetDosage: (String) -> Unit,
    onSetNote: (String) -> Unit
) {
    Column {
        Text(
            text = "💊 Loại nhắc nhở",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Toggle buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // General reminder
            FilterChip(
                selected = !isDetailedReminder,
                onClick = { onSetDetailedReminder(false) },
                label = { Text("Nhắc chung") },
                leadingIcon = if (!isDetailedReminder) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )

            // Detailed reminder
            FilterChip(
                selected = isDetailedReminder,
                onClick = { onSetDetailedReminder(true) },
                label = { Text("Nhắc chi tiết") },
                leadingIcon = if (isDetailedReminder) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Detailed reminder fields
        if (isDetailedReminder) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = medicineName,
                onValueChange = onSetMedicineName,
                label = { Text("Tên thuốc") },
                placeholder = { Text("Nhập tên thuốc...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    focusedLabelColor = EmeraldGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dosage,
                onValueChange = onSetDosage,
                label = { Text("Liều lượng") },
                placeholder = { Text("VD: 2 viên, 10ml...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    focusedLabelColor = EmeraldGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = onSetNote,
                label = { Text("Ghi chú (tùy chọn)") },
                placeholder = { Text("Uống sau bữa ăn...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    focusedLabelColor = EmeraldGreen
                )
            )
        }
    }
}

/**
 * Section chọn thời gian snooze
 */
@Composable
private fun SnoozeDurationSection(
    snoozeDuration: Int,
    onSetSnoozeDuration: (Int) -> Unit
) {
    Column {
        Text(
            text = "⏰ Thời gian nhắc lại",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Thời gian chờ khi bạn nhấn \"Nhắc lại sau\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val options = listOf(5, 10, 15, 30)

            options.forEach { minutes ->
                FilterChip(
                    selected = snoozeDuration == minutes,
                    onClick = { onSetSnoozeDuration(minutes) },
                    label = { Text("$minutes phút") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
