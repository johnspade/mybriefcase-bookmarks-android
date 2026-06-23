package dev.jspade.mybriefcase.bookmarks.data

interface FaviconSettings {
    val fetchEnabled: Boolean
    val useDuckDuckGo: Boolean

    fun setFetchEnabled(enabled: Boolean)

    fun setUseDuckDuckGo(enabled: Boolean)
}
