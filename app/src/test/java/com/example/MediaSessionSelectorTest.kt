package com.example

import com.example.media.MediaSessionSelector
import com.example.media.MediaSessionSelector.CandidateSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaSessionSelectorTest {

    @Test
    fun `empty candidate list returns null`() {
        assertNull(MediaSessionSelector.selectBestIndex(emptyList()))
    }

    @Test
    fun `prefers playing music app with metadata over Android Auto projection bridge without metadata`() {
        val candidates = listOf(
            CandidateSummary(
                packageName = "com.google.android.projection.gearhead",
                isPlaying = true,
                hasTitle = false,
                hasArtist = false,
                hasMetadata = false,
            ),
            CandidateSummary(
                packageName = "com.spotify.music",
                isPlaying = true,
                hasTitle = true,
                hasArtist = true,
                hasMetadata = true,
            ),
        )
        assertEquals(1, MediaSessionSelector.selectBestIndex(candidates))
    }

    @Test
    fun `prefers paused music app with track metadata over idle system bridge`() {
        val candidates = listOf(
            CandidateSummary(
                packageName = "com.google.android.googlequicksearchbox",
                isPlaying = false,
                hasTitle = false,
                hasArtist = false,
                hasMetadata = false,
            ),
            CandidateSummary(
                packageName = "com.google.android.apps.youtube.music",
                isPlaying = false,
                hasTitle = true,
                hasArtist = true,
                hasMetadata = true,
            ),
        )
        assertEquals(1, MediaSessionSelector.selectBestIndex(candidates))
    }
}
