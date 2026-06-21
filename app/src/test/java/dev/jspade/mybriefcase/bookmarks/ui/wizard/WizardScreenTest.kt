package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.app.Application
import android.content.Context
import android.os.Environment
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = dev.jspade.mybriefcase.bookmarks.TestApp::class,
    shadows = [ShadowEnvironmentPermissionGranted::class],
)
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
        viewModel = WizardViewModel(context as Application)
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
        // Navigate to directory slide
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_error").assertIsDisplayed()
    }

    @Test
    fun `permission slide shows granted status when permission is granted`() {
        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = {})
        }
        // Navigate to permission slide
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_slide_permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Granted").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wizard_grant_permission").assertDoesNotExist()
    }

    @Test
    @Config(shadows = [ShadowEnvironmentPermissionDenied::class])
    fun `permission slide shows grant button when permission is denied`() {
        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = {})
        }
        // Navigate to permission slide
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_slide_permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Not granted").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wizard_grant_permission").assertIsDisplayed()
    }

    @Test
    @Config(shadows = [ShadowEnvironmentPermissionDenied::class])
    fun `done button disabled without storage permission`() {
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
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_done").assertIsNotEnabled()
    }

    @Test
    fun `onComplete called exactly once when wizard finishes`() {
        val uri =
            android.net.Uri.parse(
                "content://com.android.externalstorage.documents/tree/primary%3ASyncthing%2Fbookmarks",
            )
        viewModel.onDirectorySelected(uri)

        var callCount = 0
        composeTestRule.setContent {
            WizardScreen(viewModel = viewModel, onComplete = { callCount++ })
        }
        // Navigate to last slide and finish
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_next").performClick()
        composeTestRule.onNodeWithTag("wizard_done").performClick()
        composeTestRule.waitForIdle()

        assert(callCount == 1) { "Expected onComplete called once, but was called $callCount times" }
    }
}

@Suppress("UtilityClassWithPublicConstructor", "FunctionOnlyReturningConstant")
@Implements(Environment::class)
class ShadowEnvironmentPermissionGranted {
    companion object {
        @Implementation
        @JvmStatic
        fun isExternalStorageManager(): Boolean = true
    }
}

@Suppress("UtilityClassWithPublicConstructor", "FunctionOnlyReturningConstant")
@Implements(Environment::class)
class ShadowEnvironmentPermissionDenied {
    companion object {
        @Implementation
        @JvmStatic
        fun isExternalStorageManager(): Boolean = false
    }
}
