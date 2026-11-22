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
}

enum class KaraokeFontFamily(val label: String, val category: FontCategory) {
    PLUS_JAKARTA_SANS("Jakarta", FontCategory.STAGE),
    OUTFIT("Outfit", FontCategory.STAGE),
    SPACE_GROTESK("Space Grotesk", FontCategory.STAGE),
    SORA("Sora", FontCategory.STAGE),
    DM_SANS("DM Sans", FontCategory.STAGE),
    JETBRAINS_MONO("JetBrains Mono", FontCategory.STAGE),

    INSTRUMENT_SERIF("Instrument", FontCategory.PREMIUM),
    CORMORANT_GARAMOND("Cormorant", FontCategory.PREMIUM),
    PLAYFAIR_DISPLAY("Playfair", FontCategory.PREMIUM),
    SYNE("Syne", FontCategory.PREMIUM),
    FRAUNCES("Fraunces", FontCategory.PREMIUM),
    CINZEL("Cinzel", FontCategory.PREMIUM),
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
    ALBUM_ART("Album Color"),
    STAGE_WHITE("Pure White"),
    ELECTRIC_VIOLET("Electric Violet"),
    WARM_AMBER("Stage Amber"),
    STAGE_CYAN("Neon Cyan"),
    DYNAMIC_PRIMARY("M3 Accent"),
}

enum class LyricTransitionStyle(val label: String, val description: String) {
    APPLE_GLASS("Glass Dissolve", "Frosted glass blur entry with gentle vertical lift"),
    STAGE_SLIDE("Stage Glide", "Crisp vertical glide with smooth depth scale"),
    SILK_FADE("Silk Crossfade", "Smooth atmospheric dissolve with zero vertical jump"),
    SPOTLIGHT_POP("Spotlight Pop", "Elastic scale reveal into razor-sharp focus"),
    TYPEWRITER_SNAP("Instant Snap", "Zero-lag crisp cut for rapid lyrics"),
}

enum class LyricTransitionSpeed(val label: String, val multiplier: Float) {
    SNAPPY("Fast", 0.65f),
    BALANCED("Balanced", 1.0f),
    CINEMATIC("Cinematic", 1.45f),
}

data class KaraokeStylePreferences(
    val fontFamily: KaraokeFontFamily = KaraokeFontFamily.PLUS_JAKARTA_SANS,
    val fontSizeSp: Float = 28f,
    val colorPreset: LyricColorPreset = LyricColorPreset.ALBUM_ART,
    val alignment: LyricTextAlignment = LyricTextAlignment.CENTER,
    val nextLineVisible: Boolean = true,
    val wordByWordEnabled: Boolean = true,
    val glassBlurEnabled: Boolean = true,
    val transitionStyle: LyricTransitionStyle = LyricTransitionStyle.APPLE_GLASS,
    val transitionSpeed: LyricTransitionSpeed = LyricTransitionSpeed.BALANCED,
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
