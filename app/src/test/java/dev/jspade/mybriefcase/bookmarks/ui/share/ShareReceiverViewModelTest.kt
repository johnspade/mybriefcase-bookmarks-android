package dev.jspade.mybriefcase.bookmarks.ui.share

import app.cash.turbine.test
import dev.jspade.mybriefcase.bookmarks.data.FakeBookmarkRepository
import dev.jspade.mybriefcase.bookmarks.data.FaviconFetcher
import dev.jspade.mybriefcase.bookmarks.data.FetchResult
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.FaviconFetchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareReceiverViewModelTest {
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
    fun `extracts URL from text containing a URL`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "Check this out https://example.com/page?q=1 pretty cool",
                )
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("https://example.com/page?q=1", state.url)
            }
        }

    @Test
    fun `uses raw text as URL when no URL pattern found`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "just some random text",
                )
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("just some random text", state.url)
            }
        }

    @Test
    fun `populates title from EXTRA_SUBJECT`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    extraSubject = "Example Page Title",
                )
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("Example Page Title", state.title)
                assertEquals("https://example.com", state.url)
            }
        }

    @Test
    fun `title is empty when no subject provided`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    extraSubject = null,
                )
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("", state.title)
            }
        }

    @Test
    fun `save with blank URL shows validation error`() =
        runTest {
            val viewModel = createViewModel(extraText = null)
            advanceUntilIdle()

            viewModel.updateUrl("")
            viewModel.save()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("URL is required", state.urlError)
            }
        }

    @Test
    fun `save with invalid URL shows validation error`() =
        runTest {
            val viewModel = createViewModel(extraText = "not a url")
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("Invalid URL", state.urlError)
            }
        }

    @Test
    fun `save with valid URL calls repository addBookmark`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    extraSubject = "Example",
                )
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            assertEquals(1, fakeRepo.addBookmarkCalls.size)
            val call = fakeRepo.addBookmarkCalls[0]
            assertEquals("root-id", call.first)
            assertEquals("https://example.com", call.second)
            assertEquals("Example", call.third)
        }

    @Test
    fun `save sets isSaved to true on success`() =
        runTest {
            val viewModel = createViewModel(extraText = "https://example.com")
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals(true, state.isSaved)
            }
        }

    @Test
    fun `save uses selected folder when user picks one`() =
        runTest {
            val viewModel = createViewModel(extraText = "https://example.com")
            advanceUntilIdle()

            viewModel.selectFolder("folder-1")
            viewModel.save()
            advanceUntilIdle()

            assertEquals("folder-1", fakeRepo.addBookmarkCalls[0].first)
        }

    @Test
    fun `repository error sets error state`() =
        runTest {
            val viewModel = createViewModel(extraText = "https://example.com")
            advanceUntilIdle()

            fakeRepo.addBookmarkThrow = RuntimeException("disk full")
            viewModel.save()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals("disk full", state.error)
                assertEquals(false, state.isSaved)
            }
        }

    @Test
    fun `not initialized sets isInitialized to false`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    isAppInitialized = false,
                )
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals(false, state.isInitialized)
            }
        }

    @Test
    fun `save uses URL as title when title is blank`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    extraSubject = null,
                )
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            val call = fakeRepo.addBookmarkCalls[0]
            assertEquals("https://example.com", call.third)
        }

    @Test
    fun `clearing url error on text change`() =
        runTest {
            val viewModel = createViewModel(extraText = null)
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            viewModel.updateUrl("https://fixed.com")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.urlError)
                assertEquals("https://fixed.com", state.url)
            }
        }

    @Test
    fun `fetchFavicon sets loading then success state`() =
        runTest {
            val fakeFetcher = FakeFaviconFetcher()
            fakeFetcher.result = FetchResult.Success("abc123.ico")
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    syncDirPath = "/tmp/sync",
                    faviconFetcher = fakeFetcher,
                )
            advanceUntilIdle()

            viewModel.fetchFavicon("https://example.com")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertTrue(state.faviconFetchState is FaviconFetchState.Success)
                assertEquals(
                    "abc123.ico",
                    (state.faviconFetchState as FaviconFetchState.Success).filename,
                )
            }
        }

    @Test
    fun `fetchFavicon sets error state on failure`() =
        runTest {
            val fakeFetcher = FakeFaviconFetcher()
            fakeFetcher.result = FetchResult.Failed("network error")
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    syncDirPath = "/tmp/sync",
                    faviconFetcher = fakeFetcher,
                )
            advanceUntilIdle()

            viewModel.fetchFavicon("https://example.com")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertTrue(state.faviconFetchState is FaviconFetchState.Error)
                assertEquals(
                    "network error",
                    (state.faviconFetchState as FaviconFetchState.Error).message,
                )
            }
        }

    @Test
    fun `save attaches favicon when fetch succeeded`() =
        runTest {
            val fakeFetcher = FakeFaviconFetcher()
            fakeFetcher.result = FetchResult.Success("favicon.png")
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    extraSubject = "Example",
                    syncDirPath = "/tmp/sync",
                    faviconFetcher = fakeFetcher,
                )
            advanceUntilIdle()

            viewModel.fetchFavicon("https://example.com")
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            assertEquals(1, fakeRepo.setFaviconCalls.size)
            assertEquals("favicon.png", fakeRepo.setFaviconCalls[0].second)
        }

    @Test
    fun `save does not call setFavicon when no favicon fetched`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    extraSubject = "Example",
                )
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            assertEquals(0, fakeRepo.setFaviconCalls.size)
        }

    @Test
    fun `isFaviconFetchEnabled is false when syncDirPath is null`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    syncDirPath = null,
                    faviconFetchEnabled = true,
                )
            advanceUntilIdle()

            assertEquals(false, viewModel.isFaviconFetchEnabled)
        }

    @Test
    fun `isFaviconFetchEnabled is false when setting disabled`() =
        runTest {
            val viewModel =
                createViewModel(
                    extraText = "https://example.com",
                    syncDirPath = "/tmp/sync",
                    faviconFetchEnabled = false,
                )
            advanceUntilIdle()

            assertEquals(false, viewModel.isFaviconFetchEnabled)
        }

    private fun createViewModel(
        extraText: String? = null,
        extraSubject: String? = null,
        isAppInitialized: Boolean = true,
        syncDirPath: String? = null,
        faviconFetchEnabled: Boolean = true,
        faviconFetcher: FaviconFetcher? = null,
    ): ShareReceiverViewModel =
        ShareReceiverViewModel(
            repository = fakeRepo,
            ioDispatcher = testDispatcher,
            extraText = extraText,
            extraSubject = extraSubject,
            isAppInitialized = isAppInitialized,
            syncDirPath = syncDirPath,
            faviconFetchEnabled = faviconFetchEnabled,
            faviconFetcher = faviconFetcher,
            faviconFetcherFactory = null,
        )
}

private class FakeFaviconFetcher : FaviconFetcher {
    var result: FetchResult = FetchResult.Failed("not configured")

    override suspend fun fetch(
        url: String,
        syncRoot: String,
    ): FetchResult = result
}
