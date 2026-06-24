package dev.jspade.mybriefcase.bookmarks.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import dev.jspade.mybriefcase.bookmarks.data.FaviconFetcher
import dev.jspade.mybriefcase.bookmarks.data.FaviconFetcherImpl
import dev.jspade.mybriefcase.bookmarks.data.FetchResult
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.FaviconFetchState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto

data class ShareReceiverUiState(
    val url: String = "",
    val title: String = "",
    val selectedFolderId: String? = null,
    val navTree: FolderNavTreeDto? = null,
    val urlError: String? = null,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isInitialized: Boolean = true,
    val faviconFetchState: FaviconFetchState = FaviconFetchState.Idle,
)

class ShareReceiverViewModel(
    private val repository: BookmarkRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    extraText: String?,
    extraSubject: String?,
    isAppInitialized: Boolean = true,
    private val syncDirPath: String? = null,
    private val faviconFetchEnabled: Boolean = true,
    private val faviconFetcher: FaviconFetcher? = null,
    private val faviconFetcherFactory: (() -> FaviconFetcher)? = ::defaultFaviconFetcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShareReceiverUiState())
    val uiState: StateFlow<ShareReceiverUiState> = _uiState.asStateFlow()

    val isFaviconFetchEnabled: Boolean
        get() = faviconFetchEnabled && syncDirPath != null

    init {
        val url = extractUrl(extraText)
        _uiState.value =
            ShareReceiverUiState(
                url = url,
                title = extraSubject ?: "",
                isInitialized = isAppInitialized,
            )
        if (isAppInitialized) {
            loadNavTree()
        }
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(url = url, urlError = null)
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun selectFolder(folderId: String) {
        _uiState.value = _uiState.value.copy(selectedFolderId = folderId)
    }

    private var fetchFaviconJob: Job? = null

    fun fetchFavicon(url: String) {
        val fetcher = faviconFetcher ?: faviconFetcherFactory?.invoke() ?: return
        val syncRoot = syncDirPath ?: return
        _uiState.value = _uiState.value.copy(faviconFetchState = FaviconFetchState.Loading)
        fetchFaviconJob?.cancel()
        fetchFaviconJob =
            viewModelScope.launch(ioDispatcher) {
                val result = fetcher.fetch(url, syncRoot)
                val newState =
                    when (result) {
                        is FetchResult.Success -> FaviconFetchState.Success(result.filename)
                        is FetchResult.Failed -> FaviconFetchState.Error(result.reason)
                    }
                _uiState.value = _uiState.value.copy(faviconFetchState = newState)
            }
    }

    fun save() {
        val state = _uiState.value
        val url = state.url.trim()

        if (url.isBlank()) {
            _uiState.value = state.copy(urlError = "URL is required")
            return
        }
        if (!isValidUrl(url)) {
            _uiState.value = state.copy(urlError = "Invalid URL")
            return
        }

        val folderId = state.selectedFolderId ?: state.navTree?.rootFolderId ?: return
        val title = state.title.trim().ifEmpty { url }
        val faviconState = state.faviconFetchState

        viewModelScope.launch(ioDispatcher) {
            try {
                val bookmarkId = repository.addBookmark(folderId, url, title)
                if (faviconState is FaviconFetchState.Success) {
                    repository.setFavicon(bookmarkId, faviconState.filename)
                }
                _uiState.value = _uiState.value.copy(isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun loadNavTree() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val tree = repository.getFolderNavTree()
                _uiState.value = _uiState.value.copy(navTree = tree)
            } catch (_: Exception) {
                // Non-fatal — folder picker won't be available
            }
        }
    }

    private fun extractUrl(text: String?): String {
        if (text == null) return ""
        val regex = Regex("""https?://[^\s]+""")
        return regex.find(text)?.value ?: text
    }

    private fun isValidUrl(url: String): Boolean = url.matches(Regex("""https?://.+"""))
}

private fun defaultFaviconFetcher(): FaviconFetcher = FaviconFetcherImpl()
