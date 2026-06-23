package dev.jspade.mybriefcase.bookmarks

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class TestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        val syncDir = context!!.filesDir.resolve("test_sync").also { it.mkdirs() }
        context
            .getSharedPreferences("mybriefcase", Context.MODE_PRIVATE)
            .edit()
            .putString("sync_dir", syncDir.absolutePath)
            .commit()
        return super.newApplication(cl, className, context)
    }
}
