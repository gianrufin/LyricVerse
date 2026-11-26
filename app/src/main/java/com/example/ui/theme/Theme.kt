package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val StudioDarkColorScheme = darkColorScheme(
    primary = BurnishedAmber,
    onPrimary = BurnishedAmberInk,
    primaryContainer = BurnishedAmberContainer,
    onPrimaryContainer = BurnishedAmberOnContainer,
    secondary = WarmSand,
    onSecondary = BurnishedAmberInk,
    secondaryContainer = StudioSurfaceElevated,
    onSecondaryContainer = WarmIvory,
    tertiary = IceCyan,
    onTertiary = BurnishedAmberInk,
    background = StudioObsidian,
    onBackground = WarmIvory,
    surface = StudioSurface,
    onSurface = WarmIvory,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StoneSecondary,
    surfaceContainerLowest = StageBlack,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    outline = StudioOutline,
    outlineVariant = HairlineDivider,
    error = StatusError,
    onError = BurnishedAmberInk,
)

@Composable
fun LyricVerseTheme(
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val baseScheme =
        if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamic = dynamicDarkColorScheme(context)
            StudioDarkColorScheme.copy(
                primary = dynamic.primary,
                onPrimary = dynamic.onPrimary,
                primaryContainer = dynamic.primaryContainer,
                onPrimaryContainer = dynamic.onPrimaryContainer,
                secondary = dynamic.secondary,
                onSecondary = dynamic.onSecondary,
                tertiary = dynamic.tertiary
            )
        } else {
            StudioDarkColorScheme
        }

    MaterialTheme(
        colorScheme = baseScheme,
        typography = LyricVerseTypography,
        shapes = StudioShapes,
        content = content
    )
}
