package com.comedorespopulares.registro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = OnPrimaryWhite,
    primaryContainer = GreenPrimaryLight,
    secondary = AmberAccent,
    onSecondary = OnPrimaryWhite,
    secondaryContainer = AmberAccentLight,
    background = BackgroundLight,
    onBackground = OnBackgroundDark,
    surface = SurfaceLight,
    onSurface = OnSurfaceDark,
    error = ErrorRed,
    onError = OnPrimaryWhite
)

@Composable
fun ComedoresPopularesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = ComedoresTypography,
        content = content
    )
}
