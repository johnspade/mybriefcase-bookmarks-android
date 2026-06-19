package dev.jspade.mybriefcase.bookmarks

import android.app.Application
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepositoryImpl

class MyBriefcaseApp : Application() {
    lateinit var repository: BookmarkRepository
        private set

    lateinit var syncDir: String
        private set

    lateinit var clientId: String
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = BookmarkRepositoryImpl()

        val dataDir = filesDir.absolutePath
        syncDir = resolveSyncDir()
        clientId = getOrCreateClientId()

        repository.initRepo(dataDir, syncDir, clientId)
    }

    private fun resolveSyncDir(): String {
        val external = java.io.File(SYNC_ROOT)
        if ((external.exists() || external.mkdirs()) && external.canWrite()) {
            // Verify actual write access (canWrite() is unreliable with scoped storage)
            val probe = java.io.File(external, ".probe")
            try {
                probe.writeText("ok")
                probe.delete()
                return external.absolutePath
            } catch (_: Exception) {
                // Fall through to internal storage
            }
        }
        // Fall back to internal storage when external sync dir is inaccessible
        val internal = java.io.File(filesDir, "sync")
        internal.mkdirs()
        return internal.absolutePath
    }

    override fun onTerminate() {
        super.onTerminate()
        repository.shutdown()
    }

    private fun getOrCreateClientId(): String {
        val file = java.io.File(filesDir, "client_id")
        if (file.exists()) {
            return file.readText().trim()
        }
        val model =
            android.os.Build.MODEL
                .replace(" ", "-")
        val suffix = (0 until CLIENT_ID_SUFFIX_LENGTH).map { "0123456789abcdef".random() }.joinToString("")
        val clientId = "$model-MyBriefcaseBookmarks-$suffix"
        file.writeText(clientId)
        return clientId
    }

    companion object {
        const val SYNC_ROOT = "/storage/emulated/0/Syncthing/mybriefcase_bookmarks"
        private const val CLIENT_ID_SUFFIX_LENGTH = 4

        lateinit var instance: MyBriefcaseApp
            private set
    }
}
