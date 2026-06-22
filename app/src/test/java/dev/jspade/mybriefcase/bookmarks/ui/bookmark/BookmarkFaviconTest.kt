package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.mybriefcase_bookmarks_ffi.domainColor
import uniffi.mybriefcase_bookmarks_ffi.domainLetter

class BookmarkFaviconTest {
    @Test
    fun `golden pairs match core crate`() {
        assertEquals("G", domainLetter("https://github.com/foo/bar"))
        assertEquals("#673AB7", domainColor("https://github.com/foo/bar"))

        assertEquals("R", domainLetter("https://www.reddit.com/r/rust"))
        assertEquals("#009688", domainColor("https://www.reddit.com/r/rust"))

        assertEquals("E", domainLetter("http://example.com"))
        assertEquals("#039BE5", domainColor("http://example.com"))

        assertEquals("D", domainLetter("https://docs.rs/tokio"))
        assertEquals("#0F9D58", domainColor("https://docs.rs/tokio"))
    }

    @Test
    fun `case insensitive`() {
        assertEquals(
            domainColor("https://GitHub.com/foo"),
            domainColor("https://github.com/bar"),
        )
    }

    @Test
    fun `same domain different paths same color`() {
        val a = domainColor("https://github.com/foo")
        val b = domainColor("https://github.com/bar/baz?q=1")
        assertEquals(a, b)
    }

    @Test
    fun `unparseable url returns question mark`() {
        assertEquals("?", domainLetter(""))
        assertEquals("?", domainLetter("://"))
    }
}
