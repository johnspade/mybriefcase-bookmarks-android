package dev.jspade.mybriefcase.bookmarks.ui.share

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class ShareReceiverScreenTest {
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
    fun `displays pre-filled URL and title`() {
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "https://example.com",
                extraSubject = "Example Title",
            )

        composeTestRule.setContent {
            ShareReceiverScreen(viewModel = viewModel, onFinish = {}, onRedirectToWizard = {})
        }

        composeTestRule.onNodeWithTag("share_url_field").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Example Title").assertIsDisplayed()
    }

    @Test
    fun `save button triggers save and calls onFinish`() {
        var finished = false
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "https://example.com",
                extraSubject = "Example",
            )

        composeTestRule.setContent {
            ShareReceiverScreen(viewModel = viewModel, onFinish = { finished = true }, onRedirectToWizard = {})
        }

        composeTestRule.onNodeWithTag("share_save_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(finished)
        assertEquals(1, fakeRepo.addBookmarkCalls.size)
    }

    @Test
    fun `cancel button calls onFinish without saving`() {
        var finished = false
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "https://example.com",
                extraSubject = null,
            )

        composeTestRule.setContent {
            ShareReceiverScreen(viewModel = viewModel, onFinish = { finished = true }, onRedirectToWizard = {})
        }

        composeTestRule.onNodeWithTag("share_cancel_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(finished)
        assertTrue(fakeRepo.addBookmarkCalls.isEmpty())
    }

    @Test
    fun `shows validation error when URL is invalid`() {
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "not a url",
                extraSubject = null,
            )

        composeTestRule.setContent {
            ShareReceiverScreen(viewModel = viewModel, onFinish = {}, onRedirectToWizard = {})
        }

        composeTestRule.onNodeWithTag("share_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Invalid URL").assertIsDisplayed()
    }

    @Test
    fun `shows folder picker with root selected by default`() {
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "https://example.com",
                extraSubject = null,
            )

        composeTestRule.setContent {
            ShareReceiverScreen(viewModel = viewModel, onFinish = {}, onRedirectToWizard = {})
        }

        composeTestRule.onNodeWithTag("share_folder_picker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookmarks").assertIsDisplayed()
    }

    @Test
    fun `folder picker allows selecting different folder`() {
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "https://example.com",
                extraSubject = "Test",
            )

        composeTestRule.setContent {
            ShareReceiverScreen(viewModel = viewModel, onFinish = {}, onRedirectToWizard = {})
        }

        // Click to expand folder picker
        composeTestRule.onNodeWithTag("share_folder_picker").performClick()
        composeTestRule.waitForIdle()

        // Select "Work" folder
        composeTestRule.onNodeWithText("Work").performClick()
        composeTestRule.waitForIdle()

        // Save
        composeTestRule.onNodeWithTag("share_save_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("folder-1", fakeRepo.addBookmarkCalls[0].first)
    }

    @Test
    fun `redirects to wizard when not initialized`() {
        var redirected = false
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "https://example.com",
                extraSubject = null,
                isAppInitialized = false,
            )

        composeTestRule.setContent {
            ShareReceiverScreen(viewModel = viewModel, onFinish = {}, onRedirectToWizard = { redirected = true })
        }

        composeTestRule.waitForIdle()
        assertTrue(redirected)
    }
}
