package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FontCategory
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.LyricTransitionSpeed
import com.example.domain.model.LyricTransitionStyle
import com.example.ui.LyricVerseUiState
import com.example.ui.components.SyncOffsetCalibrationRow
import com.example.ui.components.resolveAlbumLightGradient
import com.example.ui.components.toComposeFontFamily
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioRadii
import com.example.ui.theme.StudioSpacing
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.WarmIvory
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeSettingsSection(
    uiState: LyricVerseUiState,
    onSelectFontFamily: (KaraokeFontFamily) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onSelectColorPreset: (LyricColorPreset) -> Unit,
    onSelectAlignment: (LyricTextAlignment) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
) {
    val prefs = uiState.syncState.preferences
    val snap = uiState.syncState.snapshot
    val haptics = LocalHapticFeedback.current
    var activeFontCategory by remember(prefs.fontFamily) { mutableStateOf(prefs.fontFamily.category) }
    val fontsForCategory = remember(activeFontCategory) {
        KaraokeFontFamily.entries.filter { it.category == activeFontCategory }
    }
    val albumColors = remember(snap.albumLightColors, snap.trackSignatureKey) {
        resolveAlbumLightGradient(snap)
    }

    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)) {
        // Section 1: Typography & Category Toggle
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
            StudioSectionOverline(
                title = "Font Family",
                trailing = prefs.fontFamily.label
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)
            ) {
                FontCategory.entries.forEach { category ->
                    StudioOptionTile(
                        label = category.label,
                        selected = activeFontCategory == category,
                        onClick = { activeFontCategory = category },
                        testTag = "font_cat_${category.name.lowercase()}"
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(fontsForCategory) { family ->
                    val selected = prefs.fontFamily == family
                    val shape = RoundedCornerShape(StudioRadii.control)
                    Surface(
                        color = if (selected) StudioSurfaceElevated else StudioObsidian,
                        shape = shape,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else StudioOutline
                        ),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .minimumInteractiveComponentSize()
                            .clip(shape)
                            .clickable {
                                if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectFontFamily(family)
                            }
                            .testTag("font_${family.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = StudioSpacing.md, vertical = StudioSpacing.sm)
                        ) {
                            Text(
                                text = family.label,
                                fontFamily = family.toComposeFontFamily(),
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        StudioHairlineDivider()

        // Section 2: Color & Album Gradient
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
            StudioSectionOverline(
                title = "Lyric Color & Album Gradient",
                trailing = prefs.colorPreset.label
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                LyricColorPreset.entries.forEach { preset ->
                    StudioOptionTile(
                        label = preset.label,
                        selected = prefs.colorPreset == preset,
                        onClick = {
                            if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectColorPreset(preset)
                        },
                        testTag = "color_${preset.name.lowercase()}",
                        leading = {
                            val swatchModifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .border(0.5.dp, StudioOutline, CircleShape)
                            when (preset) {
                                LyricColorPreset.ALBUM_ART -> Box(
                                    modifier = swatchModifier.background(Brush.linearGradient(albumColors))
                                )
                                LyricColorPreset.STAGE_WHITE -> Box(modifier = swatchModifier.background(Color.White))
                                LyricColorPreset.WARM_AMBER -> Box(modifier = swatchModifier.background(StageAmber))
                                LyricColorPreset.WARM_IVORY -> Box(modifier = swatchModifier.background(WarmIvory))
                                LyricColorPreset.STAGE_CYAN -> Box(modifier = swatchModifier.background(StageCyan))
                                LyricColorPreset.DYNAMIC_PRIMARY -> Box(
                                    modifier = swatchModifier.background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    )
                }
            }
        }

        StudioHairlineDivider()

        // Section 3: Type Scale & Alignment
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
            StudioSectionOverline(
                title = "Type Scale",
                trailing = "${prefs.fontSizeSp.roundToInt()} sp"
            )
            Slider(
                value = prefs.fontSizeSp,
                onValueChange = onFontSizeChange,
                valueRange = 20f..42f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = StudioOutline
                )
            )
            Text(
                text = "Alignment",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)
            ) {
                LyricTextAlignment.entries.forEach { align ->
                    StudioOptionTile(
                        label = align.label,
                        selected = prefs.alignment == align,
                        onClick = {
                            if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectAlignment(align)
                        }
                    )
                }
            }
        }

        StudioHairlineDivider()

        // Section 4: System Palette Override
        StudioSwitchRow(
            title = "Dynamic Material You Accents",
            subtitle = "Optional Android 12+ wallpaper tinting; off preserves Warm Obsidian & Amber",
            checked = prefs.useDynamicColor,
            onCheckedChange = onToggleDynamicColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransitionsSettingsSection(
    uiState: LyricVerseUiState,
    onSelectTransitionStyle: (LyricTransitionStyle) -> Unit,
    onSelectTransitionSpeed: (LyricTransitionSpeed) -> Unit,
    onToggleWordByWord: (Boolean) -> Unit,
    onToggleGlassBlur: (Boolean) -> Unit,
    onToggleShowTrackIntro: (Boolean) -> Unit,
    onToggleNextLineVisible: (Boolean) -> Unit,
    onNudgeOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
) {
    val prefs = uiState.syncState.preferences
    val haptics = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)) {
        // Section 1: Line Transition Motion
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
            StudioSectionOverline(
                title = "Lyric Transition",
                trailing = prefs.transitionStyle.label
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                LyricTransitionStyle.entries.forEach { style ->
                    StudioOptionTile(
                        label = style.label,
                        selected = prefs.transitionStyle == style,
                        onClick = {
                            if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTransitionStyle(style)
                        },
                        testTag = "transition_${style.name.lowercase()}"
                    )
                }
            }
            Text(
                text = prefs.transitionStyle.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Transition Pace",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)
            ) {
                LyricTransitionSpeed.entries.forEach { speed ->
                    StudioOptionTile(
                        label = speed.label,
                        selected = prefs.transitionSpeed == speed,
                        onClick = {
                            if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTransitionSpeed(speed)
                        },
                        testTag = "speed_${speed.name.lowercase()}"
                    )
                }
            }
        }

        StudioHairlineDivider()

        // Section 2: Reveal & Stage Composition
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
            StudioSectionOverline(title = "Reveal & Composition")
            StudioSwitchRow(
                title = "Word-by-Word Lyrics Reveal",
                subtitle = "Paces each word smoothly with enhanced LRC tags or syllable cadence",
                checked = prefs.wordByWordEnabled,
                onCheckedChange = onToggleWordByWord,
                testTag = "word_by_word_switch"
            )
            StudioSwitchRow(
                title = "Apple-Style Glass Blur",
                subtitle = "Briefly blurs incoming lines and upcoming words before snapping crisp (Android 12+)",
                checked = prefs.glassBlurEnabled,
                onCheckedChange = onToggleGlassBlur,
                testTag = "glass_blur_switch"
            )
            StudioSwitchRow(
                title = "Song Title & Artist Intro",
                subtitle = "Shows track title briefly when a song starts; skipped if lyrics are unavailable",
                checked = prefs.showTrackIntroOnStart,
                onCheckedChange = onToggleShowTrackIntro,
                testTag = "track_intro_switch"
            )
            StudioSwitchRow(
                title = "Show Upcoming Next Line",
                subtitle = "Displays dimmed preview line below the active lyric on #000000",
                checked = prefs.nextLineVisible,
                onCheckedChange = onToggleNextLineVisible,
            )
        }

        StudioHairlineDivider()

        // Section 3: Timing Offset
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
            StudioSectionOverline(title = "Timing Calibration")
            SyncOffsetCalibrationRow(
                offsetMs = prefs.syncOffsetMs,
                hapticsEnabled = prefs.hapticsEnabled,
                onNudge = onNudgeOffset,
                onReset = onResetOffset
            )
        }
    }
}
