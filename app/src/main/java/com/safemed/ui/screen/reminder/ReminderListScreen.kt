package com.safemed.ui.screen.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.data.model.MedicationReminder
import com.safemed.data.model.TimeSlot

/**
 * Màn hình danh sách nhắc nhở uống thuốc
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddReminder: () -> Unit,
    onNavigateToEditReminder: (String) -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    
    // Stitch Colors
    val stitchLime = colorResource(id = R.color.stitch_lime)
    val stitchDarkGreen = colorResource(id = R.color.stitch_dark_green)
    val stitchBg = colorResource(id = R.color.stitch_bg)

    // Show error message
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Show success message
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        containerColor = stitchBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddReminder,
                containerColor = stitchLime,
                contentColor = stitchDarkGreen,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = stringResource(R.string.reminder_add),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            ReminderListHeader(onNavigateBack = onNavigateBack)

            // Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = stitchDarkGreen)
                }
            } else if (uiState.reminders.isEmpty()) {
                EmptyReminderState(onAddClick = onNavigateToAddReminder)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.reminders,
                        key = { it.reminderId }
                    ) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleActive = { viewModel.toggleReminderActive(reminder) },
                            onEdit = { onNavigateToEditReminder(reminder.reminderId) },
                            onDelete = { showDeleteDialog = reminder.reminderId }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { reminderId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(com.safemed.R.string.reminder_delete)) },
            text = { Text(stringResource(com.safemed.R.string.reminder_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReminder(reminderId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(com.safemed.R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(com.safemed.R.string.btn_cancel))
                }
            }
        )
    }
}

/**
 * Header của màn hình danh sách nhắc nhở
 */
@Composable
fun ReminderListHeader(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.reminder_back),
                    tint = colorResource(id = R.color.stitch_text_primary),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.reminder_header_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.stitch_text_primary)
            )
        }
    }
}

/**
 * Empty state khi chưa có nhắc nhở nào
 */
@Composable
private fun EmptyReminderState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔔",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(com.safemed.R.string.reminder_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(com.safemed.R.string.reminder_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.stitch_dark_green))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(com.safemed.R.string.reminder_add))
        }
    }
}

/**
 * Card hiển thị thông tin một nhắc nhở
 */
@Composable
private fun ReminderCard(
    reminder: MedicationReminder,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val stitchLime = colorResource(id = R.color.stitch_lime)
    val stitchDarkGreen = colorResource(id = R.color.stitch_dark_green)
    val darkText = colorResource(id = R.color.stitch_text_primary)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header row with title and switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val defaultTitle = stringResource(R.string.reminder_default_title)
                    Text(
                        text = if (reminder.isDetailedReminder && !reminder.medicineName.isNullOrBlank()) {
                            reminder.medicineName!!
                        } else {
                            defaultTitle
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = darkText
                    )
                }

                // Custom Switch Style: Dark Track, Green Thumb
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = stitchLime,
                        checkedTrackColor = stitchDarkGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE0E0E0),
                        checkedBorderColor = Color.Transparent,
                        uncheckedBorderColor = Color.Transparent
                    ),
                    thumbContent = null
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Time slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween // Distribute evenly
            ) {
                TimeSlot.entries.forEach { slot ->
                    val time = reminder.getTimeForSlot(slot)
                    TimeSlotChip(
                        slot = slot,
                        time = time,
                        isActive = reminder.isActive && time != null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Repeat info and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Loop, // Loop/Repeat icon
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.reminder_repeat_daily_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.reminder_action_edit),
                            tint = Color.Gray
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFF0F0), CircleShape)
                            .clip(CircleShape)
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.reminder_action_delete),
                            tint = Color(0xFFFF8A80), // Light red/pinkish
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip hiển thị thời gian cho một buổi
 */
@Composable
private fun TimeSlotChip(
    slot: TimeSlot,
    time: String?,
    isActive: Boolean
) {
    val stitchLime = colorResource(id = R.color.stitch_lime)
    val stitchDarkGreen = colorResource(id = R.color.stitch_dark_green)
    
    val backgroundColor = if (isActive) stitchLime else Color(0xFFF8F9FA) // Light Grey
    val contentColor = if (isActive) stitchDarkGreen else Color.LightGray.copy(alpha = 0.5f)

    val icon = when (slot) {
        TimeSlot.MORNING -> Icons.Default.WbSunny
        TimeSlot.NOON -> Icons.Default.LightMode
        TimeSlot.AFTERNOON -> Icons.Default.WbTwilight
        TimeSlot.EVENING -> Icons.Default.NightsStay
    }

    Column(
        modifier = Modifier
            .size(72.dp) // Fixed square size
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = time ?: "--:--",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
