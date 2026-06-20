package dev.jspade.mybriefcase.bookmarks.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jspade.mybriefcase.bookmarks.MyBriefcaseApp
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.mybriefcase_bookmarks_ffi.BookmarkHistoryEntryDto

data class HistoryUiState(
    val entries: List<BookmarkHistoryEntryDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val reverted: Boolean = false,
)

class HistoryViewModel(
    private val repository: BookmarkRepository = MyBriefcaseApp.instance.repository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadHistory(bookmarkId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(ioDispatcher) {
            try {
                val entries = repository.getBookmarkHistory(bookmarkId)
                _uiState.value = _uiState.value.copy(entries = entries, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun revertBookmark(
        bookmarkId: String,
        changeHash: String,
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.revertBookmark(bookmarkId, changeHash)
                _uiState.value = _uiState.value.copy(reverted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
