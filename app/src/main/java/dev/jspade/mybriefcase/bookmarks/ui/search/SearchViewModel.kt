package dev.jspade.mybriefcase.bookmarks.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jspade.mybriefcase.bookmarks.MyBriefcaseApp
import dev.jspade.mybriefcase.bookmarks.data.BookmarkError
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.FfiException
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: BookmarkRepository = MyBriefcaseApp.instance.repository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    debounceMs: Long = 300L,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NAME_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _error = MutableStateFlow<BookmarkError?>(null)
    val error: StateFlow<BookmarkError?> = _error.asStateFlow()

    val searchResults: StateFlow<List<BookmarkDto>> =
        combine(_query.debounce(debounceMs), _sortOrder) { q, sort ->
            q to sort
        }.flatMapLatest { (query, sort) ->
            if (query.isBlank()) {
                _error.value = null
                flowOf(emptyList())
            } else {
                flow {
                    try {
                        val results = repository.searchBookmarks(query, sort)
                        _error.value = null
                        emit(results)
                    } catch (e: Exception) {
                        _error.value = toBookmarkError(e)
                        emit(emptyList())
                    }
                }
            }
        }.flowOn(ioDispatcher)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setQuery(query: String) {
        _query.value = query
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun clearSearch() {
        _query.value = ""
    }

    private fun toBookmarkError(e: Throwable): BookmarkError =
        when (e) {
            is FfiException -> BookmarkError.from(e)
            else -> BookmarkError.Internal(e.message ?: "Unknown error")
        }
}
