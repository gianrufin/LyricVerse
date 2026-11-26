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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.example.domain.model.LyricTransitionStyle
import com.example.domain.model.LyricWord
import com.example.domain.model.PlaybackSessionStatus
import com.example.domain.model.PlaybackSnapshot
import com.example.media.AlbumArtColorExtractor
import com.example.media.CompanionSyncState
import com.example.media.LyricsStageState
import com.example.media.PlaybackClockExtrapolator
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.CormorantGaramondFamily
import com.example.ui.theme.DmSansFontFamily
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.InstrumentSerifFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LyricPast
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SoraFontFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StudioDimensions
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioRadii
import com.example.ui.theme.StudioSpacing
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.SyneFontFamily
import com.example.ui.theme.WarmIvory
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

fun KaraokeFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    KaraokeFontFamily.PLUS_JAKARTA_SANS -> PlusJakartaSansFamily
    KaraokeFontFamily.OUTFIT -> OutfitFontFamily
    KaraokeFontFamily.SPACE_GROTESK -> SpaceGroteskFamily
    KaraokeFontFamily.SORA -> SoraFontFamily
    KaraokeFontFamily.DM_SANS -> DmSansFontFamily
    KaraokeFontFamily.JETBRAINS_MONO -> JetBrainsMonoFamily
    KaraokeFontFamily.INSTRUMENT_SERIF -> InstrumentSerifFamily
    KaraokeFontFamily.CORMORANT_GARAMOND -> CormorantGaramondFamily
    KaraokeFontFamily.PLAYFAIR_DISPLAY -> PlayfairDisplayFamily
    KaraokeFontFamily.SYNE -> SyneFontFamily
    KaraokeFontFamily.FRAUNCES -> FrauncesFontFamily
    KaraokeFontFamily.CINZEL -> CinzelFontFamily
}

fun LyricTextAlignment.toComposeTextAlign(): TextAlign = when (this) {
    LyricTextAlignment.START -> TextAlign.Start
    LyricTextAlignment.CENTER -> TextAlign.Center
    LyricTextAlignment.END -> TextAlign.End
}

fun resolveAlbumLightGradient(snapshot: PlaybackSnapshot): List<Color> {
    val raw = snapshot.albumLightColors.takeIf { it.size >= 2 }
        ?: AlbumArtColorExtractor.fallbackGradientForSignature(snapshot.trackSignatureKey)
    return raw.map { Color(it) }
}

@Composable
fun LyricColorPreset.resolveActiveColor(snapshot: PlaybackSnapshot): Color = when (this) {
    LyricColorPreset.ALBUM_ART -> resolveAlbumLightGradient(snapshot).first()
    LyricColorPreset.STAGE_WHITE -> Color.White
    LyricColorPreset.WARM_AMBER -> StageAmber
    LyricColorPreset.WARM_IVORY -> WarmIvory
    LyricColorPreset.STAGE_CYAN -> StageCyan
    LyricColorPreset.DYNAMIC_PRIMARY -> MaterialTheme.colorScheme.primary
}

@Composable
fun LyricColorPreset.resolveActiveBrush(snapshot: PlaybackSnapshot): Brush? {
    if (this != LyricColorPreset.ALBUM_ART) return null
    val stops = resolveAlbumLightGradient(snapshot)
    return Brush.linearGradient(colors = stops)
}

/**
 * Minimal centered karaoke view for pure #000000 AMOLED lock-screen & live settings preview.
 * Supports brief Song Title + Artist intro on track start, album artwork light gradient typography,
 * customizable transitions, Apple-style frosted glass blur, word-by-word reveal, and landscape sizing.
 */
@Composable
fun AodTwoLineLyricsView(
    state: CompanionSyncState,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    introTriggerNonce: Int = 0,
    transitionReplayNonce: Int = 0,
) {
    val prefs = state.preferences
    val fontFamily = prefs.fontFamily.toComposeFontFamily()
    val textAlign = prefs.alignment.toComposeTextAlign()
    val snap = state.snapshot
    val activeColor = prefs.colorPreset.resolveActiveColor(snap)
    val activeBrush = prefs.colorPreset.resolveActiveBrush(snap)
    val albumGradientStops = remember(snap.albumLightColors, snap.trackSignatureKey) {
        resolveAlbumLightGradient(snap)
    }
    val doc = (state.lyricsState as? LyricsStageState.Loaded)?.document
    val lines = doc?.lines.orEmpty()
    val idx = state.activeLineIndex
    val speedMult = prefs.transitionSpeed.multiplier

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

    // Track title + artist intro gate (only triggers when displayable lyrics exist; never flashes for vlogs/podcasts)
    val trackTitle = snap.trackTitle.ifBlank { doc?.trackTitle.orEmpty() }
    val artistName = snap.artistName.ifBlank { doc?.artistName.orEmpty() }
    val trackIdentityKey = if (trackTitle.isNotBlank()) "$trackTitle|$artistName" else ""
    var showingIntro by remember { mutableStateOf(false) }

    LaunchedEffect(
        trackIdentityKey,
        state.hasDisplayableLyrics,
        prefs.showTrackIntroOnStart,
        isPreview,
        introTriggerNonce
    ) {
        val canShowIntro = prefs.showTrackIntroOnStart &&
            trackIdentityKey.isNotBlank() &&
            state.hasDisplayableLyrics &&
            (!isPreview || introTriggerNonce > 0)
        if (canShowIntro) {
            showingIntro = true
            delay(2400L)
            showingIntro = false
        } else {
            showingIntro = false
        }
    }

    // Pure #000000 silent blank canvas on the full-screen AMOLED view whenever no lyrics are displayable
    // (covers Lyrics Unavailable, YouTube vlogs, podcasts, Loading, Idle, Instrumental, and missing metadata).
    if (!isPreview && !state.hasDisplayableLyrics) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .testTag("aod_blank_state")
        )
        return
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
                val dur = (320 * speedMult).roundToInt().coerceIn(120, 600)
                fadeIn(tween(dur)) togetherWith fadeOut(tween((dur * 0.85f).roundToInt()))
            },
            label = "aod_intro_vs_lyrics"
        ) { phase ->
            if (phase.startsWith("INTRO:")) {
                TrackIntroGlassReveal(
                    title = trackTitle,
                    artist = artistName,
                    fontFamily = fontFamily,
                    activeColor = activeColor,
                    activeBrush = activeBrush,
                    baseSizeSp = baseSizeSp,
                    textAlign = textAlign,
                    horizontalAlign = horizontalAlign,
                    glassBlurEnabled = prefs.glassBlurEnabled,
                    speedMultiplier = speedMult,
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
                    val transitionKey = "${activeLine?.index ?: -1}:$primaryText:$transitionReplayNonce:${prefs.transitionStyle.name}"
                    AnimatedContent(
                        targetState = transitionKey,
                        transitionSpec = {
                            val dur = (360 * speedMult).roundToInt().coerceIn(90, 700)
                            when (prefs.transitionStyle) {
                                LyricTransitionStyle.APPLE_GLASS -> {
                                    (fadeIn(tween(dur, easing = FastOutSlowInEasing)) +
                                        slideInVertically(tween(dur)) { it / 6 }) togetherWith
                                        (fadeOut(tween((dur * 0.75f).roundToInt())) +
                                            slideOutVertically(tween(dur)) { -it / 6 })
                                }
                                LyricTransitionStyle.STAGE_SLIDE -> {
                                    (fadeIn(tween(dur)) +
                                        slideInVertically(
                                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                                        ) { it / 3 }) togetherWith
                                        (fadeOut(tween((dur * 0.7f).roundToInt())) +
                                            slideOutVertically(tween(dur)) { -it / 3 })
                                }
                                LyricTransitionStyle.SILK_FADE -> {
                                    fadeIn(tween(dur + 80, easing = FastOutSlowInEasing)) togetherWith
                                        fadeOut(tween(dur, easing = FastOutSlowInEasing))
                                }
                                LyricTransitionStyle.SPOTLIGHT_POP -> {
                                    (fadeIn(tween(dur)) +
                                        scaleIn(
                                            initialScale = 0.86f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )) togetherWith fadeOut(tween((dur * 0.65f).roundToInt()))
                                }
                                LyricTransitionStyle.TYPEWRITER_SNAP -> {
                                    fadeIn(tween(90)) togetherWith fadeOut(tween(70))
                                }
                            }
                        },
                        label = "aod_active_line"
                    ) { key ->
                        val initialBlur = when {
                            !prefs.glassBlurEnabled -> 0f
                            prefs.transitionStyle == LyricTransitionStyle.APPLE_GLASS -> 14f
                            prefs.transitionStyle == LyricTransitionStyle.SPOTLIGHT_POP -> 10f
                            prefs.transitionStyle == LyricTransitionStyle.SILK_FADE -> 6f
                            prefs.transitionStyle == LyricTransitionStyle.STAGE_SLIDE -> 4f
                            else -> 0f
                        }
                        val initialScale = when (prefs.transitionStyle) {
                            LyricTransitionStyle.SPOTLIGHT_POP -> 0.88f
                            LyricTransitionStyle.STAGE_SLIDE -> 0.93f
                            LyricTransitionStyle.APPLE_GLASS -> 0.955f
                            LyricTransitionStyle.SILK_FADE -> 0.985f
                            LyricTransitionStyle.TYPEWRITER_SNAP -> 1.0f
                        }

                        val lineBlurAnim = remember(key) { Animatable(initialBlur) }
                        val lineScaleAnim = remember(key) { Animatable(initialScale) }

                        LaunchedEffect(key) {
                            lineBlurAnim.snapTo(initialBlur)
                            if (initialBlur > 0f) {
                                val blurMs = (410 * speedMult).roundToInt().coerceIn(110, 750)
                                lineBlurAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = blurMs, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                        LaunchedEffect(key) {
                            lineScaleAnim.snapTo(initialScale)
                            if (initialScale != 1f) {
                                lineScaleAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = if (prefs.transitionStyle == LyricTransitionStyle.SPOTLIGHT_POP) {
                                            Spring.DampingRatioMediumBouncy
                                        } else {
                                            Spring.DampingRatioNoBouncy
                                        },
                                        stiffness = Spring.StiffnessMediumLow / speedMult.coerceIn(0.65f, 1.5f)
                                    )
                                )
                            }
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
                                useAlbumGradient = prefs.colorPreset == LyricColorPreset.ALBUM_ART,
                                albumGradientStops = albumGradientStops,
                                baseSizeSp = baseSizeSp,
                                alignment = prefs.alignment,
                                glassBlurEnabled = prefs.glassBlurEnabled && prefs.transitionStyle != LyricTransitionStyle.TYPEWRITER_SNAP,
                                speedMultiplier = speedMult,
                                entryBlurDp = lineBlurAnim.value,
                                entryScale = lineScaleAnim.value,
                            )
                        } else {
                            val lineTextStyle = if (activeBrush != null) {
                                TextStyle(
                                    brush = activeBrush,
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = baseSizeSp.sp,
                                    lineHeight = (baseSizeSp * 1.26f).sp,
                                    textAlign = textAlign,
                                )
                            } else {
                                TextStyle(
                                    color = activeColor,
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = baseSizeSp.sp,
                                    lineHeight = (baseSizeSp * 1.26f).sp,
                                    textAlign = textAlign,
                                )
                            }
                            Text(
                                text = primaryText,
                                style = lineTextStyle,
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
                        val nextTintColor = if (prefs.colorPreset == LyricColorPreset.ALBUM_ART) {
                            albumGradientStops.last().copy(alpha = if (prefs.glassBlurEnabled) 0.42f else 0.56f)
                        } else {
                            LyricPast.copy(alpha = if (prefs.glassBlurEnabled) 0.44f else 0.58f)
                        }
                        AnimatedContent(
                            targetState = secondaryText,
                            transitionSpec = {
                                val dur = (260 * speedMult).roundToInt().coerceIn(80, 500)
                                fadeIn(tween(dur)) togetherWith fadeOut(tween(dur))
                            },
                            label = "aod_next_line"
                        ) { next ->
                            Text(
                                text = next,
                                color = nextTintColor,
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
                                        if (prefs.glassBlurEnabled && prefs.transitionStyle != LyricTransitionStyle.TYPEWRITER_SNAP) {
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
    activeBrush: Brush?,
    baseSizeSp: Float,
    textAlign: TextAlign,
    horizontalAlign: Alignment.Horizontal,
    glassBlurEnabled: Boolean,
    speedMultiplier: Float,
) {
    val blurRadius = remember(title, artist, glassBlurEnabled) {
        Animatable(if (glassBlurEnabled) 16f else 0f)
    }
    val alphaAnim = remember(title, artist) { Animatable(0f) }

    LaunchedEffect(title, artist, glassBlurEnabled, speedMultiplier) {
        alphaAnim.snapTo(0f)
        alphaAnim.animateTo(1f, tween((340 * speedMultiplier).roundToInt().coerceIn(140, 600)))
    }
    LaunchedEffect(title, artist, glassBlurEnabled, speedMultiplier) {
        if (glassBlurEnabled) {
            blurRadius.snapTo(16f)
            blurRadius.animateTo(
                0f,
                tween((480 * speedMultiplier).roundToInt().coerceIn(180, 800), easing = FastOutSlowInEasing)
            )
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
        val introTitleStyle = if (activeBrush != null) {
            TextStyle(
                brush = activeBrush,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (baseSizeSp * 1.04f).sp,
                lineHeight = (baseSizeSp * 1.24f).sp,
                textAlign = textAlign,
            )
        } else {
            TextStyle(
                color = activeColor,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (baseSizeSp * 1.04f).sp,
                lineHeight = (baseSizeSp * 1.24f).sp,
                textAlign = textAlign,
            )
        }
        Text(
            text = title,
            style = introTitleStyle,
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
    useAlbumGradient: Boolean,
    albumGradientStops: List<Color>,
    baseSizeSp: Float,
    alignment: LyricTextAlignment,
    glassBlurEnabled: Boolean,
    speedMultiplier: Float,
    entryBlurDp: Float,
    entryScale: Float,
) {
    val flowArrangement = when (alignment) {
        LyricTextAlignment.CENTER -> Arrangement.Center
        LyricTextAlignment.START -> Arrangement.Start
        LyricTextAlignment.END -> Arrangement.End
    }

    val halfPoint = (words.size + 1) / 2
    val wordAnimDurationMs = (190 * speedMultiplier).roundToInt().coerceIn(75, 360)

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
                animationSpec = tween(durationMillis = wordAnimDurationMs, easing = FastOutSlowInEasing),
                label = "word_alpha"
            )
            val targetBlur = when {
                !glassBlurEnabled -> 0f
                isReached -> 0f
                else -> 2.4f
            }
            val wordBlur by animateFloatAsState(
                targetValue = targetBlur,
                animationSpec = tween(durationMillis = (wordAnimDurationMs * 1.15f).roundToInt(), easing = FastOutSlowInEasing),
                label = "word_blur"
            )
            val targetLiftScale = if (isCurrentlySinging) 1.04f else 1.0f
            val wordScale by animateFloatAsState(
                targetValue = targetLiftScale,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "word_scale"
            )

            val wordBaseColor = if (useAlbumGradient && albumGradientStops.size >= 2) {
                val fraction = if (words.size <= 1) 0f else wordIdx.toFloat() / words.lastIndex.toFloat()
                sampleMultiStopGradient(albumGradientStops, fraction)
            } else {
                activeColor
            }

            val wordBrush = if (useAlbumGradient && albumGradientStops.size >= 2) {
                val nextFraction = if (words.size <= 1) 1f else ((wordIdx + 1).coerceAtMost(words.lastIndex)).toFloat() / words.lastIndex.toFloat()
                val endStop = sampleMultiStopGradient(albumGradientStops, nextFraction)
                Brush.linearGradient(colors = listOf(wordBaseColor, endStop))
            } else {
                null
            }

            val wordTextStyle = if (wordBrush != null) {
                TextStyle(
                    brush = wordBrush,
                    alpha = wordAlpha,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = baseSizeSp.sp,
                    lineHeight = (baseSizeSp * 1.26f).sp,
                )
            } else {
                TextStyle(
                    color = wordBaseColor.copy(alpha = wordAlpha),
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = baseSizeSp.sp,
                    lineHeight = (baseSizeSp * 1.26f).sp,
                )
            }

            Text(
                text = if (wordIdx == words.lastIndex) word.text else "${word.text} ",
                style = wordTextStyle,
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

private fun sampleMultiStopGradient(stops: List<Color>, fraction: Float): Color {
    if (stops.isEmpty()) return Color.White
    if (stops.size == 1) return stops[0]
    val clamped = fraction.coerceIn(0f, 1f)
    val scaled = clamped * (stops.size - 1)
    val idx = scaled.toInt().coerceIn(0, stops.size - 2)
    val localFrac = scaled - idx
    return lerp(stops[idx], stops[idx + 1], localFrac)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = StudioSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(StudioSpacing.md))
            Column {
                Text(
                    text = "Sync Offset",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (offsetMs == 0L) "0 ms (centered)" else "${if (offsetMs > 0) "+" else ""}$offsetMs ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (offsetMs == 0L) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StudioSpacing.xs)
        ) {
            if (offsetMs != 0L) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.sizeIn(minWidth = StudioDimensions.minTouchTarget, minHeight = StudioDimensions.minTouchTarget)
                ) {
                    Text("Reset", style = MaterialTheme.typography.labelMedium)
                }
            }
            FilledTonalIconButton(
                onClick = {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNudge(-100L)
                },
                shape = RoundedCornerShape(StudioRadii.control),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = StudioSurfaceElevated,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .size(StudioDimensions.minTouchTarget)
                    .testTag("offset_minus_button")
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Delay lyrics by 100ms")
            }
            FilledTonalIconButton(
                onClick = {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNudge(100L)
                },
                shape = RoundedCornerShape(StudioRadii.control),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = StudioSurfaceElevated,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .size(StudioDimensions.minTouchTarget)
                    .testTag("offset_plus_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Advance lyrics by 100ms")
            }
        }
    }
}

@Composable
fun DemoTransportBar(
    trackTitle: String,
    artistName: String,
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
        color = StudioSurface,
        border = BorderStroke(StudioDimensions.dividerThickness, HairlineDivider),
        shape = RoundedCornerShape(StudioRadii.frame),
    ) {
        Column(modifier = Modifier.padding(horizontal = StudioSpacing.lg, vertical = StudioSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artistName.isNotBlank()) {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "${formatTimestamp(positionMs)} / ${formatTimestamp(durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val maxDuration = durationMs.coerceAtLeast(1000L).toFloat()
            Slider(
                value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..maxDuration,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = StudioOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("demo_seek_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrevious()
                    },
                    shape = RoundedCornerShape(StudioRadii.control),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = StudioSurfaceElevated,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .size(StudioDimensions.minTouchTarget)
                        .testTag("demo_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous_demo)
                    )
                }

                Spacer(modifier = Modifier.width(StudioSpacing.md))

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
                        .size(StudioDimensions.minTouchTarget)
                        .clip(CircleShape)
                        .testTag("demo_play_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause_demo),
                    )
                }

                Spacer(modifier = Modifier.width(StudioSpacing.md))

                FilledTonalIconButton(
                    onClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNext()
                    },
                    shape = RoundedCornerShape(StudioRadii.control),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = StudioSurfaceElevated,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .size(StudioDimensions.minTouchTarget)
                        .testTag("demo_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.cd_next_demo)
                    )
                }
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
