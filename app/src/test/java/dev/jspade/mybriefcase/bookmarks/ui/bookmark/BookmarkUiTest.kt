package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderScreen
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class BookmarkUiTest {

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
    fun `FAB opens speed-dial menu`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        // FAB should be visible
        composeTestRule.onNodeWithTag("fab_main").assertIsDisplayed()

        // Click FAB to open speed-dial
        composeTestRule.onNodeWithTag("fab_main").performClick()
        composeTestRule.waitForIdle()

        // Mini-FABs should appear
        composeTestRule.onNodeWithTag("fab_add_bookmark").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fab_add_folder").assertIsDisplayed()
    }

    @Test
    fun `FAB opens add bookmark dialog`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("fab_main").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fab_add_bookmark").performClick()
        composeTestRule.waitForIdle()

        // Add bookmark dialog should appear
        composeTestRule.onNodeWithTag("add_bookmark_dialog").assertIsDisplayed()
    }

    @Test
    fun `add dialog validates URL required`() {
        composeTestRule.setContent {
            AddBookmarkDialog(
                onDismiss = {},
                onConfirm = { _, _ -> },
            )
        }

        // Click confirm without entering URL
        composeTestRule.onNodeWithTag("add_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        // Error text should appear
        composeTestRule.onNodeWithText("URL is required").assertIsDisplayed()
    }

    @Test
    fun `detail sheet shows all fields`() {
        val bookmark = BookmarkDto(
            id = "test-id",
            url = "https://example.com",
            title = "Example",
            notes = "Some notes here",
            createdAt = "2024-01-15T10:30:00Z",
            updatedAt = "2024-02-20T14:45:00Z",
        )

        composeTestRule.setContent {
            BookmarkDetailSheet(
                bookmark = bookmark,
                onDismiss = {},
                onEdit = {},
                onOpenInBrowser = {},
                onShare = {},
                onCopyUrl = {},
            )
        }

        composeTestRule.onNodeWithTag("detail_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_url").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_notes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_created").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_updated").assertIsDisplayed()
    }

    @Test
    fun `detail sheet shows intent action buttons`() {
        val bookmark = BookmarkDto(
            id = "test-id",
            url = "https://example.com",
            title = "Example",
            notes = "",
            createdAt = "2024-01-15T10:30:00Z",
            updatedAt = "2024-02-20T14:45:00Z",
        )

        composeTestRule.setContent {
            BookmarkDetailSheet(
                bookmark = bookmark,
                onDismiss = {},
                onEdit = {},
                onOpenInBrowser = {},
                onShare = {},
                onCopyUrl = {},
            )
        }

        composeTestRule.onNode(hasTestTag("action_open_browser")).assertExists()
        composeTestRule.onNode(hasTestTag("action_share")).assertExists()
        composeTestRule.onNode(hasTestTag("action_copy")).assertExists()
        composeTestRule.onNode(hasTestTag("action_edit")).assertExists()
    }

    @Test
    fun `edit dialog pre-fills bookmark fields`() {
        val bookmark = BookmarkDto(
            id = "test-id",
            url = "https://prefilled.com",
            title = "Prefilled Title",
            notes = "Prefilled notes",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
        )

        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = bookmark,
                onDismiss = {},
                onConfirm = { _, _, _, _ -> },
            )
        }

        composeTestRule.onNodeWithText("https://prefilled.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prefilled Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prefilled notes").assertIsDisplayed()
    }

    @Test
    fun `context menu appears on long-press`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        // Navigate to folder with a bookmark
        viewModel.navigateToFolder("folder-1")
        composeTestRule.waitForIdle()

        // Long-press on the bookmark
        composeTestRule.onNodeWithText("https://github.com").performTouchInput {
            longClick()
        }
        composeTestRule.waitForIdle()

        // Context menu should show
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun `delete shows confirmation dialog`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        viewModel.navigateToFolder("folder-1")
        composeTestRule.waitForIdle()

        // Long-press to show context menu
        composeTestRule.onNodeWithText("https://github.com").performTouchInput {
            longClick()
        }
        composeTestRule.waitForIdle()

        // Click delete in context menu
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()

        // Confirmation dialog should show
        composeTestRule.onNodeWithTag("delete_confirmation_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete this bookmark?").assertIsDisplayed()
    }

    @Test
    fun `overflow menu shows settings option`() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

        composeTestRule.setContent {
            FolderScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("overflow_menu_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun `edit dialog shows folder picker with current folder`() {
        val bookmark = BookmarkDto(
            id = "test-id",
            url = "https://example.com",
            title = "Example",
            notes = "",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
        )
        val navTree = FolderNavTreeDto(
            rootFolderId = "root-id",
            folders = listOf(
                FolderNavDto(id = "root-id", title = "Bookmarks", itemCount = 2u, childFolderIds = listOf("folder-1", "folder-2")),
                FolderNavDto(id = "folder-1", title = "Work", itemCount = 1u, childFolderIds = emptyList()),
                FolderNavDto(id = "folder-2", title = "Personal", itemCount = 0u, childFolderIds = emptyList()),
            ),
        )

        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = bookmark,
                navTree = navTree,
                currentFolderId = "folder-1",
                onDismiss = {},
                onConfirm = { _, _, _, _ -> },
            )
        }

        // Folder picker should be displayed showing current folder name
        composeTestRule.onNodeWithTag("edit_bookmark_folder_picker").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
    }

    @Test
    fun `edit dialog folder picker allows selecting different folder`() {
        val bookmark = BookmarkDto(
            id = "test-id",
            url = "https://example.com",
            title = "Example",
            notes = "",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
        )
        val navTree = FolderNavTreeDto(
            rootFolderId = "root-id",
            folders = listOf(
                FolderNavDto(id = "root-id", title = "Bookmarks", itemCount = 2u, childFolderIds = listOf("folder-1", "folder-2")),
                FolderNavDto(id = "folder-1", title = "Work", itemCount = 1u, childFolderIds = emptyList()),
                FolderNavDto(id = "folder-2", title = "Personal", itemCount = 0u, childFolderIds = emptyList()),
            ),
        )
        var confirmedFolderId: String? = null

        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = bookmark,
                navTree = navTree,
                currentFolderId = "folder-1",
                onDismiss = {},
                onConfirm = { _, _, _, folderId -> confirmedFolderId = folderId },
            )
        }

        // Click the folder picker to expand it
        composeTestRule.onNodeWithTag("edit_bookmark_folder_picker").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Select "Personal" folder (indented in hierarchical tree)
        composeTestRule.onNodeWithText("Personal", substring = true).performClick()
        composeTestRule.waitForIdle()

        // Save
        composeTestRule.onNodeWithTag("edit_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals("folder-2", confirmedFolderId)
    }

    @Test
    fun `edit dialog confirm passes null folderId when folder not changed`() {
        val bookmark = BookmarkDto(
            id = "test-id",
            url = "https://example.com",
            title = "Example",
            notes = "",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
        )
        val navTree = FolderNavTreeDto(
            rootFolderId = "root-id",
            folders = listOf(
                FolderNavDto(id = "root-id", title = "Bookmarks", itemCount = 2u, childFolderIds = listOf("folder-1")),
                FolderNavDto(id = "folder-1", title = "Work", itemCount = 1u, childFolderIds = emptyList()),
            ),
        )
        var confirmedFolderId: String? = "sentinel"

        composeTestRule.setContent {
            EditBookmarkDialog(
                bookmark = bookmark,
                navTree = navTree,
                currentFolderId = "folder-1",
                onDismiss = {},
                onConfirm = { _, _, _, folderId -> confirmedFolderId = folderId },
            )
        }

        // Save without changing folder
        composeTestRule.onNodeWithTag("edit_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        assertNull(confirmedFolderId)
    }
}
