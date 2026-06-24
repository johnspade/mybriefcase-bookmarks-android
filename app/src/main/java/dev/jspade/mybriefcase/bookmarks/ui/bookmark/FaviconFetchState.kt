package dev.jspade.mybriefcase.bookmarks.ui.bookmark

sealed class FaviconFetchState {
    data object Idle : FaviconFetchState()

    data object Loading : FaviconFetchState()

    data class Success(
        val filename: String,
    ) : FaviconFetchState()

    data class Error(
        val message: String,
    ) : FaviconFetchState()
}

sealed class FaviconAction {
    data object Keep : FaviconAction()

    data class Set(
        val filename: String,
    ) : FaviconAction()

    data object Delete : FaviconAction()
}
