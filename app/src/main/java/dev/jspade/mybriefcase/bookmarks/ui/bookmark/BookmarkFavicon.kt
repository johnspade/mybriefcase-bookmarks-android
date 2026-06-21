package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File

private val PALETTE =
    listOf(
        Color(0xFFDB4437),
        Color(0xFFE91E63),
        Color(0xFF9C27B0),
        Color(0xFF673AB7),
        Color(0xFF3F51B5),
        Color(0xFF4285F4),
        Color(0xFF039BE5),
        Color(0xFF0097A7),
        Color(0xFF009688),
        Color(0xFF0F9D58),
        Color(0xFF689F38),
        Color(0xFFEF6C00),
        Color(0xFFFF5722),
        Color(0xFF757575),
    )

internal fun extractDomain(url: String): String {
    val rest =
        url.removePrefix("https://").removePrefix("http://")
    val host = rest.split('/').first()
    return host.lowercase().removePrefix("www.")
}

private fun djb2(input: String): UInt {
    var hash: UInt = 5381u
    for (b in input.encodeToByteArray()) {
        hash = hash * 33u + b.toUByte().toUInt()
    }
    return hash
}

internal fun domainLetter(url: String): String {
    val domain = extractDomain(url)
    val first = domain.firstOrNull()
    return if (first != null && first.isLetter()) {
        first.uppercaseChar().toString()
    } else {
        "?"
    }
}

internal fun domainColor(url: String): Color {
    val domain = extractDomain(url)
    val hash = djb2(domain)
    return PALETTE[(hash % PALETTE.size.toUInt()).toInt()]
}

@Composable
fun BookmarkFavicon(
    url: String,
    favicon: String?,
    syncRoot: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    if (favicon != null && syncRoot != null) {
        val file = File(syncRoot, "favicons/$favicon")
        AsyncImage(
            model = file,
            contentDescription = null,
            modifier =
                modifier
                    .size(size)
                    .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        LetterAvatar(url = url, modifier = modifier, size = size)
    }
}

@Composable
fun LetterAvatar(
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val letter = domainLetter(url)
    val bgColor = domainColor(url)

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}
