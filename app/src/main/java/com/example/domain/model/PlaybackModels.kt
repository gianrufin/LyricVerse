package com.example.domain.model

enum class PlaybackSessionStatus {
    NO_PERMISSION,
    WAITING_FOR_PLAYBACK,
    METADATA_MISSING,
    SESSION_ENDED,
    ACTIVE_EXTERNAL,
    ACTIVE_DEMO,
}

data class PlaybackSnapshot(
    val status: PlaybackSessionStatus = PlaybackSessionStatus.WAITING_FOR_PLAYBACK,
    val sourcePackage: String = "",
    val sourceAppLabel: String = "",
    val trackTitle: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isPlaying: Boolean = false,
    val lastPositionUpdateElapsedMs: Long = 0L,
    val isDemoMode: Boolean = false,
) {
    val hasValidMetadata: Boolean
        get() = trackTitle.isNotBlank() && artistName.isNotBlank()

    val trackSignatureKey: String
        get() {
            val normArtist = artistName.trim().lowercase()
            val normTitle = trackTitle.trim().lowercase()
            val durationBucket = (durationMs / 1000L)
            return "$normArtist|$normTitle|$durationBucket"
        }
}

data class DemoTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val syncedLrc: String?,
    val plainLyrics: String,
    val genreTag: String,
)
