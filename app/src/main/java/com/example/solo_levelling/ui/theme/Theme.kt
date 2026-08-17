package com.example.solo_levelling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SurfaceHighest = Color(0xFF32353C)

val DarkColorScheme = darkColorScheme(
    primary = SystemPrimary,
    onPrimary = SystemOnPrimary,
    primaryContainer = SystemPrimaryContainer,
    onPrimaryContainer = SystemOnPrimaryContainer,
    secondary = SystemSecondary,
    onSecondary = SystemOnSecondary,
    secondaryContainer = SystemSecondaryContainer,
    onSecondaryContainer = SystemSecondary,
    tertiary = SystemTertiary,
    onTertiary = SystemOnTertiary,
    tertiaryContainer = SystemTertiaryContainer,
    onTertiaryContainer = SystemOnTertiary,
    background = SystemBackground,
    onBackground = SystemForeground,
    surface = SystemSurface2,
    onSurface = SystemForeground,
    surfaceVariant = SystemMuted,
    onSurfaceVariant = SystemMutedForeground,
    surfaceContainerLowest = SystemSecondaryBackground,
    surfaceContainerLow = SystemAccent,
    surfaceContainer = SystemSurface,
    surfaceContainerHigh = SystemMuted,
    surfaceContainerHighest = SurfaceHighest,
    outline = SystemOutlineSolid,
    outlineVariant = SystemOutlineVariant,
    error = SystemError,
    onError = SystemOnError,
    errorContainer = SystemError.copy(alpha = 0.25f),
    onErrorContainer = SystemError,
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

/** Fixed dark Sovereign OS palette — wallpaper dynamic color is intentionally off. */
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
