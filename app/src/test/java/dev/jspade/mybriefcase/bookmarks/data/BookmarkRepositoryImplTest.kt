package dev.jspade.mybriefcase.bookmarks.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.BookmarkHistoryEntryDto
import uniffi.mybriefcase_bookmarks_ffi.BookmarkItemDto
import uniffi.mybriefcase_bookmarks_ffi.BreadcrumbDto
import uniffi.mybriefcase_bookmarks_ffi.FolderChildrenDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto
import uniffi.mybriefcase_bookmarks_ffi.ImportResultDto
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeFfi: RecordingFfi
    private lateinit var repo: BookmarkRepositoryImpl

    @Before
    fun setup() {
        fakeFfi = RecordingFfi()
        repo = BookmarkRepositoryImpl(ffi = fakeFfi, ioDispatcher = testDispatcher)
    }

    // --- triggerFullMerge return value ---

    @Test
    fun `triggerFullMerge returns true when FFI returns true`() =
        runTest(testDispatcher) {
            fakeFfi.mergeResult = true
            assertTrue(repo.triggerFullMerge())
        }

    @Test
    fun `triggerFullMerge returns false when FFI returns false`() =
        runTest(testDispatcher) {
            fakeFfi.mergeResult = false
            assertFalse(repo.triggerFullMerge())
        }

    // --- Error propagation ---

    @Test
    fun `getFolderChildren propagates FFI exception`() =
        runTest(testDispatcher) {
            fakeFfi.shouldThrow = RuntimeException("FFI error")
            try {
                repo.getFolderChildren("root", SortOrder.NAME_ASC)
                fail("Expected exception")
            } catch (e: RuntimeException) {
                assertEquals("FFI error", e.message)
            }
        }

    @Test
    fun `addBookmark propagates FFI exception`() =
        runTest(testDispatcher) {
            fakeFfi.shouldThrow = RuntimeException("add failed")
            try {
                repo.addBookmark("folder", "http://x.com", "X")
                fail("Expected exception")
            } catch (e: RuntimeException) {
                assertEquals("add failed", e.message)
            }
        }

    @Test
    fun `triggerFullMerge propagates FFI exception`() =
        runTest(testDispatcher) {
            fakeFfi.shouldThrow = RuntimeException("merge error")
            try {
                repo.triggerFullMerge()
                fail("Expected exception")
            } catch (e: RuntimeException) {
                assertEquals("merge error", e.message)
            }
        }

    @Test
    fun `deleteBookmark propagates FFI exception`() =
        runTest(testDispatcher) {
            fakeFfi.shouldThrow = RuntimeException("delete failed")
            try {
                repo.deleteBookmark("bm-1")
                fail("Expected exception")
            } catch (e: RuntimeException) {
                assertEquals("delete failed", e.message)
            }
        }

    @Test
    fun `searchBookmarks propagates FFI exception`() =
        runTest(testDispatcher) {
            fakeFfi.shouldThrow = RuntimeException("search error")
            try {
                repo.searchBookmarks("query", SortOrder.NAME_ASC)
                fail("Expected exception")
            } catch (e: RuntimeException) {
                assertEquals("search error", e.message)
            }
        }

    // --- FFI call pass-through ---

    @Test
    fun `getFolderChildren passes arguments to FFI`() =
        runTest(testDispatcher) {
            repo.getFolderChildren("folder-1", SortOrder.DATE_DESC)
            assertEquals("folder-1", fakeFfi.lastGetFolderChildrenArgs?.first)
            assertEquals(SortOrder.DATE_DESC, fakeFfi.lastGetFolderChildrenArgs?.second)
        }

    @Test
    fun `addBookmark passes arguments and returns ID`() =
        runTest(testDispatcher) {
            fakeFfi.nextBookmarkId = "new-id-123"
            val result = repo.addBookmark("f1", "http://example.com", "Example")
            assertEquals("new-id-123", result)
        }

    @Test
    fun `getBookmark returns null when FFI returns null`() =
        runTest(testDispatcher) {
            fakeFfi.bookmarkToReturn = null
            assertNull(repo.getBookmark("nonexistent"))
        }

    @Test
    fun `getBookmark returns bookmark when FFI returns one`() =
        runTest(testDispatcher) {
            val dto =
                BookmarkDto(
                    id = "bm-1",
                    url = "http://x.com",
                    title = "X",
                    notes = "",
                    favicon = null,
                    createdAt = "2024-01-01T00:00:00Z",
                    updatedAt = "2024-01-01T00:00:00Z",
                )
            fakeFfi.bookmarkToReturn = dto
            assertEquals(dto, repo.getBookmark("bm-1"))
        }

    @Test
    fun `importHtml returns result from FFI`() =
        runTest(testDispatcher) {
            fakeFfi.importResult = ImportResultDto(bookmarksImported = 5u, foldersImported = 2u)
            val result = repo.importHtml("root", "<html></html>")
            assertEquals(5u, result.bookmarksImported)
            assertEquals(2u, result.foldersImported)
        }

    @Test
    fun `exportHtml returns result from FFI`() =
        runTest(testDispatcher) {
            fakeFfi.exportResult = "<html>bookmarks</html>"
            assertEquals("<html>bookmarks</html>", repo.exportHtml())
        }

    // --- Lifecycle methods (non-suspend, direct delegation) ---

    @Test
    fun `initRepo delegates to FFI`() {
        repo.initRepo("/data", "/sync", "client-1")
        assertEquals(Triple("/data", "/sync", "client-1"), fakeFfi.lastInitRepoArgs)
    }

    @Test
    fun `shutdown delegates to FFI`() {
        repo.shutdown()
        assertTrue(fakeFfi.shutdownCalled)
    }
}

private class RecordingFfi : BookmarkFfi {
    var shouldThrow: Exception? = null
    var mergeResult = false
    var nextBookmarkId = "default-id"
    var bookmarkToReturn: BookmarkDto? = null
    var importResult = ImportResultDto(bookmarksImported = 0u, foldersImported = 0u)
    var exportResult = ""
    var lastGetFolderChildrenArgs: Pair<String, SortOrder>? = null
    var lastInitRepoArgs: Triple<String, String, String>? = null
    var shutdownCalled = false

    override fun initRepo(
        dataDir: String,
        syncDir: String,
        clientId: String,
    ) {
        lastInitRepoArgs = Triple(dataDir, syncDir, clientId)
    }

    override fun shutdown() {
        shutdownCalled = true
    }

    override fun getFolderChildren(
        folderId: String,
        sortBy: SortOrder,
    ): FolderChildrenDto {
        shouldThrow?.let { throw it }
        lastGetFolderChildrenArgs = folderId to sortBy
        return FolderChildrenDto(
            folderTitle = "Test",
            breadcrumbs = listOf(BreadcrumbDto(id = folderId, title = "Test")),
            folders = emptyList(),
            bookmarks =
                listOf(
                    BookmarkItemDto(
                        id = "bm-1",
                        title = "B1",
                        url = "http://b1.com",
                        favicon = null,
                        createdAt = "2024-01-01T00:00:00Z",
                    ),
                ),
        )
    }

    override fun getFolderNavTree(): FolderNavTreeDto {
        shouldThrow?.let { throw it }
        return FolderNavTreeDto(
            rootFolderId = "root",
            folders = listOf(FolderNavDto(id = "root", title = "Root", itemCount = 0u, childFolderIds = emptyList())),
        )
    }

    override fun getBookmark(bookmarkId: String): BookmarkDto? {
        shouldThrow?.let { throw it }
        return bookmarkToReturn
    }

    override fun addBookmark(
        folderId: String,
        url: String,
        title: String,
    ): String {
        shouldThrow?.let { throw it }
        return nextBookmarkId
    }

    override fun updateBookmark(
        bookmarkId: String,
        url: String?,
        title: String?,
        notes: String?,
    ) {
        shouldThrow?.let { throw it }
    }

    override fun deleteBookmark(bookmarkId: String) {
        shouldThrow?.let { throw it }
    }

    override fun createFolder(
        parentFolderId: String,
        title: String,
    ): String {
        shouldThrow?.let { throw it }
        return "new-folder-id"
    }

    override fun renameFolder(
        folderId: String,
        title: String,
    ) {
        shouldThrow?.let { throw it }
    }

    override fun deleteFolder(folderId: String) {
        shouldThrow?.let { throw it }
    }

    override fun moveItem(
        itemId: String,
        fromFolderId: String,
        toFolderId: String,
    ) {
        shouldThrow?.let { throw it }
    }

    override fun searchBookmarks(
        query: String,
        sortBy: SortOrder,
    ): List<BookmarkDto> {
        shouldThrow?.let { throw it }
        return emptyList()
    }

    override fun importHtml(
        folderId: String,
        html: String,
    ): ImportResultDto {
        shouldThrow?.let { throw it }
        return importResult
    }

    override fun exportHtml(): String {
        shouldThrow?.let { throw it }
        return exportResult
    }

    override fun getBookmarkHistory(bookmarkId: String): List<BookmarkHistoryEntryDto> {
        shouldThrow?.let { throw it }
        return emptyList()
    }

    override fun revertBookmark(
        bookmarkId: String,
        changeHash: String,
    ) {
        shouldThrow?.let { throw it }
    }

    override fun triggerFullMerge(): Boolean {
        shouldThrow?.let { throw it }
        return mergeResult
    }
}
