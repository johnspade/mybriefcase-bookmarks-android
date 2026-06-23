package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.content.Context
import androidx.core.content.edit

enum class StartupDestination {
    WIZARD,
    FOLDER,
}

object StartupDecision {
    private const val PREFS_NAME = "mybriefcase"
    private const val KEY_SYNC_DIR = "sync_dir"

    fun decide(context: Context): StartupDestination {
        val persisted = getPersistedSyncDir(context)
        return if (persisted != null) StartupDestination.FOLDER else StartupDestination.WIZARD
    }

    fun getPersistedSyncDir(context: Context): String? =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SYNC_DIR, null)

    fun persistSyncDir(
        context: Context,
        path: String,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) { putString(KEY_SYNC_DIR, path) }
    }
}
