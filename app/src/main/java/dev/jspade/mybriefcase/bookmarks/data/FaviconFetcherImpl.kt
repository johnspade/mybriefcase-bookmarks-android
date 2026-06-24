package dev.jspade.mybriefcase.bookmarks.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.security.MessageDigest

private const val DEFAULT_DUCKDUCKGO_BASE = "https://icons.duckduckgo.com/ip3/"

class FaviconFetcherImpl(
    private val client: OkHttpClient = OkHttpClient(),
    private val duckDuckGoBaseUrl: String = DEFAULT_DUCKDUCKGO_BASE,
) : FaviconFetcher {
    override suspend fun fetch(
        url: String,
        syncRoot: String,
    ): FetchResult =
        withContext(Dispatchers.IO) {
            try {
                val domain = extractDomain(url) ?: return@withContext FetchResult.Failed("Invalid URL")
                val iconUrl = "${duckDuckGoBaseUrl}$domain.ico"
                val request = Request.Builder().url(iconUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext FetchResult.Failed("DuckDuckGo fetch failed: ${response.code}")
                }
                val bytes =
                    response.body?.bytes() ?: return@withContext FetchResult.Failed("Empty response body")
                val ext = extensionFromContentType(response.header("Content-Type")) ?: "ico"
                storeContentAddressed(bytes, ext, syncRoot)
            } catch (e: Exception) {
                FetchResult.Failed(e.message ?: "Unknown error")
            }
        }

    private fun storeContentAddressed(
        bytes: ByteArray,
        ext: String,
        syncRoot: String,
    ): FetchResult {
        val hash =
            MessageDigest
                .getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it) }
        val filename = "$hash.$ext"
        val faviconsDir = File(syncRoot, "favicons")
        faviconsDir.mkdirs()
        File(faviconsDir, filename).writeBytes(bytes)
        return FetchResult.Success(filename)
    }

    private fun extractDomain(url: String): String? =
        try {
            URI(url).host
        } catch (_: Exception) {
            null
        }

    private fun extensionFromContentType(contentType: String?): String? =
        when {
            contentType == null -> null
            contentType.contains("icon") -> "ico"
            contentType.contains("png") -> "png"
            contentType.contains("jpeg") || contentType.contains("jpg") -> "jpg"
            contentType.contains("gif") -> "gif"
            contentType.contains("svg") -> "svg"
            contentType.contains("webp") -> "webp"
            else -> null
        }
}
