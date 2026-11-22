package com.example.media

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.AodLyricsActivity
import com.example.LyricVerseApplication
import com.example.R

@RequiresApi(Build.VERSION_CODES.N)
class AodLyricsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val coordinator = (application as? LyricVerseApplication)?.appContainer?.syncCoordinator
        val snap = coordinator?.syncState?.value?.snapshot
        val tile = qsTile ?: return
        tile.label = getString(R.string.qs_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notif_lyric)
        val hasActiveLyrics = snap?.hasValidMetadata == true
        tile.state = if (hasActiveLyrics) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (hasActiveLyrics) {
                getString(R.string.qs_tile_subtitle_active)
            } else {
                getString(R.string.qs_tile_subtitle_waiting)
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, AodLyricsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                301,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
