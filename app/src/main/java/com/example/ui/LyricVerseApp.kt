package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AodLyricsActivity
import com.example.ui.screens.HomeSettingsScreen
import com.example.ui.theme.LyricVerseTheme

@Composable
fun LyricVerseApp(viewModel: LyricVerseViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { viewModel.refreshPermissionsAndSessions() }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsAndSessions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val message = uiState.statusBannerMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearBanner()
        }
    }

    LyricVerseTheme(useDynamicColor = uiState.syncState.preferences.useDynamicColor) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedLayout = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            actionColor = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                },
            ) { innerPadding ->
                HomeSettingsScreen(
                    uiState = uiState,
                    isExpandedLayout = isExpandedLayout,
                    onShowLyricsClick = {
                        context.startActivity(
                            Intent(context, AodLyricsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
                    },
                    onPinQuickTileClick = viewModel::requestAddQuickSettingsTile,
                    onStartDemoTrack = viewModel::startDemoTrack,
                    onToggleDemoPlayPause = viewModel::toggleDemoPlayPause,
                    onSeekDemo = viewModel::seekDemoTo,
                    onNextDemo = viewModel::nextDemoTrack,
                    onPrevDemo = viewModel::previousDemoTrack,
                    onSimulateMissingMetadata = viewModel::simulateMissingMetadata,
                    onSelectFontFamily = viewModel::updateFontFamily,
                    onFontSizeChange = viewModel::updateFontSizeSp,
                    onSelectColorPreset = viewModel::updateColorPreset,
                    onSelectAlignment = viewModel::updateAlignment,
                    onToggleNextLineVisible = viewModel::updateNextLineVisible,
                    onToggleWordByWord = viewModel::updateWordByWordEnabled,
                    onToggleGlassBlur = viewModel::updateGlassBlurEnabled,
                    onSelectTransitionStyle = viewModel::updateTransitionStyle,
                    onSelectTransitionSpeed = viewModel::updateTransitionSpeed,
                    onToggleShowTrackIntro = viewModel::updateShowTrackIntroOnStart,
                    onSelectLockscreenOrientation = viewModel::updateLockscreenOrientation,
                    onToggleAutoShowOnLock = viewModel::updateAutoShowOnLockEnabled,
                    onToggleWakeScreenOnLock = viewModel::updateWakeScreenOnLock,
                    onTogglePixelShift = viewModel::updateOledPixelShift,
                    onToggleKeepScreenOn = viewModel::updateKeepScreenOnWhilePlaying,
                    onToggleDynamicColor = viewModel::updateUseDynamicColor,
                    onToggleHaptics = viewModel::updateHapticsEnabled,
                    onNudgeOffset = viewModel::nudgeSyncOffset,
                    onResetOffset = viewModel::resetSyncOffset,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSearchSubmit = viewModel::executeManualSearch,
                    onLoadManualDocument = viewModel::loadManualLyricsOntoStage,
                    onOpenListenerSettings = {
                        context.startActivity(viewModel.createNotificationListenerSettingsIntent())
                    },
                    onOpenOverlaySettings = {
                        context.startActivity(viewModel.createOverlaySettingsIntent())
                    },
                    onRequestPostNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !uiState.permissions.postNotificationsGranted
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(viewModel.createAppNotificationSettingsIntent())
                        }
                    },
                    onOpenBatterySettings = {
                        context.startActivity(viewModel.createBatterySettingsIntent())
                    },
                    onToggleTracking = viewModel::toggleTrackingEnabled,
                    onToggleLockScreenNotif = viewModel::updateLockScreenNotificationEnabled,
                    onToggleOnlineLookup = viewModel::updateOnlineLookupEnabled,
                    onToggleOfflineCache = viewModel::updateOfflineCacheEnabled,
                    onClearCache = viewModel::clearLyricsCache,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
