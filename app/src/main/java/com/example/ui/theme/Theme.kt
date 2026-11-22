package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ConcertStageDarkColorScheme = darkColorScheme(
    primary = StageViolet,
    onPrimary = Color(0xFF140D2B),
    primaryContainer = StageVioletDeep,
    onPrimaryContainer = LyricActive,
    secondary = StageAmber,
    onSecondary = Color(0xFF281700),
    secondaryContainer = Color(0xFF3E2912),
    onSecondaryContainer = StageAmber,
    tertiary = StageCyan,
    onTertiary = Color(0xFF002426),
    background = StageObsidian,
    onBackground = LyricActive,
    surface = StageSurface,
    onSurface = LyricActive,
    surfaceVariant = StageSurfaceElevated,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = StageObsidian,
    surfaceContainerLow = StageSurface,
    surfaceContainer = StageSurfaceElevated,
    surfaceContainerHigh = StageSurfaceHighest,
    outline = StageOutline,
    outlineVariant = Color(0xFF202336),
    error = ErrorCoral,
    onError = Color(0xFF2F0808)
)

@Composable
fun LyricVerseTheme(
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val baseScheme =
        if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamic = dynamicDarkColorScheme(context)
            ConcertStageDarkColorScheme.copy(
                primary = dynamic.primary,
                onPrimary = dynamic.onPrimary,
                primaryContainer = dynamic.primaryContainer,
                onPrimaryContainer = dynamic.onPrimaryContainer,
                secondary = dynamic.secondary,
                onSecondary = dynamic.onSecondary,
                tertiary = dynamic.tertiary
            )
        } else {
            ConcertStageDarkColorScheme
        }

    MaterialTheme(
        colorScheme = baseScheme,
        typography = LyricVerseTypography,
        content = content
    )
}
