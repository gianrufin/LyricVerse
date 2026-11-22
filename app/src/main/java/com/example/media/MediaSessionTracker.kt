package com.example.media

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import com.example.domain.model.PlaybackSessionStatus
import com.example.domain.model.PlaybackSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Read-only observer of Android's MediaSessionManager.
 * Never requests audio focus, never alters external playback, and never captures audio.
 */
class MediaSessionTracker(private val context: Context) {

    private val mediaSessionManager: MediaSessionManager? =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private val listenerComponent: ComponentName =
        ComponentName(context, LyricNotificationListenerService::class.java)

    private var activeController: MediaController? = null
    private var sessionListenerRegistered: Boolean = false
    private var lastArtSignature: String? = null
    private var cachedAlbumLightColors: List<Long> = emptyList()

    private val _externalSnapshot = MutableStateFlow(
        PlaybackSnapshot(status = initialStatus())
    )
    val externalSnapshot: StateFlow<PlaybackSnapshot> = _externalSnapshot.asStateFlow()

    private val _diagnosticInfo = MutableStateFlow(MediaSessionDiagnostics())
    val diagnosticInfo: StateFlow<MediaSessionDiagnostics> = _diagnosticInfo.asStateFlow()

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            selectBestController(controllers.orEmpty())
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishFromActiveController()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishFromActiveController()
        }

        override fun onSessionDestroyed() {
            detachActiveController()
            refreshActiveSessions()
        }
    }

    fun isNotificationListenerGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val flattened = listenerComponent.flattenToString()
        val shortFlattened = listenerComponent.flattenToShortString()
        return enabledListeners.contains(flattened) || enabledListeners.contains(shortFlattened)
    }

    fun startObserving() {
        if (!isNotificationListenerGranted()) {
            detachActiveController()
            _externalSnapshot.value = PlaybackSnapshot(status = PlaybackSessionStatus.NO_PERMISSION)
            _diagnosticInfo.value = MediaSessionDiagnostics(
                listenerGranted = false,
                activeSessionsCount = 0,
                selectedPackage = null
            )
            return
        }

        val manager = mediaSessionManager ?: return
        if (!sessionListenerRegistered) {
            try {
                manager.addOnActiveSessionsChangedListener(activeSessionsListener, listenerComponent)
                sessionListenerRegistered = true
            } catch (_: SecurityException) {
                _externalSnapshot.value = PlaybackSnapshot(status = PlaybackSessionStatus.NO_PERMISSION)
                return
            }
        }
        refreshActiveSessions()
    }

    fun stopObserving() {
        if (sessionListenerRegistered) {
            runCatching {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
            }
            sessionListenerRegistered = false
        }
        detachActiveController()
        _externalSnapshot.value = PlaybackSnapshot(
            status = if (isNotificationListenerGranted()) {
                PlaybackSessionStatus.SESSION_ENDED
            } else {
                PlaybackSessionStatus.NO_PERMISSION
            }
        )
    }

    fun refreshActiveSessions() {
        if (!isNotificationListenerGranted()) {
            _externalSnapshot.value = PlaybackSnapshot(status = PlaybackSessionStatus.NO_PERMISSION)
            return
        }
        try {
            val controllers = mediaSessionManager?.getActiveSessions(listenerComponent).orEmpty()
            selectBestController(controllers)
        } catch (_: SecurityException) {
            _externalSnapshot.value = PlaybackSnapshot(status = PlaybackSessionStatus.NO_PERMISSION)
        }
    }

    private fun selectBestController(controllers: List<MediaController>) {
        // Ignore our own package if ever present; prioritize actively playing sessions
        val candidates = controllers.filter { it.packageName != context.packageName }
        val chosen = candidates.firstOrNull { isPlayingState(it.playbackState) }
            ?: candidates.firstOrNull { it.metadata != null }
            ?: candidates.firstOrNull()

        _diagnosticInfo.value = MediaSessionDiagnostics(
            listenerGranted = true,
            activeSessionsCount = candidates.size,
            selectedPackage = chosen?.packageName,
            selectedAppLabel = chosen?.packageName?.let(::resolveAppLabel)
        )

        if (chosen == null) {
            val hadController = activeController != null
            detachActiveController()
            _externalSnapshot.value = PlaybackSnapshot(
                status = if (hadController) {
                    PlaybackSessionStatus.SESSION_ENDED
                } else {
                    PlaybackSessionStatus.WAITING_FOR_PLAYBACK
                }
            )
            return
        }

        if (activeController?.sessionToken != chosen.sessionToken) {
            detachActiveController()
            activeController = chosen
            chosen.registerCallback(controllerCallback)
        }
        publishFromActiveController()
    }

    private fun publishFromActiveController() {
        val controller = activeController ?: return
        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L

        val playing = isPlayingState(playbackState)
        val rawPosition = playbackState?.position?.coerceAtLeast(0L) ?: 0L
        val speed = playbackState?.playbackSpeed?.takeIf { it > 0f } ?: 1.0f
        val updateElapsed = playbackState?.lastPositionUpdateTime?.takeIf { it > 0L }
            ?: SystemClock.elapsedRealtime()

        val status = when {
            title.isBlank() || artist.isBlank() -> PlaybackSessionStatus.METADATA_MISSING
            playbackState?.state == PlaybackState.STATE_STOPPED ||
                playbackState?.state == PlaybackState.STATE_NONE -> PlaybackSessionStatus.SESSION_ENDED
            else -> PlaybackSessionStatus.ACTIVE_EXTERNAL
        }

        val lightColors = resolveAlbumLightColors(
            metadata = metadata,
            signature = "${artist.trim().lowercase()}|${title.trim().lowercase()}|${album.trim().lowercase()}"
        )

        _externalSnapshot.value = PlaybackSnapshot(
            status = status,
            sourcePackage = controller.packageName,
            sourceAppLabel = resolveAppLabel(controller.packageName),
            trackTitle = title,
            artistName = artist,
            albumName = album,
            durationMs = duration,
            positionMs = rawPosition,
            playbackSpeed = speed,
            isPlaying = playing,
            lastPositionUpdateElapsedMs = updateElapsed,
            isDemoMode = false,
            albumLightColors = lightColors,
        )
    }

    private fun resolveAlbumLightColors(metadata: MediaMetadata?, signature: String): List<Long> {
        val artBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val artUriStr = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: ""

        val cacheKey = "$signature|${artBitmap?.generationId ?: 0}|$artUriStr"
        if (cacheKey == lastArtSignature && cachedAlbumLightColors.isNotEmpty()) {
            return cachedAlbumLightColors
        }

        val extracted = when {
            artBitmap != null -> AlbumArtColorExtractor.extractLightGradientColors(artBitmap)
            artUriStr.startsWith("content://") || artUriStr.startsWith("android.resource://") -> {
                decodeLocalArtThumbnail(artUriStr)?.let { thumb ->
                    AlbumArtColorExtractor.extractLightGradientColors(thumb).also {
                        thumb.recycle()
                    }
                } ?: AlbumArtColorExtractor.fallbackGradientForSignature(signature)
            }
            else -> AlbumArtColorExtractor.fallbackGradientForSignature(signature)
        }
        lastArtSignature = cacheKey
        cachedAlbumLightColors = extracted
        return extracted
    }

    private fun decodeLocalArtThumbnail(uriString: String): Bitmap? {
        return runCatching {
            val uri = Uri.parse(uriString)
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }

    private fun detachActiveController() {
        activeController?.let { controller ->
            runCatching { controller.unregisterCallback(controllerCallback) }
        }
        activeController = null
    }

    private fun isPlayingState(state: PlaybackState?): Boolean {
        return when (state?.state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING -> true
            else -> false
        }
    }

    private fun resolveAppLabel(packageName: String): String {
        return runCatching {
            val pm = context.packageManager
            val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrElse {
            packageName.substringAfterLast('.').replaceFirstChar { c -> c.uppercase() }
        }
    }

    private fun initialStatus(): PlaybackSessionStatus {
        return if (isNotificationListenerGranted()) {
            PlaybackSessionStatus.WAITING_FOR_PLAYBACK
        } else {
            PlaybackSessionStatus.NO_PERMISSION
        }
    }
}

data class MediaSessionDiagnostics(
    val listenerGranted: Boolean = false,
    val activeSessionsCount: Int = 0,
    val selectedPackage: String? = null,
    val selectedAppLabel: String? = null,
)
