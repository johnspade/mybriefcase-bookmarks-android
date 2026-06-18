package dev.jspade.mybriefcase.bookmarks.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
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

    // --- Bookmark detail content ---

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
                Surface {
                    BookmarkDetailContent(bookmark)
                }
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
                Surface {
                    BookmarkDetailContent(bookmark)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/bookmark_detail_dark.png")
    }

    // --- Add bookmark dialog content ---

    @Test
    fun addBookmarkDialog_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    AddBookmarkDialogContent()
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/add_bookmark_dialog_light.png")
    }

    @Test
    fun addBookmarkDialog_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                Surface {
                    AddBookmarkDialogContent()
                }
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

@Composable
private fun BookmarkDetailContent(bookmark: BookmarkDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = bookmark.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = bookmark.url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (bookmark.notes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bookmark.notes,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Created: ${bookmark.createdAt}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Updated: ${bookmark.updatedAt}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AddBookmarkDialogContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add Bookmark", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Title (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
