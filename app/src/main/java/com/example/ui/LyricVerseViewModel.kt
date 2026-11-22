package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
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
import com.example.data.local.LyricsCacheEntity
import com.example.data.local.PreferencesDataStore
import com.example.data.lyrics.LyricsRepository
import com.example.domain.model.DemoTrack
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.KaraokeStylePreferences
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.OverlayDefaultMode
import com.example.domain.model.StageBackgroundStyle
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
    val showOverlayRationaleDialog: Boolean = false,
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
    private val _showOverlayRationale = MutableStateFlow(false)
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
        _showOverlayRationale,
    ) { sync, diag, perms, search, showRationale ->
        LyricVerseUiState(
            syncState = sync,
            diagnostics = diag,
            permissions = perms,
            searchLibrary = search,
            demoCatalog = syncCoordinator.demoController.tracks,
            showOverlayRationaleDialog = showRationale,
            statusBannerMessage = _statusBanner.value,
        )
    }.combine(_statusBanner) { state, banner ->
        state.copy(statusBannerMessage = banner)
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

    // Demo stage controls
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

    fun stopDemo() {
        syncCoordinator.demoController.stopDemo()
    }

    // Sync calibration & lookup actions
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

    // Floating overlay & companion foreground service
    fun toggleFloatingOverlay() {
        val context = getApplication<Application>()
        val currentlyVisible = uiState.value.syncState.overlayVisible
        if (currentlyVisible) {
            OverlayCompanionService.stopCompanion(context)
            return
        }
        val canDraw = Settings.canDrawOverlays(context)
        _permissions.value = readPermissionsSnapshot()
        if (!canDraw) {
            _showOverlayRationale.value = true
            return
        }
        OverlayCompanionService.startWithOverlay(context)
    }

    fun dismissOverlayRationale() {
        _showOverlayRationale.value = false
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

    // Style Studio preferences
    fun updateFontFamily(family: KaraokeFontFamily) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(fontFamily = family) } }
    }

    fun updateFontSizeSp(sizeSp: Float) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(fontSizeSp = sizeSp.coerceIn(18f, 38f)) }
        }
    }

    fun updateColorPreset(preset: LyricColorPreset) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(colorPreset = preset) } }
    }

    fun updateBackgroundStyle(style: StageBackgroundStyle) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(backgroundStyle = style) } }
    }

    fun updateAlignment(alignment: LyricTextAlignment) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(alignment = alignment) } }
    }

    fun updateStageOpacity(opacity: Float) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(stageOpacity = opacity.coerceIn(0.45f, 1.0f)) }
        }
    }

    fun updateOverlayOpacity(opacity: Float) {
        viewModelScope.launch {
            preferencesDataStore.update { it.copy(overlayOpacity = opacity.coerceIn(0.55f, 1.0f)) }
        }
    }

    fun updateOverlayDefaultMode(mode: OverlayDefaultMode) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(overlayDefaultMode = mode) } }
    }

    fun updateUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(useDynamicColor = enabled) } }
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.update { it.copy(hapticsEnabled = enabled) } }
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
        }
    }

    // Manual Search & Offline Cache
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun executeManualSearch(overrideQuery: String? = null) {
        val q = (overrideQuery ?: _searchQuery.value).trim()
        if (overrideQuery != null) {
            _searchQuery.value = overrideQuery
        }
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
        _statusBanner.value = "Loaded \"${document.trackTitle}\" onto Stage"
    }

    fun clearLyricsCache() {
        viewModelScope.launch {
            lyricsRepository.clearCache()
            _statusBanner.value = "Offline lyrics cache cleared"
        }
    }

    fun deleteCacheEntry(trackKey: String) {
        viewModelScope.launch {
            lyricsRepository.deleteCacheEntry(trackKey)
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
