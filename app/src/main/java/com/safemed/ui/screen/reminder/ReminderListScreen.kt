package com.safemed.ui.screen.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.data.model.MedicationReminder
import com.safemed.data.model.TimeSlot
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.EmeraldGreenDark

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddReminder,
                containerColor = EmeraldGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(com.safemed.R.string.reminder_add))
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
                    CircularProgressIndicator(color = EmeraldGreen)
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
    val isDarkMode = isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkMode) {
                        listOf(
                            EmeraldGreenDark.copy(alpha = 0.9f),
                            EmeraldGreenDark.copy(alpha = 0.7f)
                        )
                    } else {
                        listOf(EmeraldGreen, EmeraldGreenDark)
                    }
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
                    contentDescription = stringResource(com.safemed.R.string.reminder_back),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = stringResource(com.safemed.R.string.reminder_header_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(com.safemed.R.string.reminder_header_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
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
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with title and switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val defaultTitle = stringResource(com.safemed.R.string.reminder_default_title)
                    Text(
                        text = if (reminder.isDetailedReminder && !reminder.medicineName.isNullOrBlank()) {
                            reminder.medicineName!!
                        } else {
                            defaultTitle
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (reminder.isActive) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )

                    if (reminder.isDetailedReminder && !reminder.dosage.isNullOrBlank()) {
                        Text(
                            text = reminder.dosage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

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
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reminder.getRepeatDisplayText(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(com.safemed.R.string.reminder_action_edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(com.safemed.R.string.reminder_action_delete),
                            tint = Color.Red.copy(alpha = 0.7f)
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
    val backgroundColor = when {
        time == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        isActive -> when (slot) {
            TimeSlot.MORNING -> Color(0xFFFFF3E0)
            TimeSlot.NOON -> Color(0xFFFFFDE7)
            TimeSlot.AFTERNOON -> Color(0xFFE3F2FD)
            TimeSlot.EVENING -> Color(0xFFEDE7F6)
        }
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val textColor = when {
        time == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isActive -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    val emoji = when (slot) {
        TimeSlot.MORNING -> "🌅"
        TimeSlot.NOON -> "☀️"
        TimeSlot.AFTERNOON -> "🌤️"
        TimeSlot.EVENING -> "🌙"
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = time ?: "--:--",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (time != null) FontWeight.Medium else FontWeight.Normal,
            color = textColor
        )
    }
}
