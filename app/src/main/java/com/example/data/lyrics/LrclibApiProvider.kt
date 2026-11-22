package com.example.data.lyrics

import com.example.domain.model.LrcParser
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricSyncType
import com.example.domain.model.LyricsLookupQuery
import com.example.domain.model.LyricsLookupResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class LrclibApiProvider(
    private val service: LrclibApiService = createDefaultService(),
) : LyricsProvider {

    override val providerId: String = "lrclib_public"
    override val displayName: String = "LRCLIB Open Catalog"
    override val allowsOfflineCaching: Boolean = true

    @Volatile
    private var rateLimitedUntilEpochMs: Long = 0L

    override suspend fun fetchLyrics(query: LyricsLookupQuery): LyricsLookupResult {
        if (query.trackTitle.isBlank() || query.artistName.isBlank()) {
            return LyricsLookupResult.NotFound("Track title and artist are required")
        }

        val now = System.currentTimeMillis()
        if (now < rateLimitedUntilEpochMs) {
            val waitSeconds = ((rateLimitedUntilEpochMs - now) / 1000L).toInt().coerceAtLeast(1)
            return LyricsLookupResult.Error(
                message = "LRCLIB rate limit active. Try again in ${waitSeconds}s.",
                retryAfterSeconds = waitSeconds
            )
        }

        return try {
            val durationSec = if (query.durationMs > 1000L) (query.durationMs / 1000L).toInt() else null
            val response = service.getLyrics(
                trackName = query.trackTitle.trim(),
                artistName = query.artistName.trim(),
                albumName = query.albumName.trim().takeIf { it.isNotEmpty() },
                durationSeconds = durationSec,
            )

            when {
                response.isSuccessful -> {
                    val body = response.body()
                        ?: return LyricsLookupResult.NotFound("Empty response from LRCLIB")
                    LyricsLookupResult.Found(mapDtoToDocument(body))
                }
                response.code() == 404 -> {
                    // Fallback to search endpoint in case album/duration differed slightly
                    val fallback = tryFallbackSearch(query)
                    fallback ?: LyricsLookupResult.NotFound("No lyrics found on LRCLIB for this track")
                }
                response.code() == 429 -> {
                    val retryAfter = response.headers()["Retry-After"]?.toIntOrNull() ?: 30
                    rateLimitedUntilEpochMs = System.currentTimeMillis() + (retryAfter * 1000L)
                    LyricsLookupResult.Error(
                        message = "LRCLIB rate limit reached. Respecting Retry-After (${retryAfter}s).",
                        retryAfterSeconds = retryAfter
                    )
                }
                else -> {
                    LyricsLookupResult.Error("LRCLIB returned HTTP ${response.code()}")
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            LyricsLookupResult.Error("Network unavailable or LRCLIB unreachable: ${e.localizedMessage ?: "error"}")
        }
    }

    override suspend fun searchLyrics(queryText: String): List<LyricDocument> {
        val trimmed = queryText.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (System.currentTimeMillis() < rateLimitedUntilEpochMs) return emptyList()

        return try {
            val response = service.searchLyrics(trimmed)
            if (response.isSuccessful) {
                response.body().orEmpty().take(15).map(::mapDtoToDocument)
            } else {
                emptyList()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun tryFallbackSearch(query: LyricsLookupQuery): LyricsLookupResult? {
        val q = "${query.artistName} ${query.trackTitle}".trim()
        val response = service.searchLyrics(q)
        if (!response.isSuccessful) return null
        val candidates = response.body().orEmpty()
        if (candidates.isEmpty()) return null

        val targetDurationSec = query.durationMs / 1000.0
        val best = candidates
            .sortedByDescending { dto ->
                var score = 0
                if (!dto.syncedLyrics.isNullOrBlank()) score += 10
                if (dto.trackName.equals(query.trackTitle.trim(), ignoreCase = true)) score += 5
                if (dto.artistName.equals(query.artistName.trim(), ignoreCase = true)) score += 5
                if (targetDurationSec > 1.0 && dto.duration != null) {
                    val diff = kotlin.math.abs(dto.duration - targetDurationSec)
                    if (diff <= 2.5) score += 8
                }
                score
            }
            .firstOrNull() ?: return null

        return LyricsLookupResult.Found(mapDtoToDocument(best))
    }

    private fun mapDtoToDocument(dto: LrclibTrackDto): LyricDocument {
        val parsedSynced = dto.syncedLyrics?.let { LrcParser.parse(it) }.orEmpty()
        val plain = dto.plainLyrics.orEmpty().trim()
        val syncType = when {
            dto.instrumental -> LyricSyncType.INSTRUMENTAL
            parsedSynced.isNotEmpty() -> LyricSyncType.SYNCED
            plain.isNotEmpty() -> LyricSyncType.PLAIN
            else -> LyricSyncType.NONE
        }
        return LyricDocument(
            trackTitle = dto.trackName,
            artistName = dto.artistName,
            albumName = dto.albumName.orEmpty(),
            durationMs = ((dto.duration ?: 0.0) * 1000.0).toLong(),
            syncType = syncType,
            lines = parsedSynced,
            plainText = plain,
            providerId = providerId,
            providerDisplayName = displayName,
            allowsCaching = allowsOfflineCaching,
            fromCache = false,
        )
    }

    companion object {
        const val USER_AGENT = "LyricVerse/1.0 (https://lyricverse.app; support@lyricverse.app)"
        private const val BASE_URL = "https://lrclib.net/"

        fun createDefaultService(baseUrl: String = BASE_URL): LrclibApiService {
            val userAgentInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Lrclib-Client", USER_AGENT)
                    .build()
                chain.proceed(request)
            }

            val okHttp = OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(LrclibApiService::class.java)
        }
    }
}
