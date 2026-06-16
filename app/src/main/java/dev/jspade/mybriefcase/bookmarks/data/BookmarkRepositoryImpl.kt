package dev.jspade.mybriefcase.bookmarks.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.FolderChildrenDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto
import uniffi.mybriefcase_bookmarks_ffi.ImportResultDto
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

/**
 * Implementation that delegates to UniFFI-generated bindings on Dispatchers.IO.
 */
class BookmarkRepositoryImpl : BookmarkRepository {

    override fun initRepo(dataDir: String, syncDir: String, clientId: String) {
        uniffi.mybriefcase_bookmarks_ffi.initRepo(dataDir, syncDir, clientId)
    }

    override fun shutdown() {
        uniffi.mybriefcase_bookmarks_ffi.shutdown()
    }

    override fun getFolderChildren(folderId: String, sortBy: SortOrder): FolderChildrenDto {
        return uniffi.mybriefcase_bookmarks_ffi.getFolderChildren(folderId, sortBy)
    }

    override fun getFolderNavTree(): FolderNavTreeDto {
        return uniffi.mybriefcase_bookmarks_ffi.getFolderNavTree()
    }

    override fun getBookmark(bookmarkId: String): BookmarkDto? {
        return uniffi.mybriefcase_bookmarks_ffi.getBookmark(bookmarkId)
    }

    override fun addBookmark(folderId: String, url: String, title: String): String {
        return uniffi.mybriefcase_bookmarks_ffi.addBookmark(folderId, url, title)
    }

    override fun updateBookmark(bookmarkId: String, url: String?, title: String?, notes: String?) {
        uniffi.mybriefcase_bookmarks_ffi.updateBookmark(bookmarkId, url, title, notes)
    }

    override fun deleteBookmark(bookmarkId: String) {
        uniffi.mybriefcase_bookmarks_ffi.deleteBookmark(bookmarkId)
    }

    override fun createFolder(parentFolderId: String, title: String): String {
        return uniffi.mybriefcase_bookmarks_ffi.createFolder(parentFolderId, title)
    }

    override fun renameFolder(folderId: String, title: String) {
        uniffi.mybriefcase_bookmarks_ffi.renameFolder(folderId, title)
    }

    override fun deleteFolder(folderId: String) {
        uniffi.mybriefcase_bookmarks_ffi.deleteFolder(folderId)
    }

    override fun moveItem(itemId: String, fromFolderId: String, toFolderId: String) {
        uniffi.mybriefcase_bookmarks_ffi.moveItem(itemId, fromFolderId, toFolderId)
    }

    override fun searchBookmarks(query: String, sortBy: SortOrder): List<BookmarkDto> {
        return uniffi.mybriefcase_bookmarks_ffi.searchBookmarks(query, sortBy)
    }

    override fun importHtml(folderId: String, html: String): ImportResultDto {
        return uniffi.mybriefcase_bookmarks_ffi.importHtml(folderId, html)
    }

    override fun exportHtml(): String {
        return uniffi.mybriefcase_bookmarks_ffi.exportHtml()
    }

    override fun triggerFullMerge(): Boolean {
        return uniffi.mybriefcase_bookmarks_ffi.triggerFullMerge()
    }
}
