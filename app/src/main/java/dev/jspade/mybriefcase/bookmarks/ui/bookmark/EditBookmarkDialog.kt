package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.window.DialogProperties
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookmarkDialog(
    bookmark: BookmarkDto,
    navTree: FolderNavTreeDto? = null,
    currentFolderId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (url: String, title: String, notes: String, newFolderId: String?) -> Unit,
    validationError: String? = null,
    onValidationErrorClear: () -> Unit = {},
) {
    var url by remember { mutableStateOf(bookmark.url) }
    var title by remember { mutableStateOf(bookmark.title) }
    var notes by remember { mutableStateOf(bookmark.notes) }
    var urlError by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf(currentFolderId) }
    var folderPickerExpanded by remember { mutableStateOf(false) }

    val showError = urlError || validationError != null
    val errorText = validationError ?: if (urlError) "URL is required" else null

    val folderMap =
        remember(navTree) {
            navTree?.folders?.associateBy { it.id } ?: emptyMap()
        }
    val flatFolderList =
        remember(navTree) {
            if (navTree == null) {
                emptyList()
            } else {
                buildFlatFolderList(navTree.rootFolderId, navTree.folders.associateBy { it.id }, 0)
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bookmark") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            .testTag("edit_bookmark_url"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    maxLines = 3,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("edit_bookmark_title"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 3,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("edit_bookmark_notes"),
                )
                if (navTree != null && currentFolderId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = folderPickerExpanded,
                        onExpandedChange = { folderPickerExpanded = it },
                        modifier = Modifier.testTag("edit_bookmark_folder_picker"),
                    ) {
                        OutlinedTextField(
                            value = folderMap[selectedFolderId]?.title ?: "",
                            onValueChange = {},
                            label = { Text("Folder") },
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderPickerExpanded)
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = folderPickerExpanded,
                            onDismissRequest = { folderPickerExpanded = false },
                        ) {
                            flatFolderList.forEach { (folder, depth) ->
                                DropdownMenuItem(
                                    text = { Text(" ".repeat(depth * 4) + folder.title) },
                                    onClick = {
                                        selectedFolderId = folder.id
                                        folderPickerExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isBlank()) {
                        urlError = true
                    } else {
                        val newFolderId =
                            if (selectedFolderId != null && selectedFolderId != currentFolderId) {
                                selectedFolderId
                            } else {
                                null
                            }
                        onConfirm(url.trim(), title.trim(), notes.trim(), newFolderId)
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
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier =
            Modifier
                .imePadding()
                .testTag("edit_bookmark_dialog"),
    )
}

private fun buildFlatFolderList(
    folderId: String,
    folderMap: Map<String, FolderNavDto>,
    depth: Int,
): List<Pair<FolderNavDto, Int>> {
    val folder = folderMap[folderId] ?: return emptyList()
    val result = mutableListOf(folder to depth)
    for (childId in folder.childFolderIds) {
        result.addAll(buildFlatFolderList(childId, folderMap, depth + 1))
    }
    return result
}
