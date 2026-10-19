package com.mimiduku.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * アプリ内の画面に使うテーマ。アイコン・オーバーレイと同じネイビー（#1E3A5F）を基調にし、端末のダーク設定に追従する。
 * ダイナミックカラーは使わない（ブランド色を保つため）。
 */
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF1E3A5F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD4E3FF),
        onPrimaryContainer = Color(0xFF001C3A),
        secondary = Color(0xFF8D6E63),
        onSecondary = Color(0xFFFFFFFF),
        background = Color(0xFFF8F9FF),
        onBackground = Color(0xFF191C20),
        surface = Color(0xFFF8F9FF),
        onSurface = Color(0xFF191C20),
        surfaceVariant = Color(0xFFDFE3EB),
        onSurfaceVariant = Color(0xFF43474E),
        outline = Color(0xFF73777F),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF2F3FA),
        surfaceContainer = Color(0xFFECEEF5),
        surfaceContainerHigh = Color(0xFFE6E8EF),
        surfaceContainerHighest = Color(0xFFE0E2EA),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFA5C8FF),
        onPrimary = Color(0xFF00315E),
        primaryContainer = Color(0xFF1E3A5F),
        onPrimaryContainer = Color(0xFFD4E3FF),
        secondary = Color(0xFFD7CCC8),
        onSecondary = Color(0xFF3E2723),
        background = Color(0xFF101418),
        onBackground = Color(0xFFE0E2E8),
        surface = Color(0xFF101418),
        onSurface = Color(0xFFE0E2E8),
        surfaceVariant = Color(0xFF43474E),
        onSurfaceVariant = Color(0xFFC3C6CF),
        outline = Color(0xFF8D9199),
        surfaceContainerLowest = Color(0xFF0B0F13),
        surfaceContainerLow = Color(0xFF191C20),
        surfaceContainer = Color(0xFF1D2024),
        surfaceContainerHigh = Color(0xFF272A2F),
        surfaceContainerHighest = Color(0xFF32353A),
    )

@Composable
fun MimidukuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
