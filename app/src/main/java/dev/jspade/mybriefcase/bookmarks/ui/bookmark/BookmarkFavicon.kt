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
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage
import uniffi.mybriefcase_bookmarks_ffi.domainColor
import uniffi.mybriefcase_bookmarks_ffi.domainLetter
import java.io.File

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
    val bgColor = Color(domainColor(url).toColorInt())

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
