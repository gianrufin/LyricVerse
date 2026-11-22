package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.LyricSyncType
import com.example.domain.model.PlaybackSessionStatus
import com.example.media.LyricsStageState
import com.example.ui.LyricVerseUiState
import com.example.ui.components.DemoTransportBar
import com.example.ui.components.KaraokeSyncedLyricList
import com.example.ui.components.StageAtmosphereBackdrop
import com.example.ui.components.SyncOffsetCalibrationRow
import com.example.ui.components.formatTimestamp
import com.example.ui.components.resolveActiveColor
import com.example.ui.components.toComposeFontFamily
import com.example.ui.components.toComposeTextAlign
import com.example.ui.theme.LyricActive
import com.example.ui.theme.LyricUpcoming
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageViolet
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextMuted

@Composable
fun StageScreen(
    uiState: LyricVerseUiState,
    isExpandedLayout: Boolean,
    onStartDemoTrack: (Int) -> Unit,
    onToggleDemoPlayPause: () -> Unit,
    onSeekDemo: (Long) -> Unit,
    onNextDemo: () -> Unit,
    onPrevDemo: () -> Unit,
    onNudgeOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onRetryLookup: () -> Unit,
    onToggleOverlay: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sync = uiState.syncState
    val prefs = sync.preferences

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("stage_screen")
    ) {
        StageAtmosphereBackdrop(
            backgroundStyle = prefs.backgroundStyle,
            modifier = Modifier.fillMaxSize()
        )

        if (isExpandedLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                ) {
                    LyricsStageCanvas(
                        uiState = uiState,
                        onStartDemoTrack = onStartDemoTrack,
                        onSeekDemo = onSeekDemo,
                        onRetryLookup = onRetryLookup,
                        onNavigateToPermissions = onNavigateToPermissions,
                        onNavigateToSearch = onNavigateToSearch,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TrackHeaderCard(
                        uiState = uiState,
                        onToggleOverlay = onToggleOverlay,
                    )
                    DemoQuickCatalogStrip(
                        uiState = uiState,
                        onSelectTrack = onStartDemoTrack,
                    )
                    StageControlsBlock(
                        uiState = uiState,
                        onToggleDemoPlayPause = onToggleDemoPlayPause,
                        onSeekDemo = onSeekDemo,
                        onNextDemo = onNextDemo,
                        onPrevDemo = onPrevDemo,
                        onNudgeOffset = onNudgeOffset,
                        onResetOffset = onResetOffset,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrackHeaderCard(
                    uiState = uiState,
                    onToggleOverlay = onToggleOverlay,
                )

                if (!sync.snapshot.hasValidMetadata || sync.snapshot.isDemoMode) {
                    DemoQuickCatalogStrip(
                        uiState = uiState,
                        onSelectTrack = onStartDemoTrack,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LyricsStageCanvas(
                        uiState = uiState,
                        onStartDemoTrack = onStartDemoTrack,
                        onSeekDemo = onSeekDemo,
                        onRetryLookup = onRetryLookup,
                        onNavigateToPermissions = onNavigateToPermissions,
                        onNavigateToSearch = onNavigateToSearch,
                    )
                }

                StageControlsBlock(
                    uiState = uiState,
                    onToggleDemoPlayPause = onToggleDemoPlayPause,
                    onSeekDemo = onSeekDemo,
                    onNextDemo = onNextDemo,
                    onPrevDemo = onPrevDemo,
                    onNudgeOffset = onNudgeOffset,
                    onResetOffset = onResetOffset,
                )
            }
        }
    }
}

@Composable
private fun TrackHeaderCard(
    uiState: LyricVerseUiState,
    onToggleOverlay: () -> Unit,
) {
    val sync = uiState.syncState
    val snap = sync.snapshot
    val haptics = LocalHapticFeedback.current
    val doc = (sync.lyricsState as? LyricsStageState.Loaded)?.document

    Surface(
        color = StageSurfaceElevated.copy(alpha = 0.78f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (snap.isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                snap.hasValidMetadata -> snap.trackTitle
                                doc != null -> doc.trackTitle
                                snap.status == PlaybackSessionStatus.NO_PERMISSION -> "Connect a Music Player"
                                snap.status == PlaybackSessionStatus.METADATA_MISSING -> "Metadata Unavailable"
                                snap.status == PlaybackSessionStatus.SESSION_ENDED -> "Playback Ended"
                                else -> "LyricVerse Stage"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = LyricActive,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                snap.hasValidMetadata -> "${snap.artistName} · ${snap.sourceAppLabel}"
                                doc != null -> "${doc.artistName} · Manual Stage"
                                snap.status == PlaybackSessionStatus.NO_PERMISSION -> "Grant notification access or try a demo track"
                                snap.status == PlaybackSessionStatus.METADATA_MISSING -> "Active app omitted title or artist"
                                else -> "Play music in another app or start Demo Mode"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = {
                        if (sync.preferences.hapticsEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onToggleOverlay()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("overlay_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sync.overlayVisible) "Hide Pill" else "Overlay",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (doc != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val syncBadgeText = when (doc.syncType) {
                        LyricSyncType.SYNCED -> "TIME-SYNCED LRC"
                        LyricSyncType.PLAIN -> "PLAIN LYRICS (UN-SYNCED)"
                        LyricSyncType.INSTRUMENTAL -> "INSTRUMENTAL"
                        LyricSyncType.NONE -> "NO LYRICS"
                    }
                    val badgeColor = when (doc.syncType) {
                        LyricSyncType.SYNCED -> SuccessMint
                        LyricSyncType.PLAIN -> StageAmber
                        else -> TextMuted
                    }
                    Surface(
                        color = badgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = syncBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = StageObsidian.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (doc.fromCache) "${doc.providerDisplayName} · Cached" else doc.providerDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoQuickCatalogStrip(
    uiState: LyricVerseUiState,
    onSelectTrack: (Int) -> Unit,
) {
    val activeTitle = uiState.syncState.snapshot.trackTitle
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(uiState.demoCatalog) { index, track ->
            val selected = uiState.syncState.snapshot.isDemoMode && activeTitle == track.title
            FilterChip(
                selected = selected,
                onClick = { onSelectTrack(index) },
                label = { Text("${track.title} (${if (track.syncedLrc != null) "Synced" else "Plain"})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("demo_chip_$index")
            )
        }
    }
}

@Composable
private fun LyricsStageCanvas(
    uiState: LyricVerseUiState,
    onStartDemoTrack: (Int) -> Unit,
    onSeekDemo: (Long) -> Unit,
    onRetryLookup: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSearch: () -> Unit,
) {
    val sync = uiState.syncState
    val prefs = sync.preferences
    val snap = sync.snapshot

    when (val lyricsState = sync.lyricsState) {
        is LyricsStageState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Matching synchronized lyrics…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }

        is LyricsStageState.Loaded -> {
            val doc = lyricsState.document
            when (doc.syncType) {
                LyricSyncType.SYNCED -> {
                    KaraokeSyncedLyricList(
                        lines = doc.lines,
                        activeLineIndex = sync.activeLineIndex,
                        preferences = prefs,
                        isDemoMode = snap.isDemoMode,
                        onLineTapped = { tapped ->
                            if (snap.isDemoMode) onSeekDemo(tapped.timestampMs)
                        }
                    )
                }

                LyricSyncType.PLAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Surface(
                            color = StageAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Plain lyrics only — the provider did not supply timestamps for this track, so karaoke auto-scroll is disabled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StageAmber,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = doc.plainText,
                            color = prefs.colorPreset.resolveActiveColor().copy(alpha = prefs.stageOpacity),
                            fontFamily = prefs.fontFamily.toComposeFontFamily(),
                            fontSize = (prefs.fontSizeSp * 0.85f).sp,
                            lineHeight = (prefs.fontSizeSp * 1.30f).sp,
                            textAlign = prefs.alignment.toComposeTextAlign(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                LyricSyncType.INSTRUMENTAL -> {
                    StageEmptyStateCard(
                        icon = Icons.Rounded.MusicNote,
                        title = "Instrumental Track",
                        body = "This recording is marked as instrumental by the lyrics catalog.",
                        primaryLabel = "Search Manually",
                        onPrimaryClick = onNavigateToSearch,
                    )
                }

                LyricSyncType.NONE -> {
                    StageEmptyStateCard(
                        icon = Icons.Rounded.Lyrics,
                        title = "No Lyrics Available",
                        body = "No synced or plain lyrics were returned for this recording.",
                        primaryLabel = "Search Manually",
                        onPrimaryClick = onNavigateToSearch,
                    )
                }
            }
        }

        is LyricsStageState.Unavailable -> {
            StageEmptyStateCard(
                icon = Icons.Rounded.WarningAmber,
                title = "Lyrics Not Found",
                body = lyricsState.reason,
                primaryLabel = "Search Manually",
                onPrimaryClick = onNavigateToSearch,
                secondaryLabel = "Retry Lookup",
                onSecondaryClick = onRetryLookup,
            )
        }

        is LyricsStageState.Idle -> {
            when (snap.status) {
                PlaybackSessionStatus.NO_PERMISSION -> {
                    StageEmptyStateCard(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Ready for Your Music",
                        body = "Grant Notification Access so LyricVerse can read active track metadata from compatible music players—or launch a curated Demo LRC track immediately.",
                        primaryLabel = "Try Demo Track",
                        onPrimaryClick = { onStartDemoTrack(0) },
                        secondaryLabel = "Setup Player Access",
                        onSecondaryClick = onNavigateToPermissions,
                    )
                }

                PlaybackSessionStatus.METADATA_MISSING -> {
                    StageEmptyStateCard(
                        icon = Icons.Rounded.WarningAmber,
                        title = "Track Metadata Missing",
                        body = "The active media session did not publish a song title and artist. LyricVerse never records raw audio by default, so please search manually or switch to a compatible player.",
                        primaryLabel = "Search Manually",
                        onPrimaryClick = onNavigateToSearch,
                        secondaryLabel = "Try Demo Track",
                        onSecondaryClick = { onStartDemoTrack(0) },
                    )
                }

                PlaybackSessionStatus.SESSION_ENDED -> {
                    StageEmptyStateCard(
                        icon = Icons.Rounded.Lyrics,
                        title = "Session Stopped",
                        body = "Playback tracking paused cleanly when the session ended. Start music in your player or play a demo track anytime.",
                        primaryLabel = "Play Demo Track",
                        onPrimaryClick = { onStartDemoTrack(0) },
                    )
                }

                else -> {
                    StageEmptyStateCard(
                        icon = Icons.Rounded.Lyrics,
                        title = "Waiting for Music Playback",
                        body = "Start playing a song in Spotify, Apple Music, YouTube Music, or a local player—or tap below to explore synchronized karaoke with our built-in Demo LRC catalog.",
                        primaryLabel = "Start Demo Karaoke",
                        onPrimaryClick = { onStartDemoTrack(0) },
                        secondaryLabel = "Search Catalog",
                        onSecondaryClick = onNavigateToSearch,
                    )
                }
            }
        }
    }
}

@Composable
private fun StageEmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = StageSurfaceElevated.copy(alpha = 0.85f),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = StageViolet.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = StageViolet,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = LyricActive
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LyricUpcoming.copy(alpha = 0.82f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPrimaryClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(primaryLabel)
                    }
                    if (secondaryLabel != null && onSecondaryClick != null) {
                        OutlinedButton(onClick = onSecondaryClick) {
                            Icon(
                                imageVector = if (secondaryLabel.contains("Search")) Icons.Rounded.Search else Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(secondaryLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageControlsBlock(
    uiState: LyricVerseUiState,
    onToggleDemoPlayPause: () -> Unit,
    onSeekDemo: (Long) -> Unit,
    onNextDemo: () -> Unit,
    onPrevDemo: () -> Unit,
    onNudgeOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
) {
    val sync = uiState.syncState
    val snap = sync.snapshot
    val doc = (sync.lyricsState as? LyricsStageState.Loaded)?.document

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (doc?.syncType == LyricSyncType.SYNCED) {
            SyncOffsetCalibrationRow(
                offsetMs = sync.preferences.syncOffsetMs,
                hapticsEnabled = sync.preferences.hapticsEnabled,
                onNudge = onNudgeOffset,
                onReset = onResetOffset
            )
        }

        if (snap.isDemoMode && snap.hasValidMetadata) {
            DemoTransportBar(
                isPlaying = snap.isPlaying,
                positionMs = sync.livePositionMs,
                durationMs = snap.durationMs,
                hapticsEnabled = sync.preferences.hapticsEnabled,
                onPrevious = onPrevDemo,
                onTogglePlayPause = onToggleDemoPlayPause,
                onNext = onNextDemo,
                onSeek = onSeekDemo,
            )
        } else if (snap.status == PlaybackSessionStatus.ACTIVE_EXTERNAL && snap.durationMs > 0L) {
            // Read-only position mirror for external player (never controls external audio)
            Surface(
                color = StageSurfaceElevated.copy(alpha = 0.78f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    val progress = (sync.livePositionMs.toFloat() / snap.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = StageObsidian
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatTimestamp(sync.livePositionMs)} · Read-only mirror",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = formatTimestamp(snap.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
