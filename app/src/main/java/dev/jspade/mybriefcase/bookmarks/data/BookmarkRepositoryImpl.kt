package dev.jspade.mybriefcase.bookmarks.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.BookmarkHistoryEntryDto
import uniffi.mybriefcase_bookmarks_ffi.FolderChildrenDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto
import uniffi.mybriefcase_bookmarks_ffi.ImportResultDto
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

@Suppress("TooManyFunctions")
interface BookmarkFfi {
    fun initRepo(
        dataDir: String,
        syncDir: String,
        clientId: String,
    )

    fun shutdown()

    fun getFolderChildren(
        folderId: String,
        sortBy: SortOrder,
    ): FolderChildrenDto

    fun getFolderNavTree(): FolderNavTreeDto

    fun getBookmark(bookmarkId: String): BookmarkDto?

    fun addBookmark(
        folderId: String,
        url: String,
        title: String,
    ): String

    fun updateBookmark(
        bookmarkId: String,
        url: String?,
        title: String?,
        notes: String?,
    )

    fun deleteBookmark(bookmarkId: String)

    fun createFolder(
        parentFolderId: String,
        title: String,
    ): String

    fun renameFolder(
        folderId: String,
        title: String,
    )

    fun deleteFolder(folderId: String)

    fun moveItem(
        itemId: String,
        fromFolderId: String,
        toFolderId: String,
    )

    fun searchBookmarks(
        query: String,
        sortBy: SortOrder,
    ): List<BookmarkDto>

    fun importHtml(
        folderId: String,
        html: String,
    ): ImportResultDto

    fun exportHtml(): String

    fun getBookmarkHistory(bookmarkId: String): List<BookmarkHistoryEntryDto>

    fun revertBookmark(
        bookmarkId: String,
        changeHash: String,
    )

    fun triggerFullMerge(): Boolean
}

@Suppress("TooManyFunctions")
class DefaultBookmarkFfi : BookmarkFfi {
    override fun initRepo(
        dataDir: String,
        syncDir: String,
        clientId: String,
    ) = uniffi.mybriefcase_bookmarks_ffi.initRepo(dataDir, syncDir, clientId)

    override fun shutdown() = uniffi.mybriefcase_bookmarks_ffi.shutdown()

    override fun getFolderChildren(
        folderId: String,
        sortBy: SortOrder,
    ) = uniffi.mybriefcase_bookmarks_ffi.getFolderChildren(folderId, sortBy)

    override fun getFolderNavTree() = uniffi.mybriefcase_bookmarks_ffi.getFolderNavTree()

    override fun getBookmark(bookmarkId: String) = uniffi.mybriefcase_bookmarks_ffi.getBookmark(bookmarkId)

    override fun addBookmark(
        folderId: String,
        url: String,
        title: String,
    ) = uniffi.mybriefcase_bookmarks_ffi.addBookmark(folderId, url, title)

    override fun updateBookmark(
        bookmarkId: String,
        url: String?,
        title: String?,
        notes: String?,
    ) = uniffi.mybriefcase_bookmarks_ffi.updateBookmark(bookmarkId, url, title, notes)

    override fun deleteBookmark(bookmarkId: String) = uniffi.mybriefcase_bookmarks_ffi.deleteBookmark(bookmarkId)

    override fun createFolder(
        parentFolderId: String,
        title: String,
    ) = uniffi.mybriefcase_bookmarks_ffi.createFolder(parentFolderId, title)

    override fun renameFolder(
        folderId: String,
        title: String,
    ) = uniffi.mybriefcase_bookmarks_ffi.renameFolder(folderId, title)

    override fun deleteFolder(folderId: String) = uniffi.mybriefcase_bookmarks_ffi.deleteFolder(folderId)

    override fun moveItem(
        itemId: String,
        fromFolderId: String,
        toFolderId: String,
    ) = uniffi.mybriefcase_bookmarks_ffi.moveItem(itemId, fromFolderId, toFolderId)

    override fun searchBookmarks(
        query: String,
        sortBy: SortOrder,
    ) = uniffi.mybriefcase_bookmarks_ffi.searchBookmarks(query, sortBy)

    override fun importHtml(
        folderId: String,
        html: String,
    ) = uniffi.mybriefcase_bookmarks_ffi.importHtml(folderId, html)

    override fun exportHtml() = uniffi.mybriefcase_bookmarks_ffi.exportHtml()

    override fun getBookmarkHistory(bookmarkId: String) =
        uniffi.mybriefcase_bookmarks_ffi.getBookmarkHistory(bookmarkId)

    override fun revertBookmark(
        bookmarkId: String,
        changeHash: String,
    ) = uniffi.mybriefcase_bookmarks_ffi.revertBookmark(bookmarkId, changeHash)

    override fun triggerFullMerge() = uniffi.mybriefcase_bookmarks_ffi.triggerFullMerge()
}

@Suppress("TooManyFunctions")
class BookmarkRepositoryImpl(
    private val ffi: BookmarkFfi = DefaultBookmarkFfi(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BookmarkRepository {
    override fun initRepo(
        dataDir: String,
        syncDir: String,
        clientId: String,
    ) {
        ffi.initRepo(dataDir, syncDir, clientId)
    }

    override fun shutdown() {
        ffi.shutdown()
    }

    override suspend fun getFolderChildren(
        folderId: String,
        sortBy: SortOrder,
    ): FolderChildrenDto = withContext(ioDispatcher) { ffi.getFolderChildren(folderId, sortBy) }

    override suspend fun getFolderNavTree(): FolderNavTreeDto = withContext(ioDispatcher) { ffi.getFolderNavTree() }

    override suspend fun getBookmark(bookmarkId: String): BookmarkDto? =
        withContext(ioDispatcher) {
            ffi.getBookmark(bookmarkId)
        }

    override suspend fun addBookmark(
        folderId: String,
        url: String,
        title: String,
    ): String = withContext(ioDispatcher) { ffi.addBookmark(folderId, url, title) }

    override suspend fun updateBookmark(
        bookmarkId: String,
        url: String?,
        title: String?,
        notes: String?,
    ) = withContext(ioDispatcher) { ffi.updateBookmark(bookmarkId, url, title, notes) }

    override suspend fun deleteBookmark(bookmarkId: String) =
        withContext(ioDispatcher) { ffi.deleteBookmark(bookmarkId) }

    override suspend fun createFolder(
        parentFolderId: String,
        title: String,
    ): String = withContext(ioDispatcher) { ffi.createFolder(parentFolderId, title) }

    override suspend fun renameFolder(
        folderId: String,
        title: String,
    ) = withContext(ioDispatcher) { ffi.renameFolder(folderId, title) }

    override suspend fun deleteFolder(folderId: String) = withContext(ioDispatcher) { ffi.deleteFolder(folderId) }

    override suspend fun moveItem(
        itemId: String,
        fromFolderId: String,
        toFolderId: String,
    ) = withContext(ioDispatcher) { ffi.moveItem(itemId, fromFolderId, toFolderId) }

    override suspend fun searchBookmarks(
        query: String,
        sortBy: SortOrder,
    ): List<BookmarkDto> = withContext(ioDispatcher) { ffi.searchBookmarks(query, sortBy) }

    override suspend fun importHtml(
        folderId: String,
        html: String,
    ): ImportResultDto = withContext(ioDispatcher) { ffi.importHtml(folderId, html) }

    override suspend fun exportHtml(): String = withContext(ioDispatcher) { ffi.exportHtml() }

    override suspend fun getBookmarkHistory(bookmarkId: String): List<BookmarkHistoryEntryDto> =
        withContext(ioDispatcher) { ffi.getBookmarkHistory(bookmarkId) }

    override suspend fun revertBookmark(
        bookmarkId: String,
        changeHash: String,
    ) = withContext(ioDispatcher) { ffi.revertBookmark(bookmarkId, changeHash) }

    override suspend fun triggerFullMerge(): Boolean = withContext(ioDispatcher) { ffi.triggerFullMerge() }
}
