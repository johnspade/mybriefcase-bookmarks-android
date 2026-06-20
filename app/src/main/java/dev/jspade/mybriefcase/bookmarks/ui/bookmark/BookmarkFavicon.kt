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
import kotlin.math.absoluteValue

private val AVATAR_COLORS =
    listOf(
        Color(0xFFE57373),
        Color(0xFFF06292),
        Color(0xFFBA68C8),
        Color(0xFF9575CD),
        Color(0xFF7986CB),
        Color(0xFF64B5F6),
        Color(0xFF4FC3F7),
        Color(0xFF4DD0E1),
        Color(0xFF4DB6AC),
        Color(0xFF81C784),
        Color(0xFFAED581),
        Color(0xFFFF8A65),
        Color(0xFFA1887F),
        Color(0xFF90A4AE),
    )

internal fun extractDomain(url: String): String {
    val host =
        try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
    return host.removePrefix("www.")
}

internal fun avatarLetter(url: String): Char {
    val domain = extractDomain(url)
    return domain.firstOrNull()?.uppercaseChar() ?: '#'
}

internal fun avatarColorIndex(url: String): Int {
    val domain = extractDomain(url)
    return domain.hashCode().absoluteValue % AVATAR_COLORS.size
}

@Composable
fun BookmarkFavicon(
    url: String,
    favicon: String?,
    syncRoot: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
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
    size: Dp = 40.dp,
) {
    val letter = avatarLetter(url)
    val colorIndex = avatarColorIndex(url)
    val bgColor = AVATAR_COLORS[colorIndex]

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}
