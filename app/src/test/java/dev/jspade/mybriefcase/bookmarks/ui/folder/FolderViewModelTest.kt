package dev.jspade.mybriefcase.bookmarks.ui.folder

import app.cash.turbine.test
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
    fun `initial state is loading then content`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

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
    fun `navigateToFolder updates content`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
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
    fun `navigateToFolder updates breadcrumbs`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
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
    fun `empty folder shows no items`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
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
    fun `setSortOrder reloads with new sort`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
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
    fun `error state on repository failure`() = runTest {
        fakeRepo.shouldThrow = RuntimeException("network error")
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("network error", state.error)
            assertEquals(false, state.isLoading)
        }
    }

    @Test
    fun `nav tree is populated`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            val navTree = state.navTree
            assertEquals("root-id", navTree?.rootFolderId)
            assertEquals(3, navTree?.folders?.size)
        }
    }

    @Test
    fun `createFolder calls repository and re-fetches`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
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
    fun `renameFolder calls repository and re-fetches`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
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
    fun `deleteFolder calls repository and re-fetches`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
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
    fun `moveItem calls repository and re-fetches`() = runTest {
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
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
    fun `moveItem error sets error state`() = runTest {
        fakeRepo.moveItemThrow = RuntimeException("cannot move into descendant")
        val viewModel = FolderViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.moveItem("folder-1", "root-id", "folder-1")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("cannot move into descendant", state.error)
        }
    }

    @Test
    fun `polling calls merge at interval`() = runTest {
        var mergeCallCount = 0
        fakeRepo.onMergeCalled = { mergeCallCount++ }
        val viewModel = FolderViewModel(
            repository = fakeRepo,
            ioDispatcher = testDispatcher,
            pollIntervalMs = 1000L,
        )
        advanceUntilIdle()

        viewModel.startPolling()

        // Advance past first interval
        advanceTimeBy(1100)
        advanceUntilIdle()
        assertEquals(1, mergeCallCount)

        // Advance past second interval
        advanceTimeBy(1100)
        advanceUntilIdle()
        assertEquals(2, mergeCallCount)

        viewModel.stopPolling()
        advanceTimeBy(1100)
        advanceUntilIdle()
        // No more calls after stop
        assertEquals(2, mergeCallCount)
    }

    @Test
    fun `sync banner shown when marker file missing`() = runTest {
        val syncDir = tempFolder.newFolder("sync")
        // No .bookmarks-sync file exists
        val viewModel = FolderViewModel(
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
    fun `sync banner hidden when marker file exists`() = runTest {
        val syncDir = tempFolder.newFolder("sync")
        java.io.File(syncDir, ".bookmarks-sync").writeText("")
        val viewModel = FolderViewModel(
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
    fun `dismissSyncBanner hides the banner`() = runTest {
        val syncDir = tempFolder.newFolder("sync")
        val viewModel = FolderViewModel(
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
}
