package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.domain.model.LyricWord
import com.example.domain.model.PlaybackSessionStatus
import com.example.media.CompanionSyncState
import com.example.media.LyricsStageState
import com.example.media.PlaybackClockExtrapolator
import com.example.ui.theme.BricolageGrotesqueFamily
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.CormorantGaramondFamily
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.InstrumentSerifFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LyricActive
import com.example.ui.theme.LyricPast
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PacificoFontFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageViolet
import com.example.ui.theme.SyneFontFamily
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun KaraokeFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    KaraokeFontFamily.PLUS_JAKARTA_SANS -> PlusJakartaSansFamily
    KaraokeFontFamily.OUTFIT -> OutfitFontFamily
    KaraokeFontFamily.SPACE_GROTESK -> SpaceGroteskFamily
    KaraokeFontFamily.JETBRAINS_MONO -> JetBrainsMonoFamily
    KaraokeFontFamily.INSTRUMENT_SERIF -> InstrumentSerifFamily
    KaraokeFontFamily.CORMORANT_GARAMOND -> CormorantGaramondFamily
    KaraokeFontFamily.PLAYFAIR_DISPLAY -> PlayfairDisplayFamily
    KaraokeFontFamily.SYNE -> SyneFontFamily
    KaraokeFontFamily.FREDOKA -> FredokaFontFamily
    KaraokeFontFamily.BRICOLAGE_GROTESQUE -> BricolageGrotesqueFamily
    KaraokeFontFamily.PACIFICO -> PacificoFontFamily
    KaraokeFontFamily.CAVEAT -> CaveatFontFamily
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
 * Minimal centered karaoke view for pure #000000 AMOLED lock-screen & live settings preview.
 * Supports brief Song Title + Artist intro on track start, Apple Music-style brief glass blur
 * on line/word entry, word-by-word reveal, and landscape-responsive sizing.
 */
@Composable
fun AodTwoLineLyricsView(
    state: CompanionSyncState,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    introTriggerNonce: Int = 0,
) {
    val prefs = state.preferences
    val fontFamily = prefs.fontFamily.toComposeFontFamily()
    val textAlign = prefs.alignment.toComposeTextAlign()
    val activeColor = prefs.colorPreset.resolveActiveColor()
    val doc = (state.lyricsState as? LyricsStageState.Loaded)?.document
    val lines = doc?.lines.orEmpty()
    val idx = state.activeLineIndex
    val snap = state.snapshot

    // Smooth ~22fps clock extrapolation during active synced playback for fluid word-by-word pacing
    var liveEffectivePosMs by remember { mutableLongStateOf(state.livePositionMs + prefs.syncOffsetMs) }
    LaunchedEffect(
        snap.isPlaying,
        snap.positionMs,
        snap.lastPositionUpdateElapsedMs,
        snap.playbackSpeed,
        prefs.syncOffsetMs,
        prefs.wordByWordEnabled,
        doc?.syncType,
        state.livePositionMs,
    ) {
        liveEffectivePosMs = PlaybackClockExtrapolator.computePositionMs(
            snapshot = snap,
            currentElapsedRealtimeMs = SystemClock.elapsedRealtime()
        ) + prefs.syncOffsetMs

        if (snap.isPlaying && prefs.wordByWordEnabled && doc?.syncType == LyricSyncType.SYNCED) {
            while (isActive) {
                delay(45L)
                liveEffectivePosMs = PlaybackClockExtrapolator.computePositionMs(
                    snapshot = snap,
                    currentElapsedRealtimeMs = SystemClock.elapsedRealtime()
                ) + prefs.syncOffsetMs
            }
        }
    }

    // Track title + artist intro gate (brief ~2.4s display when a song starts or AOD opens)
    val trackTitle = snap.trackTitle.ifBlank { doc?.trackTitle.orEmpty() }
    val artistName = snap.artistName.ifBlank { doc?.artistName.orEmpty() }
    val trackIdentityKey = when {
        trackTitle.isNotBlank() -> "$trackTitle|$artistName"
        else -> ""
    }
    var showingIntro by remember { mutableStateOf(false) }

    LaunchedEffect(trackIdentityKey, prefs.showTrackIntroOnStart, isPreview, introTriggerNonce) {
        val canShowIntro = prefs.showTrackIntroOnStart &&
            trackIdentityKey.isNotBlank() &&
            (!isPreview || introTriggerNonce > 0)
        if (canShowIntro) {
            showingIntro = true
            delay(2400L)
            showingIntro = false
        } else {
            showingIntro = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompactLandscape = !isPreview && maxWidth > maxHeight && maxHeight < 460.dp
        val baseSizeSp = when {
            isPreview -> (prefs.fontSizeSp * 0.68f).coerceIn(16f, 24f)
            isCompactLandscape -> (prefs.fontSizeSp * 0.90f).coerceIn(20f, 36f)
            else -> prefs.fontSizeSp
        }

        val horizontalAlign = when (prefs.alignment) {
            LyricTextAlignment.CENTER -> Alignment.CenterHorizontally
            LyricTextAlignment.START -> Alignment.Start
            LyricTextAlignment.END -> Alignment.End
        }

        AnimatedContent(
            targetState = if (showingIntro) "INTRO:$trackIdentityKey" else "LYRICS",
            transitionSpec = {
                fadeIn(tween(340)) togetherWith fadeOut(tween(280))
            },
            label = "aod_intro_vs_lyrics"
        ) { phase ->
            if (phase.startsWith("INTRO:")) {
                TrackIntroGlassReveal(
                    title = trackTitle,
                    artist = artistName,
                    fontFamily = fontFamily,
                    activeColor = activeColor,
                    baseSizeSp = baseSizeSp,
                    textAlign = textAlign,
                    horizontalAlign = horizontalAlign,
                    glassBlurEnabled = prefs.glassBlurEnabled,
                )
            } else {
                val activeLine = if (doc?.syncType == LyricSyncType.SYNCED) {
                    when {
                        idx in lines.indices -> lines[idx]
                        lines.isNotEmpty() -> lines.first()
                        else -> null
                    }
                } else null

                val (primaryText, secondaryText) = when (state.lyricsState) {
                    is LyricsStageState.Loading -> "…" to null
                    is LyricsStageState.Unavailable -> "Lyrics unavailable" to null
                    is LyricsStageState.Loaded -> when (doc?.syncType) {
                        LyricSyncType.SYNCED -> {
                            val active = activeLine?.text ?: "♪"
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
                    is LyricsStageState.Idle -> when (snap.status) {
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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = horizontalAlign,
                    verticalArrangement = Arrangement.Center
                ) {
                    val transitionKey = "${activeLine?.index ?: -1}:$primaryText"
                    AnimatedContent(
                        targetState = transitionKey,
                        transitionSpec = {
                            (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                slideInVertically { it / 6 }) togetherWith
                                (fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                                    slideOutVertically { -it / 6 })
                        },
                        label = "aod_active_line"
                    ) { key ->
                        val lineBlurAnim = remember(key, prefs.glassBlurEnabled) {
                            Animatable(if (prefs.glassBlurEnabled) 13f else 0f)
                        }
                        val lineScaleAnim = remember(key) { Animatable(0.955f) }

                        LaunchedEffect(key, prefs.glassBlurEnabled) {
                            lineBlurAnim.snapTo(if (prefs.glassBlurEnabled) 13f else 0f)
                            lineScaleAnim.snapTo(0.955f)
                            if (prefs.glassBlurEnabled) {
                                lineBlurAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 410, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                        LaunchedEffect(key) {
                            lineScaleAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }

                        val words = activeLine?.words.orEmpty()
                        val useWordByWord = prefs.wordByWordEnabled &&
                            words.isNotEmpty() &&
                            doc?.syncType == LyricSyncType.SYNCED &&
                            (snap.isPlaying || !isPreview || idx >= 0)

                        if (useWordByWord) {
                            WordByWordLine(
                                words = words,
                                effectivePosMs = liveEffectivePosMs,
                                previewMode = isPreview && !snap.isPlaying,
                                fontFamily = fontFamily,
                                activeColor = activeColor,
                                baseSizeSp = baseSizeSp,
                                alignment = prefs.alignment,
                                glassBlurEnabled = prefs.glassBlurEnabled,
                                entryBlurDp = lineBlurAnim.value,
                                entryScale = lineScaleAnim.value,
                            )
                        } else {
                            Text(
                                text = primaryText,
                                color = activeColor,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = baseSizeSp.sp,
                                lineHeight = (baseSizeSp * 1.26f).sp,
                                textAlign = textAlign,
                                maxLines = if (isCompactLandscape) 3 else 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = lineScaleAnim.value
                                        scaleY = lineScaleAnim.value
                                    }
                                    .then(
                                        if (lineBlurAnim.value > 0.25f) {
                                            Modifier.blur(
                                                radius = lineBlurAnim.value.dp,
                                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }

                    if (!secondaryText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(if (isPreview) 8.dp else if (isCompactLandscape) 12.dp else 18.dp))
                        AnimatedContent(
                            targetState = secondaryText,
                            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(220)) },
                            label = "aod_next_line"
                        ) { next ->
                            Text(
                                text = next,
                                color = LyricPast.copy(alpha = if (prefs.glassBlurEnabled) 0.44f else 0.58f),
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = (baseSizeSp * 0.70f).sp,
                                lineHeight = (baseSizeSp * 0.94f).sp,
                                textAlign = textAlign,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (prefs.glassBlurEnabled) {
                                            Modifier.blur(
                                                radius = 1.8.dp,
                                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackIntroGlassReveal(
    title: String,
    artist: String,
    fontFamily: FontFamily,
    activeColor: Color,
    baseSizeSp: Float,
    textAlign: TextAlign,
    horizontalAlign: Alignment.Horizontal,
    glassBlurEnabled: Boolean,
) {
    val blurRadius = remember(title, artist, glassBlurEnabled) {
        Animatable(if (glassBlurEnabled) 16f else 0f)
    }
    val alphaAnim = remember(title, artist) { Animatable(0f) }

    LaunchedEffect(title, artist, glassBlurEnabled) {
        alphaAnim.snapTo(0f)
        alphaAnim.animateTo(1f, tween(340))
    }
    LaunchedEffect(title, artist, glassBlurEnabled) {
        if (glassBlurEnabled) {
            blurRadius.snapTo(16f)
            blurRadius.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
        } else {
            blurRadius.snapTo(0f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alphaAnim.value)
            .then(
                if (blurRadius.value > 0.25f) {
                    Modifier.blur(blurRadius.value.dp, BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier
                }
            )
            .testTag("aod_track_intro"),
        horizontalAlignment = horizontalAlign,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = activeColor,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (baseSizeSp * 1.04f).sp,
            lineHeight = (baseSizeSp * 1.24f).sp,
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        if (artist.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = artist,
                color = LyricPast.copy(alpha = 0.72f),
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (baseSizeSp * 0.68f).sp,
                lineHeight = (baseSizeSp * 0.90f).sp,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordByWordLine(
    words: List<LyricWord>,
    effectivePosMs: Long,
    previewMode: Boolean,
    fontFamily: FontFamily,
    activeColor: Color,
    baseSizeSp: Float,
    alignment: LyricTextAlignment,
    glassBlurEnabled: Boolean,
    entryBlurDp: Float,
    entryScale: Float,
) {
    val flowArrangement = when (alignment) {
        LyricTextAlignment.CENTER -> Arrangement.Center
        LyricTextAlignment.START -> Arrangement.Start
        LyricTextAlignment.END -> Arrangement.End
    }

    val halfPoint = (words.size + 1) / 2

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = entryScale
                scaleY = entryScale
            }
            .then(
                if (entryBlurDp > 0.25f) {
                    Modifier.blur(entryBlurDp.dp, BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = flowArrangement,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        words.forEachIndexed { wordIdx, word ->
            val isReached = if (previewMode) {
                wordIdx < halfPoint
            } else {
                effectivePosMs >= word.startMs
            }
            val isCurrentlySinging = !previewMode &&
                effectivePosMs >= word.startMs &&
                effectivePosMs <= word.endMs

            val targetAlpha = if (isReached) 1f else 0.34f
            val wordAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                label = "word_alpha"
            )
            val targetBlur = when {
                !glassBlurEnabled -> 0f
                isReached -> 0f
                else -> 2.4f
            }
            val wordBlur by animateFloatAsState(
                targetValue = targetBlur,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "word_blur"
            )
            val targetLiftScale = if (isCurrentlySinging) 1.04f else 1.0f
            val wordScale by animateFloatAsState(
                targetValue = targetLiftScale,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "word_scale"
            )

            Text(
                text = if (wordIdx == words.lastIndex) word.text else "${word.text} ",
                color = activeColor.copy(alpha = wordAlpha),
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = baseSizeSp.sp,
                lineHeight = (baseSizeSp * 1.26f).sp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = wordScale
                        scaleY = wordScale
                    }
                    .then(
                        if (wordBlur > 0.2f) {
                            Modifier.blur(wordBlur.dp, BlurredEdgeTreatment.Unbounded)
                        } else {
                            Modifier
                        }
                    )
            )
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
