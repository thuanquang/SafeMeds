package com.safemed.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SafeMedLightColors = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = ColorWhite,
    primaryContainer = EmeraldGreenDark,
    onPrimaryContainer = ColorWhite,
    secondary = Navy,
    onSecondary = ColorWhite,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = ColorWhite,
    onSurface = TextPrimary,
    surfaceVariant = Outline,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = ColorWhite
)

private val SafeMedDarkColors = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = ColorWhite,
    primaryContainer = EmeraldGreenDark,
    onPrimaryContainer = ColorWhite,
    secondary = Navy,
    onSecondary = ColorWhite,
    background = SurfaceDark,
    onBackground = ColorWhite,
    surface = NavyDark,
    onSurface = ColorWhite,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = ColorWhite
)

private val SafeMedShapes = Shapes()

@Composable
fun SafeMedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SafeMedDarkColors else SafeMedLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SafeMedTypography,
        shapes = SafeMedShapes,
        content = content
    )
}

//private val ColorWhite = androidx.compose.ui.graphics.Color.White

