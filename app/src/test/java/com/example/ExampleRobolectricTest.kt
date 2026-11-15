package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.SystemTelemetryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DeckLink", appName)
    }

    @Test
    fun `hid device api is supported on Android 10 plus`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val telemetry = SystemTelemetryRepository(context)
        assertTrue(telemetry.isBluetoothHidApiSupported())
    }

    @Test
    fun `navigate through all six remote tabs`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_status_badge").assertIsDisplayed()

        // Navigate to Mouse
        composeTestRule.onNodeWithTag("nav_mouse").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("touchpad_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mouse_left_button").assertIsDisplayed()

        // Navigate to Keyboard
        composeTestRule.onNodeWithTag("nav_keyboard").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("keyboard_text_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kb_tab_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("keyboard_list")
            .performScrollToNode(hasTestTag("key_F5"))
        composeTestRule.onNodeWithTag("key_F5").assertIsDisplayed()

        // Navigate to Media & switch to Presentation
        composeTestRule.onNodeWithTag("nav_media").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("media_play_pause_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("media_seg_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pres_next_slide").assertIsDisplayed()

        // Navigate to Shortcuts
        composeTestRule.onNodeWithTag("nav_shortcuts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shortcut_tile_win_d").assertIsDisplayed()

        // Navigate to Settings, verify Auto-Connect toggle & scroll to Wi-Fi LAN scan button
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_auto_connect_switch"))
        composeTestRule.onNodeWithTag("settings_auto_connect_switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_auto_connect_switch").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_wifi_scan_btn"))
        composeTestRule.onNodeWithTag("settings_wifi_scan_btn").assertIsDisplayed()
    }

    @Test
    fun `shortcuts screen opens dialog and saves custom shortcut`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_shortcuts").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_custom_shortcut_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("custom_shortcut_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preview: Ctrl + S").assertIsDisplayed()

        composeTestRule.onNodeWithTag("save_custom_shortcut_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeTestRule.onNodeWithText("Custom (1)").assertExists()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("filter_chip_CUSTOM")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ctrl + S").assertIsDisplayed()
    }
}
