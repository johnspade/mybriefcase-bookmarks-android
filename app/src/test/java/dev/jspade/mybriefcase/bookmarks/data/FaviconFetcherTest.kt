package dev.jspade.mybriefcase.bookmarks.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FaviconFetcherTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var syncRoot: String

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        syncRoot = tempFolder.root.absolutePath
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `DuckDuckGo fetch success stores content-addressed file`() =
        runTest {
            val iconBytes = byteArrayOf(0, 0, 1, 0, 1, 0, 16, 16) // fake ico header
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(iconBytes))
                    .setHeader("Content-Type", "image/x-icon"),
            )

            val fetcher =
                FaviconFetcherImpl(
                    duckDuckGoBaseUrl = server.url("/ip3/").toString(),
                )

            val result = fetcher.fetch("https://example.com", syncRoot)

            assertTrue(result is FetchResult.Success)
            val filename = (result as FetchResult.Success).filename
            assertTrue(filename.endsWith(".ico"))

            val stored = File(syncRoot, "favicons/$filename")
            assertTrue(stored.exists())
            assertTrue(stored.readBytes().contentEquals(iconBytes))

            // Verify content-addressed: SHA-256 of the bytes
            val expectedHash =
                java.security.MessageDigest
                    .getInstance("SHA-256")
                    .digest(iconBytes)
                    .joinToString("") { "%02x".format(it) }
            assertEquals("$expectedHash.ico", filename)
        }

    @Test
    fun `DuckDuckGo fetch failure returns Failed`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404))

            val fetcher =
                FaviconFetcherImpl(
                    duckDuckGoBaseUrl = server.url("/ip3/").toString(),
                )

            val result = fetcher.fetch("https://example.com", syncRoot)

            assertTrue(result is FetchResult.Failed)
        }

    @Test
    fun `same content produces same filename (content-addressed dedup)`() =
        runTest {
            val iconBytes = byteArrayOf(1, 2, 3, 4, 5)
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(iconBytes))
                    .setHeader("Content-Type", "image/png"),
            )
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(iconBytes))
                    .setHeader("Content-Type", "image/png"),
            )

            val fetcher =
                FaviconFetcherImpl(
                    duckDuckGoBaseUrl = server.url("/ip3/").toString(),
                )

            val result1 = fetcher.fetch("https://example.com", syncRoot)
            val result2 = fetcher.fetch("https://other.com", syncRoot)

            assertTrue(result1 is FetchResult.Success)
            assertTrue(result2 is FetchResult.Success)
            assertEquals(
                (result1 as FetchResult.Success).filename,
                (result2 as FetchResult.Success).filename,
            )
        }

    @Test
    fun `offline behavior returns Failed`() =
        runTest {
            server.shutdown()

            val fetcher =
                FaviconFetcherImpl(
                    duckDuckGoBaseUrl = "http://localhost:1/ip3/",
                )

            val result = fetcher.fetch("https://example.com", syncRoot)

            assertTrue(result is FetchResult.Failed)
        }
}
