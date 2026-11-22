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

enum class StageBackgroundStyle(val label: String) {
    CONCERT_SPOTLIGHT("Concert Spotlight"),
    DYNAMIC_OBSIDIAN("Dynamic M3"),
    DEEP_VELVET("Velvet Plum"),
    PURE_STAGE_BLACK("Pure Obsidian"),
}

enum class LyricTextAlignment(val label: String) {
    START("Left"),
    CENTER("Center"),
    END("Right"),
}

enum class LyricColorPreset(val label: String) {
    STAGE_WHITE("Stage White"),
    ELECTRIC_VIOLET("Electric Violet"),
    WARM_AMBER("Stage Amber"),
    STAGE_CYAN("Neon Cyan"),
    DYNAMIC_PRIMARY("M3 Accent"),
}

enum class OverlayDefaultMode(val label: String) {
    TWO_LINE_PILL("2-Line Pill"),
    APPLE_STYLE_CARD("Expanded Card"),
}

data class KaraokeStylePreferences(
    val fontFamily: KaraokeFontFamily = KaraokeFontFamily.PLUS_JAKARTA_SANS,
    val fontSizeSp: Float = 26f,
    val colorPreset: LyricColorPreset = LyricColorPreset.STAGE_WHITE,
    val backgroundStyle: StageBackgroundStyle = StageBackgroundStyle.CONCERT_SPOTLIGHT,
    val alignment: LyricTextAlignment = LyricTextAlignment.START,
    val stageOpacity: Float = 1.0f,
    val overlayOpacity: Float = 0.90f,
    val overlayDefaultMode: OverlayDefaultMode = OverlayDefaultMode.TWO_LINE_PILL,
    val overlayWidthScale: Float = 1.0f,
    val useDynamicColor: Boolean = true,
    val syncOffsetMs: Long = 0L,
    val hapticsEnabled: Boolean = true,
    val trackingEnabled: Boolean = true,
    val onlineLookupEnabled: Boolean = true,
    val offlineCacheEnabled: Boolean = true,
    val lockScreenNotificationEnabled: Boolean = true,
)
