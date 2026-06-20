package dev.jspade.mybriefcase.bookmarks

import android.app.Application
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepositoryImpl
import dev.jspade.mybriefcase.bookmarks.ui.wizard.StartupDecision

class MyBriefcaseApp : Application() {
    lateinit var repository: BookmarkRepository
        private set

    lateinit var syncDir: String
        private set

    lateinit var clientId: String
        private set

    private var initialized = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = BookmarkRepositoryImpl()

        val persistedDir = StartupDecision.getPersistedSyncDir(this)
        if (persistedDir != null) {
            syncDir = persistedDir
            clientId = getOrCreateClientId()
            repository.initRepo(filesDir.absolutePath, syncDir, clientId)
            initialized = true
        }
    }

    fun initFromWizard() {
        if (initialized) return
        val persistedDir = StartupDecision.getPersistedSyncDir(this) ?: return
        syncDir = persistedDir
        clientId = getOrCreateClientId()
        repository.initRepo(filesDir.absolutePath, syncDir, clientId)
        initialized = true
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
        private const val CLIENT_ID_SUFFIX_LENGTH = 4

        lateinit var instance: MyBriefcaseApp
            private set
    }
}
