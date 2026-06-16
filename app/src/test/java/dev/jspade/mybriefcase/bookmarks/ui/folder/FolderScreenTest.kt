package dev.jspade.mybriefcase.bookmarks.ui.folder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class FolderScreenTest {

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
    fun `folder items are displayed`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        // Check item count text which is unique to the main list folder items
        composeTestRule.onNodeWithText("1 items").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 items").assertIsDisplayed()
    }

    @Test
    fun `tap folder navigates into it`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        // Tap on "1 items" (the Work folder's supporting text in the list)
        composeTestRule.onNodeWithText("1 items").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("GitHub").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://github.com").assertIsDisplayed()
    }

    @Test
    fun `breadcrumbs update after navigation`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("1 items").performClick()
        composeTestRule.waitForIdle()

        // Breadcrumb separator indicates we're at a nested level
        composeTestRule.onNodeWithText(">").assertIsDisplayed()
    }

    @Test
    fun `empty folder shows empty state`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        // Tap on "0 items" (the Personal folder's supporting text)
        composeTestRule.onNodeWithText("0 items").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("This folder is empty").assertIsDisplayed()
    }

    @Test
    fun `drawer opens with folder tree`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        // Open drawer
        composeTestRule.onNode(hasContentDescription("Open drawer")).performClick()
        composeTestRule.waitForIdle()

        // Folder tree header should be displayed
        composeTestRule.onNodeWithText("Folders").assertIsDisplayed()
    }
}
