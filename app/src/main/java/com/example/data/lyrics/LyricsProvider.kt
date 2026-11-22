package com.example.data.lyrics

import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricsLookupQuery
import com.example.domain.model.LyricsLookupResult

interface LyricsProvider {
    val providerId: String
    val displayName: String
    val allowsOfflineCaching: Boolean

    suspend fun fetchLyrics(query: LyricsLookupQuery): LyricsLookupResult

    suspend fun searchLyrics(queryText: String): List<LyricDocument>
}
