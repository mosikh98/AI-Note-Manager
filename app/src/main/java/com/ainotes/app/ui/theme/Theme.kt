package com.ainotes.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Accent = Color(0xFF7C4DFF)
val SurfaceLight = Color(0xFFF7F6FB)
val SurfaceDark = Color(0xFF121218)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.14f),
    onPrimaryContainer = accent,
    secondary = Color(0xFF00A0A8),
    background = SurfaceLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFECEAF4),
    onBackground = Color(0xFF1B1B2F),
    onSurface = Color(0xFF1B1B2F),
    outline = Color(0xFFC8C6D6)
)

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.24f),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF4DD0E1),
    background = Color(0xFF0E0E14),
    surface = Color(0xFF17171F),
    surfaceVariant = Color(0xFF24242F),
    onBackground = Color(0xFFECEAF4),
    onSurface = Color(0xFFECEAF4),
    outline = Color(0xFF4A4A5A)
)

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    )
)

fun hexToColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Accent)

@Composable
fun AiNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentHex: String = "#7C4DFF",
    content: @Composable () -> Unit
) {
    val accent = hexToColor(accentHex)
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme(accent) else lightScheme(accent),
        typography = AppTypography,
        content = content
    )
}
