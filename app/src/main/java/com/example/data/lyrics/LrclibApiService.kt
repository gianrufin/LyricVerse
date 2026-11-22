package com.example.data.lyrics

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class LrclibTrackDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "trackName") val trackName: String = "",
    @Json(name = "artistName") val artistName: String = "",
    @Json(name = "albumName") val albumName: String? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "instrumental") val instrumental: Boolean = false,
    @Json(name = "plainLyrics") val plainLyrics: String? = null,
    @Json(name = "syncedLyrics") val syncedLyrics: String? = null,
)

interface LrclibApiService {

    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") durationSeconds: Int? = null,
    ): Response<LrclibTrackDto>

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String,
    ): Response<List<LrclibTrackDto>>
}
