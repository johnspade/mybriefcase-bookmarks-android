package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.net.Uri

object SyncDirResolver {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val PRIMARY_STORAGE_PREFIX = "/storage/emulated/0"

    fun resolveTreeUri(uri: Uri): String? {
        if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) return null
        val relativePath = extractPrimaryRelativePath(uri) ?: return null
        return if (relativePath.isEmpty()) PRIMARY_STORAGE_PREFIX else "$PRIMARY_STORAGE_PREFIX/$relativePath"
    }

    private fun extractPrimaryRelativePath(uri: Uri): String? {
        val treeDocId =
            uri.pathSegments
                .takeIf { it.size >= 2 && it[0] == "tree" }
                ?.get(1) ?: return null
        val colonIndex = treeDocId.indexOf(':')
        if (colonIndex < 0 || treeDocId.substring(0, colonIndex) != "primary") return null
        return treeDocId.substring(colonIndex + 1)
    }
}
