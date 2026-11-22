package com.example

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.LockscreenOrientation
import com.example.ui.components.AodTwoLineLyricsView
import com.example.ui.theme.LyricVerseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Pure #000000 AMOLED full-screen minimal lyrics display.
 * Safe to show over lock screen because it renders ONLY the song title intro (briefly)
 * and the active/next lyric lines with zero settings, personal data, or keyguard bypass.
 */
class AodLyricsActivity : ComponentActivity() {

    private var wasTriggeredFromLock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyLockScreenWindowBehavior(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.BLACK)
        )
        hideSystemBars()

        val container = (application as LyricVerseApplication).appContainer

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                dismissAod()
            }
        })

        setContent {
            val state by container.syncCoordinator.syncState.collectAsStateWithLifecycle()
            val prefs = state.preferences
            val shouldKeepScreenOn = prefs.keepScreenOnWhilePlaying &&
                state.snapshot.isPlaying &&
                state.hasDisplayableLyrics

            LaunchedEffect(prefs.lockscreenOrientation) {
                requestedOrientation = when (prefs.lockscreenOrientation) {
                    LockscreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                    LockscreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    LockscreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }

            // Dynamically hold FLAG_KEEP_SCREEN_ON only while playback is active
            DisposableEffect(shouldKeepScreenOn) {
                if (shouldKeepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Subtle OLED burn-in pixel shift cycle (~20s per gentle step)
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

            LyricVerseTheme(useDynamicColor = prefs.useDynamicColor) {
                Surface(
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("aod_lyrics_screen")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "Close AMOLED lyrics",
                            onClick = { dismissAod() }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AodTwoLineLyricsView(
                            state = state,
                            modifier = Modifier.offset {
                                IntOffset(
                                    x = (dxDp * density).roundToInt(),
                                    y = (dyDp * density).roundToInt()
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLockScreenWindowBehavior(intent)
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        (application as LyricVerseApplication).appContainer.syncCoordinator.setAodActivityForeground(true)
        hideSystemBars()
    }

    override fun onPause() {
        (application as LyricVerseApplication).appContainer.syncCoordinator.setAodActivityForeground(false)
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun dismissAod() {
        if (wasTriggeredFromLock) {
            (application as LyricVerseApplication).appContainer.syncCoordinator.markAodDismissedByUser()
        }
        finish()
    }

    private fun applyLockScreenWindowBehavior(intent: Intent?) {
        val fromLock = intent?.getBooleanExtra(EXTRA_FROM_LOCK_TRIGGER, false) == true
        val wakeScreen = intent?.getBooleanExtra(EXTRA_WAKE_SCREEN, false) == true
        if (fromLock) {
            wasTriggeredFromLock = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            if (wakeScreen) {
                setTurnScreenOn(true)
            }
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            if (wakeScreen) {
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        const val EXTRA_FROM_LOCK_TRIGGER = "extra_from_lock_trigger"
        const val EXTRA_WAKE_SCREEN = "extra_wake_screen"

        private val PIXEL_SHIFT_OFFSETS = listOf(
            0 to 0,
            6 to -4,
            10 to 5,
            -7 to 8,
            -10 to -5,
            4 to 10,
        )
    }
}
