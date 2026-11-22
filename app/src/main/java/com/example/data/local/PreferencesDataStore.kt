package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.KaraokeStylePreferences
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricTextAlignment
import com.example.domain.model.OverlayDefaultMode
import com.example.domain.model.StageBackgroundStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lyricVerseDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lyricverse_preferences"
)

class PreferencesDataStore(private val context: Context) {

    private object Keys {
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val COLOR_PRESET = stringPreferencesKey("color_preset")
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val ALIGNMENT = stringPreferencesKey("alignment")
        val STAGE_OPACITY = floatPreferencesKey("stage_opacity")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val OVERLAY_DEFAULT_MODE = stringPreferencesKey("overlay_default_mode")
        val OVERLAY_WIDTH_SCALE = floatPreferencesKey("overlay_width_scale")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SYNC_OFFSET_MS = longPreferencesKey("sync_offset_ms")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val ONLINE_LOOKUP_ENABLED = booleanPreferencesKey("online_lookup_enabled")
        val OFFLINE_CACHE_ENABLED = booleanPreferencesKey("offline_cache_enabled")
        val LOCK_SCREEN_NOTIF_ENABLED = booleanPreferencesKey("lock_screen_notif_enabled")
    }

    val preferencesFlow: Flow<KaraokeStylePreferences> = context.lyricVerseDataStore.data.map { prefs ->
        KaraokeStylePreferences(
            fontFamily = prefs[Keys.FONT_FAMILY].parseEnum(KaraokeFontFamily.PLUS_JAKARTA_SANS),
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 26f).coerceIn(18f, 38f),
            colorPreset = prefs[Keys.COLOR_PRESET].parseEnum(LyricColorPreset.STAGE_WHITE),
            backgroundStyle = prefs[Keys.BACKGROUND_STYLE].parseEnum(StageBackgroundStyle.CONCERT_SPOTLIGHT),
            alignment = prefs[Keys.ALIGNMENT].parseEnum(LyricTextAlignment.START),
            stageOpacity = (prefs[Keys.STAGE_OPACITY] ?: 1.0f).coerceIn(0.45f, 1.0f),
            overlayOpacity = (prefs[Keys.OVERLAY_OPACITY] ?: 0.90f).coerceIn(0.50f, 1.0f),
            overlayDefaultMode = prefs[Keys.OVERLAY_DEFAULT_MODE].parseEnum(OverlayDefaultMode.TWO_LINE_PILL),
            overlayWidthScale = (prefs[Keys.OVERLAY_WIDTH_SCALE] ?: 1.0f).coerceIn(0.75f, 1.25f),
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            syncOffsetMs = (prefs[Keys.SYNC_OFFSET_MS] ?: 0L).coerceIn(-3000L, 3000L),
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            trackingEnabled = prefs[Keys.TRACKING_ENABLED] ?: true,
            onlineLookupEnabled = prefs[Keys.ONLINE_LOOKUP_ENABLED] ?: true,
            offlineCacheEnabled = prefs[Keys.OFFLINE_CACHE_ENABLED] ?: true,
            lockScreenNotificationEnabled = prefs[Keys.LOCK_SCREEN_NOTIF_ENABLED] ?: true,
        )
    }

    suspend fun update(transform: (KaraokeStylePreferences) -> KaraokeStylePreferences) {
        context.lyricVerseDataStore.edit { prefs ->
            val current = KaraokeStylePreferences(
                fontFamily = prefs[Keys.FONT_FAMILY].parseEnum(KaraokeFontFamily.PLUS_JAKARTA_SANS),
                fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 26f).coerceIn(18f, 38f),
                colorPreset = prefs[Keys.COLOR_PRESET].parseEnum(LyricColorPreset.STAGE_WHITE),
                backgroundStyle = prefs[Keys.BACKGROUND_STYLE].parseEnum(StageBackgroundStyle.CONCERT_SPOTLIGHT),
                alignment = prefs[Keys.ALIGNMENT].parseEnum(LyricTextAlignment.START),
                stageOpacity = (prefs[Keys.STAGE_OPACITY] ?: 1.0f).coerceIn(0.45f, 1.0f),
                overlayOpacity = (prefs[Keys.OVERLAY_OPACITY] ?: 0.90f).coerceIn(0.50f, 1.0f),
                overlayDefaultMode = prefs[Keys.OVERLAY_DEFAULT_MODE].parseEnum(OverlayDefaultMode.TWO_LINE_PILL),
                overlayWidthScale = (prefs[Keys.OVERLAY_WIDTH_SCALE] ?: 1.0f).coerceIn(0.75f, 1.25f),
                useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
                syncOffsetMs = (prefs[Keys.SYNC_OFFSET_MS] ?: 0L).coerceIn(-3000L, 3000L),
                hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
                trackingEnabled = prefs[Keys.TRACKING_ENABLED] ?: true,
                onlineLookupEnabled = prefs[Keys.ONLINE_LOOKUP_ENABLED] ?: true,
                offlineCacheEnabled = prefs[Keys.OFFLINE_CACHE_ENABLED] ?: true,
                lockScreenNotificationEnabled = prefs[Keys.LOCK_SCREEN_NOTIF_ENABLED] ?: true,
            )
            val updated = transform(current)
            prefs[Keys.FONT_FAMILY] = updated.fontFamily.name
            prefs[Keys.FONT_SIZE_SP] = updated.fontSizeSp
            prefs[Keys.COLOR_PRESET] = updated.colorPreset.name
            prefs[Keys.BACKGROUND_STYLE] = updated.backgroundStyle.name
            prefs[Keys.ALIGNMENT] = updated.alignment.name
            prefs[Keys.STAGE_OPACITY] = updated.stageOpacity
            prefs[Keys.OVERLAY_OPACITY] = updated.overlayOpacity
            prefs[Keys.OVERLAY_DEFAULT_MODE] = updated.overlayDefaultMode.name
            prefs[Keys.OVERLAY_WIDTH_SCALE] = updated.overlayWidthScale
            prefs[Keys.USE_DYNAMIC_COLOR] = updated.useDynamicColor
            prefs[Keys.SYNC_OFFSET_MS] = updated.syncOffsetMs
            prefs[Keys.HAPTICS_ENABLED] = updated.hapticsEnabled
            prefs[Keys.TRACKING_ENABLED] = updated.trackingEnabled
            prefs[Keys.ONLINE_LOOKUP_ENABLED] = updated.onlineLookupEnabled
            prefs[Keys.OFFLINE_CACHE_ENABLED] = updated.offlineCacheEnabled
            prefs[Keys.LOCK_SCREEN_NOTIF_ENABLED] = updated.lockScreenNotificationEnabled
        }
    }

    private inline fun <reified T : Enum<T>> String?.parseEnum(default: T): T {
        if (this == null) return default
        return runCatching { enumValueOf<T>(this) }.getOrDefault(default)
    }
}
