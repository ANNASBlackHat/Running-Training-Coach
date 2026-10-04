package com.runningcompanion.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Run,
    onPrimary = AppColors.White,
    background = AppColors.Paper,
    onBackground = AppColors.Ink,
    surface = AppColors.Paper,
    onSurface = AppColors.Ink,
    outline = AppColors.Line
)

@Composable
fun RunningTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
