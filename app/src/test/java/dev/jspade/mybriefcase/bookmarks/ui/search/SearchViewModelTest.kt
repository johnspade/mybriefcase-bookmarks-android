package dev.jspade.mybriefcase.bookmarks.ui.search

import app.cash.turbine.test
import dev.jspade.mybriefcase.bookmarks.data.BookmarkError
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.FfiException
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
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
    fun `initial state has empty results`() =
        runTest {
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                val initial = awaitItem()
                assertTrue(initial.isEmpty())
            }
        }

    @Test
    fun `typing emits debounced search results`() =
        runTest {
            fakeRepo.searchResults =
                listOf(
                    BookmarkDto(
                        id = "bm-1",
                        url = "https://example.com",
                        title = "Example",
                        notes = "",
                        createdAt = "2024-01-01T00:00:00Z",
                        updatedAt = "2024-01-01T00:00:00Z",
                    ),
                )
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                // Initial empty
                assertEquals(emptyList<BookmarkDto>(), awaitItem())

                viewModel.setQuery("example")
                // Advance past debounce
                advanceTimeBy(350)
                advanceUntilIdle()

                val results = awaitItem()
                assertEquals(1, results.size)
                assertEquals("Example", results[0].title)
            }
        }

    @Test
    fun `clearing query returns empty results`() =
        runTest {
            fakeRepo.searchResults =
                listOf(
                    BookmarkDto(
                        id = "bm-1",
                        url = "https://example.com",
                        title = "Example",
                        notes = "",
                        createdAt = "2024-01-01T00:00:00Z",
                        updatedAt = "2024-01-01T00:00:00Z",
                    ),
                )
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                // Initial empty
                awaitItem()

                viewModel.setQuery("example")
                advanceTimeBy(350)
                advanceUntilIdle()
                awaitItem() // results

                viewModel.clearSearch()
                advanceTimeBy(350)
                advanceUntilIdle()

                val cleared = awaitItem()
                assertTrue(cleared.isEmpty())
            }
        }

    @Test
    fun `changing sort order re-fetches with new order`() =
        runTest {
            fakeRepo.searchResults =
                listOf(
                    BookmarkDto(
                        id = "bm-1",
                        url = "https://a.com",
                        title = "A",
                        notes = "",
                        createdAt = "2024-01-01T00:00:00Z",
                        updatedAt = "2024-01-01T00:00:00Z",
                    ),
                )
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                awaitItem() // initial empty

                viewModel.setQuery("test")
                advanceTimeBy(350)
                advanceUntilIdle()
                awaitItem() // first results

                // Change sort — should re-fetch
                fakeRepo.searchResults =
                    listOf(
                        BookmarkDto(
                            id = "bm-2",
                            url = "https://b.com",
                            title = "B",
                            notes = "",
                            createdAt = "2024-01-02T00:00:00Z",
                            updatedAt = "2024-01-02T00:00:00Z",
                        ),
                    )
                viewModel.setSortOrder(SortOrder.NAME_DESC)
                advanceTimeBy(350)
                advanceUntilIdle()

                val reordered = awaitItem()
                assertEquals(1, reordered.size)
                assertEquals("B", reordered[0].title)
                assertEquals(SortOrder.NAME_DESC, viewModel.sortOrder.value)
            }
        }

    @Test
    fun `debounce prevents rapid-fire calls`() =
        runTest {
            var callCount = 0
            fakeRepo.searchResults =
                listOf(
                    BookmarkDto(
                        id = "bm-1",
                        url = "https://example.com",
                        title = "Result",
                        notes = "",
                        createdAt = "2024-01-01T00:00:00Z",
                        updatedAt = "2024-01-01T00:00:00Z",
                    ),
                )
            fakeRepo.onSearchCalled = { callCount++ }
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                awaitItem() // initial

                // Type rapidly
                viewModel.setQuery("a")
                advanceTimeBy(100)
                viewModel.setQuery("ab")
                advanceTimeBy(100)
                viewModel.setQuery("abc")
                advanceTimeBy(350)
                advanceUntilIdle()

                awaitItem()
                // Only the final query should have triggered a search
                assertEquals(1, callCount)
                assertEquals("abc", fakeRepo.lastSearchQuery)
            }
        }

    @Test
    fun `NotFound error produces BookmarkError NotFound`() =
        runTest {
            fakeRepo.searchThrow = FfiException.NotFound("item not found")
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            // Subscribe to searchResults to start the lazy flow
            viewModel.searchResults.test {
                awaitItem() // initial empty

                viewModel.setQuery("test")
                advanceTimeBy(350)
                advanceUntilIdle()

                val err = viewModel.error.value
                assertTrue(err is BookmarkError.NotFound)
                assertEquals("item not found", err?.message)
            }
        }

    @Test
    fun `InvalidInput error produces BookmarkError InvalidInput`() =
        runTest {
            fakeRepo.searchThrow = FfiException.InvalidInput("query too long")
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                awaitItem()

                viewModel.setQuery("test")
                advanceTimeBy(350)
                advanceUntilIdle()

                val err = viewModel.error.value
                assertTrue(err is BookmarkError.InvalidInput)
                assertEquals("query too long", err?.message)
            }
        }

    @Test
    fun `IoError produces BookmarkError IoError`() =
        runTest {
            fakeRepo.searchThrow = FfiException.IoException("disk full")
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                awaitItem()

                viewModel.setQuery("test")
                advanceTimeBy(350)
                advanceUntilIdle()

                val err = viewModel.error.value
                assertTrue(err is BookmarkError.IoError)
                assertEquals("disk full", err?.message)
            }
        }

    @Test
    fun `NotInitialized error produces BookmarkError NotInitialized`() =
        runTest {
            fakeRepo.searchThrow = FfiException.NotInitialized("not initialized")
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                awaitItem()

                viewModel.setQuery("test")
                advanceTimeBy(350)
                advanceUntilIdle()

                val err = viewModel.error.value
                assertTrue(err is BookmarkError.NotInitialized)
            }
        }

    @Test
    fun `Internal error produces BookmarkError Internal`() =
        runTest {
            fakeRepo.searchThrow = FfiException.Internal("automerge corruption")
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                awaitItem()

                viewModel.setQuery("test")
                advanceTimeBy(350)
                advanceUntilIdle()

                val err = viewModel.error.value
                assertTrue(err is BookmarkError.Internal)
                assertEquals("automerge corruption", err?.message)
            }
        }

    @Test
    fun `error clears on successful search`() =
        runTest {
            fakeRepo.searchThrow = FfiException.IoException("disk full")
            val viewModel = SearchViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.searchResults.test {
                awaitItem()

                viewModel.setQuery("test")
                advanceTimeBy(350)
                advanceUntilIdle()
                // Error is now set
                assertTrue(viewModel.error.value is BookmarkError.IoError)

                // Fix the error and search again
                fakeRepo.searchThrow = null
                fakeRepo.searchResults =
                    listOf(
                        BookmarkDto(
                            id = "bm-1",
                            url = "https://example.com",
                            title = "Example",
                            notes = "",
                            createdAt = "2024-01-01T00:00:00Z",
                            updatedAt = "2024-01-01T00:00:00Z",
                        ),
                    )
                viewModel.setQuery("test2")
                advanceTimeBy(350)
                advanceUntilIdle()
                awaitItem() // successful results

                assertNull(viewModel.error.value)
            }
        }
}
