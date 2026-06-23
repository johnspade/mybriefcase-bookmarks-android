package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun FaviconFetchButton(
    fetchState: FaviconFetchState,
    onFetch: () -> Unit,
    syncRoot: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        when (fetchState) {
            is FaviconFetchState.Idle -> {
                TextButton(
                    onClick = onFetch,
                    modifier = Modifier.testTag("favicon_fetch_button"),
                ) {
                    Text("Fetch favicon")
                }
            }

            is FaviconFetchState.Loading -> {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .testTag("favicon_fetch_loading"),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetching...", style = MaterialTheme.typography.bodySmall)
            }

            is FaviconFetchState.Success -> {
                BookmarkFavicon(
                    url = "",
                    favicon = fetchState.filename,
                    syncRoot = syncRoot,
                    size = 32.dp,
                    modifier = Modifier.testTag("favicon_fetch_preview"),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onFetch,
                    modifier = Modifier.testTag("favicon_fetch_button"),
                ) {
                    Text("Refetch")
                }
            }

            is FaviconFetchState.Error -> {
                Text(
                    text = fetchState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onFetch,
                    modifier = Modifier.testTag("favicon_fetch_button"),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
