package com.example.domain.model

enum class LyricSyncType {
    SYNCED,
    PLAIN,
    INSTRUMENTAL,
    NONE,
}

data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

data class LyricLine(
    val index: Int,
    val timestampMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
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

enum class FontCategory(val label: String) {
    STAGE("Stage Clean"),
    PREMIUM("Premium Editorial"),
    FUN("Fun & Expressive"),
}

enum class KaraokeFontFamily(val label: String, val category: FontCategory) {
    PLUS_JAKARTA_SANS("Jakarta Stage", FontCategory.STAGE),
    OUTFIT("Outfit Rounded", FontCategory.STAGE),
    SPACE_GROTESK("Space Grotesk", FontCategory.STAGE),
    JETBRAINS_MONO("JetBrains Mono", FontCategory.STAGE),

    INSTRUMENT_SERIF("Instrument Serif", FontCategory.PREMIUM),
    CORMORANT_GARAMOND("Cormorant Luxury", FontCategory.PREMIUM),
    PLAYFAIR_DISPLAY("Playfair Serif", FontCategory.PREMIUM),
    SYNE("Syne Editorial", FontCategory.PREMIUM),

    FREDOKA("Fredoka Pop", FontCategory.FUN),
    BRICOLAGE_GROTESQUE("Bricolage Indie", FontCategory.FUN),
    PACIFICO("Pacifico Brush", FontCategory.FUN),
    CAVEAT("Caveat Script", FontCategory.FUN),
}

enum class LyricTextAlignment(val label: String) {
    CENTER("Centered"),
    START("Left"),
    END("Right"),
}

enum class LockscreenOrientation(val label: String) {
    AUTO("Auto"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
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
    val wordByWordEnabled: Boolean = true,
    val glassBlurEnabled: Boolean = true,
    val showTrackIntroOnStart: Boolean = true,
    val lockscreenOrientation: LockscreenOrientation = LockscreenOrientation.AUTO,
    val autoShowOnLockEnabled: Boolean = false,
    val wakeScreenOnLock: Boolean = true,
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
