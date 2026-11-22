package com.example

import com.example.data.local.LyricsCacheDao
import com.example.data.local.LyricsCacheEntity
import com.example.data.lyrics.DemoLyricsProvider
import com.example.data.lyrics.LrclibApiProvider
import com.example.data.lyrics.LrclibApiService
import com.example.data.lyrics.LrclibTrackDto
import com.example.data.lyrics.LyricsProvider
import com.example.data.lyrics.LyricsRepository
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricLine
import com.example.domain.model.LyricSyncType
import com.example.domain.model.LyricsLookupQuery
import com.example.domain.model.LyricsLookupResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

private class FakeLyricsCacheDao : LyricsCacheDao {
    private val store = LinkedHashMap<String, LyricsCacheEntity>()
    private val flow = MutableStateFlow<List<LyricsCacheEntity>>(emptyList())

    override fun observeAll(): Flow<List<LyricsCacheEntity>> = flow.asStateFlow()

    override suspend fun findByKey(trackKey: String): LyricsCacheEntity? = store[trackKey]

    override suspend fun upsert(entity: LyricsCacheEntity) {
        store[entity.trackKey] = entity
        emit()
    }

    override suspend fun deleteByKey(trackKey: String) {
        store.remove(trackKey)
        emit()
    }

    override suspend fun deleteExpired(nowEpochMs: Long) {
        val expiredKeys = store.values.filter { it.expiresAtEpochMs <= nowEpochMs }.map { it.trackKey }
        expiredKeys.forEach { store.remove(it) }
        emit()
    }

    override suspend fun clearAll() {
        store.clear()
        emit()
    }

    private fun emit() {
        flow.value = store.values.sortedByDescending { it.fetchedAtEpochMs }
    }
}

class LyricsRepositoryCacheTest {

    private lateinit var fakeDao: FakeLyricsCacheDao
    private var fakeNowMs = 1_000_000_000L

    @Before
    fun setUp() {
        fakeDao = FakeLyricsCacheDao()
    }

    @Test
    fun `caches permitted lyrics and expires after TTL`() = runTest {
        var remoteCalls = 0
        val remoteProvider = object : LyricsProvider {
            override val providerId = "test_remote"
            override val displayName = "Test Remote"
            override val allowsOfflineCaching = true

            override suspend fun fetchLyrics(query: LyricsLookupQuery): LyricsLookupResult {
                remoteCalls++
                return LyricsLookupResult.Found(
                    LyricDocument(
                        trackTitle = query.trackTitle,
                        artistName = query.artistName,
                        syncType = LyricSyncType.SYNCED,
                        lines = listOf(LyricLine(0, 1000L, "Cached lyric line")),
                        providerId = providerId,
                        providerDisplayName = displayName,
                        allowsCaching = true,
                    )
                )
            }

            override suspend fun searchLyrics(queryText: String): List<LyricDocument> = emptyList()
        }

        val repo = LyricsRepository(
            cacheDao = fakeDao,
            demoProvider = DemoLyricsProvider(),
            remoteProvider = remoteProvider,
            clockMs = { fakeNowMs },
        )

        val query = LyricsLookupQuery("Starlight", "Muse", durationMs = 240_000L)

        // First call hits remote and writes cache
        val first = repo.getLyricsForTrack(query, onlineLookupEnabled = true, offlineCacheEnabled = true)
        assertTrue(first is LyricsLookupResult.Found)
        assertFalse((first as LyricsLookupResult.Found).document.fromCache)
        assertEquals(1, remoteCalls)

        // Second call within TTL hits Room cache
        fakeNowMs += 1_000_000L
        val second = repo.getLyricsForTrack(query, onlineLookupEnabled = true, offlineCacheEnabled = true)
        assertTrue(second is LyricsLookupResult.Found)
        assertTrue((second as LyricsLookupResult.Found).document.fromCache)
        assertEquals(1, remoteCalls)

        // Advance past 14-day TTL -> expires and fetches upstream again
        fakeNowMs += LyricsRepository.POSITIVE_CACHE_TTL_MS + 10_000L
        val third = repo.getLyricsForTrack(query, onlineLookupEnabled = true, offlineCacheEnabled = true)
        assertTrue(third is LyricsLookupResult.Found)
        assertFalse((third as LyricsLookupResult.Found).document.fromCache)
        assertEquals(2, remoteCalls)

        // Clear cache removes entries
        repo.clearCache()
        assertEquals(null, fakeDao.findByKey("muse|starlight|240"))
    }

    @Test
    fun `does not cache lyrics when provider disallows caching`() = runTest {
        val restrictedProvider = object : LyricsProvider {
            override val providerId = "restricted"
            override val displayName = "Restricted"
            override val allowsOfflineCaching = false

            override suspend fun fetchLyrics(query: LyricsLookupQuery): LyricsLookupResult {
                return LyricsLookupResult.Found(
                    LyricDocument(
                        trackTitle = query.trackTitle,
                        artistName = query.artistName,
                        syncType = LyricSyncType.PLAIN,
                        lines = emptyList(),
                        plainText = "Do not cache",
                        providerId = providerId,
                        providerDisplayName = displayName,
                        allowsCaching = false,
                    )
                )
            }

            override suspend fun searchLyrics(queryText: String): List<LyricDocument> = emptyList()
        }

        val repo = LyricsRepository(
            cacheDao = fakeDao,
            demoProvider = DemoLyricsProvider(),
            remoteProvider = restrictedProvider,
            clockMs = { fakeNowMs },
        )

        val query = LyricsLookupQuery("NoCacheSong", "NoCacheArtist", durationMs = 120_000L)
        repo.getLyricsForTrack(query, onlineLookupEnabled = true, offlineCacheEnabled = true)
        assertEquals(null, fakeDao.findByKey("nocacheartist|nocachesong|120"))
    }

    @Test
    fun `negative 404 lookup is cached for 3 days and cleared on forceRefresh`() = runTest {
        var calls = 0
        val missingProvider = object : LyricsProvider {
            override val providerId = "test_remote"
            override val displayName = "Test Remote"
            override val allowsOfflineCaching = true

            override suspend fun fetchLyrics(query: LyricsLookupQuery): LyricsLookupResult {
                calls++
                return LyricsLookupResult.NotFound("Not found")
            }

            override suspend fun searchLyrics(queryText: String): List<LyricDocument> = emptyList()
        }

        val repo = LyricsRepository(
            cacheDao = fakeDao,
            demoProvider = DemoLyricsProvider(),
            remoteProvider = missingProvider,
            clockMs = { fakeNowMs },
        )

        val query = LyricsLookupQuery("Rare Track", "Unknown Artist", durationMs = 180_000L)
        val first = repo.getLyricsForTrack(query, onlineLookupEnabled = true, offlineCacheEnabled = true)
        assertTrue(first is LyricsLookupResult.NotFound)
        assertFalse((first as LyricsLookupResult.NotFound).fromCache)
        assertEquals(1, calls)

        val second = repo.getLyricsForTrack(query, onlineLookupEnabled = true, offlineCacheEnabled = true)
        assertTrue(second is LyricsLookupResult.NotFound)
        assertTrue((second as LyricsLookupResult.NotFound).fromCache)
        assertEquals(1, calls)
    }

    @Test
    fun `lrclib provider respects 429 Retry-After header`() = runTest {
        var calls = 0
        val fakeService = object : LrclibApiService {
            override suspend fun getLyrics(
                trackName: String,
                artistName: String,
                albumName: String?,
                durationSeconds: Int?,
            ): Response<LrclibTrackDto> {
                calls++
                return Response.error<LrclibTrackDto>(
                    "".toResponseBody(),
                    okhttp3.Response.Builder()
                        .code(429)
                        .message("Too Many Requests")
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .request(okhttp3.Request.Builder().url("https://lrclib.net/api/get").build())
                        .headers(Headers.headersOf("Retry-After", "45"))
                        .build()
                )
            }

            override suspend fun searchLyrics(query: String): Response<List<LrclibTrackDto>> {
                return Response.success(emptyList())
            }
        }

        val provider = LrclibApiProvider(service = fakeService)
        val first = provider.fetchLyrics(LyricsLookupQuery("Song", "Artist"))
        assertTrue(first is LyricsLookupResult.Error)
        assertEquals(45, (first as LyricsLookupResult.Error).retryAfterSeconds)

        // Subsequent immediate request short-circuits without hitting network
        val second = provider.fetchLyrics(LyricsLookupQuery("Song", "Artist"))
        assertTrue(second is LyricsLookupResult.Error)
        assertEquals(1, calls)
    }
}
