package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.LockscreenOrientation
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricSyncType
import com.example.media.LyricsStageState
import com.example.ui.LyricVerseUiState
import com.example.ui.components.DemoTransportBar
import com.example.ui.theme.StudioDimensions
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioRadii
import com.example.ui.theme.StudioSpacing
import com.example.ui.theme.StudioSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LockAodSettingsSection(
    uiState: LyricVerseUiState,
    onToggleAutoShowOnLock: (Boolean) -> Unit,
    onToggleWakeScreenOnLock: (Boolean) -> Unit,
    onSelectLockscreenOrientation: (LockscreenOrientation) -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onTogglePixelShift: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
) {
    val prefs = uiState.syncState.preferences
    val perms = uiState.permissions
    val haptics = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)) {
        // Section 1: Auto-Launch on Lock
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
            StudioSectionOverline(title = "Lock-Screen Behavior")
            StudioSwitchRow(
                title = "Auto-Show on Lock Screen",
                subtitle = "Opens AMOLED lyrics when locked during a song; skips podcasts, vlogs, or unavailable lyrics",
                checked = prefs.autoShowOnLockEnabled,
                onCheckedChange = onToggleAutoShowOnLock,
                testTag = "auto_show_lock_switch"
            )

            AnimatedVisibility(visible = prefs.autoShowOnLockEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
                    StudioSwitchRow(
                        title = "Wake Into Black AOD on Lock",
                        subtitle = "Brings up pure #000000 lyrics immediately when locked while playing",
                        checked = prefs.wakeScreenOnLock,
                        onCheckedChange = onToggleWakeScreenOnLock,
                        testTag = "wake_on_lock_switch"
                    )
                    StudioPermissionRow(
                        title = "Display Over Apps (Auto-Lock Trigger)",
                        subtitle = "Optional on Android 10+ so background players (Spotify, Apple Music) launch lock-screen lyrics automatically.",
                        granted = perms.overlayPermissionGranted,
                        buttonLabel = if (perms.overlayPermissionGranted) "Allowed" else "Allow",
                        testTag = "grant_overlay_button",
                        onClick = onOpenOverlaySettings,
                    )
                }
            }
        }

        StudioHairlineDivider()

        // Section 2: Orientation
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
            StudioSectionOverline(
                title = "Lockscreen Orientation",
                trailing = prefs.lockscreenOrientation.label
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                LockscreenOrientation.entries.forEach { orientation ->
                    StudioOptionTile(
                        label = orientation.label,
                        selected = prefs.lockscreenOrientation == orientation,
                        onClick = {
                            if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectLockscreenOrientation(orientation)
                        },
                        testTag = "orientation_${orientation.name.lowercase()}"
                    )
                }
            }
        }

        StudioHairlineDivider()

        // Section 3: OLED Protection & Tactile
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
            StudioSectionOverline(title = "OLED Protection & Tactile")
            StudioSwitchRow(
                title = "OLED Burn-In Pixel Shift",
                subtitle = "Gently drifts lyrics every 20s to protect AMOLED panels",
                checked = prefs.oledPixelShiftEnabled,
                onCheckedChange = onTogglePixelShift,
                testTag = "pixel_shift_switch"
            )
            StudioSwitchRow(
                title = "Keep Screen On While Playing",
                subtitle = "Allows screen sleep automatically when playback pauses or lyrics are unavailable",
                checked = prefs.keepScreenOnWhilePlaying,
                onCheckedChange = onToggleKeepScreenOn,
            )
            StudioSwitchRow(
                title = "Restrained Haptics",
                subtitle = "Subtle tactile confirmation on Show Lyrics and design adjustments",
                checked = prefs.hapticsEnabled,
                onCheckedChange = onToggleHaptics,
                testTag = "haptics_switch"
            )
        }
    }
}

@Composable
internal fun SourcesCalibrationSection(
    uiState: LyricVerseUiState,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onLoadManualDocument: (LyricDocument) -> Unit,
    onStartDemoTrack: (Int) -> Unit,
    onToggleDemoPlayPause: () -> Unit,
    onSeekDemo: (Long) -> Unit,
    onNextDemo: () -> Unit,
    onPrevDemo: () -> Unit,
    onSimulateMissingMetadata: () -> Unit,
    onOpenListenerSettings: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleTracking: (Boolean) -> Unit,
    onToggleLockScreenNotif: (Boolean) -> Unit,
    onToggleOnlineLookup: (Boolean) -> Unit,
    onToggleOfflineCache: (Boolean) -> Unit,
    onClearCache: () -> Unit,
) {
    val perms = uiState.permissions
    val prefs = uiState.syncState.preferences
    val sync = uiState.syncState
    val snap = sync.snapshot
    val diag = uiState.diagnostics
    val library = uiState.searchLibrary
    val cacheCount = library.cachedEntries.size

    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)) {
        // Section 1: Manual Track Lookup
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
            StudioSectionOverline(title = "Manual Track Lookup")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = library.query,
                    onValueChange = onQueryChange,
                    label = { Text("Song title or artist") },
                    singleLine = true,
                    shape = RoundedCornerShape(StudioRadii.control),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = StudioOutline,
                        focusedContainerColor = StudioSurface,
                        unfocusedContainerColor = StudioSurface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input")
                )
                Button(
                    onClick = onSearchSubmit,
                    shape = RoundedCornerShape(StudioRadii.control),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .heightIn(min = 54.dp)
                        .testTag("search_button")
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search lyrics")
                }
            }

            when {
                library.isSearching -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = StudioSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(StudioSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Searching LRCLIB catalog…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                library.hasSearched && library.results.isEmpty() -> {
                    Text(
                        text = "No matching lyrics found. Try shortening the track title or artist name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                library.results.isNotEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
                        library.results.take(4).forEachIndexed { idx, doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = StudioSpacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = StudioSpacing.md)
                                ) {
                                    Text(
                                        text = doc.trackTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${doc.artistName} · ${if (doc.syncType == LyricSyncType.SYNCED) "Synced LRC" else "Plain"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onLoadManualDocument(doc) },
                                    shape = RoundedCornerShape(StudioRadii.control),
                                    border = BorderStroke(1.dp, StudioOutline),
                                    modifier = Modifier.heightIn(min = StudioDimensions.minTouchTarget)
                                ) {
                                    Text("Load", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (idx < library.results.take(4).lastIndex) {
                                StudioHairlineDivider()
                            }
                        }
                    }
                }
            }
        }

        StudioHairlineDivider()

        // Section 2: Demo Playback & Calibration (relocated from hero)
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
            StudioSectionOverline(
                title = "Demo & Calibration",
                trailing = if (snap.isDemoMode && snap.hasValidMetadata) "Demo Active" else null
            )
            Text(
                text = "Test synchronized karaoke rendering, album gradients, or missing-metadata blanking without an active music player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                itemsIndexed(uiState.demoCatalog) { index, track ->
                    val selected = snap.isDemoMode && snap.trackTitle == track.title
                    StudioOptionTile(
                        label = "${track.title} (${if (track.syncedLrc != null) "Synced" else "Plain"})",
                        selected = selected,
                        onClick = { onStartDemoTrack(index) },
                        testTag = "demo_chip_$index",
                        leading = {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            if (snap.isDemoMode && snap.hasValidMetadata) {
                DemoTransportBar(
                    trackTitle = snap.trackTitle,
                    artistName = snap.artistName,
                    isPlaying = snap.isPlaying,
                    positionMs = sync.livePositionMs,
                    durationMs = snap.durationMs,
                    hapticsEnabled = prefs.hapticsEnabled,
                    onPrevious = onPrevDemo,
                    onTogglePlayPause = onToggleDemoPlayPause,
                    onNext = onNextDemo,
                    onSeek = onSeekDemo,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = StudioSpacing.md)) {
                    Text(
                        text = "Sessions: ${diag.activeSessionsCount} · ${diag.selectedAppLabel ?: "None"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (sync.lyricsState is LyricsStageState.Unavailable) {
                        Text(
                            text = "Status: ${sync.lyricsState.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                OutlinedButton(
                    onClick = onSimulateMissingMetadata,
                    shape = RoundedCornerShape(StudioRadii.control),
                    border = BorderStroke(1.dp, StudioOutline),
                    modifier = Modifier
                        .heightIn(min = StudioDimensions.minTouchTarget)
                        .testTag("simulate_missing_metadata_button")
                ) {
                    Text("Test Missing Metadata", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        StudioHairlineDivider()

        // Section 3: Music Access, Permissions & Cache
        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
            StudioSectionOverline(title = "Sources, Permissions & Cache")
            StudioSwitchRow(
                title = "Track Compatible Music Apps",
                subtitle = "Reads active MediaSession metadata & album colors without altering audio",
                checked = prefs.trackingEnabled,
                onCheckedChange = onToggleTracking,
                testTag = "tracking_switch"
            )
            StudioPermissionRow(
                title = "Notification Listener Access",
                subtitle = "Required to detect songs in Spotify, Apple Music, or local players. Revoke anytime in Settings.",
                granted = perms.notificationListenerGranted,
                buttonLabel = if (perms.notificationListenerGranted) "Manage" else "Grant Access",
                testTag = "grant_listener_button",
                onClick = onOpenListenerSettings,
            )
            StudioSwitchRow(
                title = "Minimal Lock-Screen Notification",
                subtitle = "Optional ongoing notification showing active lyric lines on supported lock screens",
                checked = prefs.lockScreenNotificationEnabled,
                onCheckedChange = onToggleLockScreenNotif,
                testTag = "lock_notif_switch"
            )
            if (!perms.postNotificationsGranted) {
                StudioPermissionRow(
                    title = "Post Notifications (Android 13+)",
                    subtitle = "Needed for the ongoing Lock-Screen Companion notification and Auto-Show status.",
                    granted = false,
                    buttonLabel = "Allow",
                    testTag = "grant_notifications_button",
                    onClick = onRequestPostNotifications,
                )
            }
            StudioSwitchRow(
                title = "LRCLIB Online Lookup",
                subtitle = "Sends only track title, artist, album & duration (no account or audio)",
                checked = prefs.onlineLookupEnabled,
                onCheckedChange = onToggleOnlineLookup,
                testTag = "online_lookup_switch"
            )
            StudioSwitchRow(
                title = "Permitted Offline Lyrics Cache",
                subtitle = "Caches matches locally for 14 days (3 days for misses) when allowed",
                checked = prefs.offlineCacheEnabled,
                onCheckedChange = onToggleOfflineCache,
                testTag = "offline_cache_switch"
            )

            Spacer(modifier = Modifier.height(StudioSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = onClearCache,
                    enabled = cacheCount > 0,
                    shape = RoundedCornerShape(StudioRadii.control),
                    border = BorderStroke(1.dp, StudioOutline),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = StudioDimensions.minTouchTarget)
                        .testTag("clear_cache_button")
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (cacheCount == 0) "Cache Empty" else "Clear Cache ($cacheCount)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    shape = RoundedCornerShape(StudioRadii.control),
                    border = BorderStroke(1.dp, StudioOutline),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = StudioDimensions.minTouchTarget)
                        .testTag("battery_settings_button")
                ) {
                    Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Battery", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        StudioHairlineDivider()

        // Section 4: System & Android Auto Behavior Note
        StudioDiagnosticNote(
            title = "Lock-Screen & Car Audio Behavior",
            paragraphs = listOf(
                "Android reserves hardware Doze AOD for SystemUI and blocks floating windows over the keyguard. With Auto-Show on Lock Screen enabled, LyricVerse opens its pure #000000 screen over the lock screen while keeping your unlock security intact.",
                "When audio plays through Android Auto or Bluetooth, LyricVerse keeps tracking the active music player on your phone lock screen without altering car head-unit playback."
            )
        )
    }
}
