package com.safemed.ui.screen.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
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
import java.text.SimpleDateFormat
import java.util.*

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

    val welcomeMessage = remember { viewModel.getWelcomeMessage() }
    val stitchBg = colorResource(id = R.color.stitch_bg)

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        containerColor = stitchBg,
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
            Column {
                SuggestionChipsRow(
                    onChipClick = { 
                        inputText = it 
                    }
                )
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
                    onHashClick = {
                         Toast.makeText(context, context.getString(R.string.chat_feature_coming_soon), Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorResource(id = R.color.stitch_dark_green)
                )
            } else {
                val displayMessages = if (uiState.messages.isEmpty()) {
                    listOf(ChatMessage.createAssistantMessage(welcomeMessage))
                } else {
                    uiState.messages
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(displayMessages, key = { it.messageId.ifEmpty { it.hashCode().toString() } }) { message ->
                        ChatMessageBubble(message = message)
                    }

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

@Composable
private fun ChatTopBar(
    onNavigateBack: () -> Unit,
    showMenu: Boolean,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onClearHistory: () -> Unit
) {
    val darkGreen = colorResource(id = R.color.stitch_text_primary)
    val lime = colorResource(id = R.color.stitch_lime)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Back Button + Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onNavigateBack,
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 0.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = darkGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = stringResource(R.string.chat_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(lime)
                    )
                    Text(
                        text = stringResource(R.string.chat_status_online),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorResource(id = R.color.stitch_text_secondary),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Right: Menu
        Box {
            Surface(
                onClick = onMenuClick,
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 0.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Menu",
                        tint = darkGreen
                    )
                }
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu,
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_clear_history), color = darkGreen) },
                    onClick = onClearHistory
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isFromUser = message.isFromUser()
    
    val bubbleColor = if (isFromUser) {
        Color.White
    } else {
        colorResource(id = R.color.stitch_dark_green)
    }
    
    val textColor = if (isFromUser) {
        colorResource(id = R.color.stitch_text_primary)
    } else {
        Color.White
    }
    
    val bubbleShape = if (isFromUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    val displayContent = message.content
        .replace(Regex("\\[REMINDER_ACTION\\][\\s\\S]*?\\[/REMINDER_ACTION\\]"), "")
        .trim()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 0.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isFromUser) {
                        AnnotatedString(displayContent)
                    } else {
                        parseSimpleMarkdown(displayContent, textColor)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    lineHeight = 22.sp
                )
            }
        }
        
        val authorText = if (isFromUser) stringResource(R.string.chat_sender_you) else stringResource(R.string.chat_sender_bot)
        Text(
            text = "$authorText • ${formatTimestamp(message.getTimestampMillis())}",
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(id = R.color.stitch_text_secondary),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
private fun SuggestionChipsRow(onChipClick: (String) -> Unit) {
    val chips = listOf(
        stringResource(R.string.chip_yes_log),
        stringResource(R.string.chip_tell_more),
        stringResource(R.string.chip_scan)
    )
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        items(chips) { chip ->
            Surface(
                onClick = { onChipClick(chip) },
                shape = RoundedCornerShape(50),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, colorResource(id = R.color.stitch_find_bg).copy(alpha = 0.5f))
            ) {
                Text(
                    text = chip,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.stitch_text_primary),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    onVoiceClick: () -> Unit,
    onHashClick: () -> Unit
) {
    val darkGreen = colorResource(id = R.color.stitch_dark_green)
    val lime = colorResource(id = R.color.stitch_lime)

    Surface(
        color = darkGreen,
        shape = RoundedCornerShape(100.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(72.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 8.dp)
        ) {
            // Plus Icon
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.clickable { onHashClick() }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Input Field
            Box(modifier = Modifier.weight(1f)) {
                if (inputText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_input_placeholder),
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    cursorBrush = SolidColor(lime),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                    singleLine = true
                )
            }
            
            // Mic Icon
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.clickable { onVoiceClick() }
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            // Send Button
            Surface(
                onClick = onSendClick,
                shape = CircleShape,
                color = if (isSending) Color.Gray else lime,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = darkGreen,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Send",
                            tint = darkGreen,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(-45f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = stringResource(R.string.chat_typing),
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(id = R.color.stitch_text_secondary),
            fontStyle = FontStyle.Italic
        )
    }
}

private fun formatTimestamp(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMillis)).uppercase()
}

private fun parseSimpleMarkdown(text: String, textColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldPattern = Regex("\\*\\*(.+?)\\*\\*")
        val italicPattern = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")
        val codePattern = Regex("`([^`]+)`")
        
        val allMatches = mutableListOf<Triple<Int, Int, Pair<String, SpanStyle>>>()
        
        boldPattern.findAll(text).forEach { match ->
            allMatches.add(Triple(
                match.range.first,
                match.range.last + 1,
                Pair(match.groupValues[1], SpanStyle(fontWeight = FontWeight.Bold))
            ))
        }
        
        italicPattern.findAll(text).forEach { match ->
            allMatches.add(Triple(
                match.range.first,
                match.range.last + 1,
                Pair(match.groupValues[1], SpanStyle(fontStyle = FontStyle.Italic))
            ))
        }
        
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
        
        allMatches.sortBy { it.first }
        
        var lastEnd = 0
        allMatches.forEach { (start, end, content) ->
            if (start >= lastEnd) {
                if (start > lastEnd) {
                    append(text.substring(lastEnd, start))
                }
                withStyle(content.second) {
                    append(content.first)
                }
                lastEnd = end
            }
        }
        
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
}