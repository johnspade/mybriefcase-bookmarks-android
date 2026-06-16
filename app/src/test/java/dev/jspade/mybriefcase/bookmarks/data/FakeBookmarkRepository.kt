package dev.jspade.mybriefcase.bookmarks.data

import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
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

    var navTree = FolderNavTreeDto(
        rootFolderId = "root-id",
        folders = listOf(
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

    var folderChildren = mutableMapOf(
        "root-id" to FolderChildrenDto(
            folderTitle = "Bookmarks",
            breadcrumbs = listOf(BreadcrumbDto(id = "root-id", title = "Bookmarks")),
            folders = listOf(
                FolderItemDto(id = "folder-1", title = "Work", itemCount = 1u),
                FolderItemDto(id = "folder-2", title = "Personal", itemCount = 0u),
            ),
            bookmarks = emptyList(),
        ),
        "folder-1" to FolderChildrenDto(
            folderTitle = "Work",
            breadcrumbs = listOf(
                BreadcrumbDto(id = "root-id", title = "Bookmarks"),
                BreadcrumbDto(id = "folder-1", title = "Work"),
            ),
            folders = emptyList(),
            bookmarks = listOf(
                BookmarkItemDto(
                    id = "bm-1",
                    title = "GitHub",
                    url = "https://github.com",
                    createdAt = "2024-01-01T00:00:00Z",
                ),
            ),
        ),
        "folder-2" to FolderChildrenDto(
            folderTitle = "Personal",
            breadcrumbs = listOf(
                BreadcrumbDto(id = "root-id", title = "Bookmarks"),
                BreadcrumbDto(id = "folder-2", title = "Personal"),
            ),
            folders = emptyList(),
            bookmarks = emptyList(),
        ),
    )

    var shouldThrow: Exception? = null
    var moveItemThrow: Exception? = null
    var mergeResult = false

    // Tracking calls
    var createFolderCalls = mutableListOf<Pair<String, String>>() // (parentId, title)
    var renameFolderCalls = mutableListOf<Pair<String, String>>() // (folderId, title)
    var deleteFolderCalls = mutableListOf<String>() // folderId
    var moveItemCalls = mutableListOf<Triple<String, String, String>>() // (itemId, from, to)
    var getFolderChildrenCallCount = 0
    var getNavTreeCallCount = 0

    override fun initRepo(dataDir: String, syncDir: String, clientId: String) {}
    override fun shutdown() {}

    override fun getFolderChildren(folderId: String, sortBy: SortOrder): FolderChildrenDto {
        getFolderChildrenCallCount++
        shouldThrow?.let { throw it }
        return folderChildren[folderId]
            ?: throw RuntimeException("folder not found: $folderId")
    }

    override fun getFolderNavTree(): FolderNavTreeDto {
        getNavTreeCallCount++
        shouldThrow?.let { throw it }
        return navTree
    }

    override fun getBookmark(bookmarkId: String): BookmarkDto? = null
    override fun addBookmark(folderId: String, url: String, title: String): String = "new-id"
    override fun updateBookmark(bookmarkId: String, url: String?, title: String?, notes: String?) {}
    override fun deleteBookmark(bookmarkId: String) {}
    override fun createFolder(parentFolderId: String, title: String): String {
        createFolderCalls.add(parentFolderId to title)
        return "new-folder-id"
    }

    override fun renameFolder(folderId: String, title: String) {
        renameFolderCalls.add(folderId to title)
    }

    override fun deleteFolder(folderId: String) {
        deleteFolderCalls.add(folderId)
    }

    override fun moveItem(itemId: String, fromFolderId: String, toFolderId: String) {
        moveItemThrow?.let { throw it }
        moveItemCalls.add(Triple(itemId, fromFolderId, toFolderId))
    }
    override fun searchBookmarks(query: String, sortBy: SortOrder): List<BookmarkDto> = emptyList()
    override fun importHtml(folderId: String, html: String): ImportResultDto =
        ImportResultDto(bookmarksImported = 0u, foldersImported = 0u)
    override fun exportHtml(): String = ""
    override fun triggerFullMerge(): Boolean = mergeResult
}
