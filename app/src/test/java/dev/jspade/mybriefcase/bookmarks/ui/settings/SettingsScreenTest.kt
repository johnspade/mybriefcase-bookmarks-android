package dev.jspade.mybriefcase.bookmarks.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `settings screen renders all sections`() {
        composeTestRule.setContent {
            SettingsScreen(
                syncDir = "/storage/emulated/0/Syncthing/mybriefcase_bookmarks",
                clientId = "test-client-id",
                appVersion = "1.0",
                onBack = {},
                onImport = {},
                onExport = {},
            )
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_sync_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_sync_dir").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_client_id").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_import_export_header").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_about_header").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_version").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `import and export buttons exist`() {
        composeTestRule.setContent {
            SettingsScreen(
                syncDir = "/test/path",
                clientId = "test-client",
                appVersion = "1.0",
                onBack = {},
                onImport = {},
                onExport = {},
            )
        }

        composeTestRule.onNodeWithTag("settings_import_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_export_button").assertIsDisplayed()
    }

    @Test
    fun `displays sync dir and client id values`() {
        composeTestRule.setContent {
            SettingsScreen(
                syncDir = "/my/sync/path",
                clientId = "my-device-client",
                appVersion = "2.0",
                onBack = {},
                onImport = {},
                onExport = {},
            )
        }

        composeTestRule.onNodeWithText("/my/sync/path").assertIsDisplayed()
        composeTestRule.onNodeWithText("my-device-client").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_version").performScrollTo().assertIsDisplayed()
    }
}
