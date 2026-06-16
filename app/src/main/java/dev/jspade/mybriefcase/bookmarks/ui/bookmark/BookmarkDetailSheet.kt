package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkDetailSheet(
    bookmark: BookmarkDto,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onShare: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("bookmark_detail_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("detail_title"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bookmark.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("detail_url"),
            )
            if (bookmark.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bookmark.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("detail_notes"),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Created: ${formatDate(bookmark.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("detail_created"),
            )
            Text(
                text = "Updated: ${formatDate(bookmark.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("detail_updated"),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = { onOpenInBrowser(bookmark.url) },
                    modifier = Modifier.testTag("action_open_browser"),
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                }
                IconButton(
                    onClick = { onShare(bookmark.url) },
                    modifier = Modifier.testTag("action_share"),
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(
                    onClick = { onCopyUrl(bookmark.url) },
                    modifier = Modifier.testTag("action_copy"),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL")
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("action_edit"),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkDetailSheetWithActions(
    bookmark: BookmarkDto,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    BookmarkDetailSheet(
        bookmark = bookmark,
        onDismiss = onDismiss,
        onEdit = onEdit,
        onOpenInBrowser = { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        onShare = { url ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            context.startActivity(Intent.createChooser(intent, "Share URL"))
        },
        onCopyUrl = { url ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
            Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
        },
    )
}
