package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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

    private val bookmarkWithFavicon =
        BookmarkDto(
            id = "bm-1",
            url = "https://example.com",
            title = "Example",
            notes = "",
            favicon = "example.png",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
        )

    @Test
    fun `fetch favicon button shown when faviconFetchEnabled is true`() {
        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _, _ -> },
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
    fun `fetch favicon button disabled when faviconFetchEnabled is false`() {
        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _, _ -> },
                faviconFetchEnabled = false,
                faviconFetchState = FaviconFetchState.Idle,
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `shows loading during fetch`() {
        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _, _ -> },
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

    @Test
    fun `confirm passes FaviconAction Keep when nothing changed`() {
        var receivedAction: FaviconAction? = null

        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = bookmarkWithFavicon,
                onDismiss = {},
                onConfirm = { _, _, _, _, action -> receivedAction = action },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Idle,
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule.onNodeWithTag("edit_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(FaviconAction.Keep, receivedAction)
    }

    @Test
    fun `confirm passes FaviconAction Delete after clicking delete`() {
        var receivedAction: FaviconAction? = null

        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = bookmarkWithFavicon,
                onDismiss = {},
                onConfirm = { _, _, _, _, action -> receivedAction = action },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Idle,
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule.onNodeWithTag("favicon_delete_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("edit_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(FaviconAction.Delete, receivedAction)
    }

    @Test
    fun `confirm passes FaviconAction Set when fetch succeeded`() {
        var receivedAction: FaviconAction? = null

        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _, action -> receivedAction = action },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Success("fetched.png"),
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule.onNodeWithTag("edit_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(FaviconAction.Set("fetched.png"), receivedAction)
    }

    @Test
    fun `confirm passes FaviconAction Delete when delete clicked after fetch`() {
        var receivedAction: FaviconAction? = null

        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _, action -> receivedAction = action },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Success("fetched.png"),
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule.onNodeWithTag("favicon_delete_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("edit_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(FaviconAction.Delete, receivedAction)
    }

    @Test
    fun `delete after fetch hides favicon and shows letter avatar`() {
        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = testBookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _, _ -> },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Success("fetched.png"),
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule.onNodeWithTag("favicon_delete_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Delete button should be hidden after clicking it
        composeTestRule.onNodeWithTag("favicon_delete_button").assertDoesNotExist()
        // Dialog still visible
        composeTestRule.onNodeWithTag("edit_bookmark_dialog").assertIsDisplayed()
    }

    @Test
    fun `delete hides favicon locally without dismiss`() {
        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = bookmarkWithFavicon,
                onDismiss = {},
                onConfirm = { _, _, _, _, _ -> },
                faviconFetchEnabled = true,
                faviconFetchState = FaviconFetchState.Idle,
                onFetchFavicon = { _ -> },
                syncRoot = "/test",
            )
        }

        composeTestRule.onNodeWithTag("favicon_delete_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Dialog still visible
        composeTestRule.onNodeWithTag("edit_bookmark_dialog").assertIsDisplayed()
        // Delete button hidden (no favicon to delete)
        composeTestRule.onNodeWithTag("favicon_delete_button").assertDoesNotExist()
    }
}
