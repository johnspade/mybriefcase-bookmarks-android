package dev.jspade.mybriefcase.bookmarks.ui.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.AddBookmarkDialog
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.BookmarkDetailSheet
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.BookmarkFavicon
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.EditBookmarkDialog
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.FaviconFetchState
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.LetterAvatar
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderScreen
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderViewModel
import dev.jspade.mybriefcase.bookmarks.ui.search.SearchScreen
import dev.jspade.mybriefcase.bookmarks.ui.search.SearchViewModel
import dev.jspade.mybriefcase.bookmarks.ui.settings.SettingsScreen
import dev.jspade.mybriefcase.bookmarks.ui.share.ShareReceiverScreen
import dev.jspade.mybriefcase.bookmarks.ui.share.ShareReceiverViewModel
import dev.jspade.mybriefcase.bookmarks.ui.theme.MyBriefcaseBookmarksTheme
import dev.jspade.mybriefcase.bookmarks.ui.wizard.DirectorySlide
import dev.jspade.mybriefcase.bookmarks.ui.wizard.PermissionSlide
import dev.jspade.mybriefcase.bookmarks.ui.wizard.SyncthingSlide
import dev.jspade.mybriefcase.bookmarks.ui.wizard.WelcomeSlide
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi", application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
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
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                FolderScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/folder_populated_light.png")
    }

    @Test
    fun folderScreen_populated_dark() {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
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
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
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
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
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
        fakeRepo.searchResults =
            listOf(
                BookmarkDto(
                    id = "bm-1",
                    url = "https://github.com",
                    title = "GitHub",
                    notes = "",
                    favicon = null,
                    createdAt = "2024-01-01T00:00:00Z",
                    updatedAt = "2024-01-01T00:00:00Z",
                ),
                BookmarkDto(
                    id = "bm-2",
                    url = "https://example.com",
                    title = "Example Site",
                    notes = "A sample bookmark",
                    favicon = null,
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
        fakeRepo.searchResults =
            listOf(
                BookmarkDto(
                    id = "bm-1",
                    url = "https://github.com",
                    title = "GitHub",
                    notes = "",
                    favicon = null,
                    createdAt = "2024-01-01T00:00:00Z",
                    updatedAt = "2024-01-01T00:00:00Z",
                ),
                BookmarkDto(
                    id = "bm-2",
                    url = "https://example.com",
                    title = "Example Site",
                    notes = "A sample bookmark",
                    favicon = null,
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
        val bookmark =
            BookmarkDto(
                id = "bm-1",
                url = "https://github.com",
                title = "GitHub",
                notes = "A code hosting platform",
                favicon = null,
                createdAt = "2024-01-15T10:30:00Z",
                updatedAt = "2024-02-20T14:45:00Z",
            )
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                BookmarkDetailSheet(
                    bookmark = bookmark,
                    onDismiss = {},
                    onEdit = {},
                    onHistory = {},
                    onOpenInBrowser = {},
                    onShare = {},
                    onCopyUrl = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        captureScreenRoboImage("src/test/snapshots/bookmark_detail_light.png")
    }

    @Test
    fun bookmarkDetailSheet_dark() {
        val bookmark =
            BookmarkDto(
                id = "bm-1",
                url = "https://github.com",
                title = "GitHub",
                notes = "A code hosting platform",
                favicon = null,
                createdAt = "2024-01-15T10:30:00Z",
                updatedAt = "2024-02-20T14:45:00Z",
            )
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                BookmarkDetailSheet(
                    bookmark = bookmark,
                    onDismiss = {},
                    onEdit = {},
                    onHistory = {},
                    onOpenInBrowser = {},
                    onShare = {},
                    onCopyUrl = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        captureScreenRoboImage("src/test/snapshots/bookmark_detail_dark.png")
    }

    // --- Add bookmark dialog ---

    @Test
    @Config(qualifiers = "+w320dp-h780dp-xxhdpi")
    fun addBookmarkDialog_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                AddBookmarkDialog(
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                    faviconFetchEnabled = true,
                    faviconFetchState = FaviconFetchState.Idle,
                    onFetchFavicon = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/add_bookmark_dialog_light.png")
    }

    @Test
    @Config(qualifiers = "+w320dp-h780dp-xxhdpi")
    fun addBookmarkDialog_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                AddBookmarkDialog(
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                    faviconFetchEnabled = true,
                    faviconFetchState = FaviconFetchState.Idle,
                    onFetchFavicon = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/add_bookmark_dialog_dark.png")
    }

    // --- Edit bookmark dialog ---

    @Test
    @Config(qualifiers = "+w320dp-h780dp-xxhdpi")
    fun editBookmarkDialog_light() {
        val bookmark =
            BookmarkDto(
                id = "bm-1",
                url = "https://github.com",
                title = "GitHub",
                notes = "A code hosting platform",
                favicon = null,
                createdAt = "2024-01-15T10:30:00Z",
                updatedAt = "2024-02-20T14:45:00Z",
            )
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                EditBookmarkDialog(
                    bookmark = bookmark,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _ -> },
                    faviconFetchEnabled = true,
                    faviconFetchState = FaviconFetchState.Idle,
                    onFetchFavicon = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/edit_bookmark_dialog_light.png")
    }

    @Test
    @Config(qualifiers = "+w320dp-h780dp-xxhdpi")
    fun editBookmarkDialog_dark() {
        val bookmark =
            BookmarkDto(
                id = "bm-1",
                url = "https://github.com",
                title = "GitHub",
                notes = "A code hosting platform",
                favicon = null,
                createdAt = "2024-01-15T10:30:00Z",
                updatedAt = "2024-02-20T14:45:00Z",
            )
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                EditBookmarkDialog(
                    bookmark = bookmark,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _ -> },
                    faviconFetchEnabled = true,
                    faviconFetchState = FaviconFetchState.Idle,
                    onFetchFavicon = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/edit_bookmark_dialog_dark.png")
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
                    onChangeSyncDir = {},
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
                    onChangeSyncDir = {},
                    onImport = {},
                    onExport = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/settings_dark.png")
    }

    // --- BookmarkFavicon: letter avatar ---

    @Test
    fun bookmarkFavicon_letterAvatar_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LetterAvatar(url = "https://github.com")
                        Spacer(modifier = Modifier.height(8.dp))
                        LetterAvatar(url = "https://example.org")
                        Spacer(modifier = Modifier.height(8.dp))
                        LetterAvatar(url = "https://stackoverflow.com")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/letter_avatar_light.png")
    }

    @Test
    fun bookmarkFavicon_letterAvatar_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                Surface {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LetterAvatar(url = "https://github.com")
                        Spacer(modifier = Modifier.height(8.dp))
                        LetterAvatar(url = "https://example.org")
                        Spacer(modifier = Modifier.height(8.dp))
                        LetterAvatar(url = "https://stackoverflow.com")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/letter_avatar_dark.png")
    }

    @Test
    fun bookmarkFavicon_fallbackWhenNoFavicon() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    BookmarkFavicon(
                        url = "https://github.com",
                        favicon = null,
                        syncRoot = null,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/favicon_fallback.png")
    }

    @Test
    fun bookmarkFavicon_withFaviconImage() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "screenshot_test_favicons")
        val faviconsDir = File(tempDir, "favicons")
        faviconsDir.mkdirs()
        val faviconFile = File(faviconsDir, "abc123.png")
        try {
            val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.RED)
            faviconFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

            composeTestRule.setContent {
                MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                    Surface {
                        BookmarkFavicon(
                            url = "https://github.com",
                            favicon = "abc123.png",
                            syncRoot = tempDir.absolutePath,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule.onRoot().captureRoboImage("src/test/snapshots/favicon_image.png")
        } finally {
            faviconFile.delete()
            faviconsDir.delete()
            tempDir.delete()
        }
    }

    // --- Wizard: Welcome slide ---

    @Test
    fun wizard_welcome_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                Surface { Box(modifier = Modifier.fillMaxSize()) { WelcomeSlide() } }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/wizard_welcome_light.png")
    }

    @Test
    fun wizard_welcome_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                Surface { Box(modifier = Modifier.fillMaxSize()) { WelcomeSlide() } }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/wizard_welcome_dark.png")
    }

    // --- Wizard: Syncthing slide ---

    @Test
    fun wizard_syncthing_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SyncthingSlide(LocalContext.current)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/wizard_syncthing_light.png")
    }

    @Test
    fun wizard_syncthing_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                Surface {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SyncthingSlide(LocalContext.current)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/wizard_syncthing_dark.png")
    }

    // --- Wizard: Directory slide ---

    @Test
    fun wizard_directory_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DirectorySlide(
                            selectedPath = "/storage/emulated/0/Syncthing/bookmarks",
                            error = null,
                            onChooseDirectory = {},
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/wizard_directory_light.png")
    }

    @Test
    fun wizard_directory_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                Surface {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DirectorySlide(
                            selectedPath = "/storage/emulated/0/Syncthing/bookmarks",
                            error = null,
                            onChooseDirectory = {},
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/wizard_directory_dark.png")
    }

    // --- Wizard: Permission slide ---

    @Test
    fun wizard_permission_light() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PermissionSlide(LocalContext.current, isGranted = false)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/wizard_permission_light.png")
    }

    @Test
    fun wizard_permission_dark() {
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                Surface {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PermissionSlide(LocalContext.current, isGranted = false)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/wizard_permission_dark.png")
    }

    // --- Share receiver screen ---

    @Test
    @Config(qualifiers = "+w320dp-h780dp-xxhdpi")
    fun shareReceiverScreen_light() {
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "https://example.com",
                extraSubject = "Example Site",
                syncDirPath = "/tmp/sync",
            )
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = false, dynamicColor = false) {
                ShareReceiverScreen(viewModel = viewModel, onFinish = {}, onRedirectToWizard = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/share_receiver_light.png")
    }

    @Test
    @Config(qualifiers = "+w320dp-h780dp-xxhdpi")
    fun shareReceiverScreen_dark() {
        val viewModel =
            ShareReceiverViewModel(
                repository = fakeRepo,
                ioDispatcher = testDispatcher,
                extraText = "https://example.com",
                extraSubject = "Example Site",
                syncDirPath = "/tmp/sync",
            )
        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme(darkTheme = true, dynamicColor = false) {
                ShareReceiverScreen(viewModel = viewModel, onFinish = {}, onRedirectToWizard = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/share_receiver_dark.png")
    }
}
