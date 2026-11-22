package com.example

import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricLine
import com.example.domain.model.LyricSyncType
import com.example.domain.model.PlaybackSessionStatus
import com.example.domain.model.PlaybackSnapshot
import com.example.media.CompanionSyncState
import com.example.media.LyricsStageState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AodEligibilityTest {

    private fun sampleSnapshot(title: String = "Track") = PlaybackSnapshot(
        status = PlaybackSessionStatus.ACTIVE_EXTERNAL,
        trackTitle = title,
        artistName = "Creator",
        durationMs = 1_800_000L,
        isPlaying = true,
    )

    private fun doc(
        syncType: LyricSyncType,
        lines: List<LyricLine> = emptyList(),
        plainText: String = "",
    ) = LyricDocument(
        trackTitle = "Track",
        artistName = "Creator",
        syncType = syncType,
        lines = lines,
        plainText = plainText,
        providerId = "lrclib",
        providerDisplayName = "LRCLIB",
    )

    @Test
    fun `displayable lyrics true for synced lyrics with lines and non-blank plain lyrics`() {
        val synced = CompanionSyncState(
            snapshot = sampleSnapshot(),
            lyricsState = LyricsStageState.Loaded(
                doc(
                    syncType = LyricSyncType.SYNCED,
                    lines = listOf(LyricLine(index = 0, timestampMs = 1_000L, text = "First lyric line"))
                )
            )
        )
        assertTrue(synced.hasDisplayableLyrics)

        val plain = CompanionSyncState(
            snapshot = sampleSnapshot(),
            lyricsState = LyricsStageState.Loaded(
                doc(
                    syncType = LyricSyncType.PLAIN,
                    plainText = "Verse line one\nVerse line two"
                )
            )
        )
        assertTrue(plain.hasDisplayableLyrics)
    }

    @Test
    fun `displayable lyrics false for unavailable podcasts vlogs loading idle and instrumental`() {
        val unavailable = CompanionSyncState(
            snapshot = sampleSnapshot("3 Hour Podcast Episode"),
            lyricsState = LyricsStageState.Unavailable("Lyrics not found on LRCLIB")
        )
        assertFalse(unavailable.hasDisplayableLyrics)

        val loading = CompanionSyncState(
            snapshot = sampleSnapshot("YouTube Vlog"),
            lyricsState = LyricsStageState.Loading
        )
        assertFalse(loading.hasDisplayableLyrics)

        val idle = CompanionSyncState(
            snapshot = sampleSnapshot(),
            lyricsState = LyricsStageState.Idle
        )
        assertFalse(idle.hasDisplayableLyrics)

        val instrumental = CompanionSyncState(
            snapshot = sampleSnapshot(),
            lyricsState = LyricsStageState.Loaded(doc(syncType = LyricSyncType.INSTRUMENTAL))
        )
        assertFalse(instrumental.hasDisplayableLyrics)

        val none = CompanionSyncState(
            snapshot = sampleSnapshot(),
            lyricsState = LyricsStageState.Loaded(doc(syncType = LyricSyncType.NONE))
        )
        assertFalse(none.hasDisplayableLyrics)

        val emptySynced = CompanionSyncState(
            snapshot = sampleSnapshot(),
            lyricsState = LyricsStageState.Loaded(doc(syncType = LyricSyncType.SYNCED, lines = emptyList()))
        )
        assertFalse(emptySynced.hasDisplayableLyrics)

        val tagOnlyPlain = CompanionSyncState(
            snapshot = sampleSnapshot(),
            lyricsState = LyricsStageState.Loaded(doc(syncType = LyricSyncType.PLAIN, plainText = "[ar:Artist]\n   "))
        )
        assertFalse(tagOnlyPlain.hasDisplayableLyrics)
    }
}
