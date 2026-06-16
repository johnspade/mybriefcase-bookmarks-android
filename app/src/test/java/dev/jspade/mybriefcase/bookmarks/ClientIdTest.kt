package dev.jspade.mybriefcase.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ClientIdTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * Exercises the same logic as MyBriefcaseApp.getOrCreateClientId()
     * using a temp directory instead of filesDir.
     */
    private fun getOrCreateClientId(filesDir: java.io.File, model: String): String {
        val file = java.io.File(filesDir, "client_id")
        if (file.exists()) {
            return file.readText().trim()
        }
        val sanitizedModel = model.replace(" ", "-")
        val suffix = (0 until 4).map { "0123456789abcdef".random() }.joinToString("")
        val clientId = "$sanitizedModel-MyBriefcaseBookmarks-$suffix"
        file.writeText(clientId)
        return clientId
    }

    @Test
    fun `generated client id matches expected format`() {
        val filesDir = tempFolder.newFolder("files")
        val clientId = getOrCreateClientId(filesDir, "Pixel 7")

        // Format: <model>-MyBriefcaseBookmarks-<4hex>
        val pattern = Regex("^Pixel-7-MyBriefcaseBookmarks-[0-9a-f]{4}$")
        assertTrue(
            "Client ID '$clientId' should match format <model>-MyBriefcaseBookmarks-<4hex>",
            pattern.matches(clientId),
        )
    }

    @Test
    fun `reading client id again returns the same value`() {
        val filesDir = tempFolder.newFolder("files")
        val first = getOrCreateClientId(filesDir, "Pixel 7")
        val second = getOrCreateClientId(filesDir, "Pixel 7")

        assertEquals("Reading client_id again should return the same value", first, second)
    }

    @Test
    fun `model with spaces is sanitized`() {
        val filesDir = tempFolder.newFolder("files")
        val clientId = getOrCreateClientId(filesDir, "Samsung Galaxy S24 Ultra")

        assertTrue(
            "Client ID should not contain spaces",
            !clientId.contains(" "),
        )
        assertTrue(
            "Client ID should contain sanitized model name",
            clientId.startsWith("Samsung-Galaxy-S24-Ultra-MyBriefcaseBookmarks-"),
        )
    }
}
