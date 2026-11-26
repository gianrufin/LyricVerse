package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.LockscreenOrientation
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.LyricTransitionSpeed
import com.example.domain.model.LyricTransitionStyle
import com.example.media.LyricsStageState
import com.example.ui.LyricVerseUiState
import com.example.ui.components.AodTwoLineLyricsView
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StatusAttention
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StudioDimensions
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioRadii
import com.example.ui.theme.StudioSpacing
import com.example.ui.theme.StudioSurfaceElevated

enum class SettingsCategory(
    val label: String,
    val icon: ImageVector,
    val tag: String,
) {
    THEME("Theme", Icons.Rounded.Palette, "category_theme"),
    TRANSITIONS("Transitions", Icons.Rounded.AutoAwesome, "category_transitions"),
    LOCK_AOD("Lock & AOD", Icons.Rounded.Lock, "category_lock_aod"),
    SOURCES("Sources", Icons.Rounded.Tune, "category_sources"),
}

@Composable
fun HomeSettingsScreen(
    uiState: LyricVerseUiState,
    isExpandedLayout: Boolean,
    onShowLyricsClick: () -> Unit,
    onPinQuickTileClick: () -> Unit,
    onStartDemoTrack: (Int) -> Unit,
    onToggleDemoPlayPause: () -> Unit,
    onSeekDemo: (Long) -> Unit,
    onNextDemo: () -> Unit,
    onPrevDemo: () -> Unit,
    onSimulateMissingMetadata: () -> Unit,
    onSelectFontFamily: (KaraokeFontFamily) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onSelectColorPreset: (LyricColorPreset) -> Unit,
    onSelectAlignment: (LyricTextAlignment) -> Unit,
    onToggleNextLineVisible: (Boolean) -> Unit,
    onToggleWordByWord: (Boolean) -> Unit,
    onToggleGlassBlur: (Boolean) -> Unit,
    onSelectTransitionStyle: (LyricTransitionStyle) -> Unit,
    onSelectTransitionSpeed: (LyricTransitionSpeed) -> Unit,
    onToggleShowTrackIntro: (Boolean) -> Unit,
    onSelectLockscreenOrientation: (LockscreenOrientation) -> Unit,
    onToggleAutoShowOnLock: (Boolean) -> Unit,
    onToggleWakeScreenOnLock: (Boolean) -> Unit,
    onTogglePixelShift: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onNudgeOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onLoadManualDocument: (LyricDocument) -> Unit,
    onOpenListenerSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleTracking: (Boolean) -> Unit,
    onToggleLockScreenNotif: (Boolean) -> Unit,
    onToggleOnlineLookup: (Boolean) -> Unit,
    onToggleOfflineCache: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.THEME) }
    var previewTransitionNonce by remember { mutableIntStateOf(0) }

    if (isExpandedLayout) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(StudioObsidian)
                .testTag("home_settings_screen")
        ) {
            Column(
                modifier = Modifier
                    .weight(0.88f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = StudioSpacing.xxl, vertical = StudioSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)
            ) {
                DockedStageHeader(
                    uiState = uiState,
                    compactMode = false,
                    previewTransitionNonce = previewTransitionNonce,
                    onReplayTransition = { previewTransitionNonce++ },
                    onShowLyricsClick = onShowLyricsClick,
                    onPinQuickTileClick = onPinQuickTileClick,
                )
            }

            VerticalDivider(
                thickness = StudioDimensions.dividerThickness,
                color = HairlineDivider
            )

            Column(
                modifier = Modifier
                    .weight(1.18f)
                    .fillMaxHeight()
            ) {
                ArchitecturalTabStrip(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it },
                    horizontalPadding = StudioSpacing.xxl
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = StudioSpacing.xxl,
                        vertical = StudioSpacing.xl
                    ),
                ) {
                    item(key = selectedCategory.name) {
                        CategorySettingsBody(
                            selectedCategory = selectedCategory,
                            uiState = uiState,
                            onPreviewTransition = { previewTransitionNonce++ },
                            onStartDemoTrack = onStartDemoTrack,
                            onToggleDemoPlayPause = onToggleDemoPlayPause,
                            onSeekDemo = onSeekDemo,
                            onNextDemo = onNextDemo,
                            onPrevDemo = onPrevDemo,
                            onSimulateMissingMetadata = onSimulateMissingMetadata,
                            onSelectFontFamily = onSelectFontFamily,
                            onFontSizeChange = onFontSizeChange,
                            onSelectColorPreset = onSelectColorPreset,
                            onSelectAlignment = onSelectAlignment,
                            onToggleNextLineVisible = onToggleNextLineVisible,
                            onToggleWordByWord = onToggleWordByWord,
                            onToggleGlassBlur = onToggleGlassBlur,
                            onSelectTransitionStyle = onSelectTransitionStyle,
                            onSelectTransitionSpeed = onSelectTransitionSpeed,
                            onToggleShowTrackIntro = onToggleShowTrackIntro,
                            onSelectLockscreenOrientation = onSelectLockscreenOrientation,
                            onToggleAutoShowOnLock = onToggleAutoShowOnLock,
                            onToggleWakeScreenOnLock = onToggleWakeScreenOnLock,
                            onTogglePixelShift = onTogglePixelShift,
                            onToggleKeepScreenOn = onToggleKeepScreenOn,
                            onToggleDynamicColor = onToggleDynamicColor,
                            onToggleHaptics = onToggleHaptics,
                            onNudgeOffset = onNudgeOffset,
                            onResetOffset = onResetOffset,
                            onQueryChange = onQueryChange,
                            onSearchSubmit = onSearchSubmit,
                            onLoadManualDocument = onLoadManualDocument,
                            onOpenListenerSettings = onOpenListenerSettings,
                            onOpenOverlaySettings = onOpenOverlaySettings,
                            onRequestPostNotifications = onRequestPostNotifications,
                            onOpenBatterySettings = onOpenBatterySettings,
                            onToggleTracking = onToggleTracking,
                            onToggleLockScreenNotif = onToggleLockScreenNotif,
                            onToggleOnlineLookup = onToggleOnlineLookup,
                            onToggleOfflineCache = onToggleOfflineCache,
                            onClearCache = onClearCache,
                        )
                    }
                    item { Spacer(modifier = Modifier.height(StudioSpacing.xxl)) }
                }
            }
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(StudioObsidian)
                .testTag("home_settings_screen")
        ) {
            // In split-screen or very short heights, allow the whole surface to scroll
            // so the settings pane never collapses below usable height.
            val useUnifiedScroll = maxHeight < 520.dp
            val compactHeader = maxHeight < 700.dp

            if (useUnifiedScroll) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = StudioSpacing.xxl)
                ) {
                    item {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = StudioSpacing.xl,
                                vertical = StudioSpacing.md
                            ),
                            verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)
                        ) {
                            DockedStageHeader(
                                uiState = uiState,
                                compactMode = true,
                                previewTransitionNonce = previewTransitionNonce,
                                onReplayTransition = { previewTransitionNonce++ },
                                onShowLyricsClick = onShowLyricsClick,
                                onPinQuickTileClick = onPinQuickTileClick,
                            )
                        }
                    }
                    item {
                        ArchitecturalTabStrip(
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                            horizontalPadding = StudioSpacing.xl
                        )
                    }
                    item(key = selectedCategory.name) {
                        Box(
                            modifier = Modifier.padding(
                                horizontal = StudioSpacing.xl,
                                vertical = StudioSpacing.lg
                            )
                        ) {
                            CategorySettingsBody(
                                selectedCategory = selectedCategory,
                                uiState = uiState,
                                onPreviewTransition = { previewTransitionNonce++ },
                                onStartDemoTrack = onStartDemoTrack,
                                onToggleDemoPlayPause = onToggleDemoPlayPause,
                                onSeekDemo = onSeekDemo,
                                onNextDemo = onNextDemo,
                                onPrevDemo = onPrevDemo,
                                onSimulateMissingMetadata = onSimulateMissingMetadata,
                                onSelectFontFamily = onSelectFontFamily,
                                onFontSizeChange = onFontSizeChange,
                                onSelectColorPreset = onSelectColorPreset,
                                onSelectAlignment = onSelectAlignment,
                                onToggleNextLineVisible = onToggleNextLineVisible,
                                onToggleWordByWord = onToggleWordByWord,
                                onToggleGlassBlur = onToggleGlassBlur,
                                onSelectTransitionStyle = onSelectTransitionStyle,
                                onSelectTransitionSpeed = onSelectTransitionSpeed,
                                onToggleShowTrackIntro = onToggleShowTrackIntro,
                                onSelectLockscreenOrientation = onSelectLockscreenOrientation,
                                onToggleAutoShowOnLock = onToggleAutoShowOnLock,
                                onToggleWakeScreenOnLock = onToggleWakeScreenOnLock,
                                onTogglePixelShift = onTogglePixelShift,
                                onToggleKeepScreenOn = onToggleKeepScreenOn,
                                onToggleDynamicColor = onToggleDynamicColor,
                                onToggleHaptics = onToggleHaptics,
                                onNudgeOffset = onNudgeOffset,
                                onResetOffset = onResetOffset,
                                onQueryChange = onQueryChange,
                                onSearchSubmit = onSearchSubmit,
                                onLoadManualDocument = onLoadManualDocument,
                                onOpenListenerSettings = onOpenListenerSettings,
                                onOpenOverlaySettings = onOpenOverlaySettings,
                                onRequestPostNotifications = onRequestPostNotifications,
                                onOpenBatterySettings = onOpenBatterySettings,
                                onToggleTracking = onToggleTracking,
                                onToggleLockScreenNotif = onToggleLockScreenNotif,
                                onToggleOnlineLookup = onToggleOnlineLookup,
                                onToggleOfflineCache = onToggleOfflineCache,
                                onClearCache = onClearCache,
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sticky Docked Stage Preview + Primary Actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = StudioSpacing.xl,
                                end = StudioSpacing.xl,
                                top = StudioSpacing.md,
                                bottom = StudioSpacing.md
                            ),
                        verticalArrangement = Arrangement.spacedBy(if (compactHeader) StudioSpacing.sm else StudioSpacing.md)
                    ) {
                        DockedStageHeader(
                            uiState = uiState,
                            compactMode = compactHeader,
                            previewTransitionNonce = previewTransitionNonce,
                            onReplayTransition = { previewTransitionNonce++ },
                            onShowLyricsClick = onShowLyricsClick,
                            onPinQuickTileClick = onPinQuickTileClick,
                        )
                    }

                    ArchitecturalTabStrip(
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it },
                        horizontalPadding = StudioSpacing.xl
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            horizontal = StudioSpacing.xl,
                            vertical = StudioSpacing.lg
                        )
                    ) {
                        item(key = selectedCategory.name) {
                            CategorySettingsBody(
                                selectedCategory = selectedCategory,
                                uiState = uiState,
                                onPreviewTransition = { previewTransitionNonce++ },
                                onStartDemoTrack = onStartDemoTrack,
                                onToggleDemoPlayPause = onToggleDemoPlayPause,
                                onSeekDemo = onSeekDemo,
                                onNextDemo = onNextDemo,
                                onPrevDemo = onPrevDemo,
                                onSimulateMissingMetadata = onSimulateMissingMetadata,
                                onSelectFontFamily = onSelectFontFamily,
                                onFontSizeChange = onFontSizeChange,
                                onSelectColorPreset = onSelectColorPreset,
                                onSelectAlignment = onSelectAlignment,
                                onToggleNextLineVisible = onToggleNextLineVisible,
                                onToggleWordByWord = onToggleWordByWord,
                                onToggleGlassBlur = onToggleGlassBlur,
                                onSelectTransitionStyle = onSelectTransitionStyle,
                                onSelectTransitionSpeed = onSelectTransitionSpeed,
                                onToggleShowTrackIntro = onToggleShowTrackIntro,
                                onSelectLockscreenOrientation = onSelectLockscreenOrientation,
                                onToggleAutoShowOnLock = onToggleAutoShowOnLock,
                                onToggleWakeScreenOnLock = onToggleWakeScreenOnLock,
                                onTogglePixelShift = onTogglePixelShift,
                                onToggleKeepScreenOn = onToggleKeepScreenOn,
                                onToggleDynamicColor = onToggleDynamicColor,
                                onToggleHaptics = onToggleHaptics,
                                onNudgeOffset = onNudgeOffset,
                                onResetOffset = onResetOffset,
                                onQueryChange = onQueryChange,
                                onSearchSubmit = onSearchSubmit,
                                onLoadManualDocument = onLoadManualDocument,
                                onOpenListenerSettings = onOpenListenerSettings,
                                onOpenOverlaySettings = onOpenOverlaySettings,
                                onRequestPostNotifications = onRequestPostNotifications,
                                onOpenBatterySettings = onOpenBatterySettings,
                                onToggleTracking = onToggleTracking,
                                onToggleLockScreenNotif = onToggleLockScreenNotif,
                                onToggleOnlineLookup = onToggleOnlineLookup,
                                onToggleOfflineCache = onToggleOfflineCache,
                                onClearCache = onClearCache,
                            )
                        }
                        item { Spacer(modifier = Modifier.height(StudioSpacing.xl)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockedStageHeader(
    uiState: LyricVerseUiState,
    compactMode: Boolean,
    previewTransitionNonce: Int,
    onReplayTransition: () -> Unit,
    onShowLyricsClick: () -> Unit,
    onPinQuickTileClick: () -> Unit,
) {
    val sync = uiState.syncState
    val prefs = sync.preferences
    val snap = sync.snapshot
    val haptics = LocalHapticFeedback.current
    var previewIntroNonce by remember { mutableIntStateOf(0) }

    val (statusLabel, statusDotColor) = when {
        sync.lyricsState is LyricsStageState.Loading -> "Looking up…" to MaterialTheme.colorScheme.primary
        sync.lyricsState is LyricsStageState.Unavailable -> "AOD Blank · No Lyrics" to StatusError
        snap.hasValidMetadata && snap.isPlaying && sync.hasDisplayableLyrics -> "Lyrics Ready" to StatusReady
        prefs.autoShowOnLockEnabled -> "Auto-Lock Armed" to MaterialTheme.colorScheme.primary
        uiState.permissions.notificationListenerGranted -> "Listener Active" to StatusReady
        else -> "Listener Needed" to StatusAttention
    }

    // Studio Header Row (no pill badge bloat)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = StudioSpacing.md)) {
            Text(
                text = "LyricVerse",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = when {
                    snap.hasValidMetadata -> "${snap.trackTitle} · ${snap.artistName}"
                    else -> "Pure #000000 lock-screen lyric instrument"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }

    // Pure #000000 AMOLED Stage Preview
    Surface(
        color = StageBlack,
        shape = RoundedCornerShape(StudioRadii.frame),
        border = BorderStroke(StudioDimensions.dividerThickness, StudioOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = StudioSpacing.lg,
                        vertical = if (compactMode) StudioSpacing.lg else StudioSpacing.xxl
                    ),
                contentAlignment = Alignment.Center
            ) {
                AodTwoLineLyricsView(
                    state = sync,
                    isPreview = true,
                    introTriggerNonce = previewIntroNonce,
                    transitionReplayNonce = previewTransitionNonce,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StudioSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sync.lyricsState is LyricsStageState.Unavailable) {
                    Text(
                        text = "Lock screen stays blank #000000",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusError,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = StudioSpacing.sm)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onReplayTransition,
                        modifier = Modifier.heightIn(min = StudioDimensions.minTouchTarget)
                    ) {
                        Text(
                            text = "Preview Effect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (snap.hasValidMetadata && sync.hasDisplayableLyrics && prefs.showTrackIntroOnStart) {
                        TextButton(
                            onClick = { previewIntroNonce++ },
                            modifier = Modifier
                                .heightIn(min = StudioDimensions.minTouchTarget)
                                .testTag("preview_intro_button")
                        ) {
                            Text(
                                text = "Title Intro",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Primary & Secondary Launch Actions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
    ) {
        Button(
            onClick = {
                if (prefs.hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onShowLyricsClick()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(StudioRadii.frame),
            modifier = Modifier
                .weight(1.35f)
                .height(StudioDimensions.primaryActionHeight)
                .testTag("show_lyrics_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = stringResource(R.string.cd_show_lyrics),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(StudioSpacing.sm))
            Text(
                text = "Show Lyrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = {
                if (prefs.hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onPinQuickTileClick()
            },
            shape = RoundedCornerShape(StudioRadii.frame),
            border = BorderStroke(1.dp, StudioOutline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = StudioSurfaceElevated,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .weight(0.95f)
                .height(StudioDimensions.primaryActionHeight)
                .testTag("pin_qs_tile_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.AddToHomeScreen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AOD Tile",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArchitecturalTabStrip(
    selected: SettingsCategory,
    onSelect: (SettingsCategory) -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(StudioSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsCategory.entries.forEach { category ->
                val isSelected = selected == category
                Column(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(topStart = StudioRadii.micro, topEnd = StudioRadii.micro))
                        .clickable { onSelect(category) }
                        .padding(top = StudioSpacing.sm)
                        .testTag(category.tag),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = StudioSpacing.sm)
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(2.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }
        StudioHairlineDivider()
    }
}

@Composable
private fun CategorySettingsBody(
    selectedCategory: SettingsCategory,
    uiState: LyricVerseUiState,
    onPreviewTransition: () -> Unit,
    onStartDemoTrack: (Int) -> Unit,
    onToggleDemoPlayPause: () -> Unit,
    onSeekDemo: (Long) -> Unit,
    onNextDemo: () -> Unit,
    onPrevDemo: () -> Unit,
    onSimulateMissingMetadata: () -> Unit,
    onSelectFontFamily: (KaraokeFontFamily) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onSelectColorPreset: (LyricColorPreset) -> Unit,
    onSelectAlignment: (LyricTextAlignment) -> Unit,
    onToggleNextLineVisible: (Boolean) -> Unit,
    onToggleWordByWord: (Boolean) -> Unit,
    onToggleGlassBlur: (Boolean) -> Unit,
    onSelectTransitionStyle: (LyricTransitionStyle) -> Unit,
    onSelectTransitionSpeed: (LyricTransitionSpeed) -> Unit,
    onToggleShowTrackIntro: (Boolean) -> Unit,
    onSelectLockscreenOrientation: (LockscreenOrientation) -> Unit,
    onToggleAutoShowOnLock: (Boolean) -> Unit,
    onToggleWakeScreenOnLock: (Boolean) -> Unit,
    onTogglePixelShift: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onNudgeOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onLoadManualDocument: (LyricDocument) -> Unit,
    onOpenListenerSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleTracking: (Boolean) -> Unit,
    onToggleLockScreenNotif: (Boolean) -> Unit,
    onToggleOnlineLookup: (Boolean) -> Unit,
    onToggleOfflineCache: (Boolean) -> Unit,
    onClearCache: () -> Unit,
) {
    when (selectedCategory) {
        SettingsCategory.THEME -> ThemeSettingsSection(
            uiState = uiState,
            onSelectFontFamily = onSelectFontFamily,
            onFontSizeChange = onFontSizeChange,
            onSelectColorPreset = onSelectColorPreset,
            onSelectAlignment = onSelectAlignment,
            onToggleDynamicColor = onToggleDynamicColor,
        )
        SettingsCategory.TRANSITIONS -> TransitionsSettingsSection(
            uiState = uiState,
            onSelectTransitionStyle = { style ->
                onSelectTransitionStyle(style)
                onPreviewTransition()
            },
            onSelectTransitionSpeed = { speed ->
                onSelectTransitionSpeed(speed)
                onPreviewTransition()
            },
            onToggleWordByWord = onToggleWordByWord,
            onToggleGlassBlur = onToggleGlassBlur,
            onToggleShowTrackIntro = onToggleShowTrackIntro,
            onToggleNextLineVisible = onToggleNextLineVisible,
            onNudgeOffset = onNudgeOffset,
            onResetOffset = onResetOffset,
        )
        SettingsCategory.LOCK_AOD -> LockAodSettingsSection(
            uiState = uiState,
            onToggleAutoShowOnLock = onToggleAutoShowOnLock,
            onToggleWakeScreenOnLock = onToggleWakeScreenOnLock,
            onSelectLockscreenOrientation = onSelectLockscreenOrientation,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onTogglePixelShift = onTogglePixelShift,
            onToggleKeepScreenOn = onToggleKeepScreenOn,
            onToggleHaptics = onToggleHaptics,
        )
        SettingsCategory.SOURCES -> SourcesCalibrationSection(
            uiState = uiState,
            onQueryChange = onQueryChange,
            onSearchSubmit = onSearchSubmit,
            onLoadManualDocument = onLoadManualDocument,
            onStartDemoTrack = onStartDemoTrack,
            onToggleDemoPlayPause = onToggleDemoPlayPause,
            onSeekDemo = onSeekDemo,
            onNextDemo = onNextDemo,
            onPrevDemo = onPrevDemo,
            onSimulateMissingMetadata = onSimulateMissingMetadata,
            onOpenListenerSettings = onOpenListenerSettings,
            onRequestPostNotifications = onRequestPostNotifications,
            onOpenBatterySettings = onOpenBatterySettings,
            onToggleTracking = onToggleTracking,
            onToggleLockScreenNotif = onToggleLockScreenNotif,
            onToggleOnlineLookup = onToggleOnlineLookup,
            onToggleOfflineCache = onToggleOfflineCache,
            onClearCache = onClearCache,
        )
    }
}
