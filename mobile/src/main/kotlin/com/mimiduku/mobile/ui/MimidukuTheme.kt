package com.mimiduku.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

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
        secondaryContainer = Color(0xFFDDE5F2),
        onSecondaryContainer = Color(0xFF191C20),
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
        secondaryContainer = Color(0xFF2A3B54),
        onSecondaryContainer = Color(0xFFD4E3FF),
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

/** 文字の大きさの倍率。Material3 の既定（本文 16sp・補足 12sp）は高密度の端末で小さく見えるため、全体を大きくする。 */
private const val TEXT_SCALE = 1.125f

private fun TextStyle.scaled(): TextStyle = copy(fontSize = fontSize * TEXT_SCALE, lineHeight = lineHeight * TEXT_SCALE)

private val AppTypography =
    Typography().let {
        it.copy(
            displayLarge = it.displayLarge.scaled(),
            displayMedium = it.displayMedium.scaled(),
            displaySmall = it.displaySmall.scaled(),
            headlineLarge = it.headlineLarge.scaled(),
            headlineMedium = it.headlineMedium.scaled(),
            headlineSmall = it.headlineSmall.scaled(),
            titleLarge = it.titleLarge.scaled(),
            titleMedium = it.titleMedium.scaled(),
            titleSmall = it.titleSmall.scaled(),
            bodyLarge = it.bodyLarge.scaled(),
            bodyMedium = it.bodyMedium.scaled(),
            bodySmall = it.bodySmall.scaled(),
            labelLarge = it.labelLarge.scaled(),
            labelMedium = it.labelMedium.scaled(),
            labelSmall = it.labelSmall.scaled(),
        )
    }

@Composable
fun MimidukuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
