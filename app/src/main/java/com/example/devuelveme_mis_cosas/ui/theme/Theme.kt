package com.example.devuelveme_mis_cosas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    onPrimary = Color.White,
    primaryContainer = PrimaryVioletDark,
    onPrimaryContainer = Color.White,
    secondary = Emerald,
    onSecondary = Color.White,
    tertiary = Amber,
    background = DeepDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = BorderColor,
    onSurfaceVariant = TextSecondary,
    error = Rose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryViolet,
    onPrimary = Color.White,
    primaryContainer = PrimaryViolet,
    onPrimaryContainer = Color.White,
    secondary = Emerald,
    onSecondary = Color.White,
    tertiary = Amber,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBorder,
    onSurfaceVariant = LightTextSecondary,
    error = Rose,
    onError = Color.White
)

@Composable
fun Devuelveme_mis_cosasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
