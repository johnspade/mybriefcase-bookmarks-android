package dev.jspade.mybriefcase.bookmarks.data

import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.FolderChildrenDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto
import uniffi.mybriefcase_bookmarks_ffi.ImportResultDto
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

interface BookmarkRepository {
    // Lifecycle
    fun initRepo(dataDir: String, syncDir: String, clientId: String)
    fun shutdown()

    // Read
    suspend fun getFolderChildren(folderId: String, sortBy: SortOrder): FolderChildrenDto
    suspend fun getFolderNavTree(): FolderNavTreeDto
    suspend fun getBookmark(bookmarkId: String): BookmarkDto?

    // Bookmarks
    suspend fun addBookmark(folderId: String, url: String, title: String): String
    suspend fun updateBookmark(bookmarkId: String, url: String?, title: String?, notes: String?)
    suspend fun deleteBookmark(bookmarkId: String)

    // Folders
    suspend fun createFolder(parentFolderId: String, title: String): String
    suspend fun renameFolder(folderId: String, title: String)
    suspend fun deleteFolder(folderId: String)
    suspend fun moveItem(itemId: String, fromFolderId: String, toFolderId: String)

    // Search
    suspend fun searchBookmarks(query: String, sortBy: SortOrder): List<BookmarkDto>

    // Import/Export
    suspend fun importHtml(folderId: String, html: String): ImportResultDto
    suspend fun exportHtml(): String

    // Sync
    suspend fun triggerFullMerge(): Boolean
}
