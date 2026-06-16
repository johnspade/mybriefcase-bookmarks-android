package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto

@Composable
fun EditBookmarkDialog(
    bookmark: BookmarkDto,
    onDismiss: () -> Unit,
    onConfirm: (url: String, title: String, notes: String) -> Unit,
) {
    var url by remember { mutableStateOf(bookmark.url) }
    var title by remember { mutableStateOf(bookmark.title) }
    var notes by remember { mutableStateOf(bookmark.notes) }
    var urlError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bookmark") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = false
                    },
                    label = { Text("URL") },
                    isError = urlError,
                    supportingText = if (urlError) {
                        { Text("URL is required") }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bookmark_url"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bookmark_title"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bookmark_notes"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isBlank()) {
                        urlError = true
                    } else {
                        onConfirm(url.trim(), title.trim(), notes.trim())
                    }
                },
                modifier = Modifier.testTag("edit_bookmark_confirm"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("edit_bookmark_dialog"),
    )
}
