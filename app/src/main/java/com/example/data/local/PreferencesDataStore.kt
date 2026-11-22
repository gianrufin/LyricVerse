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
import com.example.domain.model.LockscreenOrientation
import com.example.domain.model.LyricColorPreset
import com.example.domain.model.LyricTextAlignment
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
        val ALIGNMENT = stringPreferencesKey("alignment")
        val NEXT_LINE_VISIBLE = booleanPreferencesKey("next_line_visible")
        val WORD_BY_WORD_ENABLED = booleanPreferencesKey("word_by_word_enabled")
        val GLASS_BLUR_ENABLED = booleanPreferencesKey("glass_blur_enabled")
        val SHOW_TRACK_INTRO = booleanPreferencesKey("show_track_intro")
        val LOCKSCREEN_ORIENTATION = stringPreferencesKey("lockscreen_orientation")
        val AUTO_SHOW_ON_LOCK = booleanPreferencesKey("auto_show_on_lock")
        val WAKE_SCREEN_ON_LOCK = booleanPreferencesKey("wake_screen_on_lock")
        val OLED_PIXEL_SHIFT = booleanPreferencesKey("oled_pixel_shift")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SYNC_OFFSET_MS = longPreferencesKey("sync_offset_ms")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val ONLINE_LOOKUP_ENABLED = booleanPreferencesKey("online_lookup_enabled")
        val OFFLINE_CACHE_ENABLED = booleanPreferencesKey("offline_cache_enabled")
        val LOCK_SCREEN_NOTIF_ENABLED = booleanPreferencesKey("lock_screen_notif_enabled")
    }

    val preferencesFlow: Flow<KaraokeStylePreferences> = context.lyricVerseDataStore.data.map { prefs ->
        readFrom(prefs)
    }

    suspend fun update(transform: (KaraokeStylePreferences) -> KaraokeStylePreferences) {
        context.lyricVerseDataStore.edit { prefs ->
            val updated = transform(readFrom(prefs))
            prefs[Keys.FONT_FAMILY] = updated.fontFamily.name
            prefs[Keys.FONT_SIZE_SP] = updated.fontSizeSp
            prefs[Keys.COLOR_PRESET] = updated.colorPreset.name
            prefs[Keys.ALIGNMENT] = updated.alignment.name
            prefs[Keys.NEXT_LINE_VISIBLE] = updated.nextLineVisible
            prefs[Keys.WORD_BY_WORD_ENABLED] = updated.wordByWordEnabled
            prefs[Keys.GLASS_BLUR_ENABLED] = updated.glassBlurEnabled
            prefs[Keys.SHOW_TRACK_INTRO] = updated.showTrackIntroOnStart
            prefs[Keys.LOCKSCREEN_ORIENTATION] = updated.lockscreenOrientation.name
            prefs[Keys.AUTO_SHOW_ON_LOCK] = updated.autoShowOnLockEnabled
            prefs[Keys.WAKE_SCREEN_ON_LOCK] = updated.wakeScreenOnLock
            prefs[Keys.OLED_PIXEL_SHIFT] = updated.oledPixelShiftEnabled
            prefs[Keys.KEEP_SCREEN_ON] = updated.keepScreenOnWhilePlaying
            prefs[Keys.USE_DYNAMIC_COLOR] = updated.useDynamicColor
            prefs[Keys.SYNC_OFFSET_MS] = updated.syncOffsetMs
            prefs[Keys.HAPTICS_ENABLED] = updated.hapticsEnabled
            prefs[Keys.TRACKING_ENABLED] = updated.trackingEnabled
            prefs[Keys.ONLINE_LOOKUP_ENABLED] = updated.onlineLookupEnabled
            prefs[Keys.OFFLINE_CACHE_ENABLED] = updated.offlineCacheEnabled
            prefs[Keys.LOCK_SCREEN_NOTIF_ENABLED] = updated.lockScreenNotificationEnabled
        }
    }

    private fun readFrom(prefs: Preferences): KaraokeStylePreferences {
        return KaraokeStylePreferences(
            fontFamily = prefs[Keys.FONT_FAMILY].parseEnum(KaraokeFontFamily.PLUS_JAKARTA_SANS),
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 28f).coerceIn(20f, 42f),
            colorPreset = prefs[Keys.COLOR_PRESET].parseEnum(LyricColorPreset.STAGE_WHITE),
            alignment = prefs[Keys.ALIGNMENT].parseEnum(LyricTextAlignment.CENTER),
            nextLineVisible = prefs[Keys.NEXT_LINE_VISIBLE] ?: true,
            wordByWordEnabled = prefs[Keys.WORD_BY_WORD_ENABLED] ?: true,
            glassBlurEnabled = prefs[Keys.GLASS_BLUR_ENABLED] ?: true,
            showTrackIntroOnStart = prefs[Keys.SHOW_TRACK_INTRO] ?: true,
            lockscreenOrientation = prefs[Keys.LOCKSCREEN_ORIENTATION].parseEnum(LockscreenOrientation.AUTO),
            autoShowOnLockEnabled = prefs[Keys.AUTO_SHOW_ON_LOCK] ?: false,
            wakeScreenOnLock = prefs[Keys.WAKE_SCREEN_ON_LOCK] ?: true,
            oledPixelShiftEnabled = prefs[Keys.OLED_PIXEL_SHIFT] ?: true,
            keepScreenOnWhilePlaying = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            syncOffsetMs = (prefs[Keys.SYNC_OFFSET_MS] ?: 0L).coerceIn(-3000L, 3000L),
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            trackingEnabled = prefs[Keys.TRACKING_ENABLED] ?: true,
            onlineLookupEnabled = prefs[Keys.ONLINE_LOOKUP_ENABLED] ?: true,
            offlineCacheEnabled = prefs[Keys.OFFLINE_CACHE_ENABLED] ?: true,
            lockScreenNotificationEnabled = prefs[Keys.LOCK_SCREEN_NOTIF_ENABLED] ?: false,
        )
    }

    private inline fun <reified T : Enum<T>> String?.parseEnum(default: T): T {
        if (this == null) return default
        return runCatching { enumValueOf<T>(this) }.getOrDefault(default)
    }
}
