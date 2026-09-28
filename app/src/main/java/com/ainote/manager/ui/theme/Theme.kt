package com.ainote.manager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet20,
    secondary = Teal80,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = ErrorRed,
)

private val LightColors = lightColorScheme(
    primary = Violet40,
    onPrimary = PureWhite,
    secondary = Teal40,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    error = ErrorRed,
)

@Composable
fun AiNoteManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accent: String = "violet",
    background: String = "default",
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val (lightAccent, darkAccent, onAccent) = when (accent) {
        "ocean" -> Triple(Color(0xFF1769AA), Color(0xFF9CCBFF), Color(0xFFFFFFFF))
        "forest" -> Triple(Color(0xFF35734A), Color(0xFFA9D9B3), Color(0xFFFFFFFF))
        "amber" -> Triple(Color(0xFF8A5700), Color(0xFFFFD180), Color(0xFFFFFFFF))
        "rose" -> Triple(Color(0xFFB23A5B), Color(0xFFFFB1C2), Color(0xFFFFFFFF))
        else -> Triple(Violet40, Violet80, Color(0xFFFFFFFF))
    }
    val backgroundColor = when (background) {
        "paper" -> if (darkTheme) Color(0xFF211F1A) else Color(0xFFFFFBF2)
        "mist" -> if (darkTheme) Color(0xFF171F22) else Color(0xFFF1F7F7)
        else -> baseScheme.background
    }
    val surfaceColor = when (background) {
        "paper" -> if (darkTheme) Color(0xFF2B2821) else Color(0xFFFFFEFA)
        "mist" -> if (darkTheme) Color(0xFF202A2D) else Color(0xFFFAFFFF)
        else -> baseScheme.surface
    }
    val colorScheme = baseScheme.copy(
        primary = if (darkTheme) darkAccent else lightAccent,
        onPrimary = if (darkTheme) Color(0xFF202020) else onAccent,
        secondary = if (darkTheme) Teal80 else Teal40,
        background = backgroundColor,
        surface = surfaceColor,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * fontScale.coerceIn(0.85f, 1.3f))
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
