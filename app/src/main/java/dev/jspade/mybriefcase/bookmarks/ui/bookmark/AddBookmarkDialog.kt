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

@Composable
fun AddBookmarkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, title: String) -> Unit,
    validationError: String? = null,
    onValidationErrorClear: () -> Unit = {},
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf(false) }

    val showError = urlError || validationError != null
    val errorText = validationError ?: if (urlError) "URL is required" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bookmark") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = false
                        if (validationError != null) onValidationErrorClear()
                    },
                    label = { Text("URL") },
                    isError = showError,
                    supportingText = errorText?.let { { Text(it) } },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("add_bookmark_url"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("add_bookmark_title"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isBlank()) {
                        urlError = true
                    } else {
                        onConfirm(url.trim(), title.trim().ifEmpty { url.trim() })
                    }
                },
                modifier = Modifier.testTag("add_bookmark_confirm"),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("add_bookmark_dialog"),
    )
}
