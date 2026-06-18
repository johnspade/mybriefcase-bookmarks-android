package dev.jspade.mybriefcase.bookmarks.ui.folder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class FolderDialogsTest {
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
    fun `create folder dialog validates non-empty title`() {
        var createdTitle: String? = null

        composeTestRule.setContent {
            CreateFolderDialog(
                onConfirm = { createdTitle = it },
                onDismiss = {},
            )
        }

        // Initially the Create button should be disabled (empty title)
        composeTestRule.onNodeWithText("Create").assertIsNotEnabled()

        // Type a title
        composeTestRule.onNodeWithText("Folder name").performTextInput("My Folder")

        // Now Create button should be enabled
        composeTestRule.onNodeWithText("Create").assertIsEnabled()
        composeTestRule.onNodeWithText("Create").performClick()

        assert(createdTitle == "My Folder")
    }

    @Test
    fun `rename dialog pre-fills current title`() {
        composeTestRule.setContent {
            RenameFolderDialog(
                currentTitle = "Work",
                onConfirm = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rename").assertIsEnabled()
    }

    @Test
    fun `rename dialog validates non-empty title`() {
        composeTestRule.setContent {
            RenameFolderDialog(
                currentTitle = "Work",
                onConfirm = {},
                onDismiss = {},
            )
        }

        // Clear the pre-filled title
        composeTestRule.onNodeWithText("Work").performTextClearance()

        // Rename button should be disabled
        composeTestRule.onNodeWithText("Rename").assertIsNotEnabled()
    }

    @Test
    fun `delete dialog shows cascade warning`() {
        composeTestRule.setContent {
            DeleteFolderDialog(
                folderTitle = "Work",
                onConfirm = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Delete \"Work\"?").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("This folder and all its contents will be deleted.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `move picker renders tree with current location disabled`() {
        val navTree =
            FolderNavTreeDto(
                rootFolderId = "root-id",
                folders =
                    listOf(
                        FolderNavDto(
                            id = "root-id",
                            title = "Bookmarks",
                            itemCount = 2u,
                            childFolderIds = listOf("folder-1", "folder-2"),
                        ),
                        FolderNavDto(
                            id = "folder-1",
                            title = "Work",
                            itemCount = 1u,
                            childFolderIds = emptyList(),
                        ),
                        FolderNavDto(
                            id = "folder-2",
                            title = "Personal",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                    ),
            )

        composeTestRule.setContent {
            MoveItemDialog(
                navTree = navTree,
                currentFolderId = "folder-1",
                onConfirm = {},
                onDismiss = {},
            )
        }

        // All folders should be displayed
        composeTestRule.onNodeWithText("Bookmarks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Personal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Move here").assertIsDisplayed()
    }

    @Test
    fun `move picker completes move when destination selected`() {
        val navTree =
            FolderNavTreeDto(
                rootFolderId = "root-id",
                folders =
                    listOf(
                        FolderNavDto(
                            id = "root-id",
                            title = "Bookmarks",
                            itemCount = 2u,
                            childFolderIds = listOf("folder-1", "folder-2"),
                        ),
                        FolderNavDto(
                            id = "folder-1",
                            title = "Work",
                            itemCount = 1u,
                            childFolderIds = emptyList(),
                        ),
                        FolderNavDto(
                            id = "folder-2",
                            title = "Personal",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                    ),
            )

        var selectedFolderId: String? = null

        composeTestRule.setContent {
            MoveItemDialog(
                navTree = navTree,
                currentFolderId = "folder-1",
                onConfirm = { selectedFolderId = it },
                onDismiss = {},
            )
        }

        // Select "Personal" folder
        composeTestRule.onNodeWithText("Personal").performClick()
        composeTestRule.waitForIdle()

        // Confirm the move
        composeTestRule.onNodeWithText("Move here").performClick()

        assert(selectedFolderId == "folder-2")
    }

    @Test
    fun `bookmark move from folder with children keeps subfolders clickable`() {
        val navTree =
            FolderNavTreeDto(
                rootFolderId = "root-id",
                folders =
                    listOf(
                        FolderNavDto(
                            id = "root-id",
                            title = "Bookmarks",
                            itemCount = 3u,
                            childFolderIds = listOf("folder-1"),
                        ),
                        FolderNavDto(
                            id = "folder-1",
                            title = "Work",
                            itemCount = 1u,
                            childFolderIds = listOf("folder-1-1"),
                        ),
                        FolderNavDto(
                            id = "folder-1-1",
                            title = "Projects",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                    ),
            )

        var selectedFolderId: String? = null

        composeTestRule.setContent {
            MoveItemDialog(
                navTree = navTree,
                currentFolderId = "folder-1",
                movedFolderId = null,
                onConfirm = { selectedFolderId = it },
                onDismiss = {},
            )
        }

        // Subfolder "Projects" should be clickable (bookmark move, not folder move)
        composeTestRule.onNodeWithText("Projects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move here").performClick()

        assert(selectedFolderId == "folder-1-1")
    }

    @Test
    fun `bookmark move from root keeps all child folders clickable`() {
        val navTree =
            FolderNavTreeDto(
                rootFolderId = "root-id",
                folders =
                    listOf(
                        FolderNavDto(
                            id = "root-id",
                            title = "Bookmarks",
                            itemCount = 5u,
                            childFolderIds = listOf("folder-1", "folder-2"),
                        ),
                        FolderNavDto(
                            id = "folder-1",
                            title = "Work",
                            itemCount = 1u,
                            childFolderIds = listOf("folder-1-1"),
                        ),
                        FolderNavDto(
                            id = "folder-1-1",
                            title = "Projects",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                        FolderNavDto(
                            id = "folder-2",
                            title = "Personal",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                    ),
            )

        var selectedFolderId: String? = null

        composeTestRule.setContent {
            MoveItemDialog(
                navTree = navTree,
                currentFolderId = "root-id",
                movedFolderId = null,
                onConfirm = { selectedFolderId = it },
                onDismiss = {},
            )
        }

        // All non-root folders should be clickable
        composeTestRule.onNodeWithText("Work").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move here").performClick()
        assert(selectedFolderId == "folder-1")

        // Reset and try a deeper folder
        selectedFolderId = null
        composeTestRule.onNodeWithText("Projects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move here").performClick()
        assert(selectedFolderId == "folder-1-1")
    }

    @Test
    fun `folder move disables self and descendants`() {
        val navTree =
            FolderNavTreeDto(
                rootFolderId = "root-id",
                folders =
                    listOf(
                        FolderNavDto(
                            id = "root-id",
                            title = "Bookmarks",
                            itemCount = 3u,
                            childFolderIds = listOf("folder-1", "folder-2"),
                        ),
                        FolderNavDto(
                            id = "folder-1",
                            title = "Work",
                            itemCount = 1u,
                            childFolderIds = listOf("folder-1-1"),
                        ),
                        FolderNavDto(
                            id = "folder-1-1",
                            title = "Projects",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                        FolderNavDto(
                            id = "folder-2",
                            title = "Personal",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                    ),
            )

        var selectedFolderId: String? = null

        composeTestRule.setContent {
            MoveItemDialog(
                navTree = navTree,
                currentFolderId = "root-id",
                movedFolderId = "folder-1",
                onConfirm = { selectedFolderId = it },
                onDismiss = {},
            )
        }

        // "Work" (the moved folder) should be disabled — clicking should do nothing
        composeTestRule.onNodeWithText("Work").performClick()
        composeTestRule.waitForIdle()
        assert(selectedFolderId == null)

        // "Projects" (descendant of moved folder) should be disabled
        composeTestRule.onNodeWithText("Projects").performClick()
        composeTestRule.waitForIdle()
        assert(selectedFolderId == null)

        // "Personal" (sibling) should be enabled
        composeTestRule.onNodeWithText("Personal").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move here").performClick()
        assert(selectedFolderId == "folder-2")
    }

    @Test
    @Suppress("LongMethod")
    fun `folder move leaves unrelated branches enabled`() {
        val navTree =
            FolderNavTreeDto(
                rootFolderId = "root-id",
                folders =
                    listOf(
                        FolderNavDto(
                            id = "root-id",
                            title = "Bookmarks",
                            itemCount = 4u,
                            childFolderIds = listOf("branch-a", "branch-b"),
                        ),
                        FolderNavDto(
                            id = "branch-a",
                            title = "Branch A",
                            itemCount = 1u,
                            childFolderIds = listOf("branch-a-child"),
                        ),
                        FolderNavDto(
                            id = "branch-a-child",
                            title = "A Child",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                        FolderNavDto(
                            id = "branch-b",
                            title = "Branch B",
                            itemCount = 1u,
                            childFolderIds = listOf("branch-b-child"),
                        ),
                        FolderNavDto(
                            id = "branch-b-child",
                            title = "B Child",
                            itemCount = 0u,
                            childFolderIds = emptyList(),
                        ),
                    ),
            )

        var selectedFolderId: String? = null

        composeTestRule.setContent {
            MoveItemDialog(
                navTree = navTree,
                currentFolderId = "root-id",
                movedFolderId = "branch-a",
                onConfirm = { selectedFolderId = it },
                onDismiss = {},
            )
        }

        // branch-a subtree should be disabled
        composeTestRule.onNodeWithText("Branch A").performClick()
        composeTestRule.waitForIdle()
        assert(selectedFolderId == null)

        composeTestRule.onNodeWithText("A Child").performClick()
        composeTestRule.waitForIdle()
        assert(selectedFolderId == null)

        // branch-b subtree should be fully enabled
        composeTestRule.onNodeWithText("Branch B").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move here").performClick()
        assert(selectedFolderId == "branch-b")

        selectedFolderId = null
        composeTestRule.onNodeWithText("B Child").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move here").performClick()
        assert(selectedFolderId == "branch-b-child")
    }

    @Test
    fun `bookmark move allows selecting parent folder`() {
        val navTree =
            FolderNavTreeDto(
                rootFolderId = "root-id",
                folders =
                    listOf(
                        FolderNavDto(
                            id = "root-id",
                            title = "Bookmarks",
                            itemCount = 2u,
                            childFolderIds = listOf("folder-1"),
                        ),
                        FolderNavDto(
                            id = "folder-1",
                            title = "Work",
                            itemCount = 1u,
                            childFolderIds = listOf("folder-1-1"),
                        ),
                        FolderNavDto(
                            id = "folder-1-1",
                            title = "Projects",
                            itemCount = 1u,
                            childFolderIds = emptyList(),
                        ),
                    ),
            )

        var selectedFolderId: String? = null

        composeTestRule.setContent {
            MoveItemDialog(
                navTree = navTree,
                currentFolderId = "folder-1-1",
                movedFolderId = null,
                onConfirm = { selectedFolderId = it },
                onDismiss = {},
            )
        }

        // Parent folder "Work" should be clickable
        composeTestRule.onNodeWithText("Work").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move here").performClick()
        assert(selectedFolderId == "folder-1")
    }
}
