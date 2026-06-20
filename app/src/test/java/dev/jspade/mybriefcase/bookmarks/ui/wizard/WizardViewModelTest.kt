package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class WizardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        context
            .getSharedPreferences("mybriefcase", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has no selected path and done disabled`() =
        runTest {
            val viewModel = WizardViewModel(context)

            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.selectedPath)
                assertFalse(state.canFinish)
                assertNull(state.error)
            }
        }

    @Test
    fun `selecting valid primary storage URI sets path and enables done`() =
        runTest {
            val viewModel = WizardViewModel(context)

            val uri =
                Uri.parse(
                    "content://com.android.externalstorage.documents/tree/primary%3ASyncthing%2Fmybriefcase_bookmarks",
                )
            viewModel.onDirectorySelected(uri)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("/storage/emulated/0/Syncthing/mybriefcase_bookmarks", state.selectedPath)
                assertTrue(state.canFinish)
                assertNull(state.error)
            }
        }

    @Test
    fun `selecting non-primary URI shows error`() =
        runTest {
            val viewModel = WizardViewModel(context)

            val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3ASome%2FPath")
            viewModel.onDirectorySelected(uri)

            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.selectedPath)
                assertFalse(state.canFinish)
                assertEquals("Please select a folder on internal storage", state.error)
            }
        }

    @Test
    fun `finishing wizard persists path and marks complete`() =
        runTest {
            val viewModel = WizardViewModel(context)

            val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ASyncthing%2Fbookmarks")
            viewModel.onDirectorySelected(uri)
            viewModel.finish()
            advanceUntilIdle()

            val savedPath =
                context
                    .getSharedPreferences("mybriefcase", Context.MODE_PRIVATE)
                    .getString("sync_dir", null)
            assertEquals("/storage/emulated/0/Syncthing/bookmarks", savedPath)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isComplete)
            }
        }

    @Test
    fun `cannot finish without a selected path`() =
        runTest {
            val viewModel = WizardViewModel(context)

            viewModel.finish()
            advanceUntilIdle()

            val savedPath =
                context
                    .getSharedPreferences("mybriefcase", Context.MODE_PRIVATE)
                    .getString("sync_dir", null)
            assertNull(savedPath)

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isComplete)
            }
        }
}
