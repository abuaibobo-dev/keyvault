package com.keyvault.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme

private val accents = listOf(Color(0xFF7C5CFF), Color(0xFF22D3EE), Color(0xFFFF6DAB), Color(0xFF75E0A8))
val LocalAccent = staticCompositionLocalOf { Color(0xFF7C5CFF) }
val cyanAccent = Color(0xFF22D3EE)

@Composable
fun KeyVaultTheme(mode: String, accentIndex: Int, content: @Composable () -> Unit) {
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    val accent = accents.getOrElse(accentIndex) { accents[0] }
    val scheme = if (dark) darkColorScheme(
        primary = accent, secondary = cyanAccent, background = Color(0xFF0B1020),
        surface = Color(0xFF1A1931), onBackground = Color(0xFFF5F3FF), onSurface = Color(0xFFF5F3FF),
    ) else lightColorScheme(
        primary = accent, secondary = Color(0xFF008CA4), background = Color(0xFFF5F3FF),
        surface = Color.White, onBackground = Color(0xFF171428), onSurface = Color(0xFF171428),
    )
    CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun VaultBackground(mode: String, content: @Composable BoxScope.() -> Unit) {
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    val colors = if (dark) listOf(Color(0xFF0B1020), Color(0xFF1A0F2E), Color(0xFF0E1A2B))
    else listOf(Color(0xFFF8F7FF), Color(0xFFEDE8FC), Color(0xFFE8F8FB))
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors)), content = content)
}

@Composable
fun Modifier.glass(): Modifier {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return this
    .background(if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
    .border(1.dp, if (dark) Color.White.copy(alpha = 0.16f) else Color(0xFF7C5CFF).copy(alpha = 0.18f), RoundedCornerShape(24.dp))
}
