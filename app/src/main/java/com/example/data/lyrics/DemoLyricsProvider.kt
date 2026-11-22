package com.example.data.lyrics

import com.example.domain.model.DemoTrack
import com.example.domain.model.LrcParser
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricSyncType
import com.example.domain.model.LyricsLookupQuery
import com.example.domain.model.LyricsLookupResult

class DemoLyricsProvider : LyricsProvider {
    override val providerId: String = "demo_lrc_catalog"
    override val displayName: String = "Demo LRC Catalog"
    override val allowsOfflineCaching: Boolean = true

    val catalog: List<DemoTrack> = listOf(
        DemoTrack(
            id = "demo_neon_horizon",
            title = "Neon Horizon",
            artist = "The Midnight Echo",
            album = "Afterglow Sessions",
            durationMs = 54_000L,
            genreTag = "Synthwave · Synced LRC",
            albumLightColors = listOf(0xFF93E5FFL, 0xFFF5B8FFL, 0xFFC7D2FEL),
            syncedLrc = """
                [00:00.50]City lights blur through the midnight rain
                [00:04.20]Static humming down the interstate lane
                [00:08.00]Every chord echoes what we couldn't say
                [00:11.80]Hold the frequency till the break of day
                [00:15.50]We're chasing the neon horizon tonight
                [00:19.60]Turning quiet shadows into stage light
                [00:23.80]Heartbeats locking on a golden wave
                [00:27.90]Singing every word that the night gave
                [00:32.00]♪ Instrumental synth lift ♪
                [00:36.20]Turn the dial up, let the skyline glow
                [00:40.40]Every verse lands right on the tempo
                [00:44.60]We're chasing the neon horizon tonight
                [00:48.80]Until the morning fades out of sight
            """.trimIndent(),
            plainLyrics = """
                City lights blur through the midnight rain
                Static humming down the interstate lane
                Every chord echoes what we couldn't say
                Hold the frequency till the break of day
                We're chasing the neon horizon tonight
                Turning quiet shadows into stage light
                Heartbeats locking on a golden wave
                Singing every word that the night gave
                Turn the dial up, let the skyline glow
                Every verse lands right on the tempo
                We're chasing the neon horizon tonight
                Until the morning fades out of sight
            """.trimIndent()
        ),
        DemoTrack(
            id = "demo_velvet_tempo",
            title = "Velvet Tempo",
            artist = "Clara Vance",
            album = "Late Vinyl",
            durationMs = 46_000L,
            genreTag = "Indie Soul · Synced LRC",
            albumLightColors = listOf(0xFFFDE68AL, 0xFFFDBA74L, 0xFFFECDD3L),
            syncedLrc = """
                [00:00.40]Needle drops softly on the cedar floor
                [00:04.10]Warm amber spilling through the studio door
                [00:08.30]Count it in slow, let the bassline breathe
                [00:12.40]Every small silence has a story underneath
                [00:16.80]Stay in the pocket of this velvet tempo
                [00:21.20]No need to hurry where the melodies go
                [00:25.50]Hands on the keys, catching every spark
                [00:29.80]Lanterns glowing in the concert dark
                [00:34.20]Hold that harmony one breath longer
                [00:38.50]Quiet songs always linger stronger
                [00:42.40]Stay in the pocket of this velvet tempo
            """.trimIndent(),
            plainLyrics = """
                Needle drops softly on the cedar floor
                Warm amber spilling through the studio door
                Count it in slow, let the bassline breathe
                Every small silence has a story underneath
                Stay in the pocket of this velvet tempo
                No need to hurry where the melodies go
                Hands on the keys, catching every spark
                Lanterns glowing in the concert dark
                Hold that harmony one breath longer
                Quiet songs always linger stronger
                Stay in the pocket of this velvet tempo
            """.trimIndent()
        ),
        DemoTrack(
            id = "demo_paper_satellites",
            title = "Paper Satellites",
            artist = "Kairo & The Pines",
            album = "Acoustic Postcards",
            durationMs = 40_000L,
            genreTag = "Acoustic · Plain Fallback",
            albumLightColors = listOf(0xFFA7F3D0L, 0xFFBAE6FDL, 0xFFE0E7FFL),
            syncedLrc = null,
            plainLyrics = """
                [Plain Lyrics Demo — No Timestamps Supplied]

                Folded maps across the dashboard glass
                Watching mile markers quietly pass
                Radio fades between two mountain towns
                Acoustic strings turning worries down

                Send our wishes on paper satellites
                Drifting above the valley lights
                Even without a clock on every line
                Plain lyrics stay readable and clean by design
            """.trimIndent()
        )
    )

    override suspend fun fetchLyrics(query: LyricsLookupQuery): LyricsLookupResult {
        val normTitle = query.trackTitle.trim().lowercase()
        val normArtist = query.artistName.trim().lowercase()

        val match = catalog.firstOrNull { track ->
            track.title.lowercase() == normTitle &&
                (normArtist.isBlank() || track.artist.lowercase() == normArtist)
        } ?: return LyricsLookupResult.NotFound("Not in local demo catalog")

        return LyricsLookupResult.Found(toDocument(match))
    }

    override suspend fun searchLyrics(queryText: String): List<LyricDocument> {
        val q = queryText.trim().lowercase()
        if (q.isEmpty()) return catalog.map(::toDocument)
        return catalog
            .filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
            .map(::toDocument)
    }

    fun toDocument(track: DemoTrack): LyricDocument {
        val syncedLines = track.syncedLrc?.let { LrcParser.parse(it) }.orEmpty()
        val syncType = if (syncedLines.isNotEmpty()) LyricSyncType.SYNCED else LyricSyncType.PLAIN
        return LyricDocument(
            trackTitle = track.title,
            artistName = track.artist,
            albumName = track.album,
            durationMs = track.durationMs,
            syncType = syncType,
            lines = syncedLines,
            plainText = track.plainLyrics,
            providerId = providerId,
            providerDisplayName = displayName,
            allowsCaching = true,
            fromCache = false,
        )
    }
}
