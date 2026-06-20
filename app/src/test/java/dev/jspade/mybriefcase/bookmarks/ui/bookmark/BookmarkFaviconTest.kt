package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BookmarkFaviconTest {
    @Test
    fun `extractDomain returns domain from https URL`() {
        assertEquals("github.com", extractDomain("https://github.com/user/repo"))
    }

    @Test
    fun `extractDomain returns domain from http URL`() {
        assertEquals("example.org", extractDomain("http://example.org/path?q=1"))
    }

    @Test
    fun `extractDomain strips www prefix`() {
        assertEquals("example.com", extractDomain("https://www.example.com/page"))
    }

    @Test
    fun `extractDomain returns raw string for invalid URL`() {
        assertEquals("not a url", extractDomain("not a url"))
    }

    @Test
    fun `avatarLetter returns uppercase first letter of domain`() {
        assertEquals('G', avatarLetter("https://github.com"))
    }

    @Test
    fun `avatarLetter returns hash for empty domain`() {
        assertEquals('#', avatarLetter(""))
    }

    @Test
    fun `avatarColorIndex is deterministic for same domain`() {
        val idx1 = avatarColorIndex("https://github.com")
        val idx2 = avatarColorIndex("https://github.com/different/path")
        assertEquals(idx1, idx2)
    }

    @Test
    fun `avatarColorIndex differs for different domains`() {
        val idx1 = avatarColorIndex("https://github.com")
        val idx2 = avatarColorIndex("https://example.com")
        assertNotEquals(idx1, idx2)
    }
}
