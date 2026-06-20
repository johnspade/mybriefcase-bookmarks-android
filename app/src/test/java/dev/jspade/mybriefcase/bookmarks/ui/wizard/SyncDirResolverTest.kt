package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dev.jspade.mybriefcase.bookmarks.TestApp::class)
class SyncDirResolverTest {
    @Test
    fun `resolves primary storage tree URI to filesystem path`() {
        val uri =
            Uri.parse(
                "content://com.android.externalstorage.documents/tree/primary%3ASyncthing%2Fmybriefcase_bookmarks",
            )
        val result = SyncDirResolver.resolveTreeUri(uri)
        assertEquals("/storage/emulated/0/Syncthing/mybriefcase_bookmarks", result)
    }

    @Test
    fun `resolves primary root tree URI`() {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A")
        val result = SyncDirResolver.resolveTreeUri(uri)
        assertEquals("/storage/emulated/0", result)
    }

    @Test
    fun `returns null for non-primary storage URI`() {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3ASome%2FPath")
        val result = SyncDirResolver.resolveTreeUri(uri)
        assertNull(result)
    }

    @Test
    fun `returns null for unrecognized authority`() {
        val uri = Uri.parse("content://com.google.android.apps.docs.storage/tree/primary%3Afoo")
        val result = SyncDirResolver.resolveTreeUri(uri)
        assertNull(result)
    }

    @Test
    fun `handles nested path with spaces`() {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMy%20Folder%2Fbookmarks")
        val result = SyncDirResolver.resolveTreeUri(uri)
        assertEquals("/storage/emulated/0/My Folder/bookmarks", result)
    }
}
