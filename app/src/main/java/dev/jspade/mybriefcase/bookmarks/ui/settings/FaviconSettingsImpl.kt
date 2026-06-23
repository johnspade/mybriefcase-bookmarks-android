package dev.jspade.mybriefcase.bookmarks.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dev.jspade.mybriefcase.bookmarks.data.FaviconSettings

class FaviconSettingsImpl(
    context: Context,
) : FaviconSettings {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val fetchEnabled: Boolean
        get() = prefs.getBoolean(KEY_FETCH_ENABLED, true)

    override val useDuckDuckGo: Boolean
        get() = prefs.getBoolean(KEY_USE_DUCKDUCKGO, true)

    override fun setFetchEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_FETCH_ENABLED, enabled) }
    }

    override fun setUseDuckDuckGo(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_USE_DUCKDUCKGO, enabled) }
    }

    companion object {
        private const val PREFS_NAME = "favicon_settings"
        private const val KEY_FETCH_ENABLED = "fetch_enabled"
        private const val KEY_USE_DUCKDUCKGO = "use_duckduckgo"
    }
}
