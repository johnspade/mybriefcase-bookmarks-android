package dev.jspade.mybriefcase.bookmarks

import android.app.Application
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepositoryImpl

class MyBriefcaseApp : Application() {

    lateinit var repository: BookmarkRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = BookmarkRepositoryImpl()

        val dataDir = filesDir.absolutePath
        val syncDir = SYNC_ROOT
        val clientId = getOrCreateClientId()

        repository.initRepo(dataDir, syncDir, clientId)
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
        val model = android.os.Build.MODEL.replace(" ", "-")
        val suffix = (0 until 4).map { "0123456789abcdef".random() }.joinToString("")
        val clientId = "$model-MyBriefcaseBookmarks-$suffix"
        file.writeText(clientId)
        return clientId
    }

    companion object {
        const val SYNC_ROOT = "/storage/emulated/0/Syncthing/mybriefcase_bookmarks"

        lateinit var instance: MyBriefcaseApp
            private set
    }
}
