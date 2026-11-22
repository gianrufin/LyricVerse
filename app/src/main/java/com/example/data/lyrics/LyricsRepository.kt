package com.example.data.lyrics

import com.example.data.local.LyricsCacheDao
import com.example.data.local.LyricsCacheEntity
import com.example.domain.model.LrcParser
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricSyncType
import com.example.domain.model.LyricsLookupQuery
import com.example.domain.model.LyricsLookupResult
import kotlinx.coroutines.flow.Flow

class LyricsRepository(
    private val cacheDao: LyricsCacheDao,
    val demoProvider: DemoLyricsProvider,
    private val remoteProvider: LyricsProvider,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {

    val cachedEntriesFlow: Flow<List<LyricsCacheEntity>> = cacheDao.observeAll()

    suspend fun getLyricsForTrack(
        query: LyricsLookupQuery,
        onlineLookupEnabled: Boolean,
        offlineCacheEnabled: Boolean,
        forceRefresh: Boolean = false,
    ): LyricsLookupResult {
        if (query.trackTitle.isBlank() || query.artistName.isBlank()) {
            return LyricsLookupResult.NotFound("Missing track title or artist metadata")
        }

        val now = clockMs()
        val trackKey = LyricsCacheEntity.buildTrackKey(
            artist = query.artistName,
            title = query.trackTitle,
            durationMs = query.durationMs
        )

        // 1. Check demo catalog first (zero network latency, always available)
        val demoResult = demoProvider.fetchLyrics(query)
        if (demoResult is LyricsLookupResult.Found) {
            if (offlineCacheEnabled && demoProvider.allowsOfflineCaching) {
                cacheDocument(trackKey, demoResult.document, now)
            }
            return demoResult
        }

        // 2. Check permitted Room offline cache if enabled and not expired
        if (offlineCacheEnabled && !forceRefresh) {
            cacheDao.deleteExpired(now)
            val cached = cacheDao.findByKey(trackKey)
            if (cached != null && cached.expiresAtEpochMs > now) {
                return if (cached.isNotFound) {
                    LyricsLookupResult.NotFound(
                        reason = "Cached miss (retry after expiry or refresh)",
                        fromCache = true
                    )
                } else {
                    LyricsLookupResult.Found(cached.toDomainDocument())
                }
            }
        }

        // 3. Query remote provider if online lookup is enabled
        if (!onlineLookupEnabled) {
            return LyricsLookupResult.NotFound("Online lyrics lookup is turned off in Settings")
        }

        return when (val remoteResult = remoteProvider.fetchLyrics(query)) {
            is LyricsLookupResult.Found -> {
                if (offlineCacheEnabled && remoteResult.document.allowsCaching && remoteProvider.allowsOfflineCaching) {
                    cacheDocument(trackKey, remoteResult.document, now)
                }
                remoteResult
            }
            is LyricsLookupResult.NotFound -> {
                if (offlineCacheEnabled && remoteProvider.allowsOfflineCaching) {
                    cacheNotFound(trackKey, query, remoteProvider.providerId, remoteProvider.displayName, now)
                }
                remoteResult
            }
            is LyricsLookupResult.Error -> remoteResult
        }
    }

    suspend fun searchManual(
        queryText: String,
        onlineLookupEnabled: Boolean,
    ): List<LyricDocument> {
        val localMatches = demoProvider.searchLyrics(queryText)
        if (!onlineLookupEnabled || queryText.isBlank()) {
            return localMatches
        }
        val remoteMatches = remoteProvider.searchLyrics(queryText)
        return (localMatches + remoteMatches).distinctBy {
            "${it.artistName.lowercase()}|${it.trackTitle.lowercase()}|${it.syncType}"
        }
    }

    suspend fun clearCache() {
        cacheDao.clearAll()
    }

    suspend fun deleteCacheEntry(trackKey: String) {
        cacheDao.deleteByKey(trackKey)
    }

    private suspend fun cacheDocument(trackKey: String, doc: LyricDocument, now: Long) {
        val serializedLrc =
            if (doc.syncType == LyricSyncType.SYNCED) LrcParser.toLrcString(doc.lines) else ""
        cacheDao.upsert(
            LyricsCacheEntity(
                trackKey = trackKey,
                trackTitle = doc.trackTitle,
                artistName = doc.artistName,
                albumName = doc.albumName,
                durationMs = doc.durationMs,
                syncType = doc.syncType.name,
                serializedLrc = serializedLrc,
                plainText = doc.plainText,
                providerId = doc.providerId,
                providerDisplayName = doc.providerDisplayName,
                isNotFound = false,
                fetchedAtEpochMs = now,
                expiresAtEpochMs = now + POSITIVE_CACHE_TTL_MS,
            )
        )
    }

    private suspend fun cacheNotFound(
        trackKey: String,
        query: LyricsLookupQuery,
        providerId: String,
        providerDisplayName: String,
        now: Long,
    ) {
        cacheDao.upsert(
            LyricsCacheEntity(
                trackKey = trackKey,
                trackTitle = query.trackTitle,
                artistName = query.artistName,
                albumName = query.albumName,
                durationMs = query.durationMs,
                syncType = LyricSyncType.NONE.name,
                serializedLrc = "",
                plainText = "",
                providerId = providerId,
                providerDisplayName = providerDisplayName,
                isNotFound = true,
                fetchedAtEpochMs = now,
                expiresAtEpochMs = now + NEGATIVE_CACHE_TTL_MS,
            )
        )
    }

    companion object {
        const val POSITIVE_CACHE_TTL_MS = 14L * 24L * 60L * 60L * 1000L // 14 days
        const val NEGATIVE_CACHE_TTL_MS = 3L * 24L * 60L * 60L * 1000L  // 3 days
    }
}
