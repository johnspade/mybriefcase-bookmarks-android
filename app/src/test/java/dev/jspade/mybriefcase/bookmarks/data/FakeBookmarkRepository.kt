package dev.jspade.mybriefcase.bookmarks.data

import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.BookmarkHistoryEntryDto
import uniffi.mybriefcase_bookmarks_ffi.BookmarkItemDto
import uniffi.mybriefcase_bookmarks_ffi.BreadcrumbDto
import uniffi.mybriefcase_bookmarks_ffi.FolderChildrenDto
import uniffi.mybriefcase_bookmarks_ffi.FolderItemDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto
import uniffi.mybriefcase_bookmarks_ffi.ImportResultDto
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

/**
 * Fake repository for unit testing ViewModels without Rust FFI.
 */
class FakeBookmarkRepository : BookmarkRepository {
    var navTree =
        FolderNavTreeDto(
            rootFolderId = "root-id",
            folders =
                listOf(
                    FolderNavDto(
                        id = "root-id",
                        title = "Bookmarks",
                        itemCount = 2u,
                        childFolderIds = listOf("folder-1", "folder-2"),
                    ),
                    FolderNavDto(
                        id = "folder-1",
                        title = "Work",
                        itemCount = 1u,
                        childFolderIds = emptyList(),
                    ),
                    FolderNavDto(
                        id = "folder-2",
                        title = "Personal",
                        itemCount = 0u,
                        childFolderIds = emptyList(),
                    ),
                ),
        )

    var folderChildren =
        mutableMapOf(
            "root-id" to
                FolderChildrenDto(
                    folderTitle = "Bookmarks",
                    breadcrumbs = listOf(BreadcrumbDto(id = "root-id", title = "Bookmarks")),
                    folders =
                        listOf(
                            FolderItemDto(id = "folder-1", title = "Work", itemCount = 1u),
                            FolderItemDto(id = "folder-2", title = "Personal", itemCount = 0u),
                        ),
                    bookmarks = emptyList(),
                ),
            "folder-1" to
                FolderChildrenDto(
                    folderTitle = "Work",
                    breadcrumbs =
                        listOf(
                            BreadcrumbDto(id = "root-id", title = "Bookmarks"),
                            BreadcrumbDto(id = "folder-1", title = "Work"),
                        ),
                    folders = emptyList(),
                    bookmarks =
                        listOf(
                            BookmarkItemDto(
                                id = "bm-1",
                                title = "GitHub",
                                url = "https://github.com",
                                favicon = null,
                                createdAt = "2024-01-01T00:00:00Z",
                            ),
                        ),
                ),
            "folder-2" to
                FolderChildrenDto(
                    folderTitle = "Personal",
                    breadcrumbs =
                        listOf(
                            BreadcrumbDto(id = "root-id", title = "Bookmarks"),
                            BreadcrumbDto(id = "folder-2", title = "Personal"),
                        ),
                    folders = emptyList(),
                    bookmarks = emptyList(),
                ),
        )

    var bookmarks =
        mutableMapOf(
            "bm-1" to
                BookmarkDto(
                    id = "bm-1",
                    url = "https://github.com",
                    title = "GitHub",
                    notes = "A code hosting platform",
                    favicon = null,
                    createdAt = "2024-01-01T00:00:00Z",
                    updatedAt = "2024-01-01T00:00:00Z",
                ),
        )

    var shouldThrow: Exception? = null
    var moveItemThrow: Exception? = null
    var mergeResult = false
    var nextBookmarkId = "new-bm-id"
    var importResult = ImportResultDto(bookmarksImported = 0u, foldersImported = 0u)
    var exportResult = ""
    var addBookmarkCalls = mutableListOf<Triple<String, String, String>>()
    var updateBookmarkCalls = mutableListOf<List<String?>>()
    var deleteBookmarkCalls = mutableListOf<String>()
    var importHtmlCalls = mutableListOf<Pair<String, String>>()

    // Tracking calls
    var createFolderCalls = mutableListOf<Pair<String, String>>() // (parentId, title)
    var renameFolderCalls = mutableListOf<Pair<String, String>>() // (folderId, title)
    var deleteFolderCalls = mutableListOf<String>() // folderId
    var moveItemCalls = mutableListOf<Triple<String, String, String>>() // (itemId, from, to)
    var getFolderChildrenCallCount = 0
    var getNavTreeCallCount = 0

    // History
    var historyEntries: List<BookmarkHistoryEntryDto> = emptyList()
    var revertBookmarkCalls = mutableListOf<Pair<String, String>>()

    // Search/sync callbacks (from PR #11)
    var searchResults: List<BookmarkDto> = emptyList()
    var lastSearchQuery: String? = null
    var onSearchCalled: (() -> Unit)? = null
    var onMergeCalled: (() -> Unit)? = null

    override fun initRepo(
        dataDir: String,
        syncDir: String,
        clientId: String,
    ) {
        // no-op
    }

    override fun shutdown() {
        // no-op
    }

    @Suppress("TooGenericExceptionThrown")
    override suspend fun getFolderChildren(
        folderId: String,
        sortBy: SortOrder,
    ): FolderChildrenDto {
        getFolderChildrenCallCount++
        shouldThrow?.let { throw it }
        return folderChildren[folderId]
            ?: throw RuntimeException("folder not found: $folderId")
    }

    override suspend fun getFolderNavTree(): FolderNavTreeDto {
        getNavTreeCallCount++
        shouldThrow?.let { throw it }
        return navTree
    }

    override suspend fun getBookmark(bookmarkId: String): BookmarkDto? {
        shouldThrow?.let { throw it }
        return bookmarks[bookmarkId]
    }

    override suspend fun addBookmark(
        folderId: String,
        url: String,
        title: String,
    ): String {
        addBookmarkThrow?.let { throw it }
        shouldThrow?.let { throw it }
        addBookmarkCalls.add(Triple(folderId, url, title))
        val id = nextBookmarkId
        bookmarks[id] =
            BookmarkDto(
                id = id,
                url = url,
                title = title,
                notes = "",
                favicon = null,
                createdAt = "2024-06-01T12:00:00Z",
                updatedAt = "2024-06-01T12:00:00Z",
            )
        // Add to folder children
        val existing = folderChildren[folderId]
        if (existing != null) {
            folderChildren[folderId] =
                existing.copy(
                    bookmarks =
                        existing.bookmarks +
                            BookmarkItemDto(
                                id = id,
                                title = title,
                                url = url,
                                favicon = null,
                                createdAt = "2024-06-01T12:00:00Z",
                            ),
                )
        }
        return id
    }

    override suspend fun updateBookmark(
        bookmarkId: String,
        url: String?,
        title: String?,
        notes: String?,
    ) {
        updateBookmarkThrow?.let { throw it }
        shouldThrow?.let { throw it }
        updateBookmarkCalls.add(listOf(bookmarkId, url, title, notes))
        val existing = bookmarks[bookmarkId] ?: return
        bookmarks[bookmarkId] =
            existing.copy(
                url = url ?: existing.url,
                title = title ?: existing.title,
                notes = notes ?: existing.notes,
                updatedAt = "2024-06-02T12:00:00Z",
            )
    }

    override suspend fun deleteBookmark(bookmarkId: String) {
        deleteBookmarkThrow?.let { throw it }
        shouldThrow?.let { throw it }
        deleteBookmarkCalls.add(bookmarkId)
        bookmarks.remove(bookmarkId)
        // Remove from folder children
        for ((folderId, children) in folderChildren) {
            val filtered = children.bookmarks.filter { it.id != bookmarkId }
            if (filtered.size != children.bookmarks.size) {
                folderChildren[folderId] = children.copy(bookmarks = filtered)
            }
        }
    }

    var addBookmarkThrow: Exception? = null
    var updateBookmarkThrow: Exception? = null
    var deleteBookmarkThrow: Exception? = null
    var createFolderThrow: Exception? = null
    var renameFolderThrow: Exception? = null

    override suspend fun createFolder(
        parentFolderId: String,
        title: String,
    ): String {
        createFolderThrow?.let { throw it }
        shouldThrow?.let { throw it }
        createFolderCalls.add(parentFolderId to title)
        return "new-folder-id"
    }

    override suspend fun renameFolder(
        folderId: String,
        title: String,
    ) {
        renameFolderThrow?.let { throw it }
        shouldThrow?.let { throw it }
        renameFolderCalls.add(folderId to title)
    }

    override suspend fun deleteFolder(folderId: String) {
        shouldThrow?.let { throw it }
        deleteFolderCalls.add(folderId)
    }

    override suspend fun moveItem(
        itemId: String,
        fromFolderId: String,
        toFolderId: String,
    ) {
        moveItemThrow?.let { throw it }
        moveItemCalls.add(Triple(itemId, fromFolderId, toFolderId))
    }

    var searchThrow: Exception? = null

    override suspend fun searchBookmarks(
        query: String,
        sortBy: SortOrder,
    ): List<BookmarkDto> {
        searchThrow?.let { throw it }
        lastSearchQuery = query
        onSearchCalled?.invoke()
        return searchResults
    }

    override suspend fun importHtml(
        folderId: String,
        html: String,
    ): ImportResultDto {
        shouldThrow?.let { throw it }
        importHtmlCalls.add(Pair(folderId, html))
        return importResult
    }

    override suspend fun exportHtml(): String {
        shouldThrow?.let { throw it }
        return exportResult
    }

    override suspend fun getBookmarkHistory(bookmarkId: String): List<BookmarkHistoryEntryDto> {
        shouldThrow?.let { throw it }
        return historyEntries
    }

    override suspend fun revertBookmark(
        bookmarkId: String,
        changeHash: String,
    ) {
        shouldThrow?.let { throw it }
        revertBookmarkCalls.add(bookmarkId to changeHash)
    }

    var mergeThrow: Exception? = null

    override suspend fun triggerFullMerge(): Boolean {
        mergeThrow?.let { throw it }
        onMergeCalled?.invoke()
        return mergeResult
    }
}
