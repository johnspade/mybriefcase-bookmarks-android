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
    fun getFolderChildren(folderId: String, sortBy: SortOrder): FolderChildrenDto
    fun getFolderNavTree(): FolderNavTreeDto
    fun getBookmark(bookmarkId: String): BookmarkDto?

    // Bookmarks
    fun addBookmark(folderId: String, url: String, title: String): String
    fun updateBookmark(bookmarkId: String, url: String?, title: String?, notes: String?)
    fun deleteBookmark(bookmarkId: String)

    // Folders
    fun createFolder(parentFolderId: String, title: String): String
    fun renameFolder(folderId: String, title: String)
    fun deleteFolder(folderId: String)
    fun moveItem(itemId: String, fromFolderId: String, toFolderId: String)

    // Search
    fun searchBookmarks(query: String, sortBy: SortOrder): List<BookmarkDto>

    // Import/Export
    fun importHtml(folderId: String, html: String): ImportResultDto
    fun exportHtml(): String

    // Sync
    fun triggerFullMerge(): Boolean
}
