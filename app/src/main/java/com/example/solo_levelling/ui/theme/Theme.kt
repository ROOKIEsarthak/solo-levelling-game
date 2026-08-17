package com.example.solo_levelling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val DarkColorScheme = darkColorScheme(
    primary = SystemPrimary,
    onPrimary = SystemOnPrimary,
    primaryContainer = SystemAccent,
    onPrimaryContainer = SystemForeground,
    secondary = SystemSecondary,
    onSecondary = SystemOnPrimary,
    secondaryContainer = SystemMuted,
    onSecondaryContainer = SystemForeground,
    tertiary = SystemTertiary,
    onTertiary = SystemOnPrimary,
    tertiaryContainer = SystemAccent,
    onTertiaryContainer = SystemForeground,
    background = SystemBackground,
    onBackground = SystemForeground,
    surface = SystemSecondaryBackground,
    onSurface = SystemForeground,
    surfaceVariant = SystemSurface,
    onSurfaceVariant = SystemMutedForeground,
    surfaceContainerLowest = SystemBackground,
    surfaceContainerLow = SystemSidebar,
    surfaceContainer = SystemSurface,
    surfaceContainerHigh = SystemSurface2,
    surfaceContainerHighest = SystemMuted,
    outline = SystemOutline,
    outlineVariant = SystemMuted,
    error = SystemError,
    onError = SystemOnError,
    errorContainer = SystemError.copy(alpha = 0.25f),
    onErrorContainer = SystemOnError,
    inverseSurface = SystemForeground,
    inverseOnSurface = SystemBackground,
    inversePrimary = LightPrimary,
    scrim = SystemBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
)

/** Fixed dark SYSTEM palette — wallpaper dynamic color is intentionally off. */
@Composable
fun SololevellingTheme(
    darkTheme: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
