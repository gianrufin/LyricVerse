package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.KaraokeStylePreferences
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricLine
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.StageBackgroundStyle
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LyricActive
import com.example.ui.theme.LyricPast
import com.example.ui.theme.LyricUpcoming
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageViolet
import com.example.ui.theme.TextMuted

fun KaraokeFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    KaraokeFontFamily.PLUS_JAKARTA_SANS -> PlusJakartaSansFamily
    KaraokeFontFamily.OUTFIT -> OutfitFontFamily
    KaraokeFontFamily.PLAYFAIR_DISPLAY -> PlayfairDisplayFamily
    KaraokeFontFamily.JETBRAINS_MONO -> JetBrainsMonoFamily
}

fun LyricTextAlignment.toComposeTextAlign(): TextAlign = when (this) {
    LyricTextAlignment.START -> TextAlign.Start
    LyricTextAlignment.CENTER -> TextAlign.Center
    LyricTextAlignment.END -> TextAlign.End
}

@Composable
fun LyricColorPreset.resolveActiveColor(): Color = when (this) {
    LyricColorPreset.STAGE_WHITE -> LyricActive
    LyricColorPreset.ELECTRIC_VIOLET -> StageViolet
    LyricColorPreset.WARM_AMBER -> StageAmber
    LyricColorPreset.STAGE_CYAN -> StageCyan
    LyricColorPreset.DYNAMIC_PRIMARY -> MaterialTheme.colorScheme.primary
}

@Composable
fun StageAtmosphereBackdrop(
    backgroundStyle: StageBackgroundStyle,
    modifier: Modifier = Modifier,
) {
    val primaryGlow = MaterialTheme.colorScheme.primary
    val secondaryGlow = MaterialTheme.colorScheme.secondary

    Box(modifier = modifier.background(StageObsidian)) {
        if (backgroundStyle == StageBackgroundStyle.CONCERT_SPOTLIGHT) {
            Image(
                painter = painterResource(id = R.drawable.img_concert_stage_art),
                contentDescription = stringResource(id = R.string.cd_stage_artwork),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.24f)
            )
        }

        val gradientBrush = when (backgroundStyle) {
            StageBackgroundStyle.CONCERT_SPOTLIGHT -> Brush.verticalGradient(
                listOf(
                    StageViolet.copy(alpha = 0.22f),
                    StageObsidian.copy(alpha = 0.86f),
                    StageObsidian
                )
            )
            StageBackgroundStyle.DYNAMIC_OBSIDIAN -> Brush.verticalGradient(
                listOf(
                    primaryGlow.copy(alpha = 0.24f),
                    secondaryGlow.copy(alpha = 0.10f),
                    StageObsidian
                )
            )
            StageBackgroundStyle.DEEP_VELVET -> Brush.verticalGradient(
                listOf(
                    Color(0xFF28133E).copy(alpha = 0.72f),
                    Color(0xFF120B1F).copy(alpha = 0.94f),
                    StageObsidian
                )
            )
            StageBackgroundStyle.PURE_STAGE_BLACK -> Brush.verticalGradient(
                listOf(StageObsidian, StageObsidian)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        )
    }
}

@Composable
fun KaraokeSyncedLyricList(
    lines: List<LyricLine>,
    activeLineIndex: Int,
    preferences: KaraokeStylePreferences,
    isDemoMode: Boolean,
    onLineTapped: (LyricLine) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val fontFamily = preferences.fontFamily.toComposeFontFamily()
    val textAlign = preferences.alignment.toComposeTextAlign()
    val activeBaseColor = preferences.colorPreset.resolveActiveColor()

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && activeLineIndex < lines.size) {
            val target = (activeLineIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(index = target)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(
            items = lines,
            key = { idx, line -> "${idx}_${line.timestampMs}" }
        ) { index, line ->
            val isActive = index == activeLineIndex
            val isPast = activeLineIndex >= 0 && index < activeLineIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.04f else 0.96f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "lyric_line_scale"
            )
            val lineAlpha by animateFloatAsState(
                targetValue = when {
                    isActive -> 1.0f
                    isPast -> 0.36f
                    else -> 0.62f
                } * preferences.stageOpacity,
                label = "lyric_line_alpha"
            )
            val lineColor by animateColorAsState(
                targetValue = when {
                    isActive -> activeBaseColor
                    isPast -> LyricPast
                    else -> LyricUpcoming
                },
                label = "lyric_line_color"
            )

            val originX = when (preferences.alignment) {
                LyricTextAlignment.START -> 0f
                LyricTextAlignment.CENTER -> 0.5f
                LyricTextAlignment.END -> 1f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = isDemoMode) {
                        if (preferences.hapticsEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onLineTapped(line)
                    }
                    .padding(vertical = 6.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = line.text,
                    color = lineColor,
                    fontFamily = fontFamily,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = preferences.fontSizeSp.sp,
                    lineHeight = (preferences.fontSizeSp * 1.32f).sp,
                    textAlign = textAlign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = lineAlpha
                            transformOrigin = TransformOrigin(originX, 0.5f)
                        }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun SyncOffsetCalibrationRow(
    offsetMs: Long,
    hapticsEnabled: Boolean,
    onNudge: (Long) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StageSurfaceElevated.copy(alpha = 0.85f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Sync Offset",
                        style = MaterialTheme.typography.labelLarge,
                        color = LyricActive
                    )
                    Text(
                        text = if (offsetMs == 0L) "0 ms (exact)" else "${if (offsetMs > 0) "+" else ""}$offsetMs ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNudge(-100L)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("offset_minus_button")
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Delay lyrics by 100ms")
                }
                if (offsetMs != 0L) {
                    TextButton(onClick = onReset) {
                        Text("Reset")
                    }
                }
                FilledTonalIconButton(
                    onClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNudge(100L)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("offset_plus_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Advance lyrics by 100ms")
                }
            }
        }
    }
}

@Composable
fun DemoTransportBar(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    hapticsEnabled: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StageSurfaceElevated.copy(alpha = 0.92f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val maxDuration = durationMs.coerceAtLeast(1000L).toFloat()
            Slider(
                value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..maxDuration,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = StageObsidian
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("demo_seek_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPrevious()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("demo_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_previous_demo)
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePlayPause()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .testTag("demo_play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause_demo),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNext()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("demo_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.cd_next_demo)
                        )
                    }
                }

                Text(
                    text = formatTimestamp(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

fun formatTimestamp(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return "%d:%02d".format(mins, secs)
}
