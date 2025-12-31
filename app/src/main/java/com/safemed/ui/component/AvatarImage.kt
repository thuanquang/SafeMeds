package com.safemed.ui.component

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Composable để hiển thị avatar
 * Hỗ trợ cả Base64 data URL và HTTP URL
 * 
 * @param avatarUrl URL của avatar (có thể là data:image/... hoặc https://...)
 * @param size Kích thước avatar
 * @param modifier Modifier cho composable
 */
@Composable
fun AvatarImage(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    contentDescription: String = "Avatar"
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Base64 image
            avatarUrl != null && avatarUrl.startsWith("data:image") -> {
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
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    DefaultAvatarIcon(size)
                }
            }
            
            // HTTP/HTTPS URL image (from Google, etc.)
            avatarUrl != null && avatarUrl.isNotEmpty() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            // No avatar - show default icon
            else -> {
                DefaultAvatarIcon(size)
            }
        }
    }
}

@Composable
private fun DefaultAvatarIcon(size: Dp) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        modifier = Modifier.size(size * 0.5f),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Version với initial letter thay vì icon
 */
@Composable
fun AvatarImageWithInitial(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (avatarUrl.isNullOrEmpty()) backgroundColor
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Base64 image
            avatarUrl != null && avatarUrl.startsWith("data:image") -> {
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
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    InitialText(displayName, size)
                }
            }
            
            // HTTP/HTTPS URL image
            avatarUrl != null && avatarUrl.isNotEmpty() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            // No avatar - show initial
            else -> {
                InitialText(displayName, size)
            }
        }
    }
}

@Composable
private fun InitialText(displayName: String, size: Dp) {
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    androidx.compose.material3.Text(
        text = initial,
        style = when {
            size >= 100.dp -> MaterialTheme.typography.headlineLarge
            size >= 60.dp -> MaterialTheme.typography.headlineMedium
            else -> MaterialTheme.typography.titleLarge
        },
        color = Color.White
    )
}
