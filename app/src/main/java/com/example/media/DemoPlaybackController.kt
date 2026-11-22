package com.example.media

import android.os.SystemClock
import com.example.data.lyrics.DemoLyricsProvider
import com.example.domain.model.DemoTrack
import com.example.domain.model.PlaybackSessionStatus
import com.example.domain.model.PlaybackSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DemoPlaybackController(
    private val demoProvider: DemoLyricsProvider,
    private val scope: CoroutineScope,
    private val elapsedRealtimeProvider: () -> Long = { SystemClock.elapsedRealtime() },
) {
    val tracks: List<DemoTrack> = demoProvider.catalog

    private var currentIndex: Int = 0
    private var endMonitorJob: Job? = null

    private val _snapshot = MutableStateFlow(PlaybackSnapshot(status = PlaybackSessionStatus.WAITING_FOR_PLAYBACK))
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    fun startDemo(trackIndex: Int = currentIndex, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) return
        currentIndex = trackIndex.coerceIn(tracks.indices)
        val track = tracks[currentIndex]
        val now = elapsedRealtimeProvider()
        _snapshot.value = PlaybackSnapshot(
            status = PlaybackSessionStatus.ACTIVE_DEMO,
            sourcePackage = "com.example.demo",
            sourceAppLabel = "LyricVerse Demo Stage",
            trackTitle = track.title,
            artistName = track.artist,
            albumName = track.album,
            durationMs = track.durationMs,
            positionMs = 0L,
            playbackSpeed = 1.0f,
            isPlaying = autoPlay,
            lastPositionUpdateElapsedMs = now,
            isDemoMode = true,
        )
        scheduleEndMonitor()
    }

    fun togglePlayPause() {
        val current = _snapshot.value
        if (!current.isDemoMode) {
            startDemo(currentIndex, autoPlay = true)
            return
        }
        val now = elapsedRealtimeProvider()
        val livePos = PlaybackClockExtrapolator.computePositionMs(current, now)
        val nextPlaying = !current.isPlaying
        val resetPos = if (nextPlaying && livePos >= current.durationMs - 250L) 0L else livePos
        _snapshot.value = current.copy(
            positionMs = resetPos,
            isPlaying = nextPlaying,
            lastPositionUpdateElapsedMs = now,
        )
        scheduleEndMonitor()
    }

    fun seekTo(positionMs: Long) {
        val current = _snapshot.value
        if (!current.isDemoMode) return
        val now = elapsedRealtimeProvider()
        val clamped = positionMs.coerceIn(0L, current.durationMs)
        _snapshot.value = current.copy(
            positionMs = clamped,
            lastPositionUpdateElapsedMs = now,
        )
        scheduleEndMonitor()
    }

    fun nextTrack() {
        if (tracks.isEmpty()) return
        val next = (currentIndex + 1) % tracks.size
        startDemo(next, autoPlay = true)
    }

    fun previousTrack() {
        if (tracks.isEmpty()) return
        val prev = if (currentIndex - 1 < 0) tracks.lastIndex else currentIndex - 1
        startDemo(prev, autoPlay = true)
    }

    fun simulateMissingMetadata() {
        endMonitorJob?.cancel()
        _snapshot.value = PlaybackSnapshot(
            status = PlaybackSessionStatus.METADATA_MISSING,
            sourcePackage = "com.example.demo",
            sourceAppLabel = "Simulated Player (No Metadata)",
            trackTitle = "",
            artistName = "",
            albumName = "",
            durationMs = 0L,
            positionMs = 0L,
            isPlaying = true,
            lastPositionUpdateElapsedMs = elapsedRealtimeProvider(),
            isDemoMode = true,
        )
    }

    fun stopDemo() {
        endMonitorJob?.cancel()
        _snapshot.value = PlaybackSnapshot(
            status = PlaybackSessionStatus.SESSION_ENDED,
            isDemoMode = false,
        )
    }

    private fun scheduleEndMonitor() {
        endMonitorJob?.cancel()
        val current = _snapshot.value
        if (!current.isDemoMode || !current.isPlaying || current.durationMs <= 0L) return

        val remainingMs = (current.durationMs - current.positionMs).coerceAtLeast(100L)
        endMonitorJob = scope.launch {
            delay(remainingMs)
            val latest = _snapshot.value
            if (latest.isDemoMode && latest.isPlaying) {
                _snapshot.value = latest.copy(
                    positionMs = latest.durationMs,
                    isPlaying = false,
                    lastPositionUpdateElapsedMs = elapsedRealtimeProvider(),
                )
            }
        }
    }
}
