package dev.jspade.mybriefcase.bookmarks.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeBookmarkRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeBookmarkRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search input is displayed`() {
        val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, debounceMs = 0L)

        composeTestRule.setContent {
            SearchScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("search_input").assertIsDisplayed()
    }

    @Test
    fun `typing shows results`() {
        fakeRepo.searchResults = listOf(
            BookmarkDto(
                id = "bm-1",
                url = "https://example.com",
                title = "Example Site",
                notes = "",
                createdAt = "2024-01-01T00:00:00Z",
                updatedAt = "2024-01-01T00:00:00Z",
            )
        )
        val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, debounceMs = 0L)

        composeTestRule.setContent {
            SearchScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("search_input").performTextInput("example")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Example Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://example.com").assertIsDisplayed()
    }

    @Test
    fun `empty state shown for no matches`() {
        fakeRepo.searchResults = emptyList()
        val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, debounceMs = 0L)

        composeTestRule.setContent {
            SearchScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("search_input").performTextInput("nonexistent")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No results for 'nonexistent'").assertIsDisplayed()
    }

    @Test
    fun `sort chip is displayed and changes`() {
        fakeRepo.searchResults = listOf(
            BookmarkDto(
                id = "bm-1",
                url = "https://a.com",
                title = "A",
                notes = "",
                createdAt = "2024-01-01T00:00:00Z",
                updatedAt = "2024-01-01T00:00:00Z",
            )
        )
        val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, debounceMs = 0L)

        composeTestRule.setContent {
            SearchScreen(viewModel = viewModel)
        }

        // Default sort chip shows "Name A-Z"
        composeTestRule.onNodeWithText("Name A-Z").assertIsDisplayed()

        // Click sort chip to open dropdown
        composeTestRule.onNodeWithTag("sort_chip").performClick()
        composeTestRule.waitForIdle()

        // Select "Name Z-A"
        composeTestRule.onNodeWithTag("sort_option_NAME_DESC").performClick()
        composeTestRule.waitForIdle()

        // Sort chip now shows "Name Z-A"
        composeTestRule.onNodeWithText("Name Z-A").assertIsDisplayed()
    }

    @Test
    fun `clear button clears search`() {
        fakeRepo.searchResults = listOf(
            BookmarkDto(
                id = "bm-1",
                url = "https://example.com",
                title = "Example",
                notes = "",
                createdAt = "2024-01-01T00:00:00Z",
                updatedAt = "2024-01-01T00:00:00Z",
            )
        )
        val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, debounceMs = 0L)

        composeTestRule.setContent {
            SearchScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("search_input").performTextInput("example")
        composeTestRule.waitForIdle()

        // Clear button should appear
        composeTestRule.onNodeWithTag("clear_search").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clear_search").performClick()
        composeTestRule.waitForIdle()

        // Results should be gone (no results displayed for blank query)
        composeTestRule.onNode(hasTestTag("empty_state")).assertDoesNotExist()
    }
}
