package dev.jspade.mybriefcase.bookmarks.data

interface FaviconSettings {
    val fetchEnabled: Boolean

    fun setFetchEnabled(enabled: Boolean)
}
