package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `manifest declares AodLyricsActivity, AodLyricsTileService, and minimal permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("LyricVerse", context.getString(R.string.app_name))

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or PackageManager.GET_ACTIVITIES
        )
        val permissions = packageInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue(permissions.contains(android.Manifest.permission.INTERNET))
        assertFalse(permissions.contains(android.Manifest.permission.SYSTEM_ALERT_WINDOW))
        assertTrue(permissions.contains(android.Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE))

        val tileService = packageInfo.services?.firstOrNull {
            it.name.endsWith("AodLyricsTileService")
        }
        assertTrue(tileService != null)
        assertEquals(android.Manifest.permission.BIND_QUICK_SETTINGS_TILE, tileService?.permission)

        val aodActivity = packageInfo.activities?.firstOrNull {
            it.name.endsWith("AodLyricsActivity")
        }
        assertTrue(aodActivity != null)

        val companionService = packageInfo.services?.firstOrNull {
            it.name.endsWith("OverlayCompanionService")
        }
        assertTrue(companionService != null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && companionService != null) {
            assertEquals(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                companionService.foregroundServiceType
            )
        }
    }

    @Test
    fun `cuj_opens_directly_to_settings_with_show_lyrics_cta_and_demo_preview`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("show_lyrics_button").assertIsDisplayed()
        composeRule.onNodeWithTag("pin_qs_tile_button").assertIsDisplayed()

        // Start synced demo track and verify preview updates
        composeRule.onNodeWithTag("demo_chip_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("demo_play_button").assertIsDisplayed()
    }

    @Test
    fun `cuj_missing_metadata_simulation_updates_minimal_preview`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("simulate_missing_metadata_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No track metadata").assertIsDisplayed()
    }
}
