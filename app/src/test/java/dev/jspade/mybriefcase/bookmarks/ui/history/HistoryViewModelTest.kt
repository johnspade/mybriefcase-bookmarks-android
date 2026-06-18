package dev.jspade.mybriefcase.bookmarks.ui.history

import app.cash.turbine.test
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uniffi.mybriefcase_bookmarks_ffi.BookmarkHistoryEntryDto
import uniffi.mybriefcase_bookmarks_ffi.FieldChangeDto

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
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
    fun `loadHistory populates entries`() =
        runTest {
            fakeRepo.historyEntries =
                listOf(
                    BookmarkHistoryEntryDto(
                        changeHash = "a".repeat(64),
                        timestamp = 1700000000000L,
                        actor = "actor1",
                        changedFields =
                            listOf(
                                FieldChangeDto(
                                    field = "url",
                                    oldValue = "https://old.com",
                                    newValue = "https://new.com",
                                ),
                            ),
                    ),
                    BookmarkHistoryEntryDto(
                        changeHash = "b".repeat(64),
                        timestamp = 1699000000000L,
                        actor = "actor1",
                        changedFields =
                            listOf(
                                FieldChangeDto(field = "title", oldValue = null, newValue = "First Title"),
                                FieldChangeDto(field = "url", oldValue = null, newValue = "https://old.com"),
                            ),
                    ),
                )

            val viewModel = HistoryViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.uiState.test {
                awaitItem() // initial empty state

                viewModel.loadHistory("bm-1")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals(2, state.entries.size)
                assertEquals("a".repeat(64), state.entries[0].changeHash)
                assertEquals(1, state.entries[0].changedFields.size)
                assertEquals("url", state.entries[0].changedFields[0].field)
            }
        }

    @Test
    fun `revertBookmark calls repository and refreshes`() =
        runTest {
            fakeRepo.historyEntries =
                listOf(
                    BookmarkHistoryEntryDto(
                        changeHash = "c".repeat(64),
                        timestamp = 1700000000000L,
                        actor = "actor1",
                        changedFields =
                            listOf(
                                FieldChangeDto(field = "title", oldValue = "Old", newValue = "New"),
                            ),
                    ),
                )

            val viewModel = HistoryViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.loadHistory("bm-1")
            advanceUntilIdle()

            viewModel.revertBookmark("bm-1", "c".repeat(64))
            advanceUntilIdle()

            assertEquals(1, fakeRepo.revertBookmarkCalls.size)
            assertEquals("bm-1" to "c".repeat(64), fakeRepo.revertBookmarkCalls[0])
        }

    @Test
    fun `revertBookmark sets reverted flag`() =
        runTest {
            fakeRepo.historyEntries = emptyList()

            val viewModel = HistoryViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.uiState.test {
                awaitItem()

                viewModel.revertBookmark("bm-1", "d".repeat(64))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.reverted)
            }
        }

    @Test
    fun `error state on failure`() =
        runTest {
            fakeRepo.shouldThrow = RuntimeException("network error")

            val viewModel = HistoryViewModel(repository = fakeRepo, ioDispatcher = testDispatcher)

            viewModel.uiState.test {
                awaitItem()

                viewModel.loadHistory("bm-1")
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals("network error", state.error)
            }
        }
}
