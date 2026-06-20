package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class StartupDecisionTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context
            .getSharedPreferences("mybriefcase", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `shows wizard when no persisted path`() {
        val result = StartupDecision.decide(context)
        assertEquals(StartupDestination.WIZARD, result)
    }

    @Test
    fun `skips wizard when sync dir is persisted`() {
        context
            .getSharedPreferences("mybriefcase", Context.MODE_PRIVATE)
            .edit()
            .putString("sync_dir", "/storage/emulated/0/Syncthing/bookmarks")
            .commit()
        val result = StartupDecision.decide(context)
        assertEquals(StartupDestination.FOLDER, result)
    }
}
