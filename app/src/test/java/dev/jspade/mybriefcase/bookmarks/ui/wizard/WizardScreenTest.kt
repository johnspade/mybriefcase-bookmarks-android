package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class WizardScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var viewModel: WizardViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context
            .getSharedPreferences("mybriefcase", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        viewModel = WizardViewModel(context)
    }

    @Test
    fun `shows welcome slide initially`() {
        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = {})
        }
        composeTestRule
            .onNodeWithText("Welcome to MyBriefcase Bookmarks")
            .assertIsDisplayed()
    }

    @Test
    fun `back button hidden on first slide`() {
        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = {})
        }
        composeTestRule.onNodeWithTag("wizard_back").assertDoesNotExist()
    }

    @Test
    fun `next button navigates to syncthing slide`() {
        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = {})
        }
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_slide_syncthing").assertIsDisplayed()
    }

    @Test
    fun `done button disabled without directory selection`() {
        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = {})
        }
        // Navigate to last slide
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_done").assertIsNotEnabled()
    }

    @Test
    fun `done button enabled after valid directory selection`() {
        val uri =
            android.net.Uri.parse(
                "content://com.android.externalstorage.documents/tree/primary%3ASyncthing%2Fbookmarks",
            )
        viewModel.onDirectorySelected(uri)

        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = {})
        }
        // Navigate to last slide
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_done").assertIsEnabled()
    }

    @Test
    fun `error shown after invalid directory selection`() {
        val uri =
            android.net.Uri.parse(
                "content://com.android.externalstorage.documents/tree/1234-5678%3ASome%2FPath",
            )
        viewModel.onDirectorySelected(uri)

        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = {})
        }
        // Navigate to last slide
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_error").assertIsDisplayed()
    }
}
