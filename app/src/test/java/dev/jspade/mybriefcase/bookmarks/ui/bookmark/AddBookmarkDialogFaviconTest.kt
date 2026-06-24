package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class AddBookmarkDialogFaviconTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `fetch favicon button shown when enabled`() {
        composeTestRule.setContent {
            AddBookmarkDialog(
                onDismiss = {},
                onConfirm = { _, _, _ -> },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Idle,
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_button")
            .assertExists()
    }

    @Test
    fun `fetch favicon button hidden when disabled`() {
        composeTestRule.setContent {
            AddBookmarkDialog(
                onDismiss = {},
                onConfirm = { _, _, _ -> },
                faviconFetchEnabled = false,
                faviconFetchState = FaviconFetchState.Idle,
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_button")
            .assertDoesNotExist()
    }
}
