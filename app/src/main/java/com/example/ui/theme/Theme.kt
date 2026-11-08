package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.theme.PresetThemes
import com.example.data.theme.ThemeConfig

val LocalJarvisTheme = staticCompositionLocalOf { PresetThemes.JARVIS_SIGNATURE }

object JarvisTheme {
    val config: ThemeConfig
        @Composable
        @ReadOnlyComposable
        get() = LocalJarvisTheme.current
}

@Composable
fun MyApplicationTheme(
    themeConfig: ThemeConfig = PresetThemes.JARVIS_SIGNATURE,
    content: @Composable () -> Unit
) {
    val dynamicColorScheme = darkColorScheme(
        primary = themeConfig.primaryAccent,
        onPrimary = Color(0xFF001E26),
        primaryContainer = themeConfig.primaryAccent.copy(alpha = 0.25f),
        onPrimaryContainer = themeConfig.primaryAccent,
        secondary = themeConfig.secondaryAccent,
        onSecondary = Color(0xFF001533),
        secondaryContainer = themeConfig.secondaryAccent.copy(alpha = 0.25f),
        onSecondaryContainer = themeConfig.secondaryAccent,
        tertiary = themeConfig.tertiaryAccent,
        onTertiary = Color(0xFF002220),
        background = themeConfig.backgroundColor,
        onBackground = themeConfig.textPrimary,
        surface = themeConfig.surfaceColor,
        onSurface = themeConfig.textPrimary,
        surfaceVariant = themeConfig.cardBg,
        onSurfaceVariant = themeConfig.textSecondary,
        error = RedAlert,
        onError = Color.White
    )

    CompositionLocalProvider(LocalJarvisTheme provides themeConfig) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = Typography,
            content = content
        )
    }
}
