package com.xinxu.mood.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Pine = Color(0xFF39695D)
@Composable fun XinXuTheme(mode: String, content: @Composable () -> Unit) {
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFFA6D2BB), primaryContainer = Color(0xFF2C4B3C), onPrimaryContainer = Color(0xFFDAEDDF), secondary = Color(0xFFE0C89B), secondaryContainer = Color(0xFF504431), onSecondaryContainer = Color(0xFFF2E7D3), background = Color(0xFF151D19), surface = Color(0xFF1C2621), surfaceVariant = Color(0xFF2C3931)) else lightColorScheme(primary = Pine, onPrimary = Color.White, primaryContainer = Color(0xFFE2EDDF), onPrimaryContainer = Color(0xFF254A3C), secondary = Color(0xFF93764A), secondaryContainer = Color(0xFFF2E7D3), background = Color(0xFFFAF7F0), surface = Color(0xFFFFFCF5), surfaceVariant = Color(0xFFF0EDE3)), content = content)
}
