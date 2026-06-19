package dev.jspade.mybriefcase.bookmarks.ui.folder

import app.cash.turbine.test
import dev.jspade.mybriefcase.bookmarks.data.BookmarkError
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderViewModelBookmarkTest {
    private val testDispatcher = StandardTestDispatcher()
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
    fun `addBookmark triggers re-fetch with new item`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            // Navigate to folder-1 which has one bookmark
            viewModel.navigateToFolder("folder-1")
            advanceUntilIdle()

            viewModel.uiState.test {
                val before = awaitItem()
                assertEquals(1, before.bookmarks.size)

                viewModel.addBookmark("https://new.com", "New Site")
                advanceUntilIdle()

                val after = expectMostRecentItem()
                assertEquals(2, after.bookmarks.size)
                assertTrue(after.bookmarks.any { it.url == "https://new.com" })
            }
        }

    @Test
    fun `deleteBookmark triggers re-fetch without item`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.navigateToFolder("folder-1")
            advanceUntilIdle()

            viewModel.uiState.test {
                val before = awaitItem()
                assertEquals(1, before.bookmarks.size)

                viewModel.deleteBookmark("bm-1")
                advanceUntilIdle()

                val after = expectMostRecentItem()
                assertTrue(after.bookmarks.isEmpty())
            }
        }

    @Test
    fun `loadBookmarkDetail sets selectedBookmark`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem()

                viewModel.loadBookmarkDetail("bm-1")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertNotNull(state.selectedBookmark)
                assertEquals("bm-1", state.selectedBookmark!!.id)
                assertEquals("https://github.com", state.selectedBookmark!!.url)
                assertEquals("GitHub", state.selectedBookmark!!.title)
            }
        }

    @Test
    fun `loadBookmarkDetail with invalid id sets error`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem()

                viewModel.loadBookmarkDetail("nonexistent-id")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(BookmarkError.NotFound("Bookmark not found"), state.error)
            }
        }

    @Test
    fun `updateBookmark refreshes detail and folder contents`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.navigateToFolder("folder-1")
            advanceUntilIdle()

            viewModel.updateBookmark("bm-1", "https://updated.com", "Updated Title", "Some notes")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertNotNull(state.selectedBookmark)
                assertEquals("https://updated.com", state.selectedBookmark!!.url)
                assertEquals("Updated Title", state.selectedBookmark!!.title)
                assertEquals("Some notes", state.selectedBookmark!!.notes)
            }
        }

    @Test
    fun `importHtml updates state with ImportResult`() =
        runTest {
            fakeRepo.importResult =
                uniffi.mybriefcase_bookmarks_ffi.ImportResultDto(
                    bookmarksImported = 5u,
                    foldersImported = 2u,
                )
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem()

                viewModel.importHtml("<html><body>bookmarks</body></html>")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertNotNull(state.importResult)
                assertEquals(5u, state.importResult!!.bookmarksImported)
                assertEquals(2u, state.importResult!!.foldersImported)
            }
        }

    @Test
    fun `exportHtml produces non-empty string`() =
        runTest {
            fakeRepo.exportResult = "<html><body><h1>Bookmarks</h1></body></html>"
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem()

                viewModel.exportHtml()
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertNotNull(state.exportedHtml)
                assertTrue(state.exportedHtml!!.isNotEmpty())
            }
        }

    @Test
    fun `importHtml error state`() =
        runTest {
            fakeRepo.shouldThrow = RuntimeException("import failed")
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            // Clear the error from init that also throws
            fakeRepo.shouldThrow = null
            advanceUntilIdle()

            // Now set error for import
            fakeRepo.shouldThrow = RuntimeException("import failed")

            viewModel.uiState.test {
                awaitItem()

                viewModel.importHtml("<html></html>")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(BookmarkError.Internal("import failed"), state.error)
            }
        }

    @Test
    fun `exportHtml error state`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            fakeRepo.shouldThrow = RuntimeException("export failed")

            viewModel.uiState.test {
                awaitItem()

                viewModel.exportHtml()
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(BookmarkError.Internal("export failed"), state.error)
            }
        }

    @Test
    fun `updateBookmarkAndMove calls both updateBookmark and moveItem`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.navigateToFolder("folder-1")
            advanceUntilIdle()

            viewModel.updateBookmarkAndMove("bm-1", "https://updated.com", "Updated", "notes", "folder-2")
            advanceUntilIdle()

            assertEquals(1, fakeRepo.updateBookmarkCalls.size)
            assertEquals(listOf("bm-1", "https://updated.com", "Updated", "notes"), fakeRepo.updateBookmarkCalls[0])
            assertEquals(1, fakeRepo.moveItemCalls.size)
            assertEquals(Triple("bm-1", "folder-1", "folder-2"), fakeRepo.moveItemCalls[0])
        }

    @Test
    fun `updateBookmarkAndMove with null folderId does not move`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.navigateToFolder("folder-1")
            advanceUntilIdle()

            viewModel.updateBookmarkAndMove("bm-1", "https://updated.com", "Updated", "notes", null)
            advanceUntilIdle()

            assertEquals(1, fakeRepo.updateBookmarkCalls.size)
            assertTrue(fakeRepo.moveItemCalls.isEmpty())
        }

    @Test
    fun `clearSelectedBookmark clears selection`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.loadBookmarkDetail("bm-1")
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.selectedBookmark)

            viewModel.clearSelectedBookmark()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.selectedBookmark)
            }
        }

    @Test
    fun `clearImportResult clears import result`() =
        runTest {
            fakeRepo.importResult =
                uniffi.mybriefcase_bookmarks_ffi.ImportResultDto(
                    bookmarksImported = 3u,
                    foldersImported = 1u,
                )
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.importHtml("<html></html>")
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.importResult)

            viewModel.clearImportResult()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.importResult)
            }
        }

    @Test
    fun `clearExportedHtml clears exported html`() =
        runTest {
            fakeRepo.exportResult = "<html><body></body></html>"
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
            advanceUntilIdle()

            viewModel.exportHtml()
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.exportedHtml)

            viewModel.clearExportedHtml()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.exportedHtml)
            }
        }
}
