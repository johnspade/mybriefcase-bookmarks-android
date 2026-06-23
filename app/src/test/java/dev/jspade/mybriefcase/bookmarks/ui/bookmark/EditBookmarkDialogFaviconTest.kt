package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class EditBookmarkDialogFaviconTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testBookmark =
        BookmarkDto(
            id = "bm-1",
            url = "https://example.com",
            title = "Example",
            notes = "",
            favicon = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
        )

    @Test
    fun `fetch favicon button shown when faviconFetchEnabled is true`() {
        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _ -> },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Idle,
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `fetch favicon button hidden when faviconFetchEnabled is false`() {
        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _ -> },
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

    @Test
    fun `shows loading during fetch`() {
        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _ -> },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Loading,
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_loading")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
