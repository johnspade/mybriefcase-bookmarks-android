package dev.jspade.mybriefcase.bookmarks.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jspade.mybriefcase.bookmarks.MyBriefcaseApp
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.BookmarkItemDto
import uniffi.mybriefcase_bookmarks_ffi.BreadcrumbDto
import uniffi.mybriefcase_bookmarks_ffi.FolderItemDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto
import uniffi.mybriefcase_bookmarks_ffi.ImportResultDto
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

data class FolderUiState(
    val folderTitle: String = "",
    val breadcrumbs: List<BreadcrumbDto> = emptyList(),
    val folders: List<FolderItemDto> = emptyList(),
    val bookmarks: List<BookmarkItemDto> = emptyList(),
    val navTree: FolderNavTreeDto? = null,
    val currentFolderId: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedBookmark: BookmarkDto? = null,
    val importResult: ImportResultDto? = null,
    val exportedHtml: String? = null,
    val showSyncBanner: Boolean = false,
    val expandedFolderIds: Set<String> = emptySet(),
)

class FolderViewModel(
    private val repository: BookmarkRepository = MyBriefcaseApp.instance.repository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pollIntervalMs: Long = 30_000L,
    private val syncDirPath: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadNavTree()
        checkSyncBanner()
    }

    fun navigateToFolder(folderId: String) {
        _uiState.value = _uiState.value.copy(currentFolderId = folderId, isLoading = true)
        loadFolderContents(folderId)
    }

    fun toggleFolderExpanded(folderId: String) {
        val current = _uiState.value.expandedFolderIds
        val updated = if (folderId in current) current - folderId else current + folderId
        _uiState.value = _uiState.value.copy(expandedFolderIds = updated)
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
        loadFolderContents(_uiState.value.currentFolderId)
    }

    fun createFolder(title: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.createFolder(_uiState.value.currentFolderId, title)
                refreshAfterMutation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun renameFolder(folderId: String, title: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.renameFolder(folderId, title)
                refreshAfterMutation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.deleteFolder(folderId)
                refreshAfterMutation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun moveItem(itemId: String, fromFolderId: String, toFolderId: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.moveItem(itemId, fromFolderId, toFolderId)
                refreshAfterMutation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val changed = repository.triggerFullMerge()
                if (changed) {
                    loadFolderContents(_uiState.value.currentFolderId)
                    loadNavTree()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch(ioDispatcher) {
            while (isActive) {
                delay(pollIntervalMs)
                try {
                    val changed = repository.triggerFullMerge()
                    if (changed) {
                        loadFolderContents(_uiState.value.currentFolderId)
                        loadNavTree()
                    }
                } catch (_: Exception) {
                    // Polling errors are non-fatal
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun addBookmark(url: String, title: String) {
        val folderId = _uiState.value.currentFolderId
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.addBookmark(folderId, url, title)
                refreshAfterMutation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadBookmarkDetail(bookmarkId: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val bookmark = repository.getBookmark(bookmarkId)
                if (bookmark != null) {
                    _uiState.value = _uiState.value.copy(selectedBookmark = bookmark)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Bookmark not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearSelectedBookmark() {
        _uiState.value = _uiState.value.copy(selectedBookmark = null)
    }

    fun updateBookmark(bookmarkId: String, url: String?, title: String?, notes: String?) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.updateBookmark(bookmarkId, url, title, notes)
                // Reload the bookmark detail
                val updated = repository.getBookmark(bookmarkId)
                _uiState.value = _uiState.value.copy(selectedBookmark = updated)
                loadFolderContents(_uiState.value.currentFolderId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateBookmarkAndMove(bookmarkId: String, url: String?, title: String?, notes: String?, newFolderId: String?) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.updateBookmark(bookmarkId, url, title, notes)
                if (newFolderId != null) {
                    repository.moveItem(bookmarkId, _uiState.value.currentFolderId, newFolderId)
                }
                refreshAfterMutation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        val folderId = _uiState.value.currentFolderId
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.deleteBookmark(bookmarkId)
                _uiState.value = _uiState.value.copy(selectedBookmark = null)
                loadFolderContents(folderId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun importHtml(html: String) {
        val folderId = _uiState.value.currentFolderId.ifEmpty {
            _uiState.value.navTree?.rootFolderId ?: return
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val result = repository.importHtml(folderId, html)
                _uiState.value = _uiState.value.copy(importResult = result)
                refreshAfterMutation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearImportResult() {
        _uiState.value = _uiState.value.copy(importResult = null)
    }

    fun exportHtml() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val html = repository.exportHtml()
                _uiState.value = _uiState.value.copy(exportedHtml = html)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearExportedHtml() {
        _uiState.value = _uiState.value.copy(exportedHtml = null)
    }

    fun dismissSyncBanner() {
        _uiState.value = _uiState.value.copy(showSyncBanner = false)
    }

    private fun checkSyncBanner() {
        val path = syncDirPath ?: return
        val syncMarker = java.io.File(path, ".bookmarks-sync")
        if (!syncMarker.exists()) {
            _uiState.value = _uiState.value.copy(showSyncBanner = true)
        }
    }

    private fun refreshAfterMutation() {
        loadFolderContents(_uiState.value.currentFolderId)
        loadNavTree()
    }

    private fun loadNavTree() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val tree = repository.getFolderNavTree()
                _uiState.value = _uiState.value.copy(navTree = tree)
                // If no folder is selected yet, navigate to root
                if (_uiState.value.currentFolderId.isEmpty()) {
                    navigateToFolder(tree.rootFolderId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    private fun loadFolderContents(folderId: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val children = repository.getFolderChildren(folderId, _uiState.value.sortOrder)
                _uiState.value = _uiState.value.copy(
                    folderTitle = children.folderTitle,
                    breadcrumbs = children.breadcrumbs,
                    folders = children.folders,
                    bookmarks = children.bookmarks,
                    isLoading = false,
                    error = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
