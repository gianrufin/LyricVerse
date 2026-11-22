package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.LyricVerseApplication
import com.example.MainActivity
import com.example.R
import com.example.domain.model.KaraokeFontFamily
import com.example.domain.model.LyricSyncType
import com.example.domain.model.OverlayDefaultMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LyricActive
import com.example.ui.theme.LyricPast
import com.example.ui.theme.LyricUpcoming
import com.example.ui.theme.LyricVerseTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageViolet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OverlayCompanionService :
    Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceViewModelStore = ViewModelStore()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = serviceViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as? LyricVerseApplication)?.appContainer
        if (container == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                container.syncCoordinator.setCompanionServiceRunning(false)
                removeOverlayWindow()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_HIDE_OVERLAY -> {
                container.syncCoordinator.setOverlayVisible(false)
                removeOverlayWindow()
            }
            ACTION_TOGGLE_OVERLAY -> {
                val next = !container.syncCoordinator.syncState.value.overlayVisible
                if (next && Settings.canDrawOverlays(this)) {
                    container.syncCoordinator.setOverlayVisible(true)
                    attachOverlayWindow(container.syncCoordinator)
                } else {
                    container.syncCoordinator.setOverlayVisible(false)
                    removeOverlayWindow()
                }
            }
            ACTION_SHOW_OVERLAY, null -> {
                startForegroundWithNotification(container.syncCoordinator.syncState.value)
                container.syncCoordinator.setCompanionServiceRunning(true)
                if (Settings.canDrawOverlays(this)) {
                    container.syncCoordinator.setOverlayVisible(true)
                    attachOverlayWindow(container.syncCoordinator)
                }
            }
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        observeNotificationUpdates(container.syncCoordinator)
        return START_STICKY
    }

    override fun onDestroy() {
        val container = (application as? LyricVerseApplication)?.appContainer
        container?.syncCoordinator?.setCompanionServiceRunning(false)
        removeOverlayWindow()
        serviceScope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceViewModelStore.clear()
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
                        state.preferences.lockScreenNotificationEnabled to state.overlayVisible
                    )
                }
                .distinctUntilChanged()
                .collect {
                    val state = coordinator.syncState.value
                    notificationManager?.notify(NOTIFICATION_ID, buildNotification(state))
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
        val openStageIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, OverlayCompanionService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this,
            102,
            Intent(this, OverlayCompanionService::class.java).apply { action = ACTION_TOGGLE_OVERLAY },
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

        val title = if (state.snapshot.hasValidMetadata) {
            "${state.snapshot.trackTitle} — ${state.snapshot.artistName}"
        } else {
            getString(R.string.notif_companion_default_title)
        }

        val contentText = when {
            currentLine != null && nextLine != null -> "$currentLine\n$nextLine"
            currentLine != null -> currentLine
            doc?.syncType == LyricSyncType.PLAIN -> "Plain lyrics ready in LyricVerse"
            else -> getString(R.string.notif_companion_waiting_text)
        }

        val visibility = if (state.preferences.lockScreenNotificationEnabled) {
            NotificationCompat.VISIBILITY_PUBLIC
        } else {
            NotificationCompat.VISIBILITY_PRIVATE
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_lyric)
            .setContentTitle(title)
            .setContentText(currentLine ?: contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(visibility)
            .setContentIntent(openStageIntent)
            .addAction(0, getString(R.string.notif_action_toggle_overlay), toggleIntent)
            .addAction(0, getString(R.string.notif_action_stop), stopIntent)
            .build()
    }

    private fun attachOverlayWindow(coordinator: LyricSyncCoordinator) {
        if (overlayView != null || !Settings.canDrawOverlays(this)) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 140
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@OverlayCompanionService)
            setViewTreeViewModelStoreOwner(this@OverlayCompanionService)
            setViewTreeSavedStateRegistryOwner(this@OverlayCompanionService)
            setContent {
                val state by coordinator.syncState.collectAsStateWithLifecycle()
                LyricVerseTheme(useDynamicColor = state.preferences.useDynamicColor) {
                    FloatingLyricOverlayContent(
                        state = state,
                        onDrag = { dx, dy ->
                            params.x += dx.roundToInt()
                            params.y += dy.roundToInt()
                            runCatching { windowManager?.updateViewLayout(this, params) }
                        },
                        onClose = {
                            startService(
                                Intent(this@OverlayCompanionService, OverlayCompanionService::class.java)
                                    .apply { action = ACTION_HIDE_OVERLAY }
                            )
                        }
                    )
                }
            }
        }

        runCatching {
            windowManager?.addView(composeView, params)
            overlayView = composeView
            layoutParams = params
        }
    }

    private fun removeOverlayWindow() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        overlayView = null
        layoutParams = null
    }

    companion object {
        private const val CHANNEL_ID = "lyricverse_companion_channel"
        private const val NOTIFICATION_ID = 4201
        const val ACTION_SHOW_OVERLAY = "com.example.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.action.HIDE_OVERLAY"
        const val ACTION_TOGGLE_OVERLAY = "com.example.action.TOGGLE_OVERLAY"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"

        fun startWithOverlay(context: Context) {
            val intent = Intent(context, OverlayCompanionService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }
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

@Composable
fun FloatingLyricOverlayContent(
    state: CompanionSyncState,
    onDrag: (Float, Float) -> Unit,
    onClose: () -> Unit,
) {
    val prefs = state.preferences
    val haptics = LocalHapticFeedback.current
    var expanded by remember(prefs.overlayDefaultMode) {
        mutableStateOf(prefs.overlayDefaultMode == OverlayDefaultMode.APPLE_STYLE_CARD)
    }
    var localScale by remember(prefs.overlayWidthScale) {
        mutableFloatStateOf(prefs.overlayWidthScale)
    }

    val fontFamily = when (prefs.fontFamily) {
        KaraokeFontFamily.PLUS_JAKARTA_SANS -> PlusJakartaSansFamily
        KaraokeFontFamily.OUTFIT -> OutfitFontFamily
        KaraokeFontFamily.PLAYFAIR_DISPLAY -> PlayfairDisplayFamily
        KaraokeFontFamily.JETBRAINS_MONO -> JetBrainsMonoFamily
    }

    val doc = (state.lyricsState as? LyricsStageState.Loaded)?.document
    val lines = doc?.lines.orEmpty()
    val activeIdx = state.activeLineIndex
    val activeText = when {
        doc == null && !state.snapshot.hasValidMetadata -> "Waiting for music playback…"
        doc == null -> "Searching lyrics for ${state.snapshot.trackTitle}…"
        doc.syncType == LyricSyncType.PLAIN -> doc.plainText.lineSequence().firstOrNull { it.isNotBlank() }
            ?: "Plain lyrics loaded"
        activeIdx in lines.indices -> lines[activeIdx].text
        lines.isNotEmpty() -> lines.first().text
        else -> "No synced lines available"
    }
    val nextText = if (doc?.syncType == LyricSyncType.SYNCED && (activeIdx + 1) in lines.indices) {
        lines[activeIdx + 1].text
    } else if (state.snapshot.hasValidMetadata) {
        "${state.snapshot.trackTitle} · ${state.snapshot.artistName}"
    } else {
        "Drag to move · Tap expand for full card"
    }

    val cardWidth = (if (expanded) 320f else 296f) * localScale.coerceIn(0.8f, 1.2f)
    val surfaceColor = StageObsidian.copy(alpha = prefs.overlayOpacity.coerceIn(0.55f, 0.96f))

    Surface(
        modifier = Modifier
            .width(cardWidth.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        StageViolet.copy(alpha = 0.45f),
                        StageAmber.copy(alpha = 0.30f)
                    )
                ),
                shape = RoundedCornerShape(if (expanded) 24.dp else 28.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        color = surfaceColor,
        shape = RoundedCornerShape(if (expanded) 24.dp else 28.dp),
        tonalElevation = 8.dp,
        shadowElevation = 14.dp
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "overlay_expand_mode"
        ) { isExpanded ->
            if (!isExpanded) {
                // Default 2-Line Floating Pill (Apple Music-inspired active + next line)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragIndicator,
                        contentDescription = "Drag floating lyrics overlay",
                        tint = LyricPast,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeText,
                            color = LyricActive,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = nextText,
                            color = LyricPast,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            if (prefs.hapticsEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            expanded = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = "Expand floating lyrics card",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (prefs.hapticsEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onClose()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hide floating lyrics overlay",
                            tint = LyricUpcoming,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Expanded Apple Music-Style Floating Card
                val listState = rememberLazyListState()
                LaunchedEffect(activeIdx) {
                    if (activeIdx >= 0 && activeIdx < lines.size) {
                        listState.animateScrollToItem((activeIdx - 1).coerceAtLeast(0))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    StageViolet.copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(StageViolet.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = StageViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = state.snapshot.trackTitle.ifBlank { "LyricVerse Stage" },
                                    color = LyricActive,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.snapshot.artistName.ifBlank { "Apple-style floating card" },
                                    color = LyricPast,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { localScale = (localScale - 0.1f).coerceAtLeast(0.8f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Shrink overlay width",
                                    tint = LyricUpcoming,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { localScale = (localScale + 0.1f).coerceAtMost(1.2f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Grow overlay width",
                                    tint = LyricUpcoming,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { expanded = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.UnfoldLess,
                                    contentDescription = "Collapse to 2-line floating pill",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Hide floating lyrics overlay",
                                    tint = LyricUpcoming,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (doc?.syncType == LyricSyncType.SYNCED && lines.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(lines, key = { idx, item -> "${idx}_${item.timestampMs}" }) { index, line ->
                                val isCurrent = index == activeIdx
                                val isPast = activeIdx >= 0 && index < activeIdx
                                Text(
                                    text = line.text,
                                    color = when {
                                        isCurrent -> LyricActive
                                        isPast -> LyricPast.copy(alpha = 0.45f)
                                        else -> LyricUpcoming.copy(alpha = 0.65f)
                                    },
                                    fontFamily = fontFamily,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = if (isCurrent) 18.sp else 15.sp,
                                    lineHeight = if (isCurrent) 24.sp else 20.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = doc?.plainText?.takeIf { it.isNotBlank() } ?: activeText,
                            color = LyricUpcoming,
                            fontFamily = fontFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
