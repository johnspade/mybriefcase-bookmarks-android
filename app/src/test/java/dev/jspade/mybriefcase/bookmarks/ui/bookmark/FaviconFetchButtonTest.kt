package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class FaviconFetchButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `fetch button is displayed when fetch is enabled`() {
        composeTestRule.setContent {
            FaviconFetchButton(
                fetchState = FaviconFetchState.Idle,
                onFetch = {},
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_button")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking fetch button calls onFetch`() {
        var fetchCalled = false
        composeTestRule.setContent {
            FaviconFetchButton(
                fetchState = FaviconFetchState.Idle,
                onFetch = { fetchCalled = true },
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_button")
            .performClick()

        assertEquals(true, fetchCalled)
    }

    @Test
    fun `shows loading indicator during fetch`() {
        composeTestRule.setContent {
            FaviconFetchButton(
                fetchState = FaviconFetchState.Loading,
                onFetch = {},
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_loading")
            .assertIsDisplayed()
    }

    @Test
    fun `shows preview after successful fetch`() {
        composeTestRule.setContent {
            FaviconFetchButton(
                fetchState = FaviconFetchState.Success("abc123.png"),
                onFetch = {},
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithTag("favicon_fetch_preview")
            .assertIsDisplayed()
    }

    @Test
    fun `shows error message on failure`() {
        composeTestRule.setContent {
            FaviconFetchButton(
                fetchState = FaviconFetchState.Error("Not found"),
                onFetch = {},
                syncRoot = "/test",
            )
        }

        composeTestRule
            .onNodeWithText("Not found")
            .assertIsDisplayed()
    }
}
