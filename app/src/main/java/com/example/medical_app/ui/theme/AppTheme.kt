package com.example.medical_app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D28D9),        // 💜 violeta principal (premium)
    secondary = Color(0xFF7C3AED),      // violeta secundario (muy poco uso)
    background = Color(0xFFF7F7FB),     // gris claro elegante
    surface = Color(0xFFFFFFFF),        // cards blancas
    onPrimary = Color.White,
    onSurface = Color(0xFF1E293B),      // texto principal
    onSurfaceVariant = Color(0xFF64748B) // texto secundario
)


@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}
