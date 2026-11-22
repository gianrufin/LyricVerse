package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.LrcParser
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricSyncType

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val trackKey: String,
    val trackTitle: String,
    val artistName: String,
    val albumName: String,
    val durationMs: Long,
    val syncType: String,
    val serializedLrc: String,
    val plainText: String,
    val providerId: String,
    val providerDisplayName: String,
    val isNotFound: Boolean,
    val fetchedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) {
    fun toDomainDocument(): LyricDocument {
        val type = runCatching { LyricSyncType.valueOf(syncType) }.getOrDefault(LyricSyncType.NONE)
        val parsedLines = if (type == LyricSyncType.SYNCED) LrcParser.parse(serializedLrc) else emptyList()
        return LyricDocument(
            trackTitle = trackTitle,
            artistName = artistName,
            albumName = albumName,
            durationMs = durationMs,
            syncType = type,
            lines = parsedLines,
            plainText = plainText,
            providerId = providerId,
            providerDisplayName = providerDisplayName,
            allowsCaching = true,
            fromCache = true,
            fetchedAtEpochMs = fetchedAtEpochMs,
            expiresAtEpochMs = expiresAtEpochMs,
        )
    }

    companion object {
        fun buildTrackKey(artist: String, title: String, durationMs: Long): String {
            val normArtist = artist.trim().lowercase()
            val normTitle = title.trim().lowercase()
            val durationBucket = (durationMs / 2000L) * 2L
            return "$normArtist|$normTitle|$durationBucket"
        }
    }
}
