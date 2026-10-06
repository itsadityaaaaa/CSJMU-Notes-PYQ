package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pure Black & White (Monochrome) Dark Scheme
private val MonoDarkColorScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoZinc800,
    onPrimaryContainer = MonoWhite,
    secondary = MonoZinc300,
    onSecondary = MonoBlack,
    secondaryContainer = MonoZinc800,
    onSecondaryContainer = MonoWhite,
    tertiary = MonoZinc400,
    onTertiary = MonoBlack,
    background = MonoBlack,
    surface = MonoZinc900,
    surfaceVariant = MonoZinc800,
    outline = MonoZinc700,
    onBackground = MonoWhite,
    onSurface = MonoWhite,
    onSurfaceVariant = MonoZinc300
)

// Pure Black & White (Monochrome) Light Scheme
private val MonoLightColorScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoZinc100,
    onPrimaryContainer = MonoBlack,
    secondary = MonoZinc700,
    onSecondary = MonoWhite,
    secondaryContainer = MonoZinc100,
    onSecondaryContainer = MonoBlack,
    tertiary = MonoZinc600,
    onTertiary = MonoWhite,
    background = MonoWhite,
    surface = MonoWhite,
    surfaceVariant = MonoZinc100,
    outline = MonoZinc300,
    onBackground = MonoBlack,
    onSurface = MonoBlack,
    onSurfaceVariant = MonoZinc700
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MonoDarkColorScheme else MonoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
