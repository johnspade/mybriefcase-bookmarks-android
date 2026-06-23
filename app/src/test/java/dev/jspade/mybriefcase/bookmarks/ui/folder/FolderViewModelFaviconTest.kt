package dev.jspade.mybriefcase.bookmarks.ui.folder

import app.cash.turbine.test
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import dev.jspade.mybriefcase.bookmarks.data.FaviconFetcher
import dev.jspade.mybriefcase.bookmarks.data.FetchResult
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.FaviconFetchState
import kotlinx.coroutines.Dispatchers
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderViewModelFaviconTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeBookmarkRepository
    private lateinit var fakeFetcher: FakeFaviconFetcher

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeBookmarkRepository()
        fakeFetcher = FakeFaviconFetcher()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchFavicon transitions through Loading to Success`() =
        runTest {
            fakeFetcher.result = FetchResult.Success("abc123.png")
            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    syncDirPath = "/sync",
                    faviconFetcher = fakeFetcher,
                )
            advanceUntilIdle()

            viewModel.uiState.test {
                val initial = awaitItem()
                assertEquals(FaviconFetchState.Idle, initial.faviconFetchState)

                viewModel.fetchFavicon("https://example.com")
                val loading = awaitItem()
                assertEquals(FaviconFetchState.Loading, loading.faviconFetchState)

                advanceUntilIdle()
                val success = expectMostRecentItem()
                assertTrue(success.faviconFetchState is FaviconFetchState.Success)
                assertEquals("abc123.png", (success.faviconFetchState as FaviconFetchState.Success).filename)
            }
        }

    @Test
    fun `fetchFavicon transitions to Error on failure`() =
        runTest {
            fakeFetcher.result = FetchResult.Failed("Network error")
            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    syncDirPath = "/sync",
                    faviconFetcher = fakeFetcher,
                )
            advanceUntilIdle()

            viewModel.fetchFavicon("https://example.com")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.faviconFetchState is FaviconFetchState.Error)
            assertEquals("Network error", (state.faviconFetchState as FaviconFetchState.Error).message)
        }

    @Test
    fun `clearFaviconFetchState resets to Idle`() =
        runTest {
            fakeFetcher.result = FetchResult.Success("abc.png")
            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    syncDirPath = "/sync",
                    faviconFetcher = fakeFetcher,
                )
            advanceUntilIdle()

            viewModel.fetchFavicon("https://example.com")
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.faviconFetchState is FaviconFetchState.Success)

            viewModel.clearFaviconFetchState()
            assertEquals(FaviconFetchState.Idle, viewModel.uiState.value.faviconFetchState)
        }

    @Test
    fun `saveFavicon calls repository setFavicon`() =
        runTest {
            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    syncDirPath = "/sync",
                    faviconFetcher = fakeFetcher,
                )
            advanceUntilIdle()

            viewModel.saveFavicon("bm-1", "abc123.png")
            advanceUntilIdle()

            assertEquals(1, fakeRepo.setFaviconCalls.size)
            assertEquals("bm-1" to "abc123.png", fakeRepo.setFaviconCalls[0])
        }

    @Test
    fun `deleteFavicon clears favicon and preserves selectedBookmark`() =
        runTest {
            val bookmark =
                BookmarkDto(
                    id = "bm-1",
                    url = "https://example.com",
                    title = "Example",
                    notes = "",
                    favicon = "icon.png",
                    createdAt = "2024-06-01T12:00:00Z",
                    updatedAt = "2024-06-01T12:00:00Z",
                )
            fakeRepo.bookmarks["bm-1"] = bookmark

            val viewModel =
                FolderViewModel(
                    repository = fakeRepo,
                    ioDispatcher = testDispatcher,
                    syncDirPath = "/sync",
                    faviconFetcher = fakeFetcher,
                )
            advanceUntilIdle()

            viewModel.loadBookmarkDetail("bm-1")
            advanceUntilIdle()
            assertEquals("icon.png", viewModel.uiState.value.selectedBookmark?.favicon)

            viewModel.deleteFavicon("bm-1")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(FaviconFetchState.Idle, state.faviconFetchState)
            assertEquals("bm-1", state.selectedBookmark?.id)
            assertEquals(null, state.selectedBookmark?.favicon)
        }
}

class FakeFaviconFetcher : FaviconFetcher {
    var result: FetchResult = FetchResult.Failed("not configured")

    override suspend fun fetch(
        url: String,
        syncRoot: String,
    ): FetchResult = result
}
