package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.OverlayDefaultMode
import com.example.domain.model.StageBackgroundStyle
import com.example.ui.LyricVerseUiState
import com.example.ui.components.resolveActiveColor
import com.example.ui.components.toComposeFontFamily
import com.example.ui.components.toComposeTextAlign
import com.example.ui.theme.LyricActive
import com.example.ui.theme.LyricPast
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.TextMuted
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleStudioScreen(
    uiState: LyricVerseUiState,
    onSelectFontFamily: (KaraokeFontFamily) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onSelectColorPreset: (LyricColorPreset) -> Unit,
    onSelectBackground: (StageBackgroundStyle) -> Unit,
    onSelectAlignment: (LyricTextAlignment) -> Unit,
    onStageOpacityChange: (Float) -> Unit,
    onOverlayOpacityChange: (Float) -> Unit,
    onSelectOverlayMode: (OverlayDefaultMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = uiState.syncState.preferences
    val haptics = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("style_studio_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Stage Style Studio",
                style = MaterialTheme.typography.headlineMedium,
                color = LyricActive
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Customize karaoke typography, atmosphere, floating pill layout, and tactile haptics.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        // Live preview card
        item {
            Surface(
                color = StageObsidian,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "LIVE STAGE PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Turning quiet shadows into stage light",
                        color = prefs.colorPreset.resolveActiveColor().copy(alpha = prefs.stageOpacity),
                        fontFamily = prefs.fontFamily.toComposeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = prefs.fontSizeSp.sp,
                        lineHeight = (prefs.fontSizeSp * 1.28f).sp,
                        textAlign = prefs.alignment.toComposeTextAlign(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Heartbeats locking on a golden wave",
                        color = LyricPast.copy(alpha = 0.65f * prefs.stageOpacity),
                        fontFamily = prefs.fontFamily.toComposeFontFamily(),
                        fontWeight = FontWeight.Medium,
                        fontSize = (prefs.fontSizeSp * 0.88f).sp,
                        textAlign = prefs.alignment.toComposeTextAlign(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Font Family & Size
        item {
            StyleSectionCard(title = "Typography & Alignment") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KaraokeFontFamily.entries.forEach { family ->
                        FilterChip(
                            selected = prefs.fontFamily == family,
                            onClick = {
                                if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectFontFamily(family)
                            },
                            label = { Text(family.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Lyric Font Size: ${prefs.fontSizeSp.roundToInt()} sp",
                    style = MaterialTheme.typography.labelLarge,
                    color = LyricActive
                )
                Slider(
                    value = prefs.fontSizeSp,
                    onValueChange = onFontSizeChange,
                    valueRange = 18f..38f
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LyricTextAlignment.entries.forEach { align ->
                        FilterChip(
                            selected = prefs.alignment == align,
                            onClick = {
                                if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectAlignment(align)
                            },
                            label = { Text(align.label) }
                        )
                    }
                }
            }
        }

        // Color & Stage Atmosphere
        item {
            StyleSectionCard(title = "Color, Atmosphere & Opacity") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LyricColorPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = prefs.colorPreset == preset,
                            onClick = {
                                if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectColorPreset(preset)
                            },
                            label = { Text(preset.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StageBackgroundStyle.entries.forEach { bg ->
                        FilterChip(
                            selected = prefs.backgroundStyle == bg,
                            onClick = {
                                if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectBackground(bg)
                            },
                            label = { Text(bg.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Stage Lyric Opacity: ${(prefs.stageOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = LyricActive
                )
                Slider(
                    value = prefs.stageOpacity,
                    onValueChange = onStageOpacityChange,
                    valueRange = 0.45f..1.0f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Material You Accents",
                            style = MaterialTheme.typography.titleSmall,
                            color = LyricActive
                        )
                        Text(
                            text = "Blends system wallpaper accents on Android 12+",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = prefs.useDynamicColor,
                        onCheckedChange = onToggleDynamicColor
                    )
                }
            }
        }

        // Floating Overlay & Haptics
        item {
            StyleSectionCard(title = "Floating Overlay & Tactile Feedback") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlayDefaultMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.overlayDefaultMode == mode,
                            onClick = {
                                if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectOverlayMode(mode)
                            },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Overlay Glass Opacity: ${(prefs.overlayOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = LyricActive
                )
                Slider(
                    value = prefs.overlayOpacity,
                    onValueChange = onOverlayOpacityChange,
                    valueRange = 0.55f..1.0f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restrained Haptic Feedback",
                            style = MaterialTheme.typography.titleSmall,
                            color = LyricActive
                        )
                        Text(
                            text = "Subtle tactile confirmation on seek, offset steps, and overlay toggles",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = prefs.hapticsEnabled,
                        onCheckedChange = onToggleHaptics,
                        modifier = Modifier.testTag("haptics_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StageSurfaceElevated),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LyricActive
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
