package com.longdu.vehicle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 蓝色主题色板 */
private val BlueColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF34A853),
    onSecondary = Color.White,
    error = Color(0xFFEA4335),
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onSurface = Color(0xFF202124),
    onSurfaceVariant = Color(0xFF5F6368)
)

@Composable
fun VehicleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BlueColorScheme, content = content)
}
