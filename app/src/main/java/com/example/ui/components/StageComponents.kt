package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricSyncType
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.PlaybackSessionStatus
import com.example.media.CompanionSyncState
import com.example.media.LyricsStageState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LyricActive
import com.example.ui.theme.LyricPast
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
    LyricColorPreset.STAGE_WHITE -> Color.White
    LyricColorPreset.ELECTRIC_VIOLET -> StageViolet
    LyricColorPreset.WARM_AMBER -> StageAmber
    LyricColorPreset.STAGE_CYAN -> StageCyan
    LyricColorPreset.DYNAMIC_PRIMARY -> MaterialTheme.colorScheme.primary
}

/**
 * Ultra-minimal 2-line centered karaoke presentation for pure #000000 AMOLED & live preview.
 * Renders ONLY the lyrics (or minimal honest fallback phrase) with zero chrome or extra labels.
 */
@Composable
fun AodTwoLineLyricsView(
    state: CompanionSyncState,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
) {
    val prefs = state.preferences
    val fontFamily = prefs.fontFamily.toComposeFontFamily()
    val textAlign = prefs.alignment.toComposeTextAlign()
    val activeColor = prefs.colorPreset.resolveActiveColor()
    val baseSizeSp = if (isPreview) (prefs.fontSizeSp * 0.72f).coerceIn(16f, 26f) else prefs.fontSizeSp

    val doc = (state.lyricsState as? LyricsStageState.Loaded)?.document
    val lines = doc?.lines.orEmpty()
    val idx = state.activeLineIndex

    val (primaryText, secondaryText) = when (val lyrics = state.lyricsState) {
        is LyricsStageState.Loading -> "…" to null
        is LyricsStageState.Unavailable -> "Lyrics unavailable" to null
        is LyricsStageState.Loaded -> when (doc?.syncType) {
            LyricSyncType.SYNCED -> {
                val active = when {
                    idx in lines.indices -> lines[idx].text
                    lines.isNotEmpty() -> lines.first().text
                    else -> "♪"
                }
                val next = if (prefs.nextLineVisible && idx >= 0 && (idx + 1) in lines.indices) {
                    lines[idx + 1].text
                } else if (prefs.nextLineVisible && idx < 0 && lines.size > 1) {
                    lines[1].text
                } else {
                    null
                }
                active to next
            }
            LyricSyncType.PLAIN -> {
                val nonBlank = doc.plainText.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("[") }
                    .toList()
                val first = nonBlank.getOrNull(0) ?: "Plain lyrics only"
                val second = if (prefs.nextLineVisible) nonBlank.getOrNull(1) else null
                first to second
            }
            LyricSyncType.INSTRUMENTAL -> "♪" to null
            else -> "No lyrics" to null
        }
        is LyricsStageState.Idle -> when (state.snapshot.status) {
            PlaybackSessionStatus.NO_PERMISSION -> "Notification access needed" to null
            PlaybackSessionStatus.METADATA_MISSING -> "No track metadata" to null
            PlaybackSessionStatus.SESSION_ENDED -> "Playback ended" to null
            else -> if (isPreview) {
                "Turning quiet shadows into stage light" to
                    if (prefs.nextLineVisible) "Heartbeats locking on a golden wave" else null
            } else {
                "Waiting for music…" to null
            }
        }
    }

    val horizontalAlign = when (prefs.alignment) {
        LyricTextAlignment.CENTER -> Alignment.CenterHorizontally
        LyricTextAlignment.START -> Alignment.Start
        LyricTextAlignment.END -> Alignment.End
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlign,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = primaryText,
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically { it / 5 }) togetherWith
                    (fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideOutVertically { -it / 5 })
            },
            label = "aod_active_line"
        ) { line ->
            Text(
                text = line,
                color = activeColor,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = baseSizeSp.sp,
                lineHeight = (baseSizeSp * 1.28f).sp,
                textAlign = textAlign,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!secondaryText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(if (isPreview) 8.dp else 18.dp))
            AnimatedContent(
                targetState = secondaryText,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "aod_next_line"
            ) { next ->
                Text(
                    text = next,
                    color = LyricPast.copy(alpha = 0.62f),
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = (baseSizeSp * 0.72f).sp,
                    lineHeight = (baseSizeSp * 0.96f).sp,
                    textAlign = textAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
        color = StageObsidian.copy(alpha = 0.75f),
        shape = RoundedCornerShape(16.dp),
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
                        text = if (offsetMs == 0L) "0 ms" else "${if (offsetMs > 0) "+" else ""}$offsetMs ms",
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
                    TextButton(onClick = onReset) { Text("Reset") }
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
        color = StageSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            .size(50.dp)
                            .clip(CircleShape)
                            .testTag("demo_play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause_demo),
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
