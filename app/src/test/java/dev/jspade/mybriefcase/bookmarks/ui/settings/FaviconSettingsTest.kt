package dev.jspade.mybriefcase.bookmarks.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class FaviconSettingsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `master toggle shown and reflects state`() {
        composeTestRule.setContent {
            SettingsScreen(
                syncDir = "/test",
                clientId = "test",
                appVersion = "1.0",
                onBack = {},
                onChangeSyncDir = {},
                onImport = {},
                onExport = {},
                faviconFetchEnabled = true,
                useDuckDuckGo = true,
                onFaviconFetchEnabledChange = {},
                onUseDuckDuckGoChange = {},
            )
        }

        composeTestRule
            .onNodeWithTag("settings_favicon_toggle")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsOn()
    }

    @Test
    fun `DuckDuckGo sub-toggle visible when master is on`() {
        composeTestRule.setContent {
            SettingsScreen(
                syncDir = "/test",
                clientId = "test",
                appVersion = "1.0",
                onBack = {},
                onChangeSyncDir = {},
                onImport = {},
                onExport = {},
                faviconFetchEnabled = true,
                useDuckDuckGo = true,
                onFaviconFetchEnabledChange = {},
                onUseDuckDuckGoChange = {},
            )
        }

        composeTestRule
            .onNodeWithTag("settings_duckduckgo_toggle")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsOn()
    }

    @Test
    fun `DuckDuckGo sub-toggle hidden when master is off`() {
        composeTestRule.setContent {
            SettingsScreen(
                syncDir = "/test",
                clientId = "test",
                appVersion = "1.0",
                onBack = {},
                onChangeSyncDir = {},
                onImport = {},
                onExport = {},
                faviconFetchEnabled = false,
                useDuckDuckGo = true,
                onFaviconFetchEnabledChange = {},
                onUseDuckDuckGoChange = {},
            )
        }

        composeTestRule
            .onNodeWithTag("settings_duckduckgo_toggle")
            .assertDoesNotExist()
    }

    @Test
    fun `master toggle calls callback with new value`() {
        val calls = mutableListOf<Boolean>()
        composeTestRule.setContent {
            SettingsScreen(
                syncDir = "/test",
                clientId = "test",
                appVersion = "1.0",
                onBack = {},
                onChangeSyncDir = {},
                onImport = {},
                onExport = {},
                faviconFetchEnabled = true,
                useDuckDuckGo = true,
                onFaviconFetchEnabledChange = { calls.add(it) },
                onUseDuckDuckGoChange = {},
            )
        }

        composeTestRule
            .onNodeWithTag("settings_favicon_toggle")
            .performScrollTo()
            .performClick()

        assertEquals(listOf(false), calls)
    }
}
