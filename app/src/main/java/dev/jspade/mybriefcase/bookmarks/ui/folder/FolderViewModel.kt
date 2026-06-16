package dev.jspade.mybriefcase.bookmarks.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jspade.mybriefcase.bookmarks.MyBriefcaseApp
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.mybriefcase_bookmarks_ffi.BookmarkItemDto
import uniffi.mybriefcase_bookmarks_ffi.BreadcrumbDto
import uniffi.mybriefcase_bookmarks_ffi.FolderItemDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto
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
)

class FolderViewModel(
    private val repository: BookmarkRepository = MyBriefcaseApp.instance.repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    init {
        loadNavTree()
    }

    fun navigateToFolder(folderId: String) {
        _uiState.value = _uiState.value.copy(currentFolderId = folderId, isLoading = true)
        loadFolderContents(folderId)
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
        loadFolderContents(_uiState.value.currentFolderId)
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
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

    private fun loadNavTree() {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
