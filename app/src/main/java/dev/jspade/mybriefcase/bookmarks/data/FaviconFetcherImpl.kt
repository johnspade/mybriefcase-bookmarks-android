package dev.jspade.mybriefcase.bookmarks.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.security.MessageDigest

private const val DEFAULT_DUCKDUCKGO_BASE = "https://icons.duckduckgo.com/ip3/"
private const val MAX_HTML_BYTES = 16 * 1024

class FaviconFetcherImpl(
    private val client: OkHttpClient = OkHttpClient(),
    private val duckDuckGoBaseUrl: String = DEFAULT_DUCKDUCKGO_BASE,
    private val useDuckDuckGo: Boolean = true,
    private val pageBaseUrl: String? = null,
) : FaviconFetcher {
    override suspend fun fetch(
        url: String,
        syncRoot: String,
    ): FetchResult =
        withContext(Dispatchers.IO) {
            try {
                if (useDuckDuckGo) {
                    val domain = extractDomain(url) ?: return@withContext FetchResult.Failed("Invalid URL")
                    fetchFromDuckDuckGo(domain, syncRoot)
                } else {
                    fetchViaHtmlParsing(url, syncRoot)
                }
            } catch (e: Exception) {
                FetchResult.Failed(e.message ?: "Unknown error")
            }
        }

    private fun fetchFromDuckDuckGo(
        domain: String,
        syncRoot: String,
    ): FetchResult {
        val iconUrl = "${duckDuckGoBaseUrl}$domain.ico"
        val request = Request.Builder().url(iconUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return FetchResult.Failed("DuckDuckGo fetch failed: ${response.code}")
        }
        val bytes = response.body?.bytes() ?: return FetchResult.Failed("Empty response body")
        val ext = extensionFromContentType(response.header("Content-Type")) ?: "ico"
        return storeContentAddressed(bytes, ext, syncRoot)
    }

    private fun fetchViaHtmlParsing(
        url: String,
        syncRoot: String,
    ): FetchResult {
        val baseUrl =
            pageBaseUrl ?: run {
                val uri = URI(url)
                "${uri.scheme}://${uri.host}${if (uri.port > 0) ":${uri.port}" else ""}"
            }

        val htmlRequest = Request.Builder().url(url).build()
        val htmlResponse = client.newCall(htmlRequest).execute()
        if (htmlResponse.isSuccessful) {
            val html =
                htmlResponse.body?.source()?.let { source ->
                    source.request(MAX_HTML_BYTES.toLong())
                    source.buffer.readUtf8(minOf(source.buffer.size, MAX_HTML_BYTES.toLong()))
                } ?: ""

            val iconHref = parseFaviconLink(html)
            if (iconHref != null) {
                val iconUrl = resolveUrl(baseUrl, iconHref)
                val result = fetchIcon(iconUrl, syncRoot)
                if (result is FetchResult.Success) return result
            }
        }

        val faviconIcoUrl = "$baseUrl/favicon.ico"
        return fetchIcon(faviconIcoUrl, syncRoot)
    }

    private fun fetchIcon(
        iconUrl: String,
        syncRoot: String,
    ): FetchResult {
        val request = Request.Builder().url(iconUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return FetchResult.Failed("Fetch failed: ${response.code}")
        }
        val bytes = response.body?.bytes() ?: return FetchResult.Failed("Empty response body")
        val ext = extensionFromContentType(response.header("Content-Type")) ?: "ico"
        return storeContentAddressed(bytes, ext, syncRoot)
    }

    private fun parseFaviconLink(html: String): String? {
        val regex =
            Regex(
                """<link[^>]*rel\s*=\s*["'](?:shortcut )?icon["'][^>]*href\s*=\s*["']([^"']+)["'][^>]*/?>""",
                RegexOption.IGNORE_CASE,
            )
        val altRegex =
            Regex(
                """<link[^>]*href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["'](?:shortcut )?icon["'][^>]*/?>""",
                RegexOption.IGNORE_CASE,
            )
        return regex.find(html)?.groupValues?.get(1) ?: altRegex.find(html)?.groupValues?.get(1)
    }

    private fun resolveUrl(
        base: String,
        href: String,
    ): String =
        when {
            href.startsWith("http://") || href.startsWith("https://") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> "$base$href"
            else -> "$base/$href"
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
