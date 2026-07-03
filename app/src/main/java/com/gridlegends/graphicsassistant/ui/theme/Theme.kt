package com.gridlegends.graphicsassistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 暗色霓虹主题配色方案
 */
private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBackground,
    primaryContainer = NeonCyanDark,
    onPrimaryContainer = NeonCyanLight,

    secondary = AccentOrange,
    onSecondary = DarkBackground,
    secondaryContainer = AccentOrangeDark,
    onSecondaryContainer = AccentOrangeLight,

    tertiary = Color(0xFFE040FB),
    onTertiary = DarkBackground,

    error = WarningRed,
    onError = DarkBackground,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = TextDisabled,
    outlineVariant = DarkCard
)

/**
 * GRID Legends 画质助手主题
 */
@Composable
fun GridLegendsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
