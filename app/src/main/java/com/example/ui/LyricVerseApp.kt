package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.LibrarySearchScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.StageScreen
import com.example.ui.screens.StyleStudioScreen
import com.example.ui.theme.LyricVerseTheme
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageSurface

object LyricVerseRoutes {
    const val STAGE = "stage"
    const val LIBRARY = "library"
    const val STYLE = "style"
    const val ACCESS = "access"
}

private data class NavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@Composable
fun LyricVerseApp(viewModel: LyricVerseViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
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

    val navItems = remember {
        listOf(
            NavItem(
                route = LyricVerseRoutes.STAGE,
                labelRes = R.string.nav_stage,
                selectedIcon = Icons.Rounded.Lyrics,
                unselectedIcon = Icons.Outlined.Lyrics,
                testTag = "nav_stage",
            ),
            NavItem(
                route = LyricVerseRoutes.LIBRARY,
                labelRes = R.string.nav_library,
                selectedIcon = Icons.Rounded.LibraryMusic,
                unselectedIcon = Icons.Outlined.LibraryMusic,
                testTag = "nav_library",
            ),
            NavItem(
                route = LyricVerseRoutes.STYLE,
                labelRes = R.string.nav_style,
                selectedIcon = Icons.Rounded.Palette,
                unselectedIcon = Icons.Outlined.Palette,
                testTag = "nav_style",
            ),
            NavItem(
                route = LyricVerseRoutes.ACCESS,
                labelRes = R.string.nav_access,
                selectedIcon = Icons.Rounded.VerifiedUser,
                unselectedIcon = Icons.Outlined.VerifiedUser,
                testTag = "nav_access",
            ),
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: LyricVerseRoutes.STAGE

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LyricVerseTheme(useDynamicColor = uiState.syncState.preferences.useDynamicColor) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedLayout = maxWidth >= 600.dp

            Scaffold(
                containerColor = StageObsidian,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!isExpandedLayout) {
                        NavigationBar(
                            containerColor = StageSurface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedLayout) {
                        NavigationRail(
                            containerColor = StageSurface,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = LyricVerseRoutes.STAGE,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(LyricVerseRoutes.STAGE) {
                            StageScreen(
                                uiState = uiState,
                                isExpandedLayout = isExpandedLayout,
                                onStartDemoTrack = viewModel::startDemoTrack,
                                onToggleDemoPlayPause = viewModel::toggleDemoPlayPause,
                                onSeekDemo = viewModel::seekDemoTo,
                                onNextDemo = viewModel::nextDemoTrack,
                                onPrevDemo = viewModel::previousDemoTrack,
                                onNudgeOffset = viewModel::nudgeSyncOffset,
                                onResetOffset = viewModel::resetSyncOffset,
                                onRetryLookup = viewModel::retryLyricsLookup,
                                onToggleOverlay = viewModel::toggleFloatingOverlay,
                                onNavigateToPermissions = { navigateTo(LyricVerseRoutes.ACCESS) },
                                onNavigateToSearch = { navigateTo(LyricVerseRoutes.LIBRARY) },
                            )
                        }

                        composable(LyricVerseRoutes.LIBRARY) {
                            LibrarySearchScreen(
                                uiState = uiState,
                                onQueryChange = viewModel::updateSearchQuery,
                                onSearchSubmit = { viewModel.executeManualSearch() },
                                onLoadDocument = { doc ->
                                    viewModel.loadManualLyricsOntoStage(doc)
                                    navigateTo(LyricVerseRoutes.STAGE)
                                },
                                onToggleOnlineLookup = viewModel::updateOnlineLookupEnabled,
                                onToggleOfflineCache = viewModel::updateOfflineCacheEnabled,
                                onClearCache = viewModel::clearLyricsCache,
                                onDeleteCacheEntry = viewModel::deleteCacheEntry,
                            )
                        }

                        composable(LyricVerseRoutes.STYLE) {
                            StyleStudioScreen(
                                uiState = uiState,
                                onSelectFontFamily = viewModel::updateFontFamily,
                                onFontSizeChange = viewModel::updateFontSizeSp,
                                onSelectColorPreset = viewModel::updateColorPreset,
                                onSelectBackground = viewModel::updateBackgroundStyle,
                                onSelectAlignment = viewModel::updateAlignment,
                                onStageOpacityChange = viewModel::updateStageOpacity,
                                onOverlayOpacityChange = viewModel::updateOverlayOpacity,
                                onSelectOverlayMode = viewModel::updateOverlayDefaultMode,
                                onToggleDynamicColor = viewModel::updateUseDynamicColor,
                                onToggleHaptics = viewModel::updateHapticsEnabled,
                            )
                        }

                        composable(LyricVerseRoutes.ACCESS) {
                            PermissionsScreen(
                                uiState = uiState,
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
                                onSimulateMissingMetadata = {
                                    viewModel.simulateMissingMetadata()
                                    navigateTo(LyricVerseRoutes.STAGE)
                                },
                                onRefreshSessions = viewModel::refreshPermissionsAndSessions,
                            )
                        }
                    }
                }
            }

            if (uiState.showOverlayRationaleDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissOverlayRationale,
                    title = { Text("Allow Floating Lyrics Overlay?") },
                    text = {
                        Text(
                            "To display the movable 2-line floating pill and Apple Music-style lyric card over other apps, Android requires the \"Display over other apps\" permission. You can hide or revoke it anytime."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissOverlayRationale()
                                context.startActivity(viewModel.createOverlaySettingsIntent())
                            },
                            modifier = Modifier.testTag("overlay_rationale_confirm")
                        ) {
                            Text("Open Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = viewModel::dismissOverlayRationale,
                            modifier = Modifier.testTag("overlay_rationale_dismiss")
                        ) {
                            Text("Not Now")
                        }
                    }
                )
            }
        }
    }
}
