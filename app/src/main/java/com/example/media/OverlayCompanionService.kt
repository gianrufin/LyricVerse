package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.AodLyricsActivity
import com.example.LyricVerseApplication
import com.example.R
import com.example.domain.model.LyricSyncType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Optional foreground companion strictly for showing a minimal lock-screen lyric notification
 * when the user enables "Lock-Screen Lyric Notification" in Settings.
 */
class OverlayCompanionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as? LyricVerseApplication)?.appContainer
        if (container == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP_SERVICE) {
            container.syncCoordinator.setCompanionServiceRunning(false)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithNotification(container.syncCoordinator.syncState.value)
        container.syncCoordinator.setCompanionServiceRunning(true)
        observeNotificationUpdates(container.syncCoordinator)
        return START_STICKY
    }

    override fun onDestroy() {
        (application as? LyricVerseApplication)?.appContainer?.syncCoordinator?.setCompanionServiceRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeNotificationUpdates(coordinator: LyricSyncCoordinator) {
        serviceScope.launch {
            coordinator.syncState
                .map { state ->
                    Triple(
                        state.snapshot.trackTitle to state.snapshot.artistName,
                        state.activeLineIndex,
                        state.preferences.lockScreenNotificationEnabled
                    )
                }
                .distinctUntilChanged()
                .collect { (_, _, enabled) ->
                    if (!enabled) {
                        ServiceCompat.stopForeground(
                            this@OverlayCompanionService,
                            ServiceCompat.STOP_FOREGROUND_REMOVE
                        )
                        stopSelf()
                    } else {
                        notificationManager?.notify(
                            NOTIFICATION_ID,
                            buildNotification(coordinator.syncState.value)
                        )
                    }
                }
        }
    }

    private fun startForegroundWithNotification(state: CompanionSyncState) {
        val notification = buildNotification(state)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_companion_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_companion_desc)
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: CompanionSyncState): Notification {
        val openAodIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, AodLyricsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, OverlayCompanionService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.getService(
                this,
                101,
                Intent(this, OverlayCompanionService::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE }
                ?: (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        val doc = (state.lyricsState as? LyricsStageState.Loaded)?.document
        val currentLine = if (doc != null && state.activeLineIndex in doc.lines.indices) {
            doc.lines[state.activeLineIndex].text
        } else {
            null
        }
        val nextLine = if (doc != null && (state.activeLineIndex + 1) in doc.lines.indices) {
            doc.lines[state.activeLineIndex + 1].text
        } else {
            null
        }

        val title = currentLine ?: when {
            doc?.syncType == LyricSyncType.PLAIN -> doc.plainText.lineSequence().firstOrNull { it.isNotBlank() }
                ?: getString(R.string.notif_companion_default_title)
            else -> getString(R.string.notif_companion_waiting_text)
        }

        val bodyText = nextLine ?: ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_lyric)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAodIntent)
            .addAction(0, getString(R.string.notif_action_open_aod), openAodIntent)
            .addAction(0, getString(R.string.notif_action_stop), stopIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "lyricverse_companion_channel"
        private const val NOTIFICATION_ID = 4201
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"

        fun startCompanion(context: Context) {
            val intent = Intent(context, OverlayCompanionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopCompanion(context: Context) {
            val intent = Intent(context, OverlayCompanionService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
