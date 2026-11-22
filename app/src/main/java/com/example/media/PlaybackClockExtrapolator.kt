package com.example.media

import com.example.domain.model.PlaybackSnapshot

object PlaybackClockExtrapolator {

    fun computePositionMs(
        snapshot: PlaybackSnapshot,
        currentElapsedRealtimeMs: Long,
    ): Long {
        val base = snapshot.positionMs.coerceAtLeast(0L)
        if (!snapshot.isPlaying) {
            return if (snapshot.durationMs > 0L) base.coerceAtMost(snapshot.durationMs) else base
        }

        val elapsedSinceUpdate =
            if (snapshot.lastPositionUpdateElapsedMs > 0L) {
                (currentElapsedRealtimeMs - snapshot.lastPositionUpdateElapsedMs).coerceAtLeast(0L)
            } else {
                0L
            }

        val speed = if (snapshot.playbackSpeed > 0f) snapshot.playbackSpeed else 1.0f
        val extrapolated = base + (elapsedSinceUpdate * speed).toLong()
        return if (snapshot.durationMs > 0L) {
            extrapolated.coerceIn(0L, snapshot.durationMs)
        } else {
            extrapolated.coerceAtLeast(0L)
        }
    }
}
