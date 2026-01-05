package com.safemed.ui.screen.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.data.model.ChatMessage
import com.safemed.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.*

/**
 * Màn hình Chat với Hepius Bot
 * 
 * Giao diện:
 * - TopAppBar xanh với avatar bot, tên "Hepius Bot", status "Đang hoạt động"
 * - Nút back "Về trang chủ"
 * - LazyColumn hiển thị tin nhắn (bubble trái/phải)
 * - Input bar với TextField + icons mic/attach/camera + nút gửi
 * 
 * Tính năng:
 * - Debouncing: Disable nút gửi khi đang xử lý
 * - Dark/Light mode support
 * - Locale VI/EN support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // Lấy welcome message
    val welcomeMessage = remember { viewModel.getWelcomeMessage() }

    // Scroll to bottom khi có tin nhắn mới
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Hiển thị error message
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Hiển thị success message
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                onNavigateBack = onNavigateBack,
                showMenu = showMenu,
                onMenuClick = { showMenu = !showMenu },
                onDismissMenu = { showMenu = false },
                onClearHistory = {
                    viewModel.clearChatHistory()
                    showMenu = false
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSendClick = {
                    if (inputText.isNotBlank() && !uiState.isSending) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        keyboardController?.hide()
                    }
                },
                isSending = uiState.isSending,
                onVoiceClick = {
                    Toast.makeText(context, context.getString(R.string.chat_feature_coming_soon), Toast.LENGTH_SHORT).show()
                },
                onAttachClick = {
                    Toast.makeText(context, context.getString(R.string.chat_feature_coming_soon), Toast.LENGTH_SHORT).show()
                },
                onCameraClick = {
                    Toast.makeText(context, context.getString(R.string.chat_feature_coming_soon), Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = EmeraldGreen
                )
            } else {
                // Tạo danh sách tin nhắn với welcome message nếu rỗng
                val displayMessages = if (uiState.messages.isEmpty()) {
                    listOf(ChatMessage.createAssistantMessage(welcomeMessage))
                } else {
                    uiState.messages
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayMessages, key = { it.messageId.ifEmpty { it.hashCode().toString() } }) { message ->
                        ChatMessageBubble(message = message)
                    }

                    // Hiển thị typing indicator khi đang gửi
                    if (uiState.isSending) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }
}

/**
 * TopAppBar cho ChatScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    onNavigateBack: () -> Unit,
    showMenu: Boolean,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onClearHistory: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bot Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 24.sp
                    )
                }
                
                // Bot name and status
                Column {
                    Text(
                        text = stringResource(R.string.chat_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4ADE80)) // Green dot
                        )
                        Text(
                            text = stringResource(R.string.chat_status_online),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onDismissMenu
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_clear_history)) },
                        onClick = onClearHistory
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = EmeraldGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

/**
 * Chat message bubble component
 * Supports Markdown rendering for AI responses
 */
@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isFromUser = message.isFromUser()
    val isDarkTheme = isSystemInDarkTheme()
    
    // Colors based on role and theme
    // Dark mode: Bot uses surfaceContainerHigh for better contrast
    // Light mode: Bot uses white with subtle border
    val bubbleColor = if (isFromUser) {
        EmeraldGreen
    } else {
        if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White
    }
    
    val textColor = if (isFromUser) {
        Color.White
    } else {
        if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color(0xFF1F2937)
    }
    
    val bubbleShape = if (isFromUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    // Strip reminder action tags from display (they are processed by ViewModel)
    val displayContent = message.content
        .replace(Regex("\\[REMINDER_ACTION\\][\\s\\S]*?\\[/REMINDER_ACTION\\]"), "")
        .trim()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
    ) {
        // Message bubble
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = if (!isFromUser && !isDarkTheme) 1.dp else 0.dp,
            border = if (!isFromUser) {
                if (isDarkTheme) {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                }
            } else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            // Render markdown for AI messages, plain text for user messages
            Text(
                text = if (isFromUser) {
                    AnnotatedString(displayContent)
                } else {
                    parseSimpleMarkdown(displayContent, textColor)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        // Timestamp
        Text(
            text = formatTimestamp(message.getTimestampMillis()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

/**
 * Typing indicator khi AI đang xử lý
 */
@Composable
private fun TypingIndicator() {
    val isDarkTheme = isSystemInDarkTheme()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White,
            shadowElevation = if (!isDarkTheme) 1.dp else 0.dp,
            border = if (isDarkTheme) {
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    val alpha = when (index) {
                        0 -> 0.4f
                        1 -> 0.7f
                        else -> 1f
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

/**
 * Input bar ở bottom
 */
@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    onVoiceClick: () -> Unit,
    onAttachClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Input field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_input_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = EmeraldGreen
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                singleLine = true,
                trailingIcon = {
                    Row {
                        // Voice input icon
                        IconButton(
                            onClick = onVoiceClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = stringResource(R.string.chat_voice_input),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Attach file icon
                        IconButton(
                            onClick = onAttachClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = stringResource(R.string.chat_attach_file),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Camera icon
                        IconButton(
                            onClick = onCameraClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.chat_camera),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
            
            // Send button
            IconButton(
                onClick = onSendClick,
                enabled = inputText.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isSending) EmeraldGreen 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send),
                        tint = if (inputText.isNotBlank()) Color.White 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp thành "HH:mm"
 */
private fun formatTimestamp(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
}
/**
 * Parse simple Markdown to AnnotatedString
 * Supports: **bold**, *italic*, `code`, and emojis
 */
private fun parseSimpleMarkdown(text: String, textColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldPattern = Regex("\\*\\*(.+?)\\*\\*")
        val italicPattern = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")
        val codePattern = Regex("`([^`]+)`")
        
        // Combine all patterns
        val allMatches = mutableListOf<Triple<Int, Int, Pair<String, SpanStyle>>>()
        
        // Find bold matches
        boldPattern.findAll(text).forEach { match ->
            allMatches.add(Triple(
                match.range.first,
                match.range.last + 1,
                Pair(match.groupValues[1], SpanStyle(fontWeight = FontWeight.Bold))
            ))
        }
        
        // Find italic matches
        italicPattern.findAll(text).forEach { match ->
            allMatches.add(Triple(
                match.range.first,
                match.range.last + 1,
                Pair(match.groupValues[1], SpanStyle(fontStyle = FontStyle.Italic))
            ))
        }
        
        // Find code matches
        codePattern.findAll(text).forEach { match ->
            allMatches.add(Triple(
                match.range.first,
                match.range.last + 1,
                Pair(match.groupValues[1], SpanStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    background = Color(0x20000000)
                ))
            ))
        }
        
        // Sort by start index
        allMatches.sortBy { it.first }
        
        // Build annotated string
        var lastEnd = 0
        allMatches.forEach { (start, end, content) ->
            // Avoid overlapping matches
            if (start >= lastEnd) {
                // Append text before this match
                if (start > lastEnd) {
                    append(text.substring(lastEnd, start))
                }
                // Append styled text
                withStyle(content.second) {
                    append(content.first)
                }
                lastEnd = end
            }
        }
        
        // Append remaining text
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
}