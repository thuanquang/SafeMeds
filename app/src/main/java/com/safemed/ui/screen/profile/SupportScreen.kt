package com.safemed.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safemed.R
import com.safemed.ui.theme.EmeraldGreen

/**
 * Data class for FAQ item with string resource IDs
 */
data class FAQItem(
    val questionResId: Int,
    val answerResId: Int
)

/**
 * List of FAQ items using string resource IDs for localization
 */
private val faqItems = listOf(
    FAQItem(R.string.support_faq_1_q, R.string.support_faq_1_a),
    FAQItem(R.string.support_faq_2_q, R.string.support_faq_2_a),
    FAQItem(R.string.support_faq_3_q, R.string.support_faq_3_a),
    FAQItem(R.string.support_faq_4_q, R.string.support_faq_4_a),
    FAQItem(R.string.support_faq_5_q, R.string.support_faq_5_a),
    FAQItem(R.string.support_faq_6_q, R.string.support_faq_6_a),
    FAQItem(R.string.support_faq_7_q, R.string.support_faq_7_a)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.support_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Contact options
            item {
                Text(
                    text = stringResource(R.string.support_contact),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Phone,
                        title = stringResource(R.string.support_hotline),
                        subtitle = stringResource(R.string.support_hotline_number),
                        onClick = { /* TODO: Open phone dialer */ }
                    )
                    ContactCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Email,
                        title = stringResource(R.string.support_email_title),
                        subtitle = stringResource(R.string.support_email_address),
                        onClick = { /* TODO: Open email client */ }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = stringResource(R.string.support_chat_online),
                        subtitle = stringResource(R.string.support_chat_hours),
                        onClick = onNavigateToChat
                    )
                    ContactCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = stringResource(R.string.support_zalo),
                        subtitle = stringResource(R.string.support_zalo_id),
                        onClick = { /* TODO: Open Zalo */ }
                    )
                }
            }

            // FAQ Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.support_faq),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            items(faqItems.size) { index ->
                FAQItemCard(
                    faqItem = faqItems[index],
                    isExpanded = expandedFaqIndex == index,
                    onClick = {
                        expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                    }
                )
            }

            // Feedback section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.support_feedback),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            item {
                FeedbackCard()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ContactCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FAQItemCard(
    faqItem: FAQItem,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val question = stringResource(faqItem.questionResId)
    val answer = stringResource(faqItem.answerResId)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) 
                EmeraldGreen.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = EmeraldGreen
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard() {
    var feedbackText by remember { mutableStateOf("") }
    var showThankYou by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showThankYou) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldGreen
                    )
                    Text(
                        text = stringResource(R.string.support_feedback_success),
                        style = MaterialTheme.typography.bodyLarge,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.support_feedback_question),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.support_feedback_hint)) },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        cursorColor = EmeraldGreen
                    )
                )

                Button(
                    onClick = {
                        // TODO: Submit feedback
                        showThankYou = true
                        feedbackText = ""
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = feedbackText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.support_send_feedback))
                }
            }
        }
    }
}
