package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.FontCategory
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.LockscreenOrientation
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricSyncType
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.LyricTransitionSpeed
import com.example.domain.model.LyricTransitionStyle
import com.example.ui.LyricVerseUiState
import com.example.ui.components.AodTwoLineLyricsView
import com.example.ui.components.DemoTransportBar
import com.example.ui.components.SyncOffsetCalibrationRow
import com.example.ui.components.resolveAlbumLightGradient
import com.example.ui.components.toComposeFontFamily
import com.example.ui.theme.LyricActive
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageOutline
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageViolet
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import kotlin.math.roundToInt

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
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("home_settings_screen"),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HeroLaunchSection(
                    uiState = uiState,
                    previewTransitionNonce = previewTransitionNonce,
                    onReplayTransition = { previewTransitionNonce++ },
                    onShowLyricsClick = onShowLyricsClick,
                    onPinQuickTileClick = onPinQuickTileClick,
                    onStartDemoTrack = onStartDemoTrack,
                    onToggleDemoPlayPause = onToggleDemoPlayPause,
                    onSeekDemo = onSeekDemo,
                    onNextDemo = onNextDemo,
                    onPrevDemo = onPrevDemo,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1.18f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    CategorySelectorBar(
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                }
                item {
                    CategorySettingsBody(
                        selectedCategory = selectedCategory,
                        uiState = uiState,
                        onPreviewTransition = { previewTransitionNonce++ },
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
                        onSimulateMissingMetadata = onSimulateMissingMetadata,
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("home_settings_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeroLaunchSection(
                    uiState = uiState,
                    previewTransitionNonce = previewTransitionNonce,
                    onReplayTransition = { previewTransitionNonce++ },
                    onShowLyricsClick = onShowLyricsClick,
                    onPinQuickTileClick = onPinQuickTileClick,
                    onStartDemoTrack = onStartDemoTrack,
                    onToggleDemoPlayPause = onToggleDemoPlayPause,
                    onSeekDemo = onSeekDemo,
                    onNextDemo = onNextDemo,
                    onPrevDemo = onPrevDemo,
                )
            }
            item {
                CategorySelectorBar(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
            }
            item {
                CategorySettingsBody(
                    selectedCategory = selectedCategory,
                    uiState = uiState,
                    onPreviewTransition = { previewTransitionNonce++ },
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
                    onSimulateMissingMetadata = onSimulateMissingMetadata,
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CategorySelectorBar(
    selected: SettingsCategory,
    onSelect: (SettingsCategory) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(SettingsCategory.entries) { category ->
            val isSelected = selected == category
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(category) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier.testTag(category.tag)
            )
        }
    }
}

@Composable
private fun CategorySettingsBody(
    selectedCategory: SettingsCategory,
    uiState: LyricVerseUiState,
    onPreviewTransition: () -> Unit,
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
    onSimulateMissingMetadata: () -> Unit,
) {
    when (selectedCategory) {
        SettingsCategory.THEME -> ThemeDesignCategoryCard(
            uiState = uiState,
            onSelectFontFamily = onSelectFontFamily,
            onFontSizeChange = onFontSizeChange,
            onSelectColorPreset = onSelectColorPreset,
            onSelectAlignment = onSelectAlignment,
            onToggleDynamicColor = onToggleDynamicColor,
        )
        SettingsCategory.TRANSITIONS -> TransitionsEffectsCategoryCard(
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
        SettingsCategory.LOCK_AOD -> LockScreenAodCategoryCard(
            uiState = uiState,
            onToggleAutoShowOnLock = onToggleAutoShowOnLock,
            onToggleWakeScreenOnLock = onToggleWakeScreenOnLock,
            onSelectLockscreenOrientation = onSelectLockscreenOrientation,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onTogglePixelShift = onTogglePixelShift,
            onToggleKeepScreenOn = onToggleKeepScreenOn,
            onToggleHaptics = onToggleHaptics,
        )
        SettingsCategory.SOURCES -> SourcesPermissionsCategoryCard(
            uiState = uiState,
            onQueryChange = onQueryChange,
            onSearchSubmit = onSearchSubmit,
            onLoadManualDocument = onLoadManualDocument,
            onOpenListenerSettings = onOpenListenerSettings,
            onRequestPostNotifications = onRequestPostNotifications,
            onOpenBatterySettings = onOpenBatterySettings,
            onToggleTracking = onToggleTracking,
            onToggleLockScreenNotif = onToggleLockScreenNotif,
            onToggleOnlineLookup = onToggleOnlineLookup,
            onToggleOfflineCache = onToggleOfflineCache,
            onClearCache = onClearCache,
            onSimulateMissingMetadata = onSimulateMissingMetadata,
        )
    }
}

@Composable
private fun HeroLaunchSection(
    uiState: LyricVerseUiState,
    previewTransitionNonce: Int,
    onReplayTransition: () -> Unit,
    onShowLyricsClick: () -> Unit,
    onPinQuickTileClick: () -> Unit,
    onStartDemoTrack: (Int) -> Unit,
    onToggleDemoPlayPause: () -> Unit,
    onSeekDemo: (Long) -> Unit,
    onNextDemo: () -> Unit,
    onPrevDemo: () -> Unit,
) {
    val sync = uiState.syncState
    val prefs = sync.preferences
    val snap = sync.snapshot
    val haptics = LocalHapticFeedback.current
    var previewIntroNonce by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LyricVerse",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LyricActive
                )
                Text(
                    text = if (snap.hasValidMetadata) {
                        "${snap.trackTitle} · ${snap.artistName}"
                    } else {
                        "AMOLED lock-screen & karaoke settings"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = if (prefs.autoShowOnLockEnabled) {
                    StageViolet.copy(alpha = 0.2f)
                } else if (uiState.permissions.notificationListenerGranted) {
                    SuccessMint.copy(alpha = 0.14f)
                } else {
                    StageAmber.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        prefs.autoShowOnLockEnabled -> "AUTO LOCK AOD"
                        uiState.permissions.notificationListenerGranted -> "LISTENER ON"
                        else -> "DEMO READY"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        prefs.autoShowOnLockEnabled -> StageViolet
                        uiState.permissions.notificationListenerGranted -> SuccessMint
                        else -> StageAmber
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Pure #000000 AMOLED live preview card
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StageOutline, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
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
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReplayTransition) {
                        Text(
                            text = "Preview Effect",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    if (snap.hasValidMetadata && prefs.showTrackIntroOnStart) {
                        TextButton(
                            onClick = { previewIntroNonce++ },
                            modifier = Modifier.testTag("preview_intro_button")
                        ) {
                            Text(
                                text = "Title Intro",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Primary Show Lyrics button + Pin Quick Settings Tile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(54.dp)
                    .testTag("show_lyrics_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = stringResource(R.string.cd_show_lyrics),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Show Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            FilledTonalButton(
                onClick = {
                    if (prefs.hapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onPinQuickTileClick()
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Quick Demo Track Switcher for immediate karaoke testing
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(uiState.demoCatalog) { index, track ->
                val selected = snap.isDemoMode && snap.trackTitle == track.title
                FilterChip(
                    selected = selected,
                    onClick = { onStartDemoTrack(index) },
                    label = {
                        Text(
                            "${track.title} (${if (track.syncedLrc != null) "Synced" else "Plain"})",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier.testTag("demo_chip_$index")
                )
            }
        }

        if (snap.isDemoMode && snap.hasValidMetadata) {
            DemoTransportBar(
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeDesignCategoryCard(
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

    SettingsCard(title = "Theme & Typography") {
        // Compact Font Header + 2-Category Segmented Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Font Style",
                style = MaterialTheme.typography.labelLarge,
                color = LyricActive
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FontCategory.entries.forEach { category ->
                    val selected = activeFontCategory == category
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else StageObsidian,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else StageOutline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { activeFontCategory = category }
                            .testTag("font_cat_${category.name.lowercase()}")
                    ) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Compact single-row scrollable pill selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(fontsForCategory) { family ->
                val selected = prefs.fontFamily == family
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else StageObsidian,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else StageOutline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectFontFamily(family)
                        }
                        .testTag("font_${family.name.lowercase()}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = family.label,
                            fontFamily = family.toComposeFontFamily(),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) LyricActive else TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Color & Album Light Gradient Selector
        Text(
            text = "Lyric Color & Album Gradient",
            style = MaterialTheme.typography.labelLarge,
            color = LyricActive
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LyricColorPreset.entries.forEach { preset ->
                FilterChip(
                    selected = prefs.colorPreset == preset,
                    onClick = {
                        if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectColorPreset(preset)
                    },
                    leadingIcon = {
                        val swatchModifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                        when (preset) {
                            LyricColorPreset.ALBUM_ART -> Box(
                                modifier = swatchModifier.background(Brush.linearGradient(albumColors))
                            )
                            LyricColorPreset.STAGE_WHITE -> Box(modifier = swatchModifier.background(Color.White))
                            LyricColorPreset.ELECTRIC_VIOLET -> Box(modifier = swatchModifier.background(StageViolet))
                            LyricColorPreset.WARM_AMBER -> Box(modifier = swatchModifier.background(StageAmber))
                            LyricColorPreset.STAGE_CYAN -> Box(modifier = swatchModifier.background(StageCyan))
                            LyricColorPreset.DYNAMIC_PRIMARY -> Box(
                                modifier = swatchModifier.background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    },
                    label = { Text(preset.label, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag("color_${preset.name.lowercase()}")
                )
            }
        }

        Text(
            text = "Lyric Size: ${prefs.fontSizeSp.roundToInt()} sp",
            style = MaterialTheme.typography.labelLarge,
            color = LyricActive
        )
        Slider(
            value = prefs.fontSizeSp,
            onValueChange = onFontSizeChange,
            valueRange = 20f..42f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alignment",
                style = MaterialTheme.typography.labelLarge,
                color = LyricActive
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LyricTextAlignment.entries.forEach { align ->
                    FilterChip(
                        selected = prefs.alignment == align,
                        onClick = {
                            if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectAlignment(align)
                        },
                        label = { Text(align.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        SettingsSwitchRow(
            title = "Dynamic Material You Accents",
            subtitle = "Tints settings controls with system wallpaper accents on Android 12+",
            checked = prefs.useDynamicColor,
            onCheckedChange = onToggleDynamicColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransitionsEffectsCategoryCard(
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

    SettingsCard(title = "Transitions, Effects & Sync") {
        Text(
            text = "Lyric Transition Style",
            style = MaterialTheme.typography.labelLarge,
            color = LyricActive
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LyricTransitionStyle.entries.forEach { style ->
                FilterChip(
                    selected = prefs.transitionStyle == style,
                    onClick = {
                        if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectTransitionStyle(style)
                    },
                    label = { Text(style.label, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag("transition_${style.name.lowercase()}")
                )
            }
        }
        Text(
            text = prefs.transitionStyle.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transition Pace",
                style = MaterialTheme.typography.labelLarge,
                color = LyricActive
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LyricTransitionSpeed.entries.forEach { speed ->
                    FilterChip(
                        selected = prefs.transitionSpeed == speed,
                        onClick = {
                            if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTransitionSpeed(speed)
                        },
                        label = { Text(speed.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("speed_${speed.name.lowercase()}")
                    )
                }
            }
        }

        SettingsSwitchRow(
            title = "Word-by-Word Lyrics Reveal",
            subtitle = "Paces each word smoothly with the song (enhanced LRC tags or syllable cadence)",
            checked = prefs.wordByWordEnabled,
            onCheckedChange = onToggleWordByWord,
            testTag = "word_by_word_switch"
        )
        SettingsSwitchRow(
            title = "Apple-Style Glass Blur",
            subtitle = "Briefly blurs incoming lines and upcoming words before snapping crisp (Android 12+)",
            checked = prefs.glassBlurEnabled,
            onCheckedChange = onToggleGlassBlur,
            testTag = "glass_blur_switch"
        )
        SettingsSwitchRow(
            title = "Song Title & Artist Intro",
            subtitle = "Briefly shows track title and artist when a new song starts, then reveals lyrics",
            checked = prefs.showTrackIntroOnStart,
            onCheckedChange = onToggleShowTrackIntro,
            testTag = "track_intro_switch"
        )
        SettingsSwitchRow(
            title = "Show Upcoming Next Line",
            subtitle = "Displays dimmed next line below active line on pure #000000",
            checked = prefs.nextLineVisible,
            onCheckedChange = onToggleNextLineVisible,
        )

        SyncOffsetCalibrationRow(
            offsetMs = prefs.syncOffsetMs,
            hapticsEnabled = prefs.hapticsEnabled,
            onNudge = onNudgeOffset,
            onReset = onResetOffset
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LockScreenAodCategoryCard(
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

    SettingsCard(title = "Lock-Screen AOD & Panel Protection") {
        SettingsSwitchRow(
            title = "Auto-Show on Lock Screen",
            subtitle = "Automatically opens AMOLED lyrics when you play a song and lock the device",
            checked = prefs.autoShowOnLockEnabled,
            onCheckedChange = onToggleAutoShowOnLock,
            testTag = "auto_show_lock_switch"
        )

        if (prefs.autoShowOnLockEnabled) {
            SettingsSwitchRow(
                title = "Wake Into Black AOD on Lock",
                subtitle = "Brings up pure #000000 lyrics immediately when locked while playing",
                checked = prefs.wakeScreenOnLock,
                onCheckedChange = onToggleWakeScreenOnLock,
                testTag = "wake_on_lock_switch"
            )

            PermissionRow(
                title = "Display Over Apps (Auto-Lock Trigger)",
                subtitle = "Optional on Android 10+ so background players (Spotify, Apple Music) can launch lock-screen lyrics automatically.",
                granted = perms.overlayPermissionGranted,
                buttonLabel = if (perms.overlayPermissionGranted) "Allowed" else "Allow",
                testTag = "grant_overlay_button",
                onClick = onOpenOverlaySettings,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.ScreenRotation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Lockscreen Orientation",
                style = MaterialTheme.typography.labelLarge,
                color = LyricActive
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LockscreenOrientation.entries.forEach { orientation ->
                FilterChip(
                    selected = prefs.lockscreenOrientation == orientation,
                    onClick = {
                        if (prefs.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectLockscreenOrientation(orientation)
                    },
                    label = { Text(orientation.label) },
                    modifier = Modifier.testTag("orientation_${orientation.name.lowercase()}")
                )
            }
        }

        SettingsSwitchRow(
            title = "OLED Burn-In Pixel Shift",
            subtitle = "Gently drifts lyrics every 20s to protect AMOLED panels",
            checked = prefs.oledPixelShiftEnabled,
            onCheckedChange = onTogglePixelShift,
            testTag = "pixel_shift_switch"
        )
        SettingsSwitchRow(
            title = "Keep Screen On While Playing",
            subtitle = "Automatically allows screen sleep when playback pauses or ends",
            checked = prefs.keepScreenOnWhilePlaying,
            onCheckedChange = onToggleKeepScreenOn,
        )
        SettingsSwitchRow(
            title = "Restrained Haptics",
            subtitle = "Subtle tactile confirmation on Show Lyrics and design adjustments",
            checked = prefs.hapticsEnabled,
            onCheckedChange = onToggleHaptics,
            testTag = "haptics_switch"
        )
    }
}

@Composable
private fun SourcesPermissionsCategoryCard(
    uiState: LyricVerseUiState,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onLoadManualDocument: (LyricDocument) -> Unit,
    onOpenListenerSettings: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleTracking: (Boolean) -> Unit,
    onToggleLockScreenNotif: (Boolean) -> Unit,
    onToggleOnlineLookup: (Boolean) -> Unit,
    onToggleOfflineCache: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onSimulateMissingMetadata: () -> Unit,
) {
    val perms = uiState.permissions
    val prefs = uiState.syncState.preferences
    val diag = uiState.diagnostics
    val library = uiState.searchLibrary

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsCard(title = "Manual Track Lookup") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = library.query,
                    onValueChange = onQueryChange,
                    label = { Text("Search song or artist") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input")
                )
                Button(
                    onClick = onSearchSubmit,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("search_button")
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search lyrics")
                }
            }

            if (library.isSearching) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            library.results.take(4).forEach { doc ->
                Surface(
                    color = StageObsidian.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.trackTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = LyricActive,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${doc.artistName} · ${if (doc.syncType == LyricSyncType.SYNCED) "Synced LRC" else "Plain"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(onClick = { onLoadManualDocument(doc) }) {
                            Text("Load")
                        }
                    }
                }
            }
        }

        SettingsCard(title = "Music Access, Cache & Rules") {
            SettingsSwitchRow(
                title = "Track Compatible Music Apps",
                subtitle = "Reads active MediaSession metadata & album colors without altering audio",
                checked = prefs.trackingEnabled,
                onCheckedChange = onToggleTracking,
                testTag = "tracking_switch"
            )

            PermissionRow(
                title = "Notification Listener Access",
                subtitle = "Required to detect songs in Spotify, Apple Music, or local players. Revoke anytime in Settings.",
                granted = perms.notificationListenerGranted,
                buttonLabel = if (perms.notificationListenerGranted) "Manage" else "Grant Access",
                testTag = "grant_listener_button",
                onClick = onOpenListenerSettings,
            )

            SettingsSwitchRow(
                title = "Minimal Lock-Screen Notification",
                subtitle = "Optional ongoing notification showing active lyric lines on supported lock screens",
                checked = prefs.lockScreenNotificationEnabled,
                onCheckedChange = onToggleLockScreenNotif,
                testTag = "lock_notif_switch"
            )

            if (!perms.postNotificationsGranted) {
                PermissionRow(
                    title = "Post Notifications (Android 13+)",
                    subtitle = "Needed for the ongoing Lock-Screen Companion notification and Auto-Show status.",
                    granted = false,
                    buttonLabel = "Allow",
                    testTag = "grant_notifications_button",
                    onClick = onRequestPostNotifications,
                )
            }

            SettingsSwitchRow(
                title = "LRCLIB Online Lookup",
                subtitle = "Sends only track title, artist, album & duration (no account or audio)",
                checked = prefs.onlineLookupEnabled,
                onCheckedChange = onToggleOnlineLookup,
                testTag = "online_lookup_switch"
            )

            SettingsSwitchRow(
                title = "Permitted Offline Lyrics Cache",
                subtitle = "Caches matches locally for 14 days (3 days for misses) when allowed",
                checked = prefs.offlineCacheEnabled,
                onCheckedChange = onToggleOfflineCache,
                testTag = "offline_cache_switch"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_cache_button")
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Cache (${uiState.searchLibrary.cachedEntries.size})")
                }

                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("battery_settings_button")
                ) {
                    Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Battery")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sessions: ${diag.activeSessionsCount} · ${diag.selectedAppLabel ?: "None"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                OutlinedButton(
                    onClick = onSimulateMissingMetadata,
                    modifier = Modifier.testTag("simulate_missing_metadata_button")
                ) {
                    Text("Test Missing Metadata")
                }
            }

            Surface(
                color = StageObsidian.copy(alpha = 0.8f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = StageViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "About Lock-Screen & System AOD",
                            style = MaterialTheme.typography.labelLarge,
                            color = LyricActive
                        )
                    }
                    Text(
                        text = "Android reserves the OEM hardware Doze AOD exclusively for SystemUI and blocks floating windows over the keyguard. When 'Auto-Show on Lock Screen' is enabled, LyricVerse listens for screen-lock events during active playback and launches its pure #000000 AMOLED lyrics screen on top of the lock screen without bypassing your unlock security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Android Auto head units are excluded for driver safety; playback continues undisturbed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StageSurfaceElevated),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LyricActive
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = LyricActive)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    buttonLabel: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        color = StageObsidian.copy(alpha = 0.72f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (granted) SuccessMint else WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, color = LyricActive)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.testTag(testTag)
            ) {
                Text(buttonLabel)
            }
        }
    }
}
