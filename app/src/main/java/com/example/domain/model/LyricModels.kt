package com.example.domain.model

enum class LyricSyncType {
    SYNCED,
    PLAIN,
    INSTRUMENTAL,
    NONE,
}

data class LyricLine(
    val index: Int,
    val timestampMs: Long,
    val text: String,
)

data class LyricDocument(
    val trackTitle: String,
    val artistName: String,
    val albumName: String = "",
    val durationMs: Long = 0L,
    val syncType: LyricSyncType,
    val lines: List<LyricLine>,
    val plainText: String = "",
    val providerId: String,
    val providerDisplayName: String,
    val allowsCaching: Boolean = true,
    val fromCache: Boolean = false,
    val fetchedAtEpochMs: Long = System.currentTimeMillis(),
    val expiresAtEpochMs: Long = 0L,
)

data class LyricsLookupQuery(
    val trackTitle: String,
    val artistName: String,
    val albumName: String = "",
    val durationMs: Long = 0L,
)

sealed interface LyricsLookupResult {
    data class Found(val document: LyricDocument) : LyricsLookupResult
    data class NotFound(val reason: String, val fromCache: Boolean = false) : LyricsLookupResult
    data class Error(val message: String, val retryAfterSeconds: Int? = null) : LyricsLookupResult
}

enum class KaraokeFontFamily(val label: String) {
    PLUS_JAKARTA_SANS("Jakarta Stage"),
    OUTFIT("Outfit Rounded"),
    PLAYFAIR_DISPLAY("Playfair Serif"),
    JETBRAINS_MONO("JetBrains Mono"),
}

enum class LyricTextAlignment(val label: String) {
    CENTER("Centered"),
    START("Left"),
    END("Right"),
}

enum class LyricColorPreset(val label: String) {
    STAGE_WHITE("Pure White"),
    ELECTRIC_VIOLET("Electric Violet"),
    WARM_AMBER("Stage Amber"),
    STAGE_CYAN("Neon Cyan"),
    DYNAMIC_PRIMARY("M3 Accent"),
}

data class KaraokeStylePreferences(
    val fontFamily: KaraokeFontFamily = KaraokeFontFamily.PLUS_JAKARTA_SANS,
    val fontSizeSp: Float = 28f,
    val colorPreset: LyricColorPreset = LyricColorPreset.STAGE_WHITE,
    val alignment: LyricTextAlignment = LyricTextAlignment.CENTER,
    val nextLineVisible: Boolean = true,
    val oledPixelShiftEnabled: Boolean = true,
    val keepScreenOnWhilePlaying: Boolean = true,
    val useDynamicColor: Boolean = true,
    val syncOffsetMs: Long = 0L,
    val hapticsEnabled: Boolean = true,
    val trackingEnabled: Boolean = true,
    val onlineLookupEnabled: Boolean = true,
    val offlineCacheEnabled: Boolean = true,
    val lockScreenNotificationEnabled: Boolean = false,
)
