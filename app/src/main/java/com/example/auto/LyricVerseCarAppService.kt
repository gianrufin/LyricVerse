package com.example.auto

import android.app.Presentation
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.validation.HostValidator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.LyricVerseApplication
import com.example.domain.model.PlaybackSessionStatus
import com.example.ui.components.AodTwoLineLyricsView
import com.example.ui.theme.LyricVerseTheme
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Android Auto entry point for passenger lyrics display.
 * Uses NavigationTemplate + SurfaceCallback without claiming navigation trips or requesting audio focus,
 * allowing music playback and Google Maps/Waze routing to continue undisturbed.
 */
class LyricVerseCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session {
        return LyricVerseCarSession(application as LyricVerseApplication)
    }
}

class LyricVerseCarSession(
    private val app: LyricVerseApplication,
) : Session(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var surfaceRenderer: CarLyricsSurfaceRenderer? = null

    init {
        savedStateRegistryController.performAttach()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                savedStateRegistryController.performRestore(null)
                val renderer = CarLyricsSurfaceRenderer(
                    session = this@LyricVerseCarSession,
                    carContext = carContext,
                    app = app,
                )
                surfaceRenderer = renderer
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(renderer)
            }

            override fun onStart(owner: LifecycleOwner) {
                val container = app.appContainer
                if (container.mediaSessionTracker.isNotificationListenerGranted() &&
                    container.syncCoordinator.preferencesFlow.value.trackingEnabled
                ) {
                    container.mediaSessionTracker.startObserving()
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                runCatching {
                    carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
                }
                surfaceRenderer?.release()
                surfaceRenderer = null
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        return LyricVerseCarScreen(carContext = carContext, app = app)
    }
}

private class LyricVerseCarScreen(
    carContext: CarContext,
    private val app: LyricVerseApplication,
) : Screen(carContext) {

    init {
        lifecycleScope.launch {
            app.appContainer.syncCoordinator.syncState
                .map { it.preferences.androidAutoLyricsEnabled }
                .distinctUntilChanged()
                .collect {
                    invalidate()
                }
        }
    }

    override fun onGetTemplate(): Template {
        val prefs = app.appContainer.syncCoordinator.syncState.value.preferences
        val toggleLabel = if (prefs.androidAutoLyricsEnabled) "Hide" else "Show"

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(toggleLabel)
                    .setOnClickListener {
                        lifecycleScope.launch {
                            app.appContainer.preferencesDataStore.update { current ->
                                current.copy(androidAutoLyricsEnabled = !current.androidAutoLyricsEnabled)
                            }
                        }
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("-0.2s")
                    .setOnClickListener { nudgeOffset(-200L) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("+0.2s")
                    .setOnClickListener { nudgeOffset(200L) }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .build()
    }

    private fun nudgeOffset(deltaMs: Long) {
        lifecycleScope.launch {
            app.appContainer.preferencesDataStore.update { current ->
                current.copy(syncOffsetMs = (current.syncOffsetMs + deltaMs).coerceIn(-3000L, 3000L))
            }
        }
    }
}

private class CarLyricsSurfaceRenderer(
    private val session: LyricVerseCarSession,
    private val carContext: CarContext,
    private val app: LyricVerseApplication,
) : SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private val surfaceWidthPx = MutableStateFlow(0)
    private val surfaceHeightPx = MutableStateFlow(0)
    private val visibleAreaRect = MutableStateFlow<Rect?>(null)

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        release()
        val surface = surfaceContainer.surface ?: return
        val width = surfaceContainer.width.coerceAtLeast(1)
        val height = surfaceContainer.height.coerceAtLeast(1)
        val dpi = surfaceContainer.dpi.coerceAtLeast(120)

        surfaceWidthPx.value = width
        surfaceHeightPx.value = height

        val displayManager = carContext.getSystemService(DisplayManager::class.java) ?: return
        val vd = displayManager.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            width,
            height,
            dpi,
            surface,
            0
        ) ?: return
        virtualDisplay = vd

        val pres = Presentation(carContext, vd.display)
        pres.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(session)
            decor.setViewTreeSavedStateRegistryOwner(session)
        }

        val composeView = ComposeView(carContext).apply {
            setViewTreeLifecycleOwner(session)
            setViewTreeSavedStateRegistryOwner(session)
            setContent {
                val state by app.appContainer.syncCoordinator.syncState.collectAsState()
                val visibleRect by visibleAreaRect.collectAsState()
                val totalWidth by surfaceWidthPx.collectAsState()
                val totalHeight by surfaceHeightPx.collectAsState()
                val prefs = state.preferences
                val density = LocalDensity.current

                var shiftStep by remember { mutableIntStateOf(0) }
                LaunchedEffect(prefs.oledPixelShiftEnabled, state.snapshot.isPlaying) {
                    if (!prefs.oledPixelShiftEnabled) {
                        shiftStep = 0
                        return@LaunchedEffect
                    }
                    while (isActive) {
                        delay(20_000L)
                        shiftStep = (shiftStep + 1) % PIXEL_SHIFT_OFFSETS.size
                    }
                }
                val (dxDp, dyDp) = if (prefs.oledPixelShiftEnabled) {
                    PIXEL_SHIFT_OFFSETS[shiftStep]
                } else {
                    0 to 0
                }

                val leftPad = with(density) { ((visibleRect?.left ?: 0).coerceAtLeast(0)).toDp() }
                val topPad = with(density) { ((visibleRect?.top ?: 0).coerceAtLeast(0)).toDp() }
                val rightPad = with(density) {
                    if (visibleRect != null && totalWidth > 0) {
                        (totalWidth - visibleRect!!.right).coerceAtLeast(0).toDp()
                    } else {
                        0.dp
                    }
                }
                val bottomPad = with(density) {
                    if (visibleRect != null && totalHeight > 0) {
                        (totalHeight - visibleRect!!.bottom).coerceAtLeast(0).toDp()
                    } else {
                        0.dp
                    }
                }

                LyricVerseTheme(useDynamicColor = prefs.useDynamicColor) {
                    Surface(
                        color = Color.Black,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = leftPad + 28.dp,
                                    top = topPad + 16.dp,
                                    end = rightPad + 28.dp,
                                    bottom = bottomPad + 16.dp,
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                !prefs.androidAutoLyricsEnabled -> {
                                    // Passenger toggled lyrics hidden -> pure #000000 black
                                }
                                state.snapshot.status == PlaybackSessionStatus.NO_PERMISSION -> {
                                    Text(
                                        text = "Open LyricVerse on phone & allow Music Access",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                !state.hasDisplayableLyrics -> {
                                    // Pure #000000 blank when lyrics unavailable, vlog, podcast, or idle
                                }
                                else -> {
                                    AodTwoLineLyricsView(
                                        state = state,
                                        isPreview = false,
                                        modifier = Modifier.offset {
                                            IntOffset(
                                                x = (dxDp * this.density).roundToInt(),
                                                y = (dyDp * this.density).roundToInt(),
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        pres.setContentView(composeView)
        pres.show()
        presentation = pres
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        visibleAreaRect.value = Rect(visibleArea)
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        if (visibleAreaRect.value == null) {
            visibleAreaRect.value = Rect(stableArea)
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        release()
    }

    fun release() {
        runCatching { presentation?.dismiss() }
        presentation = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
    }

    companion object {
        private const val VIRTUAL_DISPLAY_NAME = "LyricVerseCarDisplay"
        private val PIXEL_SHIFT_OFFSETS = listOf(
            0 to 0,
            5 to -3,
            8 to 4,
            -6 to 6,
            -8 to -4,
            3 to 8,
        )
    }
}
