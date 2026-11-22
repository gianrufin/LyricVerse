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
    fun `app name and manifest permissions are configured properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("LyricVerse", context.getString(R.string.app_name))

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES
        )
        val permissions = packageInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue(permissions.contains(android.Manifest.permission.INTERNET))
        assertTrue(permissions.contains(android.Manifest.permission.SYSTEM_ALERT_WINDOW))
        assertTrue(permissions.contains(android.Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE))

        val overlayService = packageInfo.services?.firstOrNull {
            it.name.endsWith("OverlayCompanionService")
        }
        assertTrue(overlayService != null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && overlayService != null) {
            assertEquals(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                overlayService.foregroundServiceType
            )
        }
    }

    @Test
    fun `cuj_demo_karaoke_and_plain_fallback_render_honestly`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("stage_screen").assertIsDisplayed()

        // Start synced demo track
        composeRule.onNodeWithTag("demo_chip_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("TIME-SYNCED LRC").assertIsDisplayed()
        composeRule.onNodeWithTag("demo_play_button").assertIsDisplayed()

        // Switch to plain-lyrics-only demo track and verify honest label
        composeRule.onNodeWithTag("demo_chip_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PLAIN LYRICS (UN-SYNCED)").assertIsDisplayed()
    }

    @Test
    fun `cuj_overlay_permission_denial_shows_rationale_dialog`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overlay_toggle_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Allow Floating Lyrics Overlay?").assertIsDisplayed()
        composeRule.onNodeWithTag("overlay_rationale_dismiss").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `cuj_missing_metadata_simulation_and_navigation_tabs`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_access").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("permissions_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("simulate_missing_metadata_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Track Metadata Missing").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_style").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("style_studio_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_library").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("library_screen").assertIsDisplayed()
    }
}
