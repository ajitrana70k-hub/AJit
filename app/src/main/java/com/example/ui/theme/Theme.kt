package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = StudioCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF470B15),
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = StudioBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF172642),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = StudioEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0A3327),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = ObsidianBg,
    onBackground = InkLight,
    surface = ObsidianSurface,
    onSurface = InkLight,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = InkMutedLight,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = Color(0xFF232938),
    outline = ObsidianOutline,
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = StudioCrimson,
    onPrimary = Color.White,
    primaryContainer = StudioCrimsonSoft,
    onPrimaryContainer = Color(0xFF6B0013),
    secondary = StudioBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = StudioEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = AlabasterBg,
    onBackground = InkDark,
    surface = AlabasterSurface,
    onSurface = InkDark,
    surfaceVariant = AlabasterSurfaceHigh,
    onSurfaceVariant = InkMutedDark,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AlabasterBg,
    surfaceContainer = AlabasterSurfaceHigh,
    surfaceContainerHigh = Color(0xFFE2E7F0),
    outline = AlabasterOutline,
    error = Color(0xFFDC2626),
    onError = Color.White
)

val MyVideoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyVideoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MyVideoTypography,
        shapes = MyVideoShapes,
        content = content
    )
}
