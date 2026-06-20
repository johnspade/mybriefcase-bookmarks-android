package dev.jspade.mybriefcase.bookmarks.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uniffi.mybriefcase_bookmarks_ffi.BookmarkHistoryEntryDto
import uniffi.mybriefcase_bookmarks_ffi.FieldChangeDto

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class HistoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `renders history entries with changed fields`() {
        val entries =
            listOf(
                BookmarkHistoryEntryDto(
                    changeHash = "a".repeat(64),
                    timestamp = 1700000000000L,
                    actor = "actor1",
                    changedFields =
                        listOf(
                            FieldChangeDto(field = "url", oldValue = "https://old.com", newValue = "https://new.com"),
                            FieldChangeDto(field = "title", oldValue = "Old Title", newValue = "New Title"),
                        ),
                ),
                BookmarkHistoryEntryDto(
                    changeHash = "b".repeat(64),
                    timestamp = 1699000000000L,
                    actor = "actor1",
                    changedFields =
                        listOf(
                            FieldChangeDto(field = "title", oldValue = null, newValue = "Old Title"),
                            FieldChangeDto(field = "url", oldValue = null, newValue = "https://old.com"),
                        ),
                ),
            )

        composeTestRule.setContent {
            HistoryScreen(
                entries = entries,
                isLoading = false,
                onRevert = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("history_entry_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("history_entry_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://new.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("New Title").assertIsDisplayed()
    }

    @Test
    fun `revert button shows confirmation dialog`() {
        val entries =
            listOf(
                BookmarkHistoryEntryDto(
                    changeHash = "c".repeat(64),
                    timestamp = 1700000000000L,
                    actor = "actor1",
                    changedFields =
                        listOf(
                            FieldChangeDto(field = "title", oldValue = "Before", newValue = "After"),
                        ),
                ),
            )

        composeTestRule.setContent {
            HistoryScreen(
                entries = entries,
                isLoading = false,
                onRevert = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("revert_button_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("revert_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Before").assertIsDisplayed()
    }

    @Test
    fun `confirmation dialog triggers revert on confirm`() {
        var revertedHash: String? = null
        val entries =
            listOf(
                BookmarkHistoryEntryDto(
                    changeHash = "c".repeat(64),
                    timestamp = 1700000000000L,
                    actor = "actor1",
                    changedFields =
                        listOf(
                            FieldChangeDto(field = "title", oldValue = "Before", newValue = "After"),
                        ),
                ),
            )

        composeTestRule.setContent {
            HistoryScreen(
                entries = entries,
                isLoading = false,
                onRevert = { hash -> revertedHash = hash },
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("revert_button_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("revert_confirm_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(revertedHash == "c".repeat(64))
    }

    @Test
    fun `shows empty state when no history`() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = emptyList(),
                isLoading = false,
                onRevert = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("No history entries yet").assertIsDisplayed()
    }

    @Test
    fun `shows loading state`() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = emptyList(),
                isLoading = true,
                onRevert = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("history_loading").assertIsDisplayed()
    }
}
