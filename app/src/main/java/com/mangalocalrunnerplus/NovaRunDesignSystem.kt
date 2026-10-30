package com.mangalocalrunnerplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NovaRunPalette {
    val Background = Color(0xFF09090D)
    val Surface = Color(0xFF15151D)
    val SurfaceElevated = Color(0xFF20202B)

    val Cyan = Color(0xFF00E5FF)
    val Lime = Color(0xFFC6FF00)
    val Purple = Color(0xFF9B5CFF)
    val Pink = Color(0xFFFF3D9A)

    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9B9BA8)
    val Border = Color(0xFF454552)

    val Success = Color(0xFF7CFF6B)
    val Warning = Color(0xFFFFC857)
    val Error = Color(0xFFFF5364)
}

private val NovaRunColorScheme = darkColorScheme(
    primary = NovaRunPalette.Cyan,
    onPrimary = NovaRunPalette.Background,
    secondary = NovaRunPalette.Lime,
    onSecondary = NovaRunPalette.Background,
    tertiary = NovaRunPalette.Purple,
    background = NovaRunPalette.Background,
    onBackground = NovaRunPalette.TextPrimary,
    surface = NovaRunPalette.Surface,
    onSurface = NovaRunPalette.TextPrimary,
    surfaceVariant = NovaRunPalette.SurfaceElevated,
    onSurfaceVariant = NovaRunPalette.TextSecondary,
    outline = NovaRunPalette.Border,
    error = NovaRunPalette.Error
)

@Composable
fun NovaRunTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NovaRunColorScheme,
        content = content
    )
}
