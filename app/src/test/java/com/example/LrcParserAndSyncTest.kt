package com.example

import com.example.domain.model.LrcParser
import com.example.domain.model.PlaybackSessionStatus
import com.example.domain.model.PlaybackSnapshot
import com.example.media.PlaybackClockExtrapolator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcParserAndSyncTest {

    @Test
    fun `parses synced LRC timestamps, multi-timestamps, and offset tag in sorted order`() {
        val raw = """
            [ti:Neon Horizon]
            [offset:100]
            [00:10.00][00:30.50]Repeated chorus line
            [00:05.250]Opening verse line
        """.trimIndent()

        val lines = LrcParser.parse(raw)
        assertEquals(3, lines.size)
        assertEquals(5_350L, lines[0].timestampMs)
        assertEquals("Opening verse line", lines[0].text)
        assertEquals(10_100L, lines[1].timestampMs)
        assertEquals("Repeated chorus line", lines[1].text)
        assertEquals(30_600L, lines[2].timestampMs)
        assertEquals("Repeated chorus line", lines[2].text)
    }

    @Test
    fun `binary search finds active line accurately across forward and backward seeks`() {
        val lines = LrcParser.parse(
            """
            [00:02.00]Line one
            [00:06.00]Line two
            [00:12.50]Line three
            """.trimIndent()
        )

        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 1_500L))
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 2_000L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 8_400L))
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 20_000L))
        // Seek backward
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 3_100L))
    }

    @Test
    fun `next transition delay adjusts cleanly for playback speed`() {
        val lines = LrcParser.parse(
            """
            [00:00.00]Start
            [00:04.00]Next
            """.trimIndent()
        )
        val delayAt1x = LrcParser.computeNextTransitionDelayMs(lines, 0, 1_000L, 1.0f)
        val delayAt2x = LrcParser.computeNextTransitionDelayMs(lines, 0, 1_000L, 2.0f)
        assertEquals(3_000L, delayAt1x)
        assertEquals(1_500L, delayAt2x)
        assertNull(LrcParser.computeNextTransitionDelayMs(lines, 1, 5_000L, 1.0f))
    }

    @Test
    fun `clock extrapolator freezes while paused and advances accurately while playing`() {
        val playing = PlaybackSnapshot(
            status = PlaybackSessionStatus.ACTIVE_EXTERNAL,
            trackTitle = "Song",
            artistName = "Artist",
            durationMs = 60_000L,
            positionMs = 10_000L,
            playbackSpeed = 1.0f,
            isPlaying = true,
            lastPositionUpdateElapsedMs = 1_000L,
        )
        assertEquals(13_500L, PlaybackClockExtrapolator.computePositionMs(playing, 4_500L))

        val paused = playing.copy(isPlaying = false, positionMs = 13_500L)
        assertEquals(13_500L, PlaybackClockExtrapolator.computePositionMs(paused, 9_999L))
    }
}
