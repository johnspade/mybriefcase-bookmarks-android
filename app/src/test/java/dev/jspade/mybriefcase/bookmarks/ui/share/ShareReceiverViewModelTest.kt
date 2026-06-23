package dev.jspade.mybriefcase.bookmarks.ui.share

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
import org.junit.Assert.assertNull
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

    private fun createViewModel(
        extraText: String? = null,
        extraSubject: String? = null,
        isAppInitialized: Boolean = true,
    ): ShareReceiverViewModel =
        ShareReceiverViewModel(
            repository = fakeRepo,
            ioDispatcher = testDispatcher,
            extraText = extraText,
            extraSubject = extraSubject,
            isAppInitialized = isAppInitialized,
        )
}
