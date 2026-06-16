package dev.jspade.mybriefcase.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BookmarkDtoTest {

    @Test
    fun `placeholder - unit test infrastructure works`() {
        val title = "Example Bookmark"
        val url = "https://example.com"
        assertEquals("Example Bookmark", title)
        assertEquals("https://example.com", url)
        assertNotEquals(title, url)
    }
}
