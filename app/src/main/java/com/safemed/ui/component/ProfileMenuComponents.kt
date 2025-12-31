package com.safemed.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable Profile Menu Item Component
 * Hiển thị một menu item với icon, text và arrow indicator
 */
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showDivider: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Trailing content or default arrow
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Profile Menu Item với Switch toggle
 */
@Composable
fun ProfileMenuItemWithSwitch(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showDivider: Boolean = true
) {
    ProfileMenuItem(
        icon = icon,
        title = title,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        iconTint = iconTint,
        showDivider = showDivider,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    )
}

/**
 * Profile Menu Section Header
 */
@Composable
fun ProfileMenuSection(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/**
 * User Badge Component (Thành viên hạng đồng, bạc, vàng...)
 */
@Composable
fun MembershipBadge(
    tier: MembershipTier,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = tier.backgroundColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tier.icon,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = tier.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = tier.textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Enum class cho các hạng thành viên
 */
enum class MembershipTier(
    val displayName: String,
    val icon: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    BRONZE(
        displayName = "Thành viên hạng đồng",
        icon = "🥉",
        backgroundColor = Color(0xFFFFE4C4),
        textColor = Color(0xFF8B4513)
    ),
    SILVER(
        displayName = "Thành viên hạng bạc",
        icon = "🥈",
        backgroundColor = Color(0xFFE8E8E8),
        textColor = Color(0xFF4A4A4A)
    ),
    GOLD(
        displayName = "Thành viên hạng vàng",
        icon = "🥇",
        backgroundColor = Color(0xFFFFD700).copy(alpha = 0.3f),
        textColor = Color(0xFFB8860B)
    ),
    PLATINUM(
        displayName = "Thành viên VIP",
        icon = "💎",
        backgroundColor = Color(0xFFE0E7EE),
        textColor = Color(0xFF2C3E50)
    )
}
