package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun FaviconHero(
    url: String,
    favicon: String?,
    syncRoot: String?,
    fetchState: FaviconFetchState,
    onFetch: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    fetchEnabled: Boolean = true,
    fallbackUrl: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            when (fetchState) {
                is FaviconFetchState.Loading -> {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .testTag("favicon_fetch_loading"),
                        strokeWidth = 2.dp,
                    )
                }

                else -> {
                    if (favicon != null && syncRoot != null) {
                        BookmarkFavicon(
                            url = url,
                            favicon = favicon,
                            syncRoot = syncRoot,
                            size = 32.dp,
                            modifier = Modifier.testTag("favicon_hero_image"),
                        )
                    } else if (fallbackUrl != null) {
                        LetterAvatar(
                            url = fallbackUrl,
                            size = 32.dp,
                            modifier = Modifier.testTag("favicon_hero_image"),
                        )
                    }
                }
            }
        }
        if (fetchState is FaviconFetchState.Error) {
            Text(
                text = fetchState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onFetch,
                enabled = fetchEnabled && fetchState !is FaviconFetchState.Loading,
                modifier = Modifier.testTag("favicon_fetch_button"),
            ) {
                Text(
                    when (fetchState) {
                        is FaviconFetchState.Idle -> if (favicon != null) "Refetch" else "Fetch favicon"
                        is FaviconFetchState.Success -> "Refetch"
                        is FaviconFetchState.Error -> "Retry"
                        is FaviconFetchState.Loading -> "Fetching..."
                    },
                )
            }
            if (onDelete != null && (favicon != null || fetchState is FaviconFetchState.Success)) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("favicon_delete_button"),
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
