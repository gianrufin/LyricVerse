package com.example.media

import android.os.SystemClock
import com.example.data.local.PreferencesDataStore
import com.example.data.lyrics.LyricsRepository
import com.example.domain.model.KaraokeStylePreferences
import com.example.domain.model.LrcParser
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricSyncType
import com.example.domain.model.LyricsLookupQuery
import com.example.domain.model.LyricsLookupResult
import com.example.domain.model.PlaybackSessionStatus
import com.example.domain.model.PlaybackSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LyricsStageState {
    data object Idle : LyricsStageState
    data object Loading : LyricsStageState
    data class Loaded(val document: LyricDocument) : LyricsStageState
    data class Unavailable(val reason: String, val retryAfterSeconds: Int? = null) : LyricsStageState
}

data class CompanionSyncState(
    val snapshot: PlaybackSnapshot = PlaybackSnapshot(),
    val lyricsState: LyricsStageState = LyricsStageState.Idle,
    val activeLineIndex: Int = -1,
    val livePositionMs: Long = 0L,
    val preferences: KaraokeStylePreferences = KaraokeStylePreferences(),
    val overlayVisible: Boolean = false,
    val companionServiceRunning: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LyricSyncCoordinator(
    private val mediaSessionTracker: MediaSessionTracker,
    val demoController: DemoPlaybackController,
    private val lyricsRepository: LyricsRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val scope: CoroutineScope,
) {
    private val _lyricsState = MutableStateFlow<LyricsStageState>(LyricsStageState.Idle)
    private val _activeLineIndex = MutableStateFlow(-1)
    private val _livePositionMs = MutableStateFlow(0L)
    private val _overlayVisible = MutableStateFlow(false)
    private val _companionServiceRunning = MutableStateFlow(false)
    private val _manualOverrideDoc = MutableStateFlow<LyricDocument?>(null)

    @Volatile
    var isAodActivityForeground: Boolean = false
        private set

    @Volatile
    var dismissedAodOnCurrentLock: Boolean = false
        private set

    private var lastFetchedSignature: String? = null
    private var fetchJob: Job? = null
    private var syncLoopJob: Job? = null

    val preferencesFlow: StateFlow<KaraokeStylePreferences> =
        preferencesDataStore.preferencesFlow.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = KaraokeStylePreferences()
        )

    /**
     * Combines external media session with demo controller:
     * Active external playback always takes precedence unless tracking is disabled or demo is running.
     */
    val activeSnapshotFlow: StateFlow<PlaybackSnapshot> = combine(
        mediaSessionTracker.externalSnapshot,
        demoController.snapshot,
        preferencesFlow,
    ) { external, demo, prefs ->
        when {
            !prefs.trackingEnabled -> PlaybackSnapshot(status = PlaybackSessionStatus.SESSION_ENDED)
            external.status == PlaybackSessionStatus.ACTIVE_EXTERNAL && external.isPlaying -> external
            demo.isDemoMode -> demo
            external.status == PlaybackSessionStatus.ACTIVE_EXTERNAL ||
                external.status == PlaybackSessionStatus.METADATA_MISSING -> external
            demo.status == PlaybackSessionStatus.SESSION_ENDED -> demo
            else -> external
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = PlaybackSnapshot()
    )

    val syncState: StateFlow<CompanionSyncState> = combine(
        activeSnapshotFlow,
        _lyricsState,
        _activeLineIndex,
        _livePositionMs,
        preferencesFlow,
    ) { snapshot, lyrics, lineIdx, posMs, prefs ->
        CompanionSyncState(
            snapshot = snapshot,
            lyricsState = lyrics,
            activeLineIndex = lineIdx,
            livePositionMs = posMs,
            preferences = prefs,
            overlayVisible = _overlayVisible.value,
            companionServiceRunning = _companionServiceRunning.value,
        )
    }.combine(_overlayVisible) { state, overlay ->
        state.copy(overlayVisible = overlay)
    }.combine(_companionServiceRunning) { state, running ->
        state.copy(companionServiceRunning = running)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = CompanionSyncState()
    )

    private val _diagnosticSnapshot = MutableStateFlow(mediaSessionTracker.diagnosticInfo.value)
    val diagnosticSnapshot: StateFlow<MediaSessionDiagnostics> = _diagnosticSnapshot.asStateFlow()

    init {
        scope.launch {
            mediaSessionTracker.diagnosticInfo.collect { _diagnosticSnapshot.value = it }
        }

        // Observe track signature changes to fetch/resolve lyrics once per song
        scope.launch {
            combine(activeSnapshotFlow, preferencesFlow) { snapshot, prefs ->
                Triple(snapshot, prefs.onlineLookupEnabled, prefs.offlineCacheEnabled)
            }
                .distinctUntilChanged { old, new ->
                    old.first.status == new.first.status &&
                        old.first.trackSignatureKey == new.first.trackSignatureKey &&
                        old.second == new.second &&
                        old.third == new.third
                }
                .collect { (snapshot, onlineEnabled, cacheEnabled) ->
                    handleTrackOrStatusChange(snapshot, onlineEnabled, cacheEnabled)
                }
        }

        // Event-driven low-latency line scheduler (avoids 60fps polling storms)
        scope.launch {
            combine(
                activeSnapshotFlow,
                _lyricsState,
                preferencesFlow,
            ) { snapshot, lyrics, prefs ->
                Triple(snapshot, lyrics, prefs.syncOffsetMs)
            }.collect { (snapshot, lyrics, offsetMs) ->
                restartSyncScheduler(snapshot, lyrics, offsetMs)
            }
        }
    }

    fun setAodActivityForeground(foreground: Boolean) {
        isAodActivityForeground = foreground
        if (foreground) {
            dismissedAodOnCurrentLock = false
        }
    }

    fun markAodDismissedByUser() {
        dismissedAodOnCurrentLock = true
    }

    fun resetLockDismissalState() {
        dismissedAodOnCurrentLock = false
    }

    fun setOverlayVisible(visible: Boolean) {
        _overlayVisible.value = visible
    }

    fun setCompanionServiceRunning(running: Boolean) {
        _companionServiceRunning.value = running
        if (!running) {
            _overlayVisible.value = false
        }
    }

    fun refreshExternalSessions() {
        mediaSessionTracker.startObserving()
    }

    fun retryLyricsLookup() {
        val snapshot = activeSnapshotFlow.value
        val prefs = preferencesFlow.value
        lastFetchedSignature = null
        _manualOverrideDoc.value = null
        handleTrackOrStatusChange(
            snapshot = snapshot,
            onlineEnabled = prefs.onlineLookupEnabled,
            cacheEnabled = prefs.offlineCacheEnabled,
            forceRefresh = true
        )
    }

    fun loadManualDocument(document: LyricDocument) {
        _manualOverrideDoc.value = document
        _lyricsState.value = LyricsStageState.Loaded(document)
    }

    private fun handleTrackOrStatusChange(
        snapshot: PlaybackSnapshot,
        onlineEnabled: Boolean,
        cacheEnabled: Boolean,
        forceRefresh: Boolean = false,
    ) {
        if (!snapshot.hasValidMetadata) {
            val manual = _manualOverrideDoc.value
            if (manual != null) {
                _lyricsState.value = LyricsStageState.Loaded(manual)
                return
            }
            fetchJob?.cancel()
            lastFetchedSignature = null
            _lyricsState.value = LyricsStageState.Idle
            return
        }

        val sig = snapshot.trackSignatureKey
        if (!forceRefresh && sig == lastFetchedSignature && _lyricsState.value is LyricsStageState.Loaded) {
            return
        }

        _manualOverrideDoc.value = null
        lastFetchedSignature = sig
        fetchJob?.cancel()
        _lyricsState.value = LyricsStageState.Loading

        fetchJob = scope.launch {
            val result = lyricsRepository.getLyricsForTrack(
                query = LyricsLookupQuery(
                    trackTitle = snapshot.trackTitle,
                    artistName = snapshot.artistName,
                    albumName = snapshot.albumName,
                    durationMs = snapshot.durationMs,
                ),
                onlineLookupEnabled = onlineEnabled,
                offlineCacheEnabled = cacheEnabled,
                forceRefresh = forceRefresh,
            )
            _lyricsState.value = when (result) {
                is LyricsLookupResult.Found -> LyricsStageState.Loaded(result.document)
                is LyricsLookupResult.NotFound -> LyricsStageState.Unavailable(result.reason)
                is LyricsLookupResult.Error -> LyricsStageState.Unavailable(
                    reason = result.message,
                    retryAfterSeconds = result.retryAfterSeconds
                )
            }
        }
    }

    private fun restartSyncScheduler(
        snapshot: PlaybackSnapshot,
        lyricsState: LyricsStageState,
        offsetMs: Long,
    ) {
        syncLoopJob?.cancel()

        val initialPos = PlaybackClockExtrapolator.computePositionMs(
            snapshot = snapshot,
            currentElapsedRealtimeMs = SystemClock.elapsedRealtime()
        )
        _livePositionMs.value = initialPos

        val doc = (lyricsState as? LyricsStageState.Loaded)?.document
        if (doc == null || doc.syncType != LyricSyncType.SYNCED || doc.lines.isEmpty()) {
            _activeLineIndex.value = -1
            if (snapshot.isPlaying && snapshot.durationMs > 0L) {
                // Lightweight 500ms progress update for plain/demo scrubber without heavy redraws
                syncLoopJob = scope.launch {
                    while (isActive && snapshot.isPlaying) {
                        _livePositionMs.value = PlaybackClockExtrapolator.computePositionMs(
                            snapshot,
                            SystemClock.elapsedRealtime()
                        )
                        delay(500L)
                    }
                }
            }
            return
        }

        val initialIndex = LrcParser.findActiveLineIndex(doc.lines, initialPos + offsetMs)
        _activeLineIndex.value = initialIndex

        if (!snapshot.isPlaying) return

        syncLoopJob = scope.launch {
            while (isActive) {
                val nowPos = PlaybackClockExtrapolator.computePositionMs(
                    snapshot = snapshot,
                    currentElapsedRealtimeMs = SystemClock.elapsedRealtime()
                )
                _livePositionMs.value = nowPos
                val effectivePos = nowPos + offsetMs
                val currentIdx = LrcParser.findActiveLineIndex(doc.lines, effectivePos)
                if (_activeLineIndex.value != currentIdx) {
                    _activeLineIndex.value = currentIdx
                }

                val nextDelay = LrcParser.computeNextTransitionDelayMs(
                    lines = doc.lines,
                    activeIndex = currentIdx,
                    effectivePositionMs = effectivePos,
                    playbackSpeed = snapshot.playbackSpeed
                )
                // Wake exactly at the next lyric timestamp, or every 600ms to keep progress readout fresh
                val sleepMs = if (nextDelay != null) minOf(nextDelay, 600L) else 600L
                delay(sleepMs)
            }
        }
    }
}
