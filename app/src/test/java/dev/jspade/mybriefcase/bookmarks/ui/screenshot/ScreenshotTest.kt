package dev.jspade.mybriefcase.bookmarks.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.AddBookmarkDialog
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.BookmarkDetailSheet
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderScreen
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderViewModel
import dev.jspade.mybriefcase.bookmarks.ui.search.SearchScreen
import dev.jspade.mybriefcase.bookmarks.ui.search.SearchViewModel
import dev.jspade.mybriefcase.bookmarks.ui.settings.SettingsScreen
import dev.jspade.mybriefcase.bookmarks.ui.theme.MyBriefcaseBookmarksTheme
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
import org.robolectric.annotation.GraphicsMode
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class ScreenshotTest {

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

    // --- Folder view (populated) ---

    @Test
    fun folderScreen_populated_light() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                FolderScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/folder_populated_light.png")
    }

    @Test
    fun folderScreen_populated_dark() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                FolderScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/folder_populated_dark.png")
    }

    // --- Folder view (empty) ---

    @Test
    fun folderScreen_empty_light() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
        viewModel.navigateToFolder("folder-2") // Personal folder (empty)
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                FolderScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/folder_empty_light.png")
    }

    @Test
    fun folderScreen_empty_dark() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
        viewModel.navigateToFolder("folder-2")
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                FolderScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/folder_empty_dark.png")
    }

    // --- Search screen with results ---

    @Test
    fun searchScreen_withResults_light() {
        fakeRepo.searchResults = listOf(
            BookmarkDto(
                id = "bm-1",
                url = "https://github.com",
                title = "GitHub",
                notes = "",
                createdAt = "2024-01-01T00:00:00Z",
                updatedAt = "2024-01-01T00:00:00Z",
            ),
            BookmarkDto(
                id = "bm-2",
                url = "https://example.com",
                title = "Example Site",
                notes = "A sample bookmark",
                createdAt = "2024-02-15T10:00:00Z",
                updatedAt = "2024-02-15T10:00:00Z",
            ),
        )
        val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, debounceMs = 0L)
        viewModel.setQuery("git")
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                SearchScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/search_results_light.png")
    }

    @Test
    fun searchScreen_withResults_dark() {
        fakeRepo.searchResults = listOf(
            BookmarkDto(
                id = "bm-1",
                url = "https://github.com",
                title = "GitHub",
                notes = "",
                createdAt = "2024-01-01T00:00:00Z",
                updatedAt = "2024-01-01T00:00:00Z",
            ),
            BookmarkDto(
                id = "bm-2",
                url = "https://example.com",
                title = "Example Site",
                notes = "A sample bookmark",
                createdAt = "2024-02-15T10:00:00Z",
                updatedAt = "2024-02-15T10:00:00Z",
            ),
        )
        val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, debounceMs = 0L)
        viewModel.setQuery("git")
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                SearchScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/search_results_dark.png")
    }

    // --- Bookmark detail sheet ---

    @Test
    fun bookmarkDetailSheet_light() {
        val bookmark = BookmarkDto(
            id = "bm-1",
            url = "https://github.com",
            title = "GitHub",
            notes = "A code hosting platform",
            createdAt = "2024-01-15T10:30:00Z",
            updatedAt = "2024-02-20T14:45:00Z",
        )
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                BookmarkDetailSheet(
                    bookmark = bookmark,
                    onDismiss = {},
                    onEdit = {},
                    onOpenInBrowser = {},
                    onShare = {},
                    onCopyUrl = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/bookmark_detail_light.png")
    }

    @Test
    fun bookmarkDetailSheet_dark() {
        val bookmark = BookmarkDto(
            id = "bm-1",
            url = "https://github.com",
            title = "GitHub",
            notes = "A code hosting platform",
            createdAt = "2024-01-15T10:30:00Z",
            updatedAt = "2024-02-20T14:45:00Z",
        )
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                BookmarkDetailSheet(
                    bookmark = bookmark,
                    onDismiss = {},
                    onEdit = {},
                    onOpenInBrowser = {},
                    onShare = {},
                    onCopyUrl = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/bookmark_detail_dark.png")
    }

    // --- Add bookmark dialog ---

    @Test
    fun addBookmarkDialog_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                AddBookmarkDialog(
                    onDismiss = {},
                    onConfirm = { _, _ -> },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/add_bookmark_dialog_light.png")
    }

    @Test
    fun addBookmarkDialog_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                AddBookmarkDialog(
                    onDismiss = {},
                    onConfirm = { _, _ -> },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/add_bookmark_dialog_dark.png")
    }

    // --- Settings screen ---

    @Test
    fun settingsScreen_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                SettingsScreen(
                    syncDir = "/storage/emulated/0/Syncthing/mybriefcase_bookmarks",
                    clientId = "Pixel-7-MyBriefcaseBookmarks-a3f2",
                    appVersion = "1.0.0",
                    onBack = {},
                    onImport = {},
                    onExport = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/settings_light.png")
    }

    @Test
    fun settingsScreen_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                SettingsScreen(
                    syncDir = "/storage/emulated/0/Syncthing/mybriefcase_bookmarks",
                    clientId = "Pixel-7-MyBriefcaseBookmarks-a3f2",
                    appVersion = "1.0.0",
                    onBack = {},
                    onImport = {},
                    onExport = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/settings_dark.png")
    }
}
