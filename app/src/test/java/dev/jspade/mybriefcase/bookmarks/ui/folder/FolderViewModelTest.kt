package dev.jspade.mybriefcase.bookmarks.ui.folder

import app.cash.turbine.test
import dev.jspade.mybriefcase.bookmarks.data.BookmarkError
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import uniffi.mybriefcase_bookmarks_ffi.FfiException
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

@OptIn(ExperimentalCoroutinesApi::class)
class FolderViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

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
    fun `initial state is loading then content`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)

            viewModel.uiState.test {
                // Initial state
                val initial = awaitItem()
                assertTrue(initial.isLoading)

                // After loading nav tree and folder contents
                advanceUntilIdle()
                val loaded = expectMostRecentItem()
                assertEquals(false, loaded.isLoading)
                assertEquals("Bookmarks", loaded.folderTitle)
                assertEquals("root-id", loaded.currentFolderId)
                assertEquals(2, loaded.folders.size)
                assertTrue(loaded.bookmarks.isEmpty())
                assertNull(loaded.error)
            }
        }

    @Test
    fun `navigateToFolder updates content`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                // Skip initial
                awaitItem()

                viewModel.navigateToFolder("folder-1")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("folder-1", state.currentFolderId)
                assertEquals("Work", state.folderTitle)
                assertEquals(1, state.bookmarks.size)
                assertEquals("GitHub", state.bookmarks[0].title)
                assertTrue(state.folders.isEmpty())
            }
        }

    @Test
    fun `navigateToFolder updates breadcrumbs`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem()

                viewModel.navigateToFolder("folder-1")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(2, state.breadcrumbs.size)
                assertEquals("Bookmarks", state.breadcrumbs[0].title)
                assertEquals("Work", state.breadcrumbs[1].title)
            }
        }

    @Test
    fun `navigateUp goes to parent folder`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            // Navigate into a subfolder first
            viewModel.navigateToFolder("folder-1")
            advanceUntilIdle()

            val result = viewModel.navigateUp()
            advanceUntilIdle()

            assertTrue(result)
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("root-id", state.currentFolderId)
                assertEquals("Bookmarks", state.folderTitle)
            }
        }

    @Test
    fun `navigateUp returns false at root`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            val result = viewModel.navigateUp()

            assertFalse(result)
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("root-id", state.currentFolderId)
            }
        }

    @Test
    fun `empty folder shows no items`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem()

                viewModel.navigateToFolder("folder-2")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("Personal", state.folderTitle)
                assertTrue(state.folders.isEmpty())
                assertTrue(state.bookmarks.isEmpty())
            }
        }

    @Test
    fun `setSortOrder reloads with new sort`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem()

                viewModel.setSortOrder(SortOrder.DATE_DESC)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(SortOrder.DATE_DESC, state.sortOrder)
            }
        }

    @Test
    fun `error state on repository failure`() =
        runTest {
            fakeRepo.shouldThrow = RuntimeException("network error")
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals(BookmarkError.Internal("network error"), state.error)
                assertEquals(false, state.isLoading)
            }
        }

    @Test
    fun `NotFound error produces BookmarkError NotFound`() =
        runTest {
            fakeRepo.shouldThrow = FfiException.NotFound("folder not found: abc")
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertTrue(state.error is BookmarkError.NotFound)
                assertEquals("folder not found: abc", state.error?.message)
            }
        }

    @Test
    fun `InvalidInput error produces validationError`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            fakeRepo.createFolderThrow = FfiException.InvalidInput("title cannot be empty")
            viewModel.createFolder("")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("title cannot be empty", state.validationError)
                assertNull(state.error)
            }
        }

    @Test
    fun `clearValidationError clears validationError`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            fakeRepo.createFolderThrow = FfiException.InvalidInput("title cannot be empty")
            viewModel.createFolder("")
            advanceUntilIdle()

            viewModel.clearValidationError()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.validationError)
            }
        }

    @Test
    fun `renameFolder InvalidInput sets validationError`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            fakeRepo.renameFolderThrow = FfiException.InvalidInput("name too long")
            viewModel.renameFolder("folder-1", "x".repeat(300))
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("name too long", state.validationError)
                assertNull(state.error)
            }
        }

    @Test
    fun `clearError clears error`() =
        runTest {
            fakeRepo.shouldThrow = FfiException.IoException("disk full")
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.clearError()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.error)
            }
        }

    @Test
    fun `IoError produces BookmarkError IoError`() =
        runTest {
            fakeRepo.shouldThrow = FfiException.IoException("permission denied")
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertTrue(state.error is BookmarkError.IoError)
                assertEquals("permission denied", state.error?.message)
            }
        }

    @Test
    fun `NotInitialized error produces BookmarkError NotInitialized`() =
        runTest {
            fakeRepo.shouldThrow =
                FfiException.NotInitialized("repo not initialized: call init_repo first")
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertTrue(state.error is BookmarkError.NotInitialized)
            }
        }

    @Test
    fun `Internal error produces BookmarkError Internal`() =
        runTest {
            fakeRepo.shouldThrow = FfiException.Internal("document corrupted")
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertTrue(state.error is BookmarkError.Internal)
                assertEquals("document corrupted", state.error?.message)
            }
        }

    @Test
    fun `nav tree is populated`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                val navTree = state.navTree
                assertEquals("root-id", navTree?.rootFolderId)
                assertEquals(3, navTree?.folders?.size)
            }
        }

    @Test
    fun `createFolder calls repository and re-fetches`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            val callsBefore = fakeRepo.getFolderChildrenCallCount
            val navCallsBefore = fakeRepo.getNavTreeCallCount

            viewModel.createFolder("New Folder")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.error)
            }

            assertEquals(1, fakeRepo.createFolderCalls.size)
            assertEquals("root-id" to "New Folder", fakeRepo.createFolderCalls[0])
            // Should re-fetch folder contents and nav tree
            assertTrue(fakeRepo.getFolderChildrenCallCount > callsBefore)
            assertTrue(fakeRepo.getNavTreeCallCount > navCallsBefore)
        }

    @Test
    fun `renameFolder calls repository and re-fetches`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            val callsBefore = fakeRepo.getFolderChildrenCallCount
            val navCallsBefore = fakeRepo.getNavTreeCallCount

            viewModel.renameFolder("folder-1", "Renamed")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.error)
            }

            assertEquals(1, fakeRepo.renameFolderCalls.size)
            assertEquals("folder-1" to "Renamed", fakeRepo.renameFolderCalls[0])
            assertTrue(fakeRepo.getFolderChildrenCallCount > callsBefore)
            assertTrue(fakeRepo.getNavTreeCallCount > navCallsBefore)
        }

    @Test
    fun `deleteFolder calls repository and re-fetches`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            val callsBefore = fakeRepo.getFolderChildrenCallCount
            val navCallsBefore = fakeRepo.getNavTreeCallCount

            viewModel.deleteFolder("folder-1")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.error)
            }

            assertEquals(1, fakeRepo.deleteFolderCalls.size)
            assertEquals("folder-1", fakeRepo.deleteFolderCalls[0])
            assertTrue(fakeRepo.getFolderChildrenCallCount > callsBefore)
            assertTrue(fakeRepo.getNavTreeCallCount > navCallsBefore)
        }

    @Test
    fun `moveItem calls repository and re-fetches`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            val callsBefore = fakeRepo.getFolderChildrenCallCount
            val navCallsBefore = fakeRepo.getNavTreeCallCount

            viewModel.moveItem("bm-1", "folder-1", "folder-2")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.error)
            }

            assertEquals(1, fakeRepo.moveItemCalls.size)
            assertEquals(Triple("bm-1", "folder-1", "folder-2"), fakeRepo.moveItemCalls[0])
            assertTrue(fakeRepo.getFolderChildrenCallCount > callsBefore)
            assertTrue(fakeRepo.getNavTreeCallCount > navCallsBefore)
        }

    @Test
    fun `moveItem InvalidInput sets validationError`() =
        runTest {
            fakeRepo.moveItemThrow = FfiException.InvalidInput("cannot move into descendant")
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.moveItem("folder-1", "root-id", "folder-1")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("cannot move into descendant", state.validationError)
                assertNull(state.error)
            }
        }

    @Test
    fun `polling calls merge at interval`() =
        runTest {
            var mergeCallCount = 0
            fakeRepo.onMergeCalled = { mergeCallCount++ }
            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    pollIntervalMs = 1000L,
                    syncDirPath = null,
                )
            advanceUntilIdle()

            viewModel.startPolling()

            // Advance past first interval
            advanceTimeBy(1100)
            runCurrent()
            assertEquals(1, mergeCallCount)

            // Advance past second interval
            advanceTimeBy(1100)
            runCurrent()
            assertEquals(2, mergeCallCount)

            viewModel.stopPolling()
            advanceTimeBy(1100)
            runCurrent()
            // No more calls after stop
            assertEquals(2, mergeCallCount)
        }

    @Test
    fun `sync banner shown when marker file missing`() =
        runTest {
            val syncDir = tempFolder.newFolder("sync")
            // No .bookmarks-sync file exists
            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    syncDirPath = syncDir.absolutePath,
                )
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertTrue(state.showSyncBanner)
            }
        }

    @Test
    fun `sync banner hidden when marker file exists`() =
        runTest {
            val syncDir = tempFolder.newFolder("sync")
            java.io.File(syncDir, ".bookmarks-sync").writeText("")
            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    syncDirPath = syncDir.absolutePath,
                )
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.showSyncBanner)
            }
        }

    @Test
    fun `dismissSyncBanner hides the banner`() =
        runTest {
            val syncDir = tempFolder.newFolder("sync")
            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    syncDirPath = syncDir.absolutePath,
                )
            advanceUntilIdle()

            viewModel.dismissSyncBanner()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.showSyncBanner)
            }
        }

    @Test
    fun `refresh reloads when merge returns true`() =
        runTest {
            fakeRepo.mergeResult = true
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            val callsBefore = fakeRepo.getFolderChildrenCallCount
            val navCallsBefore = fakeRepo.getNavTreeCallCount

            viewModel.refresh()
            advanceUntilIdle()

            assertTrue(fakeRepo.getFolderChildrenCallCount > callsBefore)
            assertTrue(fakeRepo.getNavTreeCallCount > navCallsBefore)
        }

    @Test
    fun `refresh does not reload when merge returns false`() =
        runTest {
            fakeRepo.mergeResult = false
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            val callsBefore = fakeRepo.getFolderChildrenCallCount
            val navCallsBefore = fakeRepo.getNavTreeCallCount

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(callsBefore, fakeRepo.getFolderChildrenCallCount)
            assertEquals(navCallsBefore, fakeRepo.getNavTreeCallCount)
        }

    @Test
    fun `refresh sets error on failure`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            fakeRepo.mergeThrow = RuntimeException("sync failed")
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals(BookmarkError.Internal("sync failed"), state.error)
            }
        }

    @Test
    fun `toggleFolderExpanded adds and removes folder ids`() =
        runTest {
            val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher, syncDirPath = null)
            advanceUntilIdle()

            viewModel.toggleFolderExpanded("folder-1")

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertTrue("folder-1" in state.expandedFolderIds)
            }

            viewModel.toggleFolderExpanded("folder-1")

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertFalse("folder-1" in state.expandedFolderIds)
            }
        }
}
