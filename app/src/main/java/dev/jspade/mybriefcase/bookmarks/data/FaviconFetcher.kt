package dev.jspade.mybriefcase.bookmarks.data

sealed class FetchResult {
    data class Success(
        val filename: String,
    ) : FetchResult()

    data class Failed(
        val reason: String,
    ) : FetchResult()
}

interface FaviconFetcher {
    suspend fun fetch(
        url: String,
        syncRoot: String,
    ): FetchResult
}
