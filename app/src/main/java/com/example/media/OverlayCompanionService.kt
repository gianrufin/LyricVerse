package com.example.media

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Optional foreground companion for:
 * 1) Automatically showing AodLyricsActivity on device lock (ACTION_SCREEN_OFF) while music plays.
 * 2) Displaying a minimal ongoing lock-screen lyric notification when enabled by the user.
 */
class OverlayCompanionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationManager: NotificationManager? = null
    private var observerJob: Job? = null
    private var delayedLockLaunchJob: Job? = null
    private var screenReceiverRegistered = false

    private val lockScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val coordinator = (application as? LyricVerseApplication)?.appContainer?.syncCoordinator ?: return
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    coordinator.resetLockDismissalState()
                    val state = coordinator.syncState.value
                    val prefs = state.preferences
                    if (!prefs.autoShowOnLockEnabled) return
                    val hasPlayableLyrics = state.snapshot.isPlaying &&
                        (state.snapshot.hasValidMetadata || state.lyricsState is LyricsStageState.Loaded)
                    if (!hasPlayableLyrics) return
                    if (coordinator.isAodActivityForeground || coordinator.dismissedAodOnCurrentLock) return

                    delayedLockLaunchJob?.cancel()
                    delayedLockLaunchJob = serviceScope.launch {
                        // Brief settle delay so Android completes keyguard lock transition cleanly
                        delay(380L)
                        val latest = coordinator.syncState.value
                        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                        val stillPlaying = latest.snapshot.isPlaying &&
                            (latest.snapshot.hasValidMetadata || latest.lyricsState is LyricsStageState.Loaded)
                        if (stillPlaying &&
                            latest.preferences.autoShowOnLockEnabled &&
                            !coordinator.isAodActivityForeground &&
                            !coordinator.dismissedAodOnCurrentLock
                        ) {
                            val launchIntent = Intent(this@OverlayCompanionService, AodLyricsActivity::class.java).apply {
                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                                )
                                putExtra(AodLyricsActivity.EXTRA_FROM_LOCK_TRIGGER, true)
                                putExtra(
                                    AodLyricsActivity.EXTRA_WAKE_SCREEN,
                                    latest.preferences.wakeScreenOnLock || keyguard?.isKeyguardLocked == true
                                )
                            }
                            runCatching { startActivity(launchIntent) }
                        }
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    delayedLockLaunchJob?.cancel()
                    coordinator.resetLockDismissalState()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel()
        registerLockScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as? LyricVerseApplication)?.appContainer
        if (container == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP_SERVICE) {
            delayedLockLaunchJob?.cancel()
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
        delayedLockLaunchJob?.cancel()
        observerJob?.cancel()
        unregisterLockScreenReceiver()
        (application as? LyricVerseApplication)?.appContainer?.syncCoordinator?.setCompanionServiceRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerLockScreenReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(lockScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(lockScreenReceiver, filter)
        }
        screenReceiverRegistered = true
    }

    private fun unregisterLockScreenReceiver() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(lockScreenReceiver) }
        screenReceiverRegistered = false
    }

    private fun observeNotificationUpdates(coordinator: LyricSyncCoordinator) {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            coordinator.syncState
                .map { state ->
                    CompanionNotifKey(
                        trackKey = state.snapshot.trackSignatureKey,
                        lineIndex = state.activeLineIndex,
                        lockNotifEnabled = state.preferences.lockScreenNotificationEnabled,
                        autoLockEnabled = state.preferences.autoShowOnLockEnabled,
                    )
                }
                .distinctUntilChanged()
                .collect { key ->
                    if (!key.lockNotifEnabled && !key.autoLockEnabled) {
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

        val title = when {
            state.preferences.lockScreenNotificationEnabled && currentLine != null -> currentLine
            state.snapshot.hasValidMetadata -> "${state.snapshot.trackTitle} · ${state.snapshot.artistName}"
            doc?.syncType == LyricSyncType.PLAIN -> doc.plainText.lineSequence().firstOrNull { it.isNotBlank() }
                ?: getString(R.string.notif_companion_default_title)
            else -> getString(R.string.notif_companion_default_title)
        }

        val bodyText = when {
            state.preferences.lockScreenNotificationEnabled && !nextLine.isNullOrBlank() -> nextLine
            state.preferences.autoShowOnLockEnabled -> "Auto-shows AMOLED lyrics when locked during playback"
            else -> getString(R.string.notif_companion_waiting_text)
        }

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

    private data class CompanionNotifKey(
        val trackKey: String,
        val lineIndex: Int,
        val lockNotifEnabled: Boolean,
        val autoLockEnabled: Boolean,
    )

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
            runCatching { context.startService(intent) }
        }
    }
}
