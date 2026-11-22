package com.example.ui

import android.app.Application
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.LyricVerseApplication
import com.example.R
import com.example.data.local.LyricsCacheEntity
import com.example.data.local.PreferencesDataStore
import com.example.data.lyrics.LyricsRepository
import com.example.domain.model.DemoTrack
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.LockscreenOrientation
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.LyricTransitionSpeed
import com.example.domain.model.LyricTransitionStyle
import com.example.media.AodLyricsTileService
import com.example.media.CompanionSyncState
import com.example.media.LyricSyncCoordinator
import com.example.media.MediaSessionDiagnostics
import com.example.media.MediaSessionTracker
import com.example.media.OverlayCompanionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStatusSnapshot(
    val notificationListenerGranted: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val postNotificationsGranted: Boolean = true,
    val ignoringBatteryOptimizations: Boolean = false,
)

data class SearchLibraryUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<LyricDocument> = emptyList(),
    val hasSearched: Boolean = false,
    val cachedEntries: List<LyricsCacheEntity> = emptyList(),
)

data class LyricVerseUiState(
    val syncState: CompanionSyncState = CompanionSyncState(),
    val diagnostics: MediaSessionDiagnostics = MediaSessionDiagnostics(),
    val permissions: PermissionStatusSnapshot = PermissionStatusSnapshot(),
    val searchLibrary: SearchLibraryUiState = SearchLibraryUiState(),
    val demoCatalog: List<DemoTrack> = emptyList(),
    val statusBannerMessage: String? = null,
)

class LyricVerseViewModel(
    application: Application,
    private val syncCoordinator: LyricSyncCoordinator,
    private val mediaSessionTracker: MediaSessionTracker,
    private val lyricsRepository: LyricsRepository,
    private val preferencesDataStore: PreferencesDataStore,
) : AndroidViewModel(application) {

    private val _permissions = MutableStateFlow(readPermissionsSnapshot())
    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)
    private val _searchResults = MutableStateFlow<List<LyricDocument>>(emptyList())
    private val _hasSearched = MutableStateFlow(false)
    private val _statusBanner = MutableStateFlow<String?>(null)

    private val searchLibraryFlow = combine(
        _searchQuery,
        _isSearching,
        _searchResults,
        _hasSearched,
        lyricsRepository.cachedEntriesFlow,
    ) { query, searching, results, hasSearched, cached ->
        SearchLibraryUiState(
            query = query,
            isSearching = searching,
            results = results,
            hasSearched = hasSearched,
            cachedEntries = cached,
        )
    }

    val uiState: StateFlow<LyricVerseUiState> = combine(
        syncCoordinator.syncState,
        syncCoordinator.diagnosticSnapshot,
        _permissions,
        searchLibraryFlow,
        _statusBanner,
    ) { sync, diag, perms, search, banner ->
        LyricVerseUiState(
            syncState = sync,
            diagnostics = diag,
            permissions = perms,
            searchLibrary = search,
            demoCatalog = syncCoordinator.demoController.tracks,
            statusBannerMessage = banner,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LyricVerseUiState(
            demoCatalog = syncCoordinator.demoController.tracks,
            permissions = readPermissionsSnapshot(),
        )
    )

    init {
        refreshPermissionsAndSessions()
    }

    fun refreshPermissionsAndSessions() {
        val snapshot = readPermissionsSnapshot()
        _permissions.value = snapshot
        if (snapshot.notificationListenerGranted && syncCoordinator.preferencesFlow.value.trackingEnabled) {
            mediaSessionTracker.startObserving()
        } else {
            mediaSessionTracker.refreshActiveSessions()
        }
    }

    // Demo playback controls
    fun startDemoTrack(index: Int = 0) {
        syncCoordinator.demoController.startDemo(trackIndex = index, autoPlay = true)
    }

    fun toggleDemoPlayPause() {
        syncCoordinator.demoController.togglePlayPause()
    }

    fun seekDemoTo(positionMs: Long) {
        syncCoordinator.demoController.seekTo(positionMs)
    }

    fun nextDemoTrack() {
        syncCoordinator.demoController.nextTrack()
    }

    fun previousDemoTrack() {
        syncCoordinator.demoController.previousTrack()
    }

    fun simulateMissingMetadata() {
        syncCoordinator.demoController.simulateMissingMetadata()
    }

    // Sync calibration & lookup
    fun nudgeSyncOffset(deltaMs: Long) {
        viewModelScope.launch {
            preferencesDataStore.update { prefs ->
                prefs.copy(syncOffsetMs = (prefs.syncOffsetMs + deltaMs).coerceIn(-3000L, 3000L))
            }
        }
    }

    fun resetSyncOffset() {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(syncOffsetMs = 0L) }
        }
    }

    fun retryLyricsLookup() {
        syncCoordinator.retryLyricsLookup()
    }

    // Quick Settings Tile pin prompt (Android 13+)
    fun requestAddQuickSettingsTile() {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            if (statusBarManager != null) {
                statusBarManager.requestAddTileService(
                    ComponentName(context, AodLyricsTileService::class.java),
                    context.getString(R.string.qs_tile_label),
                    Icon.createWithResource(context, R.drawable.ic_notif_lyric),
                    context.mainExecutor
                ) { resultCode ->
                    _statusBanner.value = when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            "Added AOD Lyrics tile to Quick Settings"
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            "AOD Lyrics tile is already in your Quick Settings"
                        else ->
                            "Swipe down Quick Settings and drag 'AOD Lyrics' to pin it"
                    }
                }
                return
            }
        }
        _statusBanner.value = "Swipe down Quick Settings, tap Edit, and drag 'AOD Lyrics' into active tiles"
    }

    // Theme & AMOLED Design preferences
    fun updateFontFamily(family: KaraokeFontFamily) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(fontFamily = family) } }
    }

    fun updateFontSizeSp(sizeSp: Float) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(fontSizeSp = sizeSp.coerceIn(20f, 42f)) }
        }
    }

    fun updateColorPreset(preset: LyricColorPreset) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(colorPreset = preset) } }
    }

    fun updateAlignment(alignment: LyricTextAlignment) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(alignment = alignment) } }
    }

    fun updateNextLineVisible(visible: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(nextLineVisible = visible) } }
    }

    fun updateWordByWordEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(wordByWordEnabled = enabled) } }
    }

    fun updateGlassBlurEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(glassBlurEnabled = enabled) } }
    }

    fun updateTransitionStyle(style: LyricTransitionStyle) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(transitionStyle = style) } }
    }

    fun updateTransitionSpeed(speed: LyricTransitionSpeed) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(transitionSpeed = speed) } }
    }

    fun updateShowTrackIntroOnStart(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(showTrackIntroOnStart = enabled) } }
    }

    fun updateLockscreenOrientation(orientation: LockscreenOrientation) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(lockscreenOrientation = orientation) }
        }
    }

    fun updateAutoShowOnLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(autoShowOnLockEnabled = enabled) }
            syncCompanionServiceState()
            if (enabled && !readPermissionsSnapshot().overlayPermissionGranted) {
                _statusBanner.value =
                    "Auto-Show enabled. Grant 'Display Over Apps' in Lock & AOD for background music players."
            }
        }
    }

    fun updateWakeScreenOnLock(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(wakeScreenOnLock = enabled) } }
    }

    fun updateOledPixelShift(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(oledPixelShiftEnabled = enabled) } }
    }

    fun updateKeepScreenOnWhilePlaying(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(keepScreenOnWhilePlaying = enabled) }
        }
    }

    fun updateUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(useDynamicColor = enabled) } }
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(hapticsEnabled = enabled) } }
    }

    fun toggleTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(trackingEnabled = enabled) }
            if (enabled) {
                mediaSessionTracker.startObserving()
            } else {
                mediaSessionTracker.stopObserving()
                OverlayCompanionService.stopCompanion(getApplication())
            }
        }
    }

    fun updateOnlineLookupEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(onlineLookupEnabled = enabled) } }
    }

    fun updateOfflineCacheEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(offlineCacheEnabled = enabled) } }
    }

    fun updateLockScreenNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(lockScreenNotificationEnabled = enabled) }
            syncCompanionServiceState()
        }
    }

    private fun syncCompanionServiceState() {
        val prefs = syncCoordinator.preferencesFlow.value
        val needsService = prefs.autoShowOnLockEnabled || prefs.lockScreenNotificationEnabled
        val context = getApplication<Application>()
        if (needsService) {
            OverlayCompanionService.startCompanion(context)
        } else {
            OverlayCompanionService.stopCompanion(context)
        }
    }

    // Manual Search & Offline Cache
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun executeManualSearch() {
        val q = _searchQuery.value.trim()
        viewModelScope.launch {
            _isSearching.value = true
            val online = uiState.value.syncState.preferences.onlineLookupEnabled
            val results = lyricsRepository.searchManual(q, onlineLookupEnabled = online)
            _searchResults.value = results
            _hasSearched.value = true
            _isSearching.value = false
        }
    }

    fun loadManualLyricsOntoStage(document: LyricDocument) {
        syncCoordinator.loadManualDocument(document)
        _statusBanner.value = "Loaded \"${document.trackTitle}\" for AMOLED display"
    }

    fun clearLyricsCache() {
        viewModelScope.launch {
            lyricsRepository.clearCache()
            _statusBanner.value = "Offline lyrics cache cleared"
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    // System settings intents
    fun createNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun createOverlaySettingsIntent(): Intent {
        val pkg = getApplication<Application>().packageName
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$pkg")
        )
    }

    fun createAppNotificationSettingsIntent(): Intent {
        val pkg = getApplication<Application>().packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
        }
    }

    fun createBatterySettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    private fun readPermissionsSnapshot(): PermissionStatusSnapshot {
        val context = getApplication<Application>()
        val listenerGranted = mediaSessionTracker.isNotificationListenerGranted()
        val overlayGranted = Settings.canDrawOverlays(context)
        val postNotifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBattery = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        return PermissionStatusSnapshot(
            notificationListenerGranted = listenerGranted,
            overlayPermissionGranted = overlayGranted,
            postNotificationsGranted = postNotifGranted,
            ignoringBatteryOptimizations = ignoringBattery,
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LyricVerseApplication)
                val container = app.appContainer
                LyricVerseViewModel(
                    application = app,
                    syncCoordinator = container.syncCoordinator,
                    mediaSessionTracker = container.mediaSessionTracker,
                    lyricsRepository = container.lyricsRepository,
                    preferencesDataStore = container.preferencesDataStore,
                )
            }
        }
    }
}
